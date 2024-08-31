/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kcrud.base.database.schema.contact

import kcrud.base.database.columns.autoGenerate
import kcrud.base.database.columns.encryptedValidVarChar
import kcrud.base.database.columns.kotlinUuid
import kcrud.base.database.columns.references
import kcrud.base.database.schema.base.TimestampedTable
import kcrud.base.database.schema.employee.EmployeeTable
import kcrud.base.persistence.validators.impl.EmailValidator
import kcrud.base.security.utils.EncryptionUtils
import org.jetbrains.exposed.crypt.Encryptor
import org.jetbrains.exposed.crypt.encryptedVarchar
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import kotlin.uuid.Uuid

/**
 * Database table definition for employee contact details.
 * Demonstrates how to encrypt data in the database,
 * in addition of how to validate column data.
 *
 * Even though the project already has email validation at the serializer level,
 * and also at the service level, this table also demonstrates how to also validate
 * the email at column level with a custom [encryptedValidVarChar] extension function.
 *
 * For the phone we could do the same as for the email, but for the sake of simplicity
 * we just encrypt it with the [encryptedVarchar] extension function, and leave
 * the validation up to the service layer.
 *
 * For encrypted fields, the lengths are larger than the actual length of the data,
 * since the encrypted data will be larger than the original value.
 */
public object ContactTable : TimestampedTable(name = "contact") {
    private val encryptor: Encryptor = EncryptionUtils.getEncryptor(type = EncryptionUtils.Type.AT_REST)

    /**
     * The unique id of the contact record.
     */
    public val id: Column<Uuid> = kotlinUuid(
        name = "contact_id"
    ).autoGenerate()

    /**
     * The id of the employee to which the contact details belong.
     */
    public val employeeId: Column<Uuid> = kotlinUuid(
        name = "employee_id"
    ).references(
        ref = EmployeeTable.id,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.RESTRICT,
        fkName = "fk_contact__employee_id"
    )

    /**
     * The contact's email.
     * Must be a valid email.
     */
    public val email: Column<String> = encryptedValidVarChar(
        name = "email",
        cipherTextLength = 256,
        encryptor = encryptor,
        validator = EmailValidator
    )

    /**
     * The contact's phone.
     * Must be a valid phone number.
     */
    public val phone: Column<String> = encryptedVarchar(
        name = "phone",
        cipherTextLength = encryptor.maxColLength(inputByteSize = 128),
        encryptor = encryptor
    )

    /**
     * The table's primary key.
     */
    override val primaryKey: Table.PrimaryKey = PrimaryKey(
        firstColumn = id,
        name = "pk_contact_id"
    )

    init {
        index(
            customIndexName = "ix_contact__employee_id",
            isUnique = false,
            columns = arrayOf(employeeId)
        )
    }
}
