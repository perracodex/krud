/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.access.domain.token.service

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.TokenExpiredException
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.http.*
import io.ktor.http.auth.*
import krud.access.context.SessionContextFactory
import krud.access.domain.token.annotation.TokenApi
import krud.base.context.SessionContext
import krud.base.env.Tracer
import krud.base.settings.AppSettings
import krud.base.settings.catalog.section.security.node.auth.JwtAuthSettings
import java.util.*
import kotlin.time.Duration.Companion.seconds

/**
 * Security class responsible for verifying JWT tokens.
 *
 * This class contains the logic for verifying JWT tokens using `HMAC256` algorithm.
 * If the token is invalid or any exception occurs, an UnauthorizedException will be thrown.
 */
@TokenApi
internal object TokenService {
    private val tracer: Tracer = Tracer<TokenService>()

    /**
     * Identifies the key under which the session data is stored within JWT payload claims.
     */
    const val SESSION_JWT_CLAIM_KEY: String = "session_context"

    /**
     * Represents a standard response containing an HTTP status code and a message.
     *
     * @property statusCode The HTTP status code.
     * @property message The response message.
     */
    data class Response(
        val statusCode: HttpStatusCode,
        val message: String
    )

    /**
     * The token evaluation result, containing the state and the token if it is valid.
     */
    sealed class TokenState {
        /**
         * The token is valid and can be used.
         *
         * @property token The valid token.
         */
        data class Valid(val token: String) : TokenState()

        /**
         * The token has expired and is no longer valid.
         */
        data object Expired : TokenState()

        /**
         * The token is invalid, either due to a format issue or a verification problem.
         *
         * @property reason The reason why the token is invalid.
         */
        data class Invalid(val reason: String) : TokenState()
    }

    /**
     * Generates a new token based on the provided session context.
     *
     * @param sessionContext The session context of the authenticated user.
     * @return A [Response] representing the HTTP response to be sent.
     */
    fun createToken(sessionContext: SessionContext): Response {
        return try {
            val token: String = generate(sessionContext = sessionContext)
            Response(statusCode = HttpStatusCode.OK, message = token)
        } catch (e: Exception) {
            tracer.error(message = "Error creating token.", cause = e)
            Response(statusCode = HttpStatusCode.InternalServerError, message = "Failed to create token.")
        }
    }

    /**
     * Refreshes the token based on the provided headers and session context.
     *
     * @param headers The HTTP headers containing the token.
     * @return A [Response] representing the HTTP response to be sent.
     */
    suspend fun refreshToken(headers: Headers): Response {
        return try {
            when (val tokenState: TokenState = getState(headers = headers)) {
                is TokenState.Valid -> {
                    // Token is still valid; return the same token.
                    Response(statusCode = HttpStatusCode.OK, message = tokenState.token)
                }

                is TokenState.Expired -> {
                    // Token has expired; generate a new token.
                    val token: String = getTokenFromHeader(headers = headers)
                    val sessionContext: SessionContext? = SessionContextFactory.from(jwtToken = token)
                    if (sessionContext == null) {
                        tracer.error("Failed to extract session context from expired token.")
                        return Response(HttpStatusCode.Unauthorized, "Failed to refresh token.")
                    }
                    val newToken: String = generate(sessionContext = sessionContext)
                    Response(statusCode = HttpStatusCode.OK, message = newToken)
                }

                is TokenState.Invalid -> {
                    // Token is invalid; respond with Unauthorized status.
                    Response(statusCode = HttpStatusCode.Unauthorized, message = "Invalid token.")
                }
            }
        } catch (e: Exception) {
            tracer.error("Error refreshing token.", e)
            Response(HttpStatusCode.InternalServerError, "Failed to refresh token.")
        }
    }

    /**
     * Returns the current token state from the header authorization token.
     *
     * @param headers The request [Headers] to extract the token from.
     * @return The [TokenState] containing the state and the token if it is valid.
     */
    fun getState(headers: Headers): TokenState {
        return try {
            val token: String = getTokenFromHeader(headers = headers)
            val algorithm: Algorithm = Algorithm.HMAC256(AppSettings.security.jwtAuth.secretKey)
            val verifier: JWTVerifier = JWT.require(algorithm).build()
            val decodedToken: DecodedJWT = JWT.decode(token)

            // Verify the token. This step will throw an exception if invalid or continue if valid.
            verifier.verify(decodedToken)
            TokenState.Valid(token = token)
        } catch (e: TokenExpiredException) {
            tracer.info("Token expired: ${e.message}")
            TokenState.Expired
        } catch (e: Exception) {
            tracer.error(message = "Token verification failed.", cause = e)
            TokenState.Invalid(reason = "Token verification failed.")
        }
    }

    /**
     * Returns the current authorization token from the headers.
     *
     * @param headers The request [Headers] to extract the token from.
     */
    private fun getTokenFromHeader(headers: Headers): String {
        val authHeader: String? = headers.entries().find { header ->
            header.key.equals(other = HttpHeaders.Authorization, ignoreCase = true) ||
                    header.key.equals(other = HttpHeaders.AuthenticationInfo, ignoreCase = true)
        }?.value?.get(index = 0)

        require(!authHeader.isNullOrBlank() && authHeader.startsWith(prefix = AuthScheme.Bearer, ignoreCase = true)) {
            "Invalid Authorization header format. Expected format: 'Bearer <token>'."
        }

        return authHeader.substring(AuthScheme.Bearer.length).trim()
    }

    /**
     * Generate a new authorization token.
     *
     * @param sessionContext The [SessionContext] details to embed in the token.
     * @return The generated JWT token.
     */
    fun generate(sessionContext: SessionContext): String {
        val jwtAuthSettings: JwtAuthSettings = AppSettings.security.jwtAuth
        val tokenLifetimeSec: Long = jwtAuthSettings.tokenLifetimeSec
        val expirationDate = Date(System.currentTimeMillis() + tokenLifetimeSec.seconds.inWholeMilliseconds)
        val claim: String = sessionContext.actorId.toString()

        tracer.debug("Generating new authorization token. Expiration: $expirationDate.")

        return JWT.create()
            .withClaim(SESSION_JWT_CLAIM_KEY, claim)
            .withAudience(jwtAuthSettings.audience)
            .withIssuer(jwtAuthSettings.issuer)
            .withExpiresAt(expirationDate)
            .sign(Algorithm.HMAC256(jwtAuthSettings.secretKey))
    }
}
