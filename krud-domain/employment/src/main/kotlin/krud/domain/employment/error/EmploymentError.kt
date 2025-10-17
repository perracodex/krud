/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.domain.employment.error

import io.ktor.http.*
import kotlinx.datetime.LocalDate
import krud.base.error.AppException
import kotlin.uuid.Uuid

/**
 * Concrete errors for the Employment domain.
 *
 * @param statusCode The [HttpStatusCode] associated with this error.
 * @param errorCode A unique code identifying the type of error.
 * @param description A human-readable description of the error.
 * @param field Optional field identifier, typically the input field that caused the error.
 * @param reason Optional human-readable reason for the exception, providing more context.
 * @param cause Optional underlying cause of the exception, if any.
 */
internal sealed class EmploymentError(
    statusCode: HttpStatusCode,
    errorCode: String,
    description: String,
    field: String? = null,
    reason: String? = null,
    cause: Throwable? = null
) : AppException(
    statusCode = statusCode,
    errorCode = errorCode,
    context = "EMPLOYMENT",
    description = description,
    field = field,
    reason = reason,
    cause = cause
) {
    /**
     * Error for when an `employment` is not found for a concrete employee.
     *
     * @param employeeId The affected employee id.
     * @param employmentId The employment id that was not found.
     * @param reason Optional human-readable reason for the exception, providing more context.
     * @param cause Optional underlying cause of the exception, if any.
     */
    class EmploymentNotFound(
        employeeId: Uuid,
        employmentId: Uuid,
        reason: String? = null,
        cause: Throwable? = null
    ) : EmploymentError(
        statusCode = STATUS_CODE,
        errorCode = ERROR_CODE,
        description = "Employment not found. Employee Id: $employeeId. Employment Id: $employmentId.",
        reason = reason,
        cause = cause
    ) {
        companion object {
            val STATUS_CODE: HttpStatusCode = HttpStatusCode.NotFound
            const val ERROR_CODE: String = "EMPLOYMENT_NOT_FOUND"
        }
    }

    /**
     * Error for when an employee is not found for a concrete `employment`.
     *
     * @param employeeId The employee id that was not found.
     * @param reason Optional human-readable reason for the exception, providing more context.
     * @param cause Optional underlying cause of the exception, if any.
     */
    class EmployeeNotFound(
        employeeId: Uuid,
        reason: String? = null,
        cause: Throwable? = null
    ) : EmploymentError(
        statusCode = STATUS_CODE,
        errorCode = ERROR_CODE,
        description = "Employee not found. Employee Id: $employeeId.",
        reason = reason,
        cause = cause
    ) {
        companion object {
            val STATUS_CODE: HttpStatusCode = HttpStatusCode.NotFound
            const val ERROR_CODE: String = "EMPLOYEE_NOT_FOUND"
        }
    }

    /**
     * Error when there is an inconsistency in the period dates,
     * where the end date is prior to the start date.
     *
     * @param employeeId The affected employee id.
     * @param employmentId The employment id associated with the error. `null` If the employment is not yet created.
     * @param startDate The start date of the employment period.
     * @param endDate The end date of the employment period.
     * @param field Optional field identifier, typically the input field that caused the error.
     * @param reason Optional human-readable reason for the exception, providing more context.
     * @param cause Optional underlying cause of the exception, if any.
     */
    class PeriodDatesMismatch(
        employeeId: Uuid,
        employmentId: Uuid?,
        startDate: LocalDate,
        endDate: LocalDate,
        field: String? = null,
        reason: String? = null,
        cause: Throwable? = null
    ) : EmploymentError(
        statusCode = STATUS_CODE,
        errorCode = ERROR_CODE,
        description = "Employment end date cannot be prior to the start date. " +
                "Start Date: $startDate. End Date: $endDate." +
                "Employee Id: $employeeId.${
                    employmentId?.let { id ->
                        " Employment Id: $id."
                    }.orEmpty()
                }",
        field = field,
        reason = reason,
        cause = cause
    ) {
        companion object {
            val STATUS_CODE: HttpStatusCode = HttpStatusCode.BadRequest
            const val ERROR_CODE: String = "PERIOD_DATES_MISMATCH"
        }
    }

    /**
     * Error when the probation end date is prior to the employment start date.
     *
     * @param employeeId The affected employee id.
     * @param employmentId The employment id associated with the error. `null` If the employment is not yet created.
     * @param startDate The start date of the employment period.
     * @param probationEndDate The probation end date of the employment period.
     * @param field Optional field identifier, typically the input field that caused the error.
     * @param reason Optional human-readable reason for the exception, providing more context.
     * @param cause Optional underlying cause of the exception, if any.
     */
    class InvalidProbationEndDate(
        employeeId: Uuid,
        employmentId: Uuid?,
        startDate: LocalDate,
        probationEndDate: LocalDate,
        field: String? = null,
        reason: String? = null,
        cause: Throwable? = null
    ) : EmploymentError(
        statusCode = STATUS_CODE,
        errorCode = ERROR_CODE,
        description = "Employment probation end date cannot be prior to the start date. " +
                "Start Date: $startDate. Probation End Date: $probationEndDate." +
                "Employee Id: $employeeId.${
                    employmentId?.let { id ->
                        " Employment Id: $id."
                    }.orEmpty()
                }",
        field = field,
        reason = reason,
        cause = cause
    ) {
        companion object {
            val STATUS_CODE: HttpStatusCode = HttpStatusCode.BadRequest
            const val ERROR_CODE: String = "INVALID_PROBATION_END_DATE"
        }
    }
}
