/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.access.domain.rbac.service

import kotlinx.serialization.json.Json
import krud.access.domain.rbac.annotation.RbacApi
import krud.access.domain.rbac.model.role.RbacRole
import krud.access.domain.rbac.model.scope.RbacScopeRuleRequest
import krud.access.domain.rbac.view.RbacDashboardView
import krud.base.context.SessionContext
import krud.database.schema.admin.rbac.type.RbacAccessLevel
import krud.database.schema.admin.rbac.type.RbacScope
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.uuid.Uuid

/**
 * Manages access control, role determination, and updates for the RBAC dashboard,
 * including permission checks, role-specific access level determination,
 * and processing of administrative updates.
 */
@RbacApi
internal object RbacDashboardManager : KoinComponent {

    /**
     * Checks if the [SessionContext] actor has permission to access the RBAC dashboard.
     */
    suspend fun hasPermission(sessionContext: SessionContext): Boolean {
        val rbacService: RbacService by inject()
        return rbacService.hasPermission(
            sessionContext = sessionContext,
            scope = RbacScope.RBAC_DASHBOARD,
            accessLevel = RbacAccessLevel.VIEW
        )
    }

    /**
     * Determines the current actor's role and access level within the
     * RBAC dashboard scope based on the provided [sessionContext] and [roleId].
     *
     * @param sessionContext The current actor's [SessionContext].
     * @param roleId The role ID if specified, otherwise null.
     * @return [Context] containing the access details for the RBAC dashboard.
     */
    suspend fun determineAccessDetails(sessionContext: SessionContext, roleId: Uuid?): Context {
        val rbacService: RbacService by inject()

        // Fetch the current actor's permission level for the RBAC dashboard scope.
        val sessionRolePermissionLevel: RbacAccessLevel = rbacService.getPermissionLevel(
            sessionContext = sessionContext,
            scope = RbacScope.RBAC_DASHBOARD
        )

        // Retrieve all RBAC roles.
        val rbacRoles: List<RbacRole> = rbacService.findAllRoles()
        val sessionRoleName: String = rbacService.findRoleById(roleId = sessionContext.roleId)?.roleName.orEmpty()

        // Select the current role based on the role ID or default to the first role if none specified.
        val targetRole: RbacRole = roleId?.let { id ->
            rbacRoles.find { it.id == id }
        } ?: rbacRoles.first()
        val isViewOnly: Boolean = (sessionRolePermissionLevel == RbacAccessLevel.VIEW) or (targetRole.isSuper)

        return Context(
            isViewOnly = isViewOnly,
            rbacRoles = rbacRoles,
            targetRole = targetRole,
            sessionRoleName = sessionRoleName
        )
    }

    /**
     * Process updates to the RBAC dashboard.
     *
     * @param sessionContext The current actor's [SessionContext].
     * @param roleId The role ID to update.
     * @param updates The updates to apply to the role.
     * @return The result of the update operation.
     */
    suspend fun processUpdate(sessionContext: SessionContext, roleId: Uuid, updates: Map<String, String>): UpdateResult {
        // Update role-specific scope rules based on submitted parameters.
        applyRoleUpdates(roleId = roleId, updates = updates)

        // Check if the actor has permission to access the RBAC dashboard.
        val rbacService: RbacService by inject()
        val sessionRolePermissionLevel: RbacAccessLevel = rbacService.getPermissionLevel(
            sessionContext = sessionContext,
            scope = RbacScope.RBAC_DASHBOARD
        )
        if (sessionRolePermissionLevel == RbacAccessLevel.NONE) {
            return UpdateResult.Unauthorized
        }

        // Fetch updated roles for immediate feedback on the dashboard.
        val rbacRoles: List<RbacRole> = rbacService.findAllRoles()
        val targetRole: RbacRole = rbacRoles.first { it.id == roleId }
        val isViewOnly: Boolean = (sessionRolePermissionLevel == RbacAccessLevel.VIEW) or (targetRole.isSuper)
        val sessionRoleName: String = rbacService.findRoleById(roleId = sessionContext.roleId)?.roleName.orEmpty()

        // Return the updated roles and current role ID.
        return UpdateResult.Success(
            dashboardContext = Context(
                isViewOnly = isViewOnly,
                rbacRoles = rbacRoles,
                targetRole = targetRole,
                sessionRoleName = sessionRoleName
            )
        )
    }

    /**
     * Process updates to the RBAC settings for a specific role.
     *
     * @param roleId The role ID to update.
     * @param updates The updates to apply to the role.
     */
    private suspend fun applyRoleUpdates(roleId: Uuid, updates: Map<String, String>) {
        // Collect role-specific access rules from parameters.
        val scopeRulesRequests: MutableList<RbacScopeRuleRequest> = mutableListOf()

        // Decode and prepare new access rules from received parameters.
        updates.filter {
            it.key.startsWith(prefix = RbacDashboardView.ROLE_ITEM_KEY)
        }.map {
            it to Json.decodeFromString<RbacDashboardView.AccessLevelKeyData>(
                it.key.removePrefix(prefix = RbacDashboardView.ROLE_ITEM_KEY)
            )
        }.filter { (_, accessKey) ->
            !accessKey.isLocked
        }.forEach { (paramEntry, accessKey) ->
            val accessLevel: RbacAccessLevel = RbacAccessLevel.valueOf(paramEntry.value)

            // Add valid scope rule requests for update.
            if (accessLevel != RbacAccessLevel.NONE) {
                val scopeRuleRequest = RbacScopeRuleRequest(
                    scope = RbacScope.valueOf(accessKey.scope),
                    accessLevel = accessLevel
                )
                scopeRulesRequests.add(scopeRuleRequest)
            }
        }

        // Update role with new access rules.
        val rbacService: RbacService by inject()
        rbacService.updateScopeRules(
            roleId = roleId,
            scopeRuleRequests = scopeRulesRequests.toList()
        )
    }

    /**
     * Sealed class to represent the possible outcomes of an RBAC dashboard update.
     */
    sealed class UpdateResult {
        /**
         * Represents a successful update operation.
         *
         * @property dashboardContext The updated list of RBAC roles.
         */
        data class Success(val dashboardContext: Context) : UpdateResult()

        /**
         * Represents a failed update operation due to unauthorized access.
         */
        data object Unauthorized : UpdateResult()
    }

    /**
     * Data class to hold the details necessary for rendering the RBAC dashboard.
     *
     * @param isViewOnly Whether the actor has view-only access to the dashboard or can make changes.
     * @param rbacRoles The full list of RBAC roles.
     * @param targetRole The role being updated.
     * @param sessionRoleName The name of the current session role. Not necessarily the role being updated.
     */
    data class Context(
        val isViewOnly: Boolean,
        val rbacRoles: List<RbacRole>,
        val targetRole: RbacRole,
        val sessionRoleName: String
    )
}
