/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.database.column

import krud.base.error.validator.base.IValidator
import krud.base.error.validator.base.ValidationException
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.crypt.*

/**
 * Custom encrypted column with validation.
 * For a non-encrypted column, use instead [validVarchar].
 *
 * @param name The name of the column in the database.
 * @param length The maximum length of the value column. Will be adjusted to accommodate the encrypted value.
 * @param encryptor The [Encryptor] responsible for performing encryption and decryption of stored values.
 * @param validator The [IValidator] responsible for validating the value before insertion.
 * @return A Column<String> representing the values in the database table.
 * @throws ValidationException if the value is invalid during data insertion.
 *
 * @see [IValidator]
 * @see [validVarchar]
 */
internal fun Table.validEncryptedVarchar(
    name: String,
    length: Int,
    encryptor: Encryptor,
    validator: IValidator<String>
): Column<String> {
    return registerColumn(
        name = name,
        type = EncryptedVarCharColumnType(
            encryptor = encryptor,
            colLength = encryptor.maxColLength(inputByteSize = length)
        )
    ).transform(
        wrap = { it },
        unwrap = { value ->
            validator.verify(value = value).getOrThrow()
        }
    )
}
