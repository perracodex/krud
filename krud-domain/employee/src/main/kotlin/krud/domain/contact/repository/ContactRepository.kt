/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.domain.contact.repository

import io.perracodex.exposed.pagination.Page
import io.perracodex.exposed.pagination.Pageable
import io.perracodex.exposed.pagination.paginate
import krud.base.context.SessionContext
import krud.database.schema.contact.ContactTable
import krud.database.util.transaction
import krud.domain.contact.model.Contact
import krud.domain.contact.model.ContactRequest
import krud.domain.employee.model.EmployeeRequest
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.*
import kotlin.uuid.Uuid

/**
 * Implementation of [IContactRepository].
 * Responsible for managing [Contact] data.
 */
internal class ContactRepository(
    private val sessionContext: SessionContext
) : IContactRepository {

    override fun findById(contactId: Uuid): Contact? {
        return transaction(sessionContext = sessionContext) {
            ContactTable.selectAll().where {
                ContactTable.id eq contactId
            }.singleOrNull()?.let { resultRow ->
                Contact.from(row = resultRow)
            }
        }
    }

    override fun findByEmployeeId(employeeId: Uuid): Contact? {
        return transaction(sessionContext = sessionContext) {
            ContactTable.selectAll().where {
                ContactTable.employeeId eq employeeId
            }.singleOrNull()?.let { resultRow ->
                Contact.from(row = resultRow)
            }
        }
    }

    override fun findAll(pageable: Pageable?): Page<Contact> {
        return transaction(sessionContext = sessionContext) {
            ContactTable.selectAll().paginate(pageable = pageable, map = Contact)
        }
    }

    override fun syncWithEmployee(employeeId: Uuid, employeeRequest: EmployeeRequest): Uuid? {
        return employeeRequest.contact?.let { contactRequest ->
            val contactId: Uuid? = findByEmployeeId(employeeId = employeeId)?.id

            // If the contact already exists, update it, otherwise create it.
            contactId?.let { newContactId ->
                val updateCount = update(
                    employeeId = employeeId,
                    contactId = newContactId,
                    request = contactRequest
                )

                newContactId.takeIf { updateCount > 0 }
            } ?: create(employeeId = employeeId, request = contactRequest)
        } ?: run {
            // If the request does not contain a contact, delete any existing one.
            deleteByEmployeeId(employeeId = employeeId)
            null
        }
    }

    override fun create(employeeId: Uuid, request: ContactRequest): Uuid {
        return transaction(sessionContext = sessionContext) {
            ContactTable.insert { statement ->
                statement.toInsertStatement(
                    employeeId = employeeId,
                    request = request
                )
            } get ContactTable.id
        }
    }

    override fun update(employeeId: Uuid, contactId: Uuid, request: ContactRequest): Int {
        return transaction(sessionContext = sessionContext) {
            ContactTable.update(
                where = {
                    ContactTable.id eq contactId
                }
            ) { statement ->
                statement.toUpdateStatement(
                    employeeId = employeeId,
                    request = request
                )
            }
        }
    }

    override fun delete(contactId: Uuid): Int {
        return transaction(sessionContext = sessionContext) {
            ContactTable.deleteWhere {
                id eq contactId
            }
        }
    }

    override fun deleteByEmployeeId(employeeId: Uuid): Int {
        return transaction(sessionContext = sessionContext) {
            ContactTable.deleteWhere {
                ContactTable.employeeId eq employeeId
            }
        }
    }

    override fun count(employeeId: Uuid?): Int {
        return transaction(sessionContext = sessionContext) {
            ContactTable.selectAll().apply {
                employeeId?.let { id ->
                    where { ContactTable.employeeId eq id }
                }
            }.count().toInt()
        }
    }

    /**
     * Populates an SQL [UpdateBuilder] with data from a [ContactRequest] instance,
     * without assigning the `createdBy` field. This is suitable for updates.
     */
    private fun UpdateBuilder<Int>.toUpdateStatement(employeeId: Uuid, request: ContactRequest) {
        this[ContactTable.employeeId] = employeeId
        this[ContactTable.email] = request.email.trim()
        this[ContactTable.phone] = request.phone.trim()
        this[ContactTable.updatedBy] = sessionContext.actorId
    }

    /**
     * Populates an SQL [UpdateBuilder] with data from a [ContactRequest] instance,
     * including the `createdBy` field. This is suitable for inserts.
     */
    private fun UpdateBuilder<Int>.toInsertStatement(employeeId: Uuid, request: ContactRequest) {
        toUpdateStatement(employeeId = employeeId, request = request)
        this[ContactTable.createdBy] = sessionContext.actorId
    }
}
