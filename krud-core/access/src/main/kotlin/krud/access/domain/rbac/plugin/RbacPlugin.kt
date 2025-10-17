/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.access.domain.rbac.plugin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import krud.access.context.SessionContextFactory
import krud.access.domain.rbac.annotation.RbacApi
import krud.access.domain.rbac.service.RbacService
import krud.base.context.SessionContext
import krud.base.context.sessionContext
import krud.base.context.sessionContextOrNull
import krud.database.schema.admin.rbac.type.RbacAccessLevel
import krud.database.schema.admin.rbac.type.RbacScope
import org.koin.ktor.ext.inject

/**
 * Custom Ktor RBAC plugin intercepting calls to routes and
 * applying RBAC checks based on the configured scope and access level.
 *
 * It ensures that only authorized Actors, as per the RBAC settings,
 * can access specific routes.
 */
@RbacApi
internal val RbacPlugin: RouteScopedPlugin<RbacPluginConfig> = createRouteScopedPlugin(
    name = "RbacPlugin",
    createConfiguration = ::RbacPluginConfig
) {
    on(hook = AuthenticationChecked) { call ->
        // This hook is triggered after authentication checks. For JWT and other token-based authentications,
        // the SessionContext is typically derived directly from the token, populating the call pipeline automatically.
        // In contrast, form-based authentication does not inherently carry the SessionContext across requests,
        // so it must be manually set in the call pipeline from the persistence provided by the Sessions plugin.
        val sessionContext: SessionContext? = call.sessionContextOrNull
            ?: SessionContextFactory.from(sessions = call.sessions)?.let { sessionContext ->
                call.sessionContext = sessionContext
                sessionContext
            }

        sessionContext?.let {
            val rbacService: RbacService by call.application.inject()
            val rbacScope: RbacScope = pluginConfig.scope
            val rbacAccessLevel: RbacAccessLevel = pluginConfig.accessLevel

            val hasPermission: Boolean = rbacService.hasPermission(
                sessionContext = sessionContext,
                scope = rbacScope,
                accessLevel = rbacAccessLevel
            )

            if (hasPermission) {
                // The call is authorized to proceed.
                return@on
            }
        }

        call.respond(status = HttpStatusCode.Forbidden, message = "Access denied.")
    }
}

/**
 * Configuration for the RBAC plugin.
 * Holds the RBAC target scope and the required access level.
 */
@RbacApi
internal class RbacPluginConfig {
    /** The RBAC scope associated with the route, defining the scope of access control. */
    lateinit var scope: RbacScope

    /** The RBAC access level required for accessing the route, defining the degree of access control. */
    lateinit var accessLevel: RbacAccessLevel
}
