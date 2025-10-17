/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.access.domain.actor.model

import kotlin.uuid.Uuid

/**
 * Request to create/update an Actor.
 * An Actor is a user with a specific role and designated access to a set of concrete scopes.
 *
 * #### Locked State
 * The [isLocked] flag represents whether an Actor is currently locked out of the system, suspending all
 * of its privileges and access, regardless of its assigned role.
 *
 * #### Caution
 * Manipulating the [isLocked] flag through public-facing APIs can lead to potential security risks.
 * It is advisable to restrict the ability to unlock an actor to internal mechanisms only, ensuring that any
 * changes to this status through external endpoints are rigorously validated and securely handled.
 *
 * @property roleId The target Actor's role id.
 * @property username The Actor's unique username.
 * @property password The Actor's password in plaintext. Must ensure secure handling and storage.
 * @property isLocked Whether the Actor is locked, so its role and scope rules are ignored, loosing all accesses.
 */
internal data class ActorRequest(
    val roleId: Uuid,
    val username: String,
    val password: String,
    val isLocked: Boolean
)
