/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.access.domain.rbac.model.field

import kotlinx.serialization.Serializable
import krud.database.model.Meta
import krud.database.schema.admin.rbac.RbacFieldRuleTable
import krud.database.schema.admin.rbac.type.RbacAccessLevel
import krud.database.schema.admin.rbac.type.RbacScope
import org.jetbrains.exposed.v1.core.ResultRow
import kotlin.uuid.Uuid

/**
 * Represents a single RBAC field level rule.
 *
 * @property id The unique id of the field rule record.
 * @property scopeRuleId The associated parent [RbacScope] id.
 * @property fieldName The name of the field being targeted.
 * @property accessLevel The field [RbacAccessLevel].
 * @property meta The metadata of the record.
 */
@Serializable
public data class RbacFieldRule private constructor(
    val id: Uuid,
    val scopeRuleId: Uuid,
    val fieldName: String,
    val accessLevel: RbacAccessLevel,
    val meta: Meta
) {
    public companion object {
        /**
         * Maps a [ResultRow] to a [RbacFieldRule] instance.
         *
         * @param row The [ResultRow] to map.
         * @return The mapped [RbacFieldRule] instance.
         */
        public fun from(row: ResultRow): RbacFieldRule {
            return RbacFieldRule(
                id = row[RbacFieldRuleTable.id],
                scopeRuleId = row[RbacFieldRuleTable.scopeRuleId],
                fieldName = row[RbacFieldRuleTable.fieldName],
                accessLevel = row[RbacFieldRuleTable.accessLevel],
                meta = Meta.from(row = row, table = RbacFieldRuleTable)
            )
        }
    }
}
