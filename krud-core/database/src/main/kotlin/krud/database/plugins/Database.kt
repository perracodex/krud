/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.database.plugins

import io.ktor.server.application.*
import krud.base.env.Telemetry
import krud.database.schema.admin.actor.ActorTable
import krud.database.schema.admin.rbac.RbacFieldRuleTable
import krud.database.schema.admin.rbac.RbacRoleTable
import krud.database.schema.admin.rbac.RbacScopeRuleTable
import krud.database.schema.contact.ContactTable
import krud.database.schema.employee.EmployeeTable
import krud.database.schema.employment.EmploymentTable

/**
 * Configures the custom [DbPlugin].
 *
 * This will set up and configure the database, including the connection pool, and register
 * the database schema tables so that the ORM can interact with them.
 *
 * @see [DbPlugin]
 */
public fun Application.configureDatabase() {
    install(plugin = DbPlugin) {
        telemetryRegistry = Telemetry.registry

        tables.addAll(
            listOf(
                // System tables.
                RbacFieldRuleTable,
                RbacScopeRuleTable,
                RbacRoleTable,
                ActorTable,

                // Domain tables
                ContactTable,
                EmployeeTable,
                EmploymentTable
            )
        )
    }
}
