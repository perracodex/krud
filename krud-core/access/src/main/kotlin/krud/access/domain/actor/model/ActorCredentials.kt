/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.access.domain.actor.model

import krud.access.credential.CredentialService
import krud.database.schema.admin.actor.ActorTable
import org.jetbrains.exposed.v1.core.ResultRow
import kotlin.uuid.Uuid

/**
 * Represents the credentials from a concrete [Actor].
 *
 * #### Security
 * This data is meant to be short-lived, as it contains the Actor's password.
 * For in-memory caching the [CredentialService] is used, which hashes and salts the password.
 *
 * @property id The Actor's unique id.
 * @property username The Actor's unique username.
 * @property password The unencrypted Actor's password.
 *
 * @see [Actor]
 * @see [CredentialService]
 */
internal data class ActorCredentials private constructor(
    var id: Uuid,
    val username: String,
    val password: String,
) {
    companion object {
        /**
         * Maps a [ResultRow] to an [ActorCredentials] instance.
         *
         * @param row The [ResultRow] to map.
         * @return The mapped [ActorCredentials] instance.
         */
        fun from(row: ResultRow): ActorCredentials {
            return ActorCredentials(
                id = row[ActorTable.id],
                username = row[ActorTable.username],
                password = row[ActorTable.password],
            )
        }
    }
}
