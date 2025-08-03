/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.database.plugins

import io.ktor.server.application.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import krud.base.settings.AppSettings
import krud.database.service.DatabaseService
import org.jetbrains.exposed.v1.core.Table

/**
 * Custom Ktor plugin to configure the database.
 */
internal val DbPlugin: ApplicationPlugin<DbPluginConfig> = createApplicationPlugin(
    name = "DbPlugin",
    createConfiguration = ::DbPluginConfig
) {
    DatabaseService.init(
        settings = AppSettings.database,
        environment = AppSettings.runtime.environment,
        telemetryRegistry = pluginConfig.telemetryRegistry
    ) {
        pluginConfig.tables.forEach { table ->
            addTable(table)
        }
    }
}

/**
 * Configuration for the [DbPlugin].
 */
internal class DbPluginConfig {
    /** List of tables to be registered with the database. */
    val tables: MutableList<Table> = mutableListOf()

    /** Optional metrics registry for telemetry monitoring. */
    var telemetryRegistry: PrometheusMeterRegistry? = null
}
