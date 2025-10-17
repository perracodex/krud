/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.access.domain.actor.service

import kotlinx.coroutines.*
import krud.access.credential.CredentialService
import krud.access.domain.actor.model.ActorRequest
import krud.access.domain.rbac.model.role.RbacRole
import krud.access.domain.rbac.model.role.RbacRoleRequest
import krud.access.domain.rbac.model.scope.RbacScopeRuleRequest
import krud.access.domain.rbac.service.RbacService
import krud.base.env.Tracer
import krud.database.schema.admin.rbac.type.RbacAccessLevel
import krud.database.schema.admin.rbac.type.RbacScope
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Singleton service responsible for synchronizing Actors and refreshing related services.
 *
 * It ensures that the database has actors, creating default ones if none are found.
 * Additionally, it refreshes the Credential and RBAC services to maintain up-to-date
 * caches and configurations.
 */
public object ActorSyncService : KoinComponent {
    private val tracer: Tracer = Tracer<ActorSyncService>()

    /**
     * The default role names.
     * Ideally, these should be defined in a database table, instead of an enum.
     * These are used to create default Actors and their respective roles,
     * when none are found in the database.
     */
    internal enum class RoleName {
        /** ADMIN actor role. Usually with full access. */
        ADMIN,

        /** GUEST actor role. Usually with very limited access. */
        GUEST
    }

    /**
     * Refreshes the Credential and RBAC services to ensure caches are up to date,
     * and provisions default Actors and roles if none exist.
     */
    public suspend fun refresh(): Unit = withContext(Dispatchers.IO) {
        tracer.info("Refreshing actors.")

        // Ensure the database has any Actors, if none exist, then create the default ones.
        createIfMissing()

        val credentialJob: Job = launch {
            val credentialService: CredentialService by inject()
            credentialService.refreshActors()
        }

        val rbacJob: Job = launch {
            val rbacService: RbacService by inject()
            rbacService.refreshActors()
        }

        joinAll(credentialJob, rbacJob)
    }

    /**
     * Creates default Actors with their respective roles if none are found in the database.
     */
    private suspend fun createIfMissing() {
        val actorService: ActorService by inject()
        if (actorService.actorsExist()) {
            return
        }

        tracer.info("No actors found. Creating default ones.")
        val rbacRoles: List<RbacRole> = getRoles()

        rbacRoles.forEach { role ->
            val credential: String = role.roleName.lowercase()

            actorService.create(
                actorRequest = ActorRequest(
                    roleId = role.id,
                    username = credential,
                    password = credential,
                    isLocked = false
                )
            )
        }
    }

    /**
     * Gets the default roles, creating them if none are found in the database.
     */
    private suspend fun getRoles(): List<RbacRole> {
        val rbacService: RbacService by inject()
        val rbacRoles: List<RbacRole> = rbacService.findAllRoles()
        if (rbacRoles.isNotEmpty()) {
            return rbacRoles
        }

        // Create default roles if none are found.
        RoleName.entries.forEach { role ->
            val adminScopeRules: List<RbacScopeRuleRequest> = if (role == RoleName.ADMIN) {
                RbacScope.entries.map { scope ->
                    RbacScopeRuleRequest(
                        scope = scope,
                        accessLevel = RbacAccessLevel.FULL,
                        fieldRules = null
                    )
                }
            } else {
                emptyList()
            }

            val roleRequest = RbacRoleRequest(
                roleName = role.name,
                description = null,
                isSuper = (role == RoleName.ADMIN),
                scopeRules = adminScopeRules
            )

            rbacService.createRole(roleRequest = roleRequest)
        }

        return rbacService.findAllRoles()
    }
}
