/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.database.service

import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.Serializable
import krud.base.env.HealthCheckApi
import krud.base.env.Tracer
import krud.base.settings.catalog.section.DatabaseSettings
import org.jetbrains.exposed.v1.core.vendors.currentDialect
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.statements.api.ExposedConnection
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Represents the database health check.
 *
 * @property errors The list of errors found during the check.
 * @property isAlive True if the database is alive, false otherwise.
 * @property datasource The [Datasource] information.
 * @property connectionTest The [ConnectionTest] information.
 * @property configuration The [DatabaseConfiguration] settings.
 */
@HealthCheckApi
@Serializable
public data class DatabaseHealth private constructor(
    val errors: MutableList<String> = mutableListOf(),
    val isAlive: Boolean,
    val datasource: Datasource?,
    val connectionTest: ConnectionTest?,
    val configuration: DatabaseConfiguration
) {
    init {
        val className: String? = this::class.simpleName

        if (!isAlive) {
            errors.add("$className. Database is not responding. $configuration")
        }

        datasource ?: run {
            errors.add("$className. Undefined datasource. $configuration")
        }

        connectionTest?.let {
            if (!it.established) {
                errors.add("$className. Database connection not established.")
            }

            if (it.isReadOnly) {
                errors.add("$className. Database connection is read-only.")
            }
        } ?: errors.add("$className. Unable to test database connection.")
    }

    /**
     * Represents the database connection test.
     *
     * @property established True if the connection is established, false otherwise.
     * @property isReadOnly Whether the connection is in read-only mode.
     * @property name The name of the database obtained from its connection URL.
     * @property version The database version.
     * @property dialect The database dialect name.
     * @property url The connection URL for the database.
     * @property vendor The name of the database based on the name of the underlying JDBC driver.
     * @property autoCommit Whether the connection is in auto-commit mode.
     * @property catalog The name of the connection's catalog.
     */
    @Serializable
    public data class ConnectionTest private constructor(
        val established: Boolean,
        val isReadOnly: Boolean,
        val name: String,
        val version: String,
        val dialect: String,
        val url: String,
        val vendor: String,
        val autoCommit: Boolean,
        val catalog: String,
    ) {
        internal companion object {
            /**
             * Builds a [ConnectionTest] object from a [Database] object.
             *
             * @param database The source [Database] object to test.
             * @return A [Result] containing the [ConnectionTest] instance, or an exception if the test failed.
             */
            fun build(database: Database?): Result<ConnectionTest> {
                return runCatching {
                    requireNotNull(value = database) { "Database must not be null." }
                    val connector: ExposedConnection<*> = database.connector()

                    try {
                        ConnectionTest(
                            established = !connector.isClosed,
                            isReadOnly = connector.readOnly,
                            name = database.name,
                            version = database.version.toString(),
                            dialect = transaction(db = database) { currentDialect.name },
                            url = database.url,
                            vendor = database.vendor,
                            autoCommit = connector.autoCommit,
                            catalog = connector.catalog
                        )
                    } finally {
                        connector.close()
                    }
                }.recoverCatching { error ->
                    throw IllegalStateException("${ConnectionTest::class.simpleName}: ${error.message}", error)
                }
            }
        }
    }

    /**
     * Represents the datasource information.
     *
     * @property isPoolRunning Whether the pool is started and is not suspended or shutdown.
     * @property totalConnections The total number of connections in the pool
     * @property activeConnections The current number of active (in-use) connections in the pool.
     * @property idleConnections The current number of idle connections in the pool.
     * @property threadsAwaitingConnection The number of threads awaiting connection.
     * @property connectionTimeout The maximum number of milliseconds that a client will wait for a connection from the pool, (ms).
     * @property maxLifetime The maximum connection lifetime, (ms).
     * @property keepaliveTime The interval in which connections is tested for aliveness, (ms).
     * @property maxPoolSize Maximum number of connections kept in the pool, including both idle and in-use connections.
     */
    @Serializable
    public data class Datasource private constructor(
        val isPoolRunning: Boolean,
        val totalConnections: Int,
        val activeConnections: Int,
        val idleConnections: Int,
        val threadsAwaitingConnection: Int,
        val connectionTimeout: Long,
        val maxLifetime: Long,
        val keepaliveTime: Long,
        val maxPoolSize: Int
    ) {
        internal companion object {
            /**
             * Builds a [Datasource] instance from a [HikariDataSource] source.
             *
             * @param datasource The source [HikariDataSource] to build from.
             * @return The build [Datasource] instance.
             */
            fun build(datasource: HikariDataSource?): Datasource? {
                return datasource?.let {
                    Datasource(
                        isPoolRunning = datasource.isRunning,
                        totalConnections = datasource.hikariPoolMXBean?.totalConnections ?: 0,
                        activeConnections = datasource.hikariPoolMXBean?.activeConnections ?: 0,
                        idleConnections = datasource.hikariPoolMXBean?.idleConnections ?: 0,
                        threadsAwaitingConnection = datasource.hikariPoolMXBean?.threadsAwaitingConnection ?: 0,
                        connectionTimeout = datasource.connectionTimeout,
                        maxLifetime = datasource.maxLifetime,
                        keepaliveTime = datasource.keepaliveTime,
                        maxPoolSize = datasource.maximumPoolSize
                    )
                }
            }
        }
    }

    /**
     * Represents the database configuration.
     *
     * @property poolSize The database connection pool size. 0 if no connection pooling.
     * @property connectionTimeout The database connection pool timeout, (ms).
     * @property transactionRetryAttempts Max retries inside a transaction if a SQLException happens.
     * @property warnLongQueriesDurationMs Threshold to log queries exceeding the threshold with WARN level.
     * @property jdbcDriver The JDBC driver class name.
     * @property jdbcUrl The JDBC url database connection.
     */
    @Serializable
    public data class DatabaseConfiguration private constructor(
        val poolSize: Int,
        val connectionTimeout: Long,
        val transactionRetryAttempts: Int,
        val warnLongQueriesDurationMs: Long,
        val jdbcDriver: String,
        val jdbcUrl: String
    ) {
        internal companion object {
            /**
             * Builds a [DatabaseConfiguration] instance from the given [settings].
             *
             * @param settings The [DatabaseSettings] configuration to build from.
             * @return The build [DatabaseConfiguration] instance.
             */
            fun build(settings: DatabaseSettings): DatabaseConfiguration {
                return DatabaseConfiguration(
                    poolSize = settings.connectionPoolSize,
                    connectionTimeout = settings.connectionPoolTimeoutMs,
                    transactionRetryAttempts = settings.transactionMaxAttempts,
                    warnLongQueriesDurationMs = settings.warnLongQueriesDurationMs,
                    jdbcDriver = settings.jdbcDriver,
                    jdbcUrl = settings.jdbcUrl
                )
            }
        }
    }

    public companion object {
        /**
         * Retrieves the database health check.
         *
         * @param settings The [DatabaseSettings] configuration to use for the health check.
         */
        public fun create(settings: DatabaseSettings): DatabaseHealth {
            return runCatching {
                val databaseTest: Result<ConnectionTest> = ConnectionTest.build(database = DatabaseService.database)

                val isAlive: Boolean = DatabaseService.ping()
                val connectionTest: ConnectionTest? = databaseTest.getOrNull()
                val datasource: Datasource? = Datasource.build(datasource = DatabaseService.hikariDataSource)
                val configuration: DatabaseConfiguration = DatabaseConfiguration.build(settings = settings)

                val databaseHealth = DatabaseHealth(
                    isAlive = isAlive,
                    connectionTest = connectionTest,
                    datasource = datasource,
                    configuration = configuration
                )

                if (databaseTest.isFailure) {
                    databaseHealth.errors.add(databaseTest.exceptionOrNull()?.message ?: "Database connection test failed.")
                }

                databaseHealth
            }.getOrElse { error ->
                Tracer(ref = ::create).error(message = "Failed to retrieve database health check.", cause = error)
                DatabaseHealth(
                    isAlive = false,
                    connectionTest = null,
                    datasource = null,
                    configuration = DatabaseConfiguration.build(settings = settings),
                ).apply {
                    errors.add("Failed to retrieve database health check. ${error.message}")
                }
            }
        }
    }
}
