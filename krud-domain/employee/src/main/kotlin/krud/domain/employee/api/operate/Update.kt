/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.domain.employee.api.operate

import io.github.perracodex.kopapi.dsl.operation.api
import io.github.perracodex.kopapi.dsl.parameter.pathParameter
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import krud.base.context.sessionContext
import krud.base.error.AppException
import krud.base.util.toUuid
import krud.domain.employee.api.EmployeeRouteApi
import krud.domain.employee.error.EmployeeError
import krud.domain.employee.model.Employee
import krud.domain.employee.model.EmployeeRequest
import krud.domain.employee.service.EmployeeService
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope
import kotlin.uuid.Uuid

@EmployeeRouteApi
internal fun Route.updateEmployeeByIdRoute() {
    put("/api/v1/employees/{employee_id}") {
        val employeeId: Uuid = call.parameters.getOrFail(name = "employee_id").toUuid()
        val request: EmployeeRequest = call.receive<EmployeeRequest>()

        val service: EmployeeService = call.scope.get<EmployeeService> { parametersOf(call.sessionContext) }
        val updatedEmployee: Employee? = service.update(
            employeeId = employeeId,
            request = request
        ).getOrThrow()

        if (updatedEmployee == null) {
            throw EmployeeError.EmployeeNotFound(employeeId = employeeId)
        } else {
            call.respond(status = HttpStatusCode.OK, message = updatedEmployee)
        }
    } api {
        tags = setOf("Employee")
        summary = "Update an employee by ID."
        description = "Update an employee's details by their unique ID."
        operationId = "updateEmployeeById"
        pathParameter<Uuid>(name = "employee_id") {
            description = "The unique identifier of the employee."
        }
        requestBody<EmployeeRequest> {
            description = "The employee details to update."
        }
        response<Employee>(status = HttpStatusCode.OK) {
            description = "Employee updated."
        }
        response<AppException.ErrorResponse>(status = EmployeeError.EmployeeNotFound.STATUS_CODE) {
            description = "Employee not found. Code: ${EmployeeError.EmployeeNotFound.ERROR_CODE}"
        }
        bearerSecurity(name = "Authentication") {
            description = "Access to employee data."
        }
    }
}
