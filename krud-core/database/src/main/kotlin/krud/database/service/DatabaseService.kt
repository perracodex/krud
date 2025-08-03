/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.database.service

import com.zaxxer.hikari.HikariDataSource
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import krud.base.env.EnvironmentType
import krud.base.env.Tracer
import krud.base.settings.catalog.section.DatabaseSettings
import krud.database.util.IsolationLevel
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Manages database configurations and provides utility methods for database maintenance,
 * serving as a centralized point for setting up database connections, and other
 * database-related configurations.
 *
 * #### References
 * [Exposed](https://github.com/JetBrains/Exposed/wiki)
 */
internal object DatabaseService {
    private val tracer: Tracer = Tracer<DatabaseService>()

    /** The database instance held by the service. */
    lateinit var database: Database
        private set

    /** The HikariCP DataSource instance held by the service. */
    var hikariDataSource: HikariDataSource? = null
        private set

    /**
     * Initializes the database connection based on the provided mode and database type.
     *
     * @receiver [SchemaBuilder] Optional lambda to setup the database schema.
     *
     * @param settings The [DatabaseSettings] to be used to configure the database.
     * @param environment The runtime environment.
     * @param isolationLevel The isolation level to use for the database transactions.
     * @param telemetryRegistry Optional metrics registry for telemetry monitoring.
     * @param schemaSetup Optional lambda to setup the database schema.
     */
    fun init(
        settings: DatabaseSettings,
        environment: EnvironmentType,
        isolationLevel: IsolationLevel = IsolationLevel.TRANSACTION_REPEATABLE_READ,
        telemetryRegistry: PrometheusMeterRegistry? = null,
        schemaSetup: SchemaBuilder.() -> Unit = {}
    ) {
        buildDatabase(settings = settings)

        // Creates a HikariCP DataSource if the connection pool size is set.
        hikariDataSource = if (settings.connectionPoolSize > 0) {
            DatabasePooling.createDataSource(
                settings = settings,
                isolationLevel = isolationLevel,
                telemetryRegistry = telemetryRegistry
            )
        } else {
            null
        }

        // Establishes a database connection.
        val databaseInstance: Database = connectDatabase(
            settings = settings,
            isolationLevel = isolationLevel,
            datasource = hikariDataSource
        )

        schemaSetup.let {
            tracer.info("Setting database schema.")
            val schemaBuilder = SchemaBuilder()
            schemaSetup.invoke(schemaBuilder)
            setupDatabaseSchema(
                environment = environment,
                settings = settings,
                database = databaseInstance,
                schemaBuilder = schemaBuilder
            )
        }

        database = databaseInstance

        tracer.info("Database ready.")
    }

    /**
     * Opens a database connection using the provided settings.
     *
     * @param settings The database settings to use.
     * @param datasource Optional HikariCP DataSource to use for the connection.
     * @return The database instance.
     */
    private fun connectDatabase(
        settings: DatabaseSettings,
        isolationLevel: IsolationLevel,
        datasource: HikariDataSource? = null
    ): Database {
        val databaseConfig = DatabaseConfig {
            defaultIsolationLevel = isolationLevel.id
            defaultMaxAttempts = settings.transactionMaxAttempts
            defaultMinRetryDelay = settings.transactionMinRetryDelayMs
            warnLongQueriesDuration = settings.warnLongQueriesDurationMs
        }

        // If a configured DataSource is provided, use it to create the connection.
        datasource?.let {
            return Database.connect(
                datasource = it,
                databaseConfig = databaseConfig
            )
        }

        // If no DataSource is provided, use the JDBC connection details to create the connection.
        return Database.connect(
            url = settings.jdbcUrl,
            driver = settings.jdbcDriver,
            user = settings.username.orEmpty(),
            password = settings.password.orEmpty(),
            databaseConfig = databaseConfig
        )
    }

    /**
     * Creates the database schema if such does not exist.
     *
     * @param settings The target [DatabaseSettings] to be used for the migration.
     * @param environment The runtime environment.
     * @param database The database instance to use.
     * @param schemaBuilder The schema builder to generate the database schema.
     */
    private fun setupDatabaseSchema(
        settings: DatabaseSettings,
        environment: EnvironmentType,
        database: Database,
        schemaBuilder: SchemaBuilder
    ) {
        if (settings.updateSchemaEnvironments.contains(environment)) {
            transaction(db = database) {
                if (settings.useMigrations) {
                    runMigrations(settings = settings)
                } else {
                    schemaBuilder.createTables()
                }
            }
        } else {
            tracer.info("Database schema update skipped for environment: $environment.")
        }
    }

    /**
     * Applies database migrations with the provided database connection details
     * by running the migration scripts found in the default locations.
     *
     * Note that this should never be part of the server execution.
     * Instead, should be decoupled and be executed as a completely independent maintenance step.
     * It is added here just as an example to show how to use Flyway.
     *
     * It is assumed that the migration scripts are located in the `db/migration`
     * directory within the `resources` folder.
     *
     * @param settings The target [DatabaseSettings] to be used for the migration.
     */
    private fun runMigrations(settings: DatabaseSettings) {
        val flyway: Flyway = Flyway.configure().dataSource(
            settings.jdbcUrl,
            settings.username,
            settings.password
        ).load()

        // Repair the schema history to fix any inconsistencies.
        flyway.repair()

        flyway.info().pending().let { pending ->
            if (pending.isEmpty()) {
                tracer.info("No migrations to apply.")
            } else {
                val migrations: String = pending.joinToString(separator = "\n") { migration ->
                    "Version: ${migration.version}. " +
                            "Description: ${migration.description}. " +
                            "Type: ${migration.type}. " +
                            "State: ${migration.state}. " +
                            "Script: ${migration.script}"
                }

                tracer.info("Migrations being applied:\n$migrations")
            }
        }

        flyway.migrate()
    }

    /**
     * Checks whether the database is alive.
     */
    fun ping(): Boolean {
        return runCatching {
            transaction(db = database) {
                @Suppress("SqlDialectInspection", "SqlNoDataSourceInspection")
                exec(stmt = "SELECT 1;")
                return@transaction true
            }
        }.getOrElse { error ->
            tracer.error(message = "Database is not alive.", cause = error)
            return@getOrElse false
        }
    }

    /**
     * Builds the database location directory if the database is a local file.
     */
    private fun buildDatabase(settings: DatabaseSettings) {
        require(settings.path.isNotBlank()) { "Database path is required." }

        if (settings.isLocalFile) {
            val currentWorkingPath: Path = Paths.get("").toAbsolutePath()
            val targetPath: Path = currentWorkingPath.resolve(settings.path)
            Files.createDirectories(targetPath)
        } else {
            tracer.debug("Database is not a local file. Path: ${settings.path}")
        }
    }

    /**
     * Closes the database connection.
     * Primarily used for testing purposes.
     */
    fun close() {
        hikariDataSource?.close()
    }

    /**
     * Builder class for setting up the database schema.
     *
     * Setting up the schema is optional, as it can be created also by migrations.
     */
    class SchemaBuilder {
        private val tables: MutableList<Table> = mutableListOf()

        /**
         * Adds a table to the schema. If the table already exists, it will be ignored.
         *
         * @param table The table to add.
         */
        fun addTable(table: Table) {
            tables.add(table)
        }

        fun createTables() {
            if (tables.isNotEmpty()) {
                SchemaUtils.create(tables = tables.toTypedArray())
            }
        }
    }
}
