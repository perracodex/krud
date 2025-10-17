/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.base.context

import io.ktor.server.application.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import krud.base.error.UnauthorizedException
import krud.base.settings.AppSettings
import krud.base.util.toUuid
import kotlin.uuid.Uuid

/**
 * Extension property for getting or setting the current [SessionContext] in an [ApplicationCall],
 * in addition to setting it also in the [Sessions] property to persist between different HTTP requests.
 *
 * The [SessionContext] is typically set by authentication plugins following successful authorizations.
 *
 * Prefer this method over `call.principal<SessionContext>()` to ensure consistent handling
 * of security and session validation.
 *
 * - **Getter**: Returns the stored [SessionContext]. If none is set and security is enabled, it throws
 *   [UnauthorizedException]. If security its disabled, a default "empty" [SessionContext] is returned.
 * - **Setter**: Writes the given [SessionContext] into the [ApplicationCall] attributes and the [Sessions] store,
 *   allowing it to persist across multiple HTTP requests.
 *
 * @throws UnauthorizedException If security is enabled and no [SessionContext] is present on get.
 * @see [sessionContextOrNull]
 * @see [clearSessionContext]
 */
public var ApplicationCall.sessionContext: SessionContext
    get() = sessionContextOrNull
        ?: throw UnauthorizedException("Session context not found.")
    set(value) {
        this.attributes.put(key = SessionContextUtils.sessionContextKey, value = value)
        this.sessions.set(name = SessionContext.SESSION_NAME, value = value.actorId)
    }

/**
 * Extension property for *optionally* retrieving the current [SessionContext] from the [ApplicationCall].
 *
 * - Returns `null` if no session context is found **and security is enabled**.
 * - Returns a default "empty" [SessionContext] if security is disabled and none is found.
 * - If a valid [SessionContext] is stored, returns it directly.
 *
 * The [SessionContext] is typically set by authentication plugins following successful authorizations.
 *
 * This is useful when we must handle missing sessions gracefully (e.g., optional authentication scenarios).
 *
 * @see [sessionContext]
 * @see [clearSessionContext]
 */
public val ApplicationCall.sessionContextOrNull: SessionContext?
    get() = this.attributes.getOrNull(key = SessionContextUtils.sessionContextKey)
        ?: SessionContextUtils.emptySessionContext.takeIf { !AppSettings.security.isEnabled }

/**
 * Extension function to clear the [SessionContext] from the [ApplicationCall] attributes and [Sessions].
 *
 * @see [sessionContext]
 * @see [sessionContextOrNull]
 */
public fun ApplicationCall.clearSessionContext() {
    this.attributes.remove(key = SessionContextUtils.sessionContextKey)
    this.sessions.clear(name = SessionContext.SESSION_NAME)
}

/**
 * Utility object for handling [SessionContext] operations.
 */
private object SessionContextUtils {
    /**
     * Attribute key for storing the [SessionContext] into the [ApplicationCall] attributes.
     */
    val sessionContextKey: AttributeKey<SessionContext> = AttributeKey(name = "SESSION_CONTEXT")

    /**
     * A default empty [SessionContext] instance, when security is disabled.
     */
    val emptySessionContext: SessionContext by lazy {
        val uuid: Uuid = "00000000-0000-0000-0000-000000000000".toUuid()
        SessionContext(
            actorId = uuid,
            username = "no-actor",
            roleId = uuid
        )
    }
}
