/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.access.domain.actor.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import krud.access.credential.CredentialService
import krud.access.domain.actor.model.Actor
import krud.access.domain.actor.model.ActorCredentials
import krud.access.domain.actor.model.ActorRequest
import krud.access.domain.actor.repository.IActorRepository
import krud.access.domain.rbac.repository.role.IRbacRoleRepository
import krud.access.domain.rbac.service.RbacService
import krud.base.env.Tracer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.uuid.Uuid

/**
 * Service to handle Actor operations.
 *
 * @property actorRepository The [IActorRepository] to handle Actor persistence operations.
 * @property roleRepository The [IRbacRoleRepository] to handle RBAC role operations.
 */
internal class ActorService(
    private val actorRepository: IActorRepository,
    @Suppress("unused") private val roleRepository: IRbacRoleRepository
) : KoinComponent {
    private val tracer: Tracer = Tracer<ActorService>()

    /**
     * Finds the [Actor] for the given id.
     *
     * @param actorId The id of the [Actor] to find.
     * @return The [Actor] for the given id, or null if it doesn't exist.
     */
    suspend fun findById(actorId: Uuid): Actor? = withContext(Dispatchers.IO) {
        return@withContext actorRepository.findById(actorId = actorId)
    }

    /**
     * Finds the [Actor] for the given username.
     * @param username The username of the [Actor] to find.
     * @return The [Actor] for the given username, or null if it doesn't exist.
     */
    suspend fun findByUsername(username: String): Actor? = withContext(Dispatchers.IO) {
        return@withContext actorRepository.findByUsername(username = username)
    }

    /**
     * Finds all existing [Actor] entries.
     * @return A list with all existing [Actor] entries.
     */
    suspend fun findAll(): List<Actor> = withContext(Dispatchers.IO) {
        return@withContext actorRepository.findAll()
    }

    /**
     * Finds the credentials for the given [actorId].
     *
     * @param actorId The id of the actor to find the credentials for.
     * @return The resolved [ActorCredentials], or null if the actor doesn't exist.
     */
    suspend fun findCredentials(actorId: Uuid): ActorCredentials? = withContext(Dispatchers.IO) {
        return@withContext actorRepository.findCredentials(actorId = actorId)
    }

    /**
     * Finds all existing [ActorCredentials] for all actors.
     * @return A list with all existing [ActorCredentials] entries.
     */
    suspend fun findAllCredentials(): List<ActorCredentials> = withContext(Dispatchers.IO) {
        return@withContext actorRepository.findAllCredentials()
    }

    /**
     * Creates a new [Actor].
     * @param actorRequest The [ActorRequest] to create.
     * @return The id of the [Actor] created.
     */
    suspend fun create(actorRequest: ActorRequest): Uuid = withContext(Dispatchers.IO) {
        tracer.debug("Creating actor with username: ${actorRequest.username}")
        val actorId: Uuid = actorRepository.create(actorRequest = actorRequest)
        refresh(actorId = actorId)
        return@withContext actorId
    }

    /**
     * Updates an existing [Actor].
     *
     * @param actorId The id of the Actor to update.
     * @param actorRequest The new details for the [Actor].
     * @return The number of records that were updated.
     */
    suspend fun update(actorId: Uuid, actorRequest: ActorRequest): Int = withContext(Dispatchers.IO) {
        tracer.debug("Updating actor with ID: $actorId")
        val count: Int = actorRepository.update(actorId = actorId, actorRequest = actorRequest)
        refresh(actorId = actorId)
        return@withContext count
    }

    /**
     * Sets the lock status for the given [Actor].
     *
     * @param actorId The id of the [Actor] to lock/unlock.
     * @param isLocked Whether the [Actor] should be locked or unlocked.
     */
    suspend fun setLockedState(actorId: Uuid, isLocked: Boolean): Unit = withContext(Dispatchers.IO) {
        tracer.debug("Setting lock state for actor with ID: $actorId")
        actorRepository.setLockedState(actorId = actorId, isLocked = isLocked)
        refresh(actorId = actorId)
    }

    /**
     * Checks if there are any Actors in the database or if the given usernames exist.
     *
     * @param usernames The actors usernames to check for. If null, checks for any Actors.
     * @return True if there are actors for the given usernames in the database, false otherwise.
     */
    suspend fun actorsExist(usernames: List<String>? = null): Boolean = withContext(Dispatchers.IO) {
        return@withContext actorRepository.actorsExist(usernames = usernames)
    }

    /**
     * Refreshes the [CredentialService] and [RbacService] to reflect an Actor creation or update.
     *
     * @param actorId The id of the Actor to refresh.
     */
    private suspend fun refresh(actorId: Uuid) {
        tracer.info("Triggering refresh in Credentials and RBAC services for actor with ID: $actorId")

        val credentialService: CredentialService by inject()
        credentialService.refreshActor(actorId = actorId)

        val rbacService: RbacService by inject()
        rbacService.refreshActor(actorId = actorId)
    }
}
