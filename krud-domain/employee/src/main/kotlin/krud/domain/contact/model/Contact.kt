/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.domain.contact.model

import io.perracodex.exposed.pagination.MapModel
import kotlinx.serialization.Serializable
import krud.database.model.Meta
import krud.database.schema.contact.ContactTable
import org.jetbrains.exposed.v1.core.ResultRow
import kotlin.uuid.Uuid

/**
 * Represents a concrete contact detail for an employee.
 *
 * @property id The contact's id.
 * @property email The contact's email.
 * @property phone The contact's phone number.
 * @property meta The metadata of the record.
 */
@Serializable
public data class Contact internal constructor(
    val id: Uuid,
    val email: String,
    val phone: String,
    val meta: Meta
) {
    internal companion object : MapModel<Contact> {
        override fun from(row: ResultRow): Contact {
            return Contact(
                id = row[ContactTable.id],
                email = row[ContactTable.email],
                phone = row[ContactTable.phone],
                meta = Meta.from(row = row, table = ContactTable)
            )
        }
    }
}
