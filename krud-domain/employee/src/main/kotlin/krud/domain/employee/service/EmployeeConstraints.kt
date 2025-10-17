/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.domain.employee.service

import krud.base.error.AppException
import krud.base.error.CompositeAppException
import krud.base.error.validator.EmailValidator
import krud.base.error.validator.PhoneValidator
import krud.base.util.getPropertyName
import krud.domain.contact.model.ContactRequest
import krud.domain.employee.error.EmployeeError
import krud.domain.employee.model.EmployeeRequest
import krud.domain.employee.repository.EmployeeRepository
import krud.domain.employee.repository.IEmployeeRepository
import kotlin.uuid.Uuid

/**
 * Provides verification methods for employee-related operations.
 */
internal object EmployeeConstraints {
    /**
     * Verifies if the integrity of the given [request] is valid.
     *
     * Note: For the email verification, it could be done alternatively with the EmailString,
     * but such would show only a generic error to the client, without any contextual information.
     *
     * @param employeeId The ID of the employee being verified.
     * @param request The [EmployeeRequest] details to be verified.
     * @param reason The reason for the verification. To be included in error messages.
     * @param repository The [EmployeeRepository] to perform additional checks.
     * @return A [Result] with verification state.
     */
    fun verify(
        employeeId: Uuid?,
        request: EmployeeRequest,
        reason: String,
        repository: IEmployeeRepository,
    ): Result<Unit> {
        val errors: MutableList<AppException> = mutableListOf()

        // Check the work email.
        checkWorkEmail(
            employeeId = employeeId,
            request = request,
            reason = reason,
            repository = repository,
            errors = errors
        )

        // Check the contact's personal email and phone.
        request.contact?.let { contact ->
            checkContact(
                employeeId = employeeId,
                contactRequest = contact,
                reason = reason,
                errors = errors
            )
        }

        if (errors.isNotEmpty()) {
            return Result.failure(CompositeAppException(errors))
        }

        return Result.success(Unit)
    }

    /**
     * Verifies the work email of the employee. Format and uniqueness are checked.
     *
     * @param employeeId The ID of the employee being verified. `null` If the employee is new.
     * @param request The request details to be verified.
     * @param reason The reason for the verification. To be included in error messages.
     * @param repository The [EmployeeRepository] to check for uniqueness.
     * @param errors The list of errors to append to.
     */
    private fun checkWorkEmail(
        employeeId: Uuid?,
        request: EmployeeRequest,
        reason: String,
        repository: IEmployeeRepository,
        errors: MutableList<AppException>
    ) {
        val workEmail: String = request.workEmail

        // Check the work email format.
        EmailValidator.verify(value = workEmail).onFailure { error ->
            errors.add(
                EmployeeError.InvalidEmail(
                    employeeId = employeeId,
                    email = workEmail,
                    field = getPropertyName(root = EmployeeRequest::workEmail),
                    reason = reason,
                    cause = error
                )
            )
        }

        // Check if the work email is already in use.
        repository.findByWorkEmail(
            workEmail = workEmail,
            excludeEmployeeId = employeeId
        )?.let { employee ->
            errors.add(
                EmployeeError.WorkEmailInUse(
                    affectedEmployeeId = employeeId,
                    usedByEmployeeId = employee.id,
                    workEmail = workEmail,
                    field = getPropertyName(root = EmployeeRequest::workEmail),
                    reason = reason,
                    cause = null
                )
            )
        }
    }

    /**
     * Verifies the contact details of the employee.
     *
     * @param employeeId The ID of the employee being verified. `null` If the employee is new.
     * @param contactRequest The [ContactRequest] details to be verified.
     * @param reason The reason for the verification. To be included in error messages.
     * @param errors The list of errors to append to.
     */
    private fun checkContact(
        employeeId: Uuid?,
        contactRequest: ContactRequest,
        reason: String,
        errors: MutableList<AppException>
    ) {
        // Check the contact's personal email and phone.
        EmailValidator.verify(value = contactRequest.email).onFailure { error ->
            errors.add(
                EmployeeError.InvalidEmail(
                    employeeId = employeeId,
                    email = contactRequest.email,
                    field = getPropertyName(root = ContactRequest::email),
                    reason = reason,
                    cause = error
                )
            )
        }

        PhoneValidator.verify(value = contactRequest.phone).onFailure { error ->
            errors.add(
                EmployeeError.InvalidPhoneNumber(
                    employeeId = employeeId,
                    phone = contactRequest.phone,
                    field = getPropertyName(root = ContactRequest::phone),
                    reason = reason,
                    cause = error
                )
            )
        }
    }
}
