/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.database.schema.admin.rbac

import krud.database.column.autoGenerate
import krud.database.column.enumerationById
import krud.database.column.kotlinUuid
import krud.database.schema.admin.rbac.type.RbacAccessLevel
import krud.database.schema.base.TimestampedTable
import org.jetbrains.exposed.v1.core.*
import kotlin.uuid.Uuid

/**
 * Database table definition holding the RBAC field level rules.
 *
 * By default, all database fields should be returned as is,
 * unless added to this table and associated to a [RbacScopeRuleTable] record,
 * in which case the fields should be handled according to their access level.
 */
public object RbacFieldRuleTable : TimestampedTable(name = "rbac_field_rule") {
    /**
     * The unique id of the field rule record.
     */
    public val id: Column<Uuid> = kotlinUuid(
        name = "field_rule_id"
    ).autoGenerate()

    /**
     * The associated [RbacScopeRuleTable] id.
     */
    public val scopeRuleId: Column<Uuid> = kotlinUuid(
        name = "scope_rule_id"
    ).references(
        ref = RbacScopeRuleTable.id,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.RESTRICT,
        fkName = "fk_rbac_field_rule__scope_rule_id"
    )

    /**
     * The name of the field being targeted.
     */
    public val fieldName: Column<String> = varchar(
        name = "field_name",
        length = 64
    )

    /**
     * The [RbacAccessLevel] representing the access level for the field.
     */
    public val accessLevel: Column<RbacAccessLevel> = enumerationById(
        name = "access_level_id",
        entries = RbacAccessLevel.entries
    )

    /**
     * The table's primary key.
     */
    override val primaryKey: PrimaryKey = PrimaryKey(
        firstColumn = id,
        name = "pk_field_rule_id"
    )

    init {
        uniqueIndex(
            customIndexName = "uq_rbac_field_rule__scope_rule_id__field_name",
            columns = arrayOf(scopeRuleId, fieldName)
        )
    }
}
