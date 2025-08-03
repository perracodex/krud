/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.domain.employee.model

import io.perracodex.exposed.pagination.MapModel
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import krud.base.util.DateTimeUtils.age
import krud.database.model.Meta
import krud.database.schema.contact.ContactTable
import krud.database.schema.employee.EmployeeTable
import krud.database.schema.employee.type.Honorific
import krud.database.schema.employee.type.MaritalStatus
import krud.database.schema.employment.EmploymentTable
import krud.domain.contact.model.Contact
import krud.domain.employment.model.Employment
import org.jetbrains.exposed.v1.core.ResultRow
import kotlin.uuid.Uuid

/**
 * Represents a concrete employee.
 *
 * @property id The employee's id.
 * @property firstName The first name of the employee.
 * @property lastName The last name of the employee.
 * @property fullName The full name of the employee, computed as "lastName, firstName".
 * @property workEmail The unique work email of the employee.
 * @property dob The date of birth of the employee.
 * @property age The age of the employee, computed from [dob].
 * @property maritalStatus The [MaritalStatus] of the employee.
 * @property honorific The [Honorific] or title of the employee.
 * @property contact Optional list of [Contact] details of the employee.
 * @property employments Optional list of [Employment] entries for the employee.
 * @property meta The metadata of the record.
 */
@Serializable
public data class Employee internal constructor(
    val id: Uuid,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val workEmail: String,
    val dob: LocalDate,
    val age: Int,
    val maritalStatus: MaritalStatus,
    val honorific: Honorific,
    val contact: List<Contact>?,
    val employments: List<Employment>?,
    val meta: Meta
) {
    internal companion object : MapModel<Employee> {
        override fun from(row: ResultRow): Employee {
            val dob: LocalDate = row[EmployeeTable.dob]
            val firstName: String = row[EmployeeTable.firstName]
            val lastName: String = row[EmployeeTable.lastName]

            return Employee(
                id = row[EmployeeTable.id],
                firstName = firstName,
                lastName = lastName,
                fullName = "$lastName, $firstName",
                workEmail = row[EmployeeTable.workEmail],
                dob = dob,
                age = dob.age(),
                maritalStatus = row[EmployeeTable.maritalStatus],
                honorific = row[EmployeeTable.honorific],
                contact = null,
                employments = null,
                meta = Meta.from(row = row, table = EmployeeTable)
            )
        }

        override fun from(rows: List<ResultRow>): Employee? {
            if (rows.isEmpty()) {
                return null
            }

            // As we are handling a 1 -> N relationship,
            // we only need the first row to extract the top-level record.
            val topLevelRecord: ResultRow = rows.first()
            val employee: Employee = from(row = topLevelRecord)

            // Extract Contacts.
            val contact: List<Contact> = rows.distinctBy { row ->
                // Ensure no deduplicates due to 1 -> N relationship.
                row[ContactTable.id]
            }.mapNotNull { row ->
                row.getOrNull(ContactTable.id)?.let {
                    Contact.from(row = row)
                }
            }

            // Extract Employments.
            val employments: List<Employment> = rows.distinctBy { row ->
                // Ensure no deduplicates due to 1 -> N relationship.
                row[EmploymentTable.id]
            }.mapNotNull { row ->
                row.getOrNull(EmploymentTable.id)?.let {
                    Employment.from(row = row)
                }
            }

            return employee.copy(
                contact = contact.takeIf { it.isNotEmpty() },
                employments = employments.takeIf { it.isNotEmpty() }
            )
        }
    }
}
