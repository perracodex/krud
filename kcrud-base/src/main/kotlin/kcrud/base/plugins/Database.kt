/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kcrud.base.plugins

import io.ktor.server.application.*
import kcrud.base.database.plugin.DbPlugin
import kcrud.base.database.schema.admin.actor.ActorTable
import kcrud.base.database.schema.admin.rbac.RbacFieldRuleTable
import kcrud.base.database.schema.admin.rbac.RbacRoleTable
import kcrud.base.database.schema.admin.rbac.RbacScopeRuleTable
import kcrud.base.database.schema.contact.ContactTable
import kcrud.base.database.schema.employee.EmployeeTable
import kcrud.base.database.schema.employment.EmploymentTable
import kcrud.base.database.schema.scheduler.SchedulerAuditTable
import kcrud.base.env.MetricsRegistry

/**
 * Configures the custom [DbPlugin].
 *
 * This will set up and configure database, including the connection pool, and register
 * the database schema tables so that the ORM can interact with them.
 *
 * @see DbPlugin
 */
public fun Application.configureDatabase() {

    install(plugin = DbPlugin) {
        micrometerRegistry = MetricsRegistry.registry

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
                EmploymentTable,

                // Scheduler tables.
                SchedulerAuditTable
            )
        )
    }
}
