/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.domain.employee.api

import io.ktor.server.routing.*
import krud.access.domain.rbac.plugin.withRbac
import krud.database.schema.admin.rbac.type.RbacAccessLevel
import krud.database.schema.admin.rbac.type.RbacScope
import krud.domain.employee.api.delete.deleteAllEmployeesRoute
import krud.domain.employee.api.delete.deleteEmployeeByIdRoute
import krud.domain.employee.api.fetch.filterEmployeeRoute
import krud.domain.employee.api.fetch.findAllEmployeesRoute
import krud.domain.employee.api.fetch.findEmployeeByIdRoute
import krud.domain.employee.api.fetch.searchEmployeeRoute
import krud.domain.employee.api.operate.createEmployeeRoute
import krud.domain.employee.api.operate.updateEmployeeByIdRoute

/**
 * Employee-related endpoints.
 *
 * #### References
 * - [Application Structure](https://ktor.io/docs/server-application-structure.html) for examples
 * of how to organize routes in diverse ways.
 */
@OptIn(EmployeeRouteApi::class)
public fun Route.employeeRoutes() {
    withRbac(scope = RbacScope.EMPLOYEE_RECORDS, accessLevel = RbacAccessLevel.FULL) {
        createEmployeeRoute()
        updateEmployeeByIdRoute()

        deleteAllEmployeesRoute()
        deleteEmployeeByIdRoute()
    }

    withRbac(scope = RbacScope.EMPLOYEE_RECORDS, accessLevel = RbacAccessLevel.VIEW) {
        findAllEmployeesRoute()
        findEmployeeByIdRoute()
        filterEmployeeRoute()
        searchEmployeeRoute()
    }
}

/**
 * Annotation for controlled access to the Employee Routes API.
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR, message = "Only to be used within the Employee Routes API.")
@Retention(AnnotationRetention.BINARY)
internal annotation class EmployeeRouteApi
