/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.access.domain.rbac.api.dashboard

import io.github.perracodex.kopapi.dsl.operation.api
import io.ktor.http.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import krud.access.domain.rbac.annotation.RbacApi
import krud.access.domain.rbac.service.RbacDashboardManager
import krud.access.domain.rbac.view.RbacDashboardView
import krud.access.domain.rbac.view.RbacLoginView
import krud.base.context.SessionContext
import krud.base.context.clearSessionContext
import krud.base.context.sessionContext
import krud.base.util.toUuid
import kotlin.uuid.Uuid

/**
 * Processes updates to RBAC settings based on actor submissions from the dashboard form.
 * Validates the [SessionContext] and authorizes modifications, redirecting to the login screen if unauthorized.
 */
@RbacApi
internal fun Route.rbacDashboardUpdateRoute() {
    post("/rbac/dashboard") {
        // Retrieve SessionContext or redirect to the login screen if it's missing.
        val sessionContext: SessionContext = call.sessionContext
        if (!RbacDashboardManager.hasPermission(sessionContext = sessionContext)) {
            call.clearSessionContext()
            call.respondRedirect(url = RbacLoginView.RBAC_LOGIN_PATH)
            return@post
        }

        // Receive and process form parameters.
        val parameters: Parameters = call.receiveParameters()
        val currentRoleId: Uuid = parameters.getOrFail(name = RbacDashboardView.ROLE_KEY).toUuid()

        // Fetch the role-specific scope rules for the current role
        // and update the rules based on the submitted parameters.
        RbacDashboardManager.processUpdate(
            sessionContext = sessionContext,
            roleId = currentRoleId,
            updates = parameters.entries().associate { it.key to it.value.first() }
        ).let { result ->
            when (result) {
                // If the update was successful, render the updated RBAC dashboard.
                is RbacDashboardManager.UpdateResult.Success -> call.respondHtml(HttpStatusCode.OK) {
                    RbacDashboardView.build(
                        html = this,
                        isUpdated = true,
                        dashboardContext = result.dashboardContext
                    )
                }

                // If the update was unauthorized, clear the session and redirect to the login screen.
                is RbacDashboardManager.UpdateResult.Unauthorized -> call.run {
                    call.clearSessionContext()
                    respondRedirect(url = RbacLoginView.RBAC_LOGIN_PATH)
                }
            }
        }
    } api {
        tags = setOf("RBAC")
        summary = "Update RBAC settings."
        description = "Update RBAC settings based on actor submissions from the dashboard form."
        operationId = "rbacDashboardUpdate"
        response<String>(status = HttpStatusCode.OK) {
            description = "The updated RBAC dashboard."
        }
        response<String>(status = HttpStatusCode.Found) {
            description = "Redirect to the RBAC login page."
        }
    }
}
