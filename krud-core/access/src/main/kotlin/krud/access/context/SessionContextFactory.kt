/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.access.context

import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.sessions.*
import krud.access.credential.CredentialService
import krud.access.domain.actor.model.Actor
import krud.access.domain.rbac.service.RbacService
import krud.access.domain.token.annotation.TokenApi
import krud.access.domain.token.service.TokenService
import krud.access.plugins.configureBasicAuthentication
import krud.access.plugins.configureJwtAuthentication
import krud.access.plugins.configureOAuthAuthentication
import krud.base.context.SessionContext
import krud.base.env.Tracer
import krud.base.settings.AppSettings
import krud.base.util.toUuid
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.uuid.Uuid

/**
 * Provides factory methods for constructing [SessionContext] instances from various authentication credential flows.
 * This object is primarily used by authentication mechanisms like JWT and OAuth to validate credentials
 * and generate [SessionContext] instances. These instances are crucial for populating the [ApplicationCall]
 * with session details and actor-specific information throughout the lifecycle of an API call.
 *
 * Using this factory ensures that all authentication methods adhere to a consistent approach in
 * constructing [SessionContext] instances, which is vital for security and traceability within the application.
 *
 * @see [configureJwtAuthentication]
 * @see [configureBasicAuthentication]
 * @see [configureOAuthAuthentication]
 */
internal object SessionContextFactory : KoinComponent {
    private val tracer: Tracer = Tracer<SessionContextFactory>()

    /**
     * Extracts the SessionContext from the token without verifying it.
     *
     * @param jwtToken The JWT token to extract the SessionContext from.
     * @return A [SessionContext] instance if the token is valid; null otherwise.
     */
    @TokenApi
    suspend fun from(jwtToken: String): SessionContext? {
        return try {
            val decodedToken: DecodedJWT = JWT.decode(jwtToken)
            val payload: String? = decodedToken.getClaim(TokenService.SESSION_JWT_CLAIM_KEY)?.asString()

            if (payload.isNullOrBlank()) {
                tracer.error("Missing JWT payload.")
                null
            } else {
                val actorId: Uuid = payload.toUuid()
                return fromActor(actorId = actorId)
            }
        } catch (e: JWTDecodeException) {
            tracer.error("Invalid JWT token. ${e.message}")
            null
        }
    }

    /**
     * Creates a [SessionContext] instance from a JWT [JWTCredential].
     *
     * @param headers The [Headers] containing the request headers.
     * @param jwtCredential The [JWTCredential] containing actor-related claims.
     * @return A [SessionContext] instance if actor details and validations pass; null otherwise.
     */
    @TokenApi
    suspend fun from(
        headers: Headers,
        jwtCredential: JWTCredential
    ): SessionContext? {
        // Check if the JWT audience claim matches the configured audience.
        // This ensures the token is intended for the application.
        if (!jwtCredential.payload.audience.contains(AppSettings.security.jwtAuth.audience)) {
            tracer.error("Invalid JWT audience: ${jwtCredential.payload.audience}")
            return null
        }

        // Check if the JWT issuer matches the configured issuer.
        // This ensures the token was issued by a trusted source.
        if (jwtCredential.payload.issuer != AppSettings.security.jwtAuth.issuer) {
            tracer.error("Invalid JWT issuer: ${jwtCredential.payload.issuer}")
            return null
        }

        // Extract the serialized SessionContext from the JWT claims.
        // This payload contains key session details serialized as a string,
        // intended for reconstructing the SessionContext.
        // If absent or blank, it indicates the JWT does not contain the required SessionContext data.
        val payload: String? = jwtCredential.payload.getClaim(TokenService.SESSION_JWT_CLAIM_KEY)?.asString()
        if (payload.isNullOrBlank()) {
            tracer.error("Missing JWT payload.")
            return null
        }

        // Validate the JWT token.
        TokenService.getState(headers = headers).let { tokenState ->
            when (tokenState) {
                is TokenService.TokenState.Valid -> {
                    tracer.info("JWT token is valid.")
                }

                is TokenService.TokenState.Expired -> {
                    tracer.error("JWT token has expired.")
                    return null
                }

                is TokenService.TokenState.Invalid -> {
                    tracer.error("Invalid JWT token: ${tokenState.reason}")
                    return null
                }
            }
        }

        // Return a fully constructed SessionContext for the reconstructed payload.
        val actorId: Uuid = payload.toUuid()
        return fromActor(actorId = actorId)
    }

