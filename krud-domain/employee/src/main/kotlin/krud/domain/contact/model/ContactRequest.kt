/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.domain.contact.model

import kotlinx.serialization.Serializable
import krud.base.serializer.EmailString

/**
 * Represents the request to create/update an employee's contact details.
 *
 * The phone and email are verified at service layer level, throwing custom
 * errors when invalid.
 * For the email field, it could alternatively be verified using instead
 * the [EmailString] serializer, but such an approach although simpler,
 * would not allow to send a more detailed custom error to the client
 * which could include more contextually relevant information.
 *
 * @property email The contact's email.
 * @property phone The contact's phone number.
 */
@Serializable
public data class ContactRequest internal constructor(
    val email: String,
    val phone: String
)
