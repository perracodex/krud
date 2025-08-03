/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.access.domain.rbac.model.role

import kotlinx.serialization.Serializable
import krud.access.domain.rbac.model.field.RbacFieldRule
import krud.access.domain.rbac.model.scope.RbacScopeRule
import krud.database.model.Meta
import krud.database.schema.admin.rbac.RbacFieldRuleTable
import krud.database.schema.admin.rbac.RbacRoleTable
import krud.database.schema.admin.rbac.RbacScopeRuleTable
import org.jetbrains.exposed.v1.core.ResultRow
import kotlin.uuid.Uuid

/**
 * Represents a single RBAC Role.
 *
 * @property id The unique id of the role record.
 * @property roleName The unique role name.
 * @property description Optional role description.
 * @property isSuper Whether this is a super-role, in which case it should have all permissions granted.
 * @property scopeRules The list of [RbacScopeRule] entries for the role.
 * @property meta The metadata of the record.
 */
@Serializable
public data class RbacRole private constructor(
    val id: Uuid,
    val roleName: String,
    val description: String?,
    val isSuper: Boolean,
    val scopeRules: List<RbacScopeRule>,
    val meta: Meta
) {
    internal companion object {
        /**
         * Maps a list of [ResultRow]s to a [RbacRole] instance.
         * Each row is expected to represent a different scope rule.
         *
         * @param roleId The id of the role.
         * @param rows The [ResultRow] list to map.
         * @return The mapped [RbacRole] instance.
         */
        fun from(roleId: Uuid, rows: List<ResultRow>): RbacRole {
            // Construct the list of RbacScopeRule child entries, (if any).
            // - Filter out rows without a valid ScopeRule ID.
            // - Group the remaining rows by ScopeRule ID.
            // - For each group:
            //   - Use the first row as the representative ScopeRule.
            //   - Collect associated FieldRules from rows with a valid FieldRule ID.
            //   - Create an RbacScopeRule with its FieldRules.
            val scopeRules: List<RbacScopeRule> = rows
                .filter { it.getOrNull(RbacScopeRuleTable.id) != null }
                .groupBy { it[RbacScopeRuleTable.id] }
                .map { (_, scopeRows) ->
                    val scopeRuleRow: ResultRow = scopeRows.first()

                    // Collect associated FieldRules.
                    val fieldRules: List<RbacFieldRule>? = scopeRows
                        .filter { it.getOrNull(RbacFieldRuleTable.id) != null }
                        .distinctBy { it[RbacFieldRuleTable.id] }
                        .map { RbacFieldRule.from(row = it) }
                        .ifEmpty { null }

                    RbacScopeRule.from(row = scopeRuleRow, fieldRules = fieldRules)
                }

            // Use the first row for the main role information.
            // Data comes from a flattened 1-N relationship.
            val record: ResultRow = rows.first()

            return RbacRole(
                id = roleId,
                roleName = record[RbacRoleTable.role_name],
                description = record[RbacRoleTable.description],
                isSuper = record[RbacRoleTable.isSuper],
                scopeRules = scopeRules,
                meta = Meta.from(row = record, table = RbacRoleTable)
            )
        }
    }
}
