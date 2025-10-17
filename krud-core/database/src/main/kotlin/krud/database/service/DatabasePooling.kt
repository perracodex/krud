/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.database.service

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import krud.base.settings.catalog.section.DatabaseSettings
import krud.database.util.IsolationLevel

internal object DatabasePooling {

    /**
     * Create a HikariDataSource to enable database connection pooling.
     *
     * @param settings The [DatabaseSettings] settings to be used for the database connection pooling.
     * @param isolationLevel The isolation level to use for the database transactions.
     * @param telemetryRegistry Optional metrics registry for telemetry monitoring.
     */
    fun createDataSource(
        settings: DatabaseSettings,
        isolationLevel: IsolationLevel = IsolationLevel.TRANSACTION_REPEATABLE_READ,
        telemetryRegistry: PrometheusMeterRegistry? = null
    ): HikariDataSource {
        require(value = settings.connectionPoolSize > 0) { "Database connection pooling must be >= 1." }

        val hikariConfig: HikariConfig = HikariConfig().apply {
            // Specifies the class name of the JDBC driver to be used,
            // and JDBC URL for the database connection.
            driverClassName = settings.jdbcDriver
            jdbcUrl = settings.jdbcUrl

            // Maximum number of connections in the pool.
            maximumPoolSize = settings.connectionPoolSize

            // Maximum wait time in milliseconds for an application component to get a connection from the pool.
            connectionTimeout = settings.connectionPoolTimeoutMs

            // Transaction isolation level for the connections within the pool.
            transactionIsolation = isolationLevel.name

            // Minimum number of idle connections maintained by HikariCP in the pool.
            minimumIdle = settings.minimumPoolIdle

            // Disables auto-commit on connections to allow explicit transaction management,
            // thereby ensuring each transaction is atomic and preventing automatic commits
            // after each statement.
            isAutoCommit = false

            // Database credentials for authentication.
            this.username = settings.username.orEmpty()
            this.password = settings.password.orEmpty()

            // Integrates a telemetry registry for monitoring and metrics.
            telemetryRegistry?.let {
                metricRegistry = telemetryRegistry
            }

            validate()
        }

        return HikariDataSource(hikariConfig)
    }
}
