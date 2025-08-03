/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.database.schema.admin.rbac

import krud.database.column.autoGenerate
import krud.database.column.enumerationById
import krud.database.column.kotlinUuid
import krud.database.schema.admin.rbac.type.RbacAccessLevel
import krud.database.schema.admin.rbac.type.RbacScope
import krud.database.schema.base.TimestampedTable
import org.jetbrains.exposed.v1.core.*
import kotlin.uuid.Uuid

/**
 * Database table definition holding RBAC rules for a concrete [RbacRoleTable] record.
 *
 * A scope can be any concept: a database table, a REST endpoint, a UI element, etc.
 * Is up to the designer to define what a scope is, and act accordingly when its
 * associated RBAC rule is verified.
 *
 * @see [RbacRoleTable]
 */
public object RbacScopeRuleTable : TimestampedTable(name = "rbac_scope_rule") {
    /**
     * The unique id of the scope rule record.
     */
    public val id: Column<Uuid> = kotlinUuid(
        name = "scope_rule_id"
    ).autoGenerate()

    /**
     * The associated [RbacRoleTable] id.
     */
    public val roleId: Column<Uuid> = kotlinUuid(
        name = "role_id"
    ).references(
        ref = RbacRoleTable.id,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.RESTRICT,
        fkName = "fk_rbac_scope_rule__role_id"
    )

    /**
     * The [RbacScope] the rule is meant to target.
     */
    public val scope: Column<RbacScope> = enumerationById(
        name = "scope_id",
        entries = RbacScope.entries
    )

    /**
     * The [RbacAccessLevel] representing the access level for the [RbacScope].
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
        name = "pk_scope_rule_id"
    )

    init {
        uniqueIndex(
            customIndexName = "uq_rbac_scope_rule__role_id__scope",
            columns = arrayOf(roleId, scope)
        )
    }
}