    /**
     * Creates a [SessionContext] by authenticating a [UserPasswordCredential].
     * Authenticates the actor's credentials and retrieves actor details from the database.
     *
     * @param credential The [UserPasswordCredential] of the actor attempting to authenticate.
     * @return A [SessionContext] instance if actor details and validations pass; null otherwise.
     */
    suspend fun from(credential: UserPasswordCredential): SessionContext? {
        // Resolve the UserIdPrincipal. Return null if the authentication fails to provide it.
        val credentialService: CredentialService by inject()
        val userIdPrincipal: UserIdPrincipal = credentialService.authenticate(credential = credential) ?: run {
            tracer.error("Failed to resolve UserIdPrincipal from UserPasswordCredential. Invalid credentials.")
            return null
        }

        // Resolve the SessionContext given a username.
        // Return null if no actor corresponds to the provided username.
        return fromActor(username = userIdPrincipal.name)
    }

    /**
     * Creates a [SessionContext] instance from an OAuth [OAuthAccessTokenResponse.OAuth2].
     *
     * @param oauth2 The OAuth [OAuthAccessTokenResponse.OAuth2] containing actor-related claims.
     * @return A [SessionContext] instance if the auth token is valid; null otherwise.
     */
    suspend fun from(oauth2: OAuthAccessTokenResponse.OAuth2): SessionContext? {
        // Resolve the JWT.
        val jwt: DecodedJWT = try {
            JWT.decode(oauth2.extraParameters["id_token"] as String).also { decodedJwt ->
                // Validate JWT immediately after decoding.
                if (decodedJwt.subject.isNullOrBlank()) {
                    tracer.error("Invalid OAuth token: Missing subject.")
                    return null
                }
            }
        } catch (e: JWTDecodeException) {
            tracer.error("Invalid OAuth token. ${e.message}")
            return null
        }

        // Ensure the OAuth issuer matches the expected configuration.
        if (!AppSettings.security.oAuth.authorizeUrl.startsWith(jwt.issuer)) {
            tracer.error("Invalid OAuth issuer: ${jwt.issuer}")
            return null
        }

        // Verify the audience to ensure the token was intended for this application.
        if (jwt.audience.isEmpty() || jwt.audience[0] != AppSettings.security.oAuth.clientId) {
            tracer.error("Invalid OAuth audience: ${jwt.audience}")
            return null
        }

        // Resolve the Actor ID. Return null if the username claim is missing or blank.
        val actorId: Uuid = jwt.claims["actorId"]?.asString()?.toUuid() ?: run {
            tracer.error("Invalid OAuth username: Empty or missing username.")
            return null
        }

        // Resolve the SessionContext given an Actor ID.
        // Return null if no actor corresponds to the provided username.
        return fromActor(actorId = actorId)
    }

    /**
     * Creates a [SessionContext] instance from the [Sessions] defined in the [ApplicationCall].
     *
     * @param sessions The [Sessions] defined in the [ApplicationCall].
     * @return A [SessionContext] instance if found; null otherwise.
     */
    suspend fun from(sessions: CurrentSession): SessionContext? {
        // The session is configured to store the actor ID as a UUID.
        // If more information is needed, then a serializable data class could be used instead of the Actor Uuid.
        // In such a case, the Sessions plugin configuration would need to be updated too beside the next line.
        val actorId: Uuid? = sessions.get(name = SessionContext.SESSION_NAME) as Uuid?
        return actorId?.let {
            fromActor(actorId = actorId)
        }
    }

    /**
     * Attempts to build a [SessionContext] instance resolving the actor from the given [actorId].
     *
     * @param actorId The actor ID to resolve the [SessionContext] from.
     * @return A [SessionContext] instance if the actor is found; null otherwise.
     */
    private suspend fun fromActor(actorId: Uuid): SessionContext? {
        val rbacService: RbacService by inject()
        return rbacService.findActor(actorId = actorId)?.let { actor ->
            buildSessionContext(actor = actor)
        } ?: run {
            tracer.error("No actor found in the system for actor: $actorId")
            return null
        }
    }

    /**
     * Attempts to build a [SessionContext] instance resolving the actor from the given [username].
     * @param username The username of the actor to resolve the [SessionContext] from.
     * @return A [SessionContext] instance if the actor is found; null otherwise.
     */
    private suspend fun fromActor(username: String): SessionContext? {
        val rbacService: RbacService by inject()
        return rbacService.findActor(username = username)?.let { actor ->
            buildSessionContext(actor = actor)
        } ?: run {
            tracer.error("No actor found in the system for username: $username")
            return null
        }
    }

    /**
     * Constructs a [SessionContext] instance from the provided [actor].
     *
     * @param actor The [Actor] to build the [SessionContext] from.
     * @return A [SessionContext] instance representing the provided [actor].
     */
    private fun buildSessionContext(actor: Actor): SessionContext {
        return SessionContext(
            actorId = actor.id,
            username = actor.username,
            roleId = actor.role.id
        )
    }
}
