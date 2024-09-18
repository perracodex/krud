/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kcrud.access.rbac.plugin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kcrud.access.rbac.plugin.annotation.RbacAPI
import kcrud.access.rbac.service.RbacService
import kcrud.base.database.schema.admin.rbac.types.RbacAccessLevel
import kcrud.base.database.schema.admin.rbac.types.RbacScope
import kcrud.base.env.CallContext
import org.koin.ktor.ext.inject

/**
 * Custom Ktor RBAC plugin intercepting calls to routes, and
 * applying RBAC checks based on the configured scope and access level.
 *
 * It ensures that only authorized Actors, as per the RBAC settings,
 * can access specific routes.
 */
@RbacAPI
internal val RbacPlugin: RouteScopedPlugin<RbacPluginConfig> = createRouteScopedPlugin(
    name = "RbacPlugin",
    createConfiguration = ::RbacPluginConfig
) {
    on(hook = AuthenticationChecked) { call ->
        val callContext: CallContext? = call.principal<CallContext>()
            ?: call.sessions.get(name = CallContext.SESSION_NAME) as CallContext?

        callContext?.let {
            val rbacService: RbacService by call.application.inject()
            val rbacScope: RbacScope = pluginConfig.scope
            val rbacAccessLevel: RbacAccessLevel = pluginConfig.accessLevel

            val hasPermission: Boolean = rbacService.hasPermission(
                callContext = callContext,
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
@RbacAPI
internal class RbacPluginConfig {
    /** The RBAC scope associated with the route, defining the scope of access control. */
    lateinit var scope: RbacScope

    /** The RBAC access level required for accessing the route, defining the degree of access control. */
    lateinit var accessLevel: RbacAccessLevel
}
