/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.database.schema.employment

import kotlinx.datetime.LocalDate
import krud.base.security.util.EncryptionUtils
import krud.database.column.autoGenerate
import krud.database.column.enumerationById
import krud.database.column.kotlinUuid
import krud.database.schema.base.PeriodTable
import krud.database.schema.employee.EmployeeTable
import krud.database.schema.employment.type.EmploymentStatus
import krud.database.schema.employment.type.WorkModality
import org.jetbrains.exposed.v1.crypt.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.datetime.date
import kotlin.uuid.Uuid

/**
 * Database table definition for employments.
 *
 * An employee may have multiple employments, which indicates re-hiring.
 */
public object EmploymentTable : PeriodTable(name = "employment") {
    private val encryptor: Encryptor = EncryptionUtils.getEncryptor(type = EncryptionUtils.Type.AT_REST)

    /**
     * The unique id of the employment record.
     */
    public val id: Column<Uuid> = kotlinUuid(
        name = "employment_id"
    ).autoGenerate()

    /**
     * The id of the employee to which the employment belongs.
     */
    public val employeeId: Column<Uuid> = kotlinUuid(
        name = "employee_id"
    ).references(
        ref = EmployeeTable.id,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.RESTRICT,
        fkName = "fk_employment__employee_id"
    ).index(
        customIndexName = "ix_employment__employee_id"
    )

    /**
     * The status of the employment.
     */
    public val status: Column<EmploymentStatus> = enumerationById(
        name = "status_id",
        entries = EmploymentStatus.entries
    )

    /**
     * The date the employment started.
     */
    public val probationEndDate: Column<LocalDate?> = date(
        name = "probation_end_date"
    ).nullable()

    /**
     * The [WorkModality] of the employment.
     */
    public val workModality: Column<WorkModality> = enumerationById(
        name = "work_modality_id",
        entries = WorkModality.entries
    )

    /**
     * Demonstrates how to use an encrypted column.
     *
     * In encrypted fields, the lengths are larger than the actual length of the unencrypted data,
     * since the encrypted final output will be larger than the original unencrypted value.
     * The [Encryptor.maxColLength] method must be used to calculate the maximum length
     * of the encrypted data, passing as input the maximum length of the unencrypted data.
     *
     * Encrypted fields cannot be defined as `unique` at the database level due to varying
     * encrypted outputs for the same data.
     */
    public val sensitiveData: Column<String?> = encryptedVarchar(
        name = "sensitive_data",
        cipherTextLength = encryptor.maxColLength(inputByteSize = 512),
        encryptor = encryptor
    ).nullable()

    /**
     * The table's primary key.
     */
    override val primaryKey: PrimaryKey = PrimaryKey(
        firstColumn = id,
        name = "pk_employment_id"
    )
}
