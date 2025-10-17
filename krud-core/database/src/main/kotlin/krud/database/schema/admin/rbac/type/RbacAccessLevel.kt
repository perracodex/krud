/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.database.schema.admin.rbac.type

import krud.database.column.IEnumWithId

/**
 * Enum representing the access level to a scope.
 *
 * This enum can be expanded to encompass more granular permissions, like:
 * DELETE, UPDATE, CREATE, PRINT, etc.
 *
 * In conjunction with [RbacScope], it defines the access level to a scope,
 * facilitating role-based access control.
 *
 * @property id The unique identifier of the access level.
 *
 * @see [RbacScope]
 */
public enum class RbacAccessLevel(override val id: Int) : IEnumWithId {
    /** No access rights to a scope. */
    NONE(id = 0),

    /** Read-only access to a scope. */
    VIEW(id = 1),

    /** Full unrestricted access rights to a scope. */
    FULL(id = 100);

    /**
     * Determines if the current RBAC access level has sufficient privileges as compared to a required
     * access level, to determine if the current access level encompasses the required permissions.
     *
     * This could also be compared with the id access level, so the higher the id, the higher the privileges.
     * But such an approach would mean that the ids would have to be sequential, and it would make it
     * not trivial to create isolated concrete levels of access.
     *
     * @param requiredAccessLevel The [RbacAccessLevel] to be compared against the current access level.
     * @return True if the current access level has sufficient privileges for the required one, False otherwise.
     */
    public fun hasSufficientPrivileges(requiredAccessLevel: RbacAccessLevel): Boolean {
        return when (this) {
            FULL -> true // Full level encompasses all privileges.
            VIEW -> (requiredAccessLevel == VIEW)
            NONE -> false
        }
    }
}
