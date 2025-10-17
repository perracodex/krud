/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.domain.employee.repository

import io.perracodex.exposed.pagination.Page
import io.perracodex.exposed.pagination.Pageable
import io.perracodex.exposed.pagination.paginate
import krud.base.context.SessionContext
import krud.database.schema.contact.ContactTable
import krud.database.schema.employee.EmployeeTable
import krud.database.schema.employment.EmploymentTable
import krud.database.util.transaction
import krud.domain.contact.repository.IContactRepository
import krud.domain.employee.model.Employee
import krud.domain.employee.model.EmployeeFilterSet
import krud.domain.employee.model.EmployeeRequest
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.*
import kotlin.uuid.Uuid

/**
 * Implementation of the [IEmployeeRepository] interface.
 * Responsible for managing employee data.
 */
internal class EmployeeRepository(
    private val sessionContext: SessionContext,
    private val contactRepository: IContactRepository
) : IEmployeeRepository {

    override fun findById(employeeId: Uuid): Employee? {
        return transaction(sessionContext = sessionContext) {
            val records: List<ResultRow> = EmployeeTable
                .leftJoin(otherTable = ContactTable)
                .leftJoin(otherTable = EmploymentTable)
                .selectAll().where {
                    EmployeeTable.id eq employeeId
                }.toList()

            Employee.from(rows = records)
        }
    }

    override fun findAll(pageable: Pageable?): Page<Employee> {
        return transaction(sessionContext = sessionContext) {
            EmployeeTable
                .leftJoin(otherTable = ContactTable)
                .leftJoin(otherTable = EmploymentTable)
                .selectAll()
                .paginate(
                    pageable = pageable,
                    map = Employee,
                    groupBy = EmployeeTable.id
                )
        }
    }

    override fun findByWorkEmail(workEmail: String, excludeEmployeeId: Uuid?): Employee? {
        return transaction(sessionContext = sessionContext) {
            val query: Query = EmployeeTable
                .leftJoin(otherTable = ContactTable)
                .leftJoin(otherTable = EmploymentTable)
                .selectAll().where {
                    EmployeeTable.workEmail eq workEmail
                }

            excludeEmployeeId?.let { employeeId ->
                query.andWhere {
                    EmployeeTable.id neq employeeId
                }
            }

            val records: List<ResultRow> = query.toList()
            Employee.from(rows = records)
        }
    }

    override fun filter(filterSet: EmployeeFilterSet, pageable: Pageable?): Page<Employee> {
        return transaction(sessionContext = sessionContext) {
            EmployeeTable
                .leftJoin(otherTable = ContactTable)
                .leftJoin(otherTable = EmploymentTable)
                .selectAll().apply {
                    // Apply filters dynamically based on the presence of criteria in filterSet.
                    // Using lowerCase() to make the search case-insensitive.
                    // This could be removed if the database is configured to use a case-insensitive collation.

                    if (!filterSet.firstName.isNullOrBlank()) {
                        andWhere {
                            EmployeeTable.firstName.lowerCase() like "%${filterSet.firstName.trim().lowercase()}%"
                        }
                    }
                    if (!filterSet.lastName.isNullOrBlank()) {
                        andWhere {
                            EmployeeTable.lastName.lowerCase() like "%${filterSet.lastName.trim().lowercase()}%"
                        }
                    }
                    if (!filterSet.workEmail.isNullOrBlank()) {
                        andWhere {
                            EmployeeTable.workEmail.lowerCase() like "%${filterSet.workEmail.trim().lowercase()}%"
                        }
                    }
                    if (!filterSet.contactEmail.isNullOrBlank()) {
                        andWhere {
                            ContactTable.email.lowerCase() like "%${filterSet.contactEmail.trim().lowercase()}%"
                        }
                    }
                    if (!filterSet.honorific.isNullOrEmpty()) {
                        andWhere {
                            EmployeeTable.honorific inList filterSet.honorific
                        }
                    }
                    if (!filterSet.maritalStatus.isNullOrEmpty()) {
                        andWhere {
                            EmployeeTable.maritalStatus inList filterSet.maritalStatus
                        }
                    }
                }.paginate(
                    pageable = pageable,
                    map = Employee,
                    groupBy = EmployeeTable.id
                )
        }
    }

    override fun search(term: String, pageable: Pageable?): Page<Employee> {
        // Normalize the search term.
        // The lowercasing could be removed if the database is configured to use a case-insensitive collation.
        val searchTerm: String = term.trim().lowercase()

        // Pattern to match any part within an email local segment, before '@'.
        val emailLocalSegmentPattern: Expression<String> = stringParam(value = "([^@]*?$searchTerm[^@]*)")

        return transaction(sessionContext = sessionContext) {
            EmployeeTable.join(
                otherTable = ContactTable,
                joinType = JoinType.LEFT,
                onColumn = EmployeeTable.id,
                otherColumn = ContactTable.employeeId
            ).join(
                otherTable = EmploymentTable,
                joinType = JoinType.LEFT,
                onColumn = EmployeeTable.id,
                otherColumn = EmploymentTable.employeeId
            ).selectAll().where {
                // Search in the first name.
                (EmployeeTable.firstName.lowerCase() like "%$searchTerm%")
            }.orWhere {
                // Search in the last name.
                (EmployeeTable.lastName.lowerCase() like "%$searchTerm%")
            }.orWhere {
                // Search within the local part of the work email (before '@').
                EmployeeTable.workEmail.regexp(pattern = emailLocalSegmentPattern, caseSensitive = false)
            }.orWhere {
                // Search work email starting with the search term.
                (EmployeeTable.workEmail.lowerCase() like "$searchTerm%")
            }.orWhere {
                // Search work email ending with the search term.
                (EmployeeTable.workEmail.lowerCase() like "%$searchTerm")
            }.orWhere {
                // Search within the local part of the contact email (before '@').
                ContactTable.email.regexp(pattern = emailLocalSegmentPattern, caseSensitive = false)
            }.orWhere {
                // Search contact email starting with the search term.
                (ContactTable.email.lowerCase() like "$searchTerm%")
            }.orWhere {
                // Search contact email ending with the search term.
                (ContactTable.email.lowerCase() like "%$searchTerm")
            }.paginate(
                pageable = pageable,
                map = Employee,
                groupBy = EmployeeTable.id
            )
        }
    }

    override fun create(request: EmployeeRequest): Employee {
        return transaction(sessionContext = sessionContext) {
            EmployeeTable.insert { statement ->
                statement.toInsertStatement(request = request)
            }[EmployeeTable.id].let { employeeId ->
                request.contact?.let { contactRequest ->
                    contactRepository.create(
                        employeeId = employeeId,
                        request = contactRequest
                    )
                }

                val employee: Employee? = findById(employeeId = employeeId)
                checkNotNull(employee) { "Failed to create Employee." }
                employee
            }
        }
    }

    override fun update(employeeId: Uuid, request: EmployeeRequest): Employee? {
        return transaction(sessionContext = sessionContext) {
            EmployeeTable.update(
                where = {
                    EmployeeTable.id eq employeeId
                }
            ) { statement ->
                statement.toUpdateStatement(request = request)
            }.takeIf { it > 0 }?.let {
                contactRepository.syncWithEmployee(
                    employeeId = employeeId,
                    employeeRequest = request
                )

                findById(employeeId = employeeId)
            }
        }
    }

    override fun delete(employeeId: Uuid): Int {
        return transaction(sessionContext = sessionContext) {
            EmployeeTable.deleteWhere {
                id eq employeeId
            }
        }
    }

    override fun deleteAll(): Int {
        return transaction(sessionContext = sessionContext) {
            EmployeeTable.deleteAll()
        }
    }

    override fun count(): Int {
        return transaction(sessionContext = sessionContext) {
            EmployeeTable.selectAll().count().toInt()
        }
    }

    /**
     * Populates an SQL [UpdateBuilder] with data from an [EmployeeRequest] instance,
     * without assigning the `createdBy` field. This is suitable for updates.
     */
    private fun UpdateBuilder<Int>.toUpdateStatement(request: EmployeeRequest) {
        this[EmployeeTable.firstName] = request.firstName.trim()
        this[EmployeeTable.lastName] = request.lastName.trim()
        this[EmployeeTable.workEmail] = request.workEmail.trim()
        this[EmployeeTable.dob] = request.dob
        this[EmployeeTable.maritalStatus] = request.maritalStatus
        this[EmployeeTable.honorific] = request.honorific
        this[EmployeeTable.updatedBy] = sessionContext.actorId
    }

    /**
     * Populates an SQL [UpdateBuilder] with data from a [EmployeeRequest] instance,
     * including the `createdBy` field. This is suitable for inserts.
     */
    private fun UpdateBuilder<Int>.toInsertStatement(request: EmployeeRequest) {
        toUpdateStatement(request = request)
        this[EmployeeTable.createdBy] = sessionContext.actorId
    }
}
