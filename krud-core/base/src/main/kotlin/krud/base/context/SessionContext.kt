/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.base.context

import org.jetbrains.exposed.v1.jdbc.Database
import kotlin.uuid.Uuid

/**
 * Data class holding concrete [SessionContext] information passed around in the application
 * when an Actor is authenticated.
 *
 * This object lives only within the scope of a single ApplicationCall execution, and does
 * not persist beyond its intended single scope lifecycle.
 *
 * Passwords or sensitive information are never stored in the [SessionContext].
 * Instead, passwords are securely hashed and cached in-memory for fast and secure authentication,
 * so that the application never has access to any plain-text password, nor has to keep querying
 * the database for credentials.
 *
 * The optional [schema] can be used to specify a database schema to use for the session.
 * This is useful for multi-tenant applications where each tenant maye have its own schema.
 *
 * The optional [db] can be used to specify a database connection to use for the session.
 * This is useful in microservices where different services have different databases,
 * or in multi-tenant applications where each tenant may have its own database.
 *
 * @property actorId The unique actor identifier.
 * @property username The unique username, which can be null or blank for anonymous actor.
 * @property roleId The associated role id for the actor.
 * @property schema Optional database schema targeted by the session. For example in multi-tenant applications.
 * @property db Optional database connection associated with the session. For example in multi-tenant applications.
 */
public data class SessionContext(
    val actorId: Uuid,
    val username: String,
    val roleId: Uuid,
    val schema: String? = null,
    val db: Database? = null
) {
    public companion object {
        /**
         * Specifies the cookie name used for session tracking. This name is used to reference
         * the cookie that carries [SessionContext] information across requests.
         */
        public const val SESSION_NAME: String = "_ctx"
    }
}
