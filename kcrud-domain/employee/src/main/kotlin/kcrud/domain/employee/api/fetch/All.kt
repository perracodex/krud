/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kcrud.domain.employee.api.fetch

import io.github.perracodex.kopapi.dsl.operation.api
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.perracodex.exposed.pagination.Page
import io.perracodex.exposed.pagination.getPageable
import kcrud.core.context.getContext
import kcrud.domain.employee.api.EmployeeRouteApi
import kcrud.domain.employee.model.Employee
import kcrud.domain.employee.service.EmployeeService
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope

@EmployeeRouteApi
internal fun Route.findAllEmployeesRoute() {
    get("/api/v1/employees") {
        val service: EmployeeService = call.scope.get<EmployeeService> { parametersOf(call.getContext()) }
        val employees: Page<Employee> = service.findAll(pageable = call.getPageable())
        call.respond(status = HttpStatusCode.OK, message = employees)
    } api {
        tags = setOf("Employee")
        summary = "Find all employees."
        description = "Retrieve all employees in the system."
        operationId = "findAllEmployees"
        response<Page<Employee>>(status = HttpStatusCode.OK) {
            description = "Employees found."
        }
        bearerSecurity(name = "Authentication") {
            description = "Access to employee data."
        }
    }
}