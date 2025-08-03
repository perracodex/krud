/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.access.domain.rbac.repository.role

import krud.access.domain.rbac.model.role.RbacRole
import krud.access.domain.rbac.model.role.RbacRoleRequest
import krud.access.domain.rbac.repository.scope.IRbacScopeRuleRepository
import krud.database.schema.admin.actor.ActorTable
import krud.database.schema.admin.rbac.RbacFieldRuleTable
import krud.database.schema.admin.rbac.RbacRoleTable
import krud.database.schema.admin.rbac.RbacScopeRuleTable
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.Uuid

/**
 * Implementation of [IRbacRoleRepository].
 * Handles the persistence of [RbacRole] data.
 *
 * @see [IRbacRoleRepository]
 */
internal class RbacRoleRepository(
    private val scopeRuleRepository: IRbacScopeRuleRepository
) : IRbacRoleRepository {

    override fun findById(roleId: Uuid): RbacRole? {
        return transaction {
            RbacRoleTable
                .leftJoin(otherTable = RbacScopeRuleTable)
                .leftJoin(otherTable = RbacFieldRuleTable)
                .selectAll()
                .where { RbacRoleTable.id eq roleId }
                .groupBy { it[RbacRoleTable.id] }
                .map { (_, rows) ->
                    RbacRole.from(roleId = roleId, rows = rows)
                }.singleOrNull()
        }
    }

    override fun findByActorId(actorId: Uuid): RbacRole? {
        return transaction {
            // Filter out the Actor table columns. Only include the RBAC columns.
            val columns: List<Column<*>> = listOf(
                RbacRoleTable.columns,
                RbacScopeRuleTable.columns,
                RbacFieldRuleTable.columns
            ).flatten()

            RbacRoleTable
                .innerJoin(otherTable = ActorTable)
                .leftJoin(otherTable = RbacScopeRuleTable)
                .leftJoin(otherTable = RbacFieldRuleTable)
                .select(columns = columns)
                .where { ActorTable.id eq actorId }
                .groupBy { it[RbacRoleTable.id] }
                .map { (roleId, rows) ->
                    RbacRole.from(roleId = roleId, rows = rows)
                }.singleOrNull()
        }
    }

    override fun findAll(): List<RbacRole> {
        return transaction {
            RbacRoleTable
                .leftJoin(otherTable = RbacScopeRuleTable)
                .leftJoin(otherTable = RbacFieldRuleTable)
                .selectAll()
                .groupBy { it[RbacRoleTable.id] }
                .map { (roleId, rows) ->
                    RbacRole.from(roleId = roleId, rows = rows)
                }
        }
    }

    override fun create(roleRequest: RbacRoleRequest): RbacRole {
        return transaction {
            val roleId: Uuid = RbacRoleTable.insert { statement ->
                statement.toStatement(roleRequest = roleRequest)
            } get RbacRoleTable.id

            // If the role insert was successful, insert the scope rules.
            if (!roleRequest.scopeRules.isNullOrEmpty()) {
                scopeRuleRepository.replace(
                    roleId = roleId,
                    scopeRuleRequests = roleRequest.scopeRules
                )
            }

            val rbacRole: RbacRole? = findById(roleId = roleId)
            checkNotNull(rbacRole) { "New record not found." }
            rbacRole
        }
    }

    override fun update(roleId: Uuid, roleRequest: RbacRoleRequest): RbacRole? {
        return transaction {
            RbacRoleTable.update(
                where = {
                    RbacRoleTable.id eq roleId
                }
            ) { statement ->
                statement.toStatement(roleRequest = roleRequest)
            }.takeIf { it > 0 }?.let {
                scopeRuleRepository.replace(
                    roleId = roleId,
                    scopeRuleRequests = roleRequest.scopeRules
                )

                findById(roleId = roleId)
            }
        }
    }

    /**
     * Populates an SQL [UpdateBuilder] with data from an [RbacRoleRequest] instance,
     * so that it can be used to update or create a database record.
     */
    private fun UpdateBuilder<Int>.toStatement(roleRequest: RbacRoleRequest) {
        this[RbacRoleTable.role_name] = roleRequest.roleName.trim()
        this[RbacRoleTable.description] = roleRequest.description?.trim()
        this[RbacRoleTable.isSuper] = roleRequest.isSuper
    }
}
