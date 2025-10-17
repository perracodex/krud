/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.access.domain.rbac.repository.role

import krud.access.domain.rbac.model.role.RbacRole
import krud.access.domain.rbac.model.role.RbacRoleRequest
import kotlin.uuid.Uuid

/**
 * Repository for [RbacRole] data.
 *
 * @see [RbacRoleRequest]
 */
internal interface IRbacRoleRepository {

    /**
     * Finds the [RbacRole] for the given [roleId].
     *
     * @param roleId The unique id of the [RbacRole] to find.
     * @return The [RbacRole] for the given [roleId], or null if not found.
     */
    fun findById(roleId: Uuid): RbacRole?

    /**
     * Finds the [RbacRole] for the given [actorId].
     *
     * @param actorId The id of the actor with which the role is associated.
     * @return The [RbacRole] for the given [actorId], or null if it doesn't exist.
     */
    fun findByActorId(actorId: Uuid): RbacRole?

    /**
     * Finds all existing [RbacRole] entries.
     *
     * @return List of all [RbacRole] entries.
     */
    fun findAll(): List<RbacRole>

    /**
     * Creates a new [RbacRole].
     *
     * @param roleRequest The [RbacRoleRequest] to create the [RbacRole] from.
     * @return The newly created [RbacRole].
     */
    fun create(roleRequest: RbacRoleRequest): RbacRole

    /**
     * Updates an existing [RbacRole] for the given [roleId].
     *
     * The new ones will replace the current existing roles.
     *
     * @param roleId The id of the [RbacRole] to update.
     * @param roleRequest The [RbacRoleRequest] to update the [RbacRole] from.
     * @return The updated [RbacRole], or null if the [roleId] doesn't exist.
     */
    fun update(roleId: Uuid, roleRequest: RbacRoleRequest): RbacRole?
}
