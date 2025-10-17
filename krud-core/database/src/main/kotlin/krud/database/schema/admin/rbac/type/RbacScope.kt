/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.database.schema.admin.rbac.type

import krud.database.column.IEnumWithId

/**
 * Enum to represent the scopes that can be accessed.
 *
 * A scope can be any concept: a database table, a REST endpoint, a UI element, etc.
 * It is up to the designer to define what a scope is and act accordingly when its
 * associated RBAC rule is verified.
 *
 * In conjunction with [RbacAccessLevel], this enum defines the access level to a scope.
 *
 * @property id The id of the RBAC scope.
 *
 * @see [RbacAccessLevel]
 */
public enum class RbacScope(override val id: Int) : IEnumWithId {
    /** RBAC dashboard. Allows access to RBAC administrative functions. */
    RBAC_DASHBOARD(id = 100),

    /** System scope. Allows access to the system settings. */
    SYSTEM(id = 101),

    /** Employee records scope. Allows access to the employee records. */
    EMPLOYEE_RECORDS(id = 102),

    /** Employee contact records scope. Allows access to the employee contact records. */
    EMPLOYEE_CONTACT_RECORDS(id = 103),

    /** Employee employment records scope. Allows access to the employee employment records. */
    EMPLOYMENT_RECORDS(id = 104)
}
