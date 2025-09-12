/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.access.domain.actor.repository

import krud.access.domain.actor.model.Actor
import krud.access.domain.actor.model.ActorCredentials
import krud.access.domain.actor.model.ActorRequest
import krud.access.domain.rbac.repository.role.IRbacRoleRepository
import krud.access.error.RbacError
import krud.database.schema.admin.actor.ActorTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.Uuid

/**
 * Implementation of [IActorRepository].
 * Responsible for managing [Actor] data.
 */
internal class ActorRepository(
    private val roleRepository: IRbacRoleRepository
) : IActorRepository {

    override suspend fun findById(actorId: Uuid): Actor? {
        return transaction {
            ActorTable.selectAll().where {
                ActorTable.id eq actorId
            }.singleOrNull()?.let { resultRow ->
                roleRepository.findByActorId(actorId = actorId)?.let { role ->
                    Actor.from(row = resultRow, role = role)
                }
            }
        }
    }

    override suspend fun findByUsername(username: String): Actor? {
        return transaction {
            ActorTable.selectAll().where {
                ActorTable.username.eq(username)
            }.singleOrNull()?.let { resultRow ->
                val actorId: Uuid = resultRow[ActorTable.id]
                roleRepository.findByActorId(actorId = actorId)?.let { role ->
                    Actor.from(row = resultRow, role = role)
                } ?: throw RbacError.ActorWithNoRoles(actorId = actorId)
            }
        }
    }

    override suspend fun findAll(): List<Actor> {
        return transaction {
            ActorTable.selectAll().map { resultRow ->
                val actorId: Uuid = resultRow[ActorTable.id]
                roleRepository.findByActorId(actorId = actorId)?.let { role ->
                    Actor.from(row = resultRow, role = role)
                } ?: throw RbacError.ActorWithNoRoles(actorId = actorId)
            }
        }
    }

    override suspend fun findCredentials(actorId: Uuid): ActorCredentials? {
        return transaction {
            ActorTable.selectAll().where {
                ActorTable.id eq actorId
            }.singleOrNull()?.let { resultRow ->
                ActorCredentials.from(row = resultRow)
            }
        }
    }

    override suspend fun findAllCredentials(): List<ActorCredentials> {
        return transaction {
            ActorTable.selectAll().map { resultRow ->
                ActorCredentials.from(row = resultRow)
            }
        }
    }

    override suspend fun create(actorRequest: ActorRequest): Uuid {
        return transaction {
            ActorTable.insert { statement ->
                statement.toStatement(request = actorRequest)
            } get ActorTable.id
        }
    }

    override suspend fun update(actorId: Uuid, actorRequest: ActorRequest): Int {
        return transaction {
            ActorTable.update(
                where = {
                    ActorTable.id eq actorId
                }
            ) { statement ->
                statement.toStatement(request = actorRequest)
            }
        }
    }

    override suspend fun setLockedState(actorId: Uuid, isLocked: Boolean) {
        transaction {
            ActorTable.update(
                where = {
                    ActorTable.id eq actorId
                }
            ) { statement ->
                statement[ActorTable.isLocked] = isLocked
            }
        }
    }

    override suspend fun actorsExist(usernames: List<String>?): Boolean {
        return transaction {
            if (usernames.isNullOrEmpty()) {
                ActorTable.select(
                    column = ActorTable.id
                ).count() > 0L
            } else {
                usernames.map { it.lowercase() }.let { targetUserNames ->
                    ActorTable.select(column = ActorTable.id)
                        .where { ActorTable.username.lowerCase() inList targetUserNames }
                        .count() > 0L
                }
            }
        }
    }

    /**
     * Populates an SQL [UpdateBuilder] with data from an [ActorRequest] instance,
     * so that it can be used to update or create a database record.
     */
    private fun UpdateBuilder<Int>.toStatement(request: ActorRequest) {
        this[ActorTable.username] = request.username.lowercase()
        this[ActorTable.password] = request.password
        this[ActorTable.roleId] = request.roleId
        this[ActorTable.isLocked] = request.isLocked
    }
}
