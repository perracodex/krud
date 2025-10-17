/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.base.settings.catalog.section

import kotlinx.serialization.Serializable
import krud.base.env.EnvironmentType

/**
 * Database-related settings.
 *
 * @property name The name of the database.
 * @property path The database file location.
 * @property isLocalFile Whether the database is a local file, for example, an H2 embedded database.
 * @property jdbcUrl The JDBC url database connection.
 * @property jdbcDriver The JDBC driver class name.
 * @property transactionMaxAttempts Max retries inside a transaction if a SQLException happens. Overridable per-transaction level.
 * @property transactionMinRetryDelayMs Delay between retries in a transaction if a SQLException happens. Overridable per-transaction level.
 * @property warnLongQueriesDurationMs Threshold to log queries exceeding the threshold with WARN level. Overridable per-transaction level.
 * @property connectionPoolSize The database connection pool size. 0 for no connection pooling.
 * @property minimumPoolIdle The minimum number of idle connections to maintain in the pool.
 * @property connectionPoolTimeoutMs The database connection pool timeout, (ms).
 * @property updateSchemaEnvironments The list f environments under which it is allowed to update the database schema.
 * @property useMigrations Whether to use migrations to set the database, or the schema creation via exposed.
 * @property username Optional database username.
 * @property password Optional database password.
 */
@Serializable
public data class DatabaseSettings internal constructor(
    val name: String,
    val path: String,
    val isLocalFile: Boolean,
    val jdbcUrl: String,
    val jdbcDriver: String,
    val transactionMaxAttempts: Int,
    val transactionMinRetryDelayMs: Long,
    val warnLongQueriesDurationMs: Long,
    val connectionPoolSize: Int,
    val minimumPoolIdle: Int,
    val connectionPoolTimeoutMs: Long,
    val updateSchemaEnvironments: List<EnvironmentType>,
    val useMigrations: Boolean,
    val username: String? = null,
    val password: String? = null,
)
