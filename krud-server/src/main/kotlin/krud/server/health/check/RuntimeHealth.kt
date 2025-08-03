/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.server.health.check

import io.ktor.server.application.*
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import krud.base.env.EnvironmentType
import krud.base.env.HealthCheckApi
import krud.base.settings.catalog.section.RuntimeSettings
import krud.base.util.DateTimeUtils.current

/**
 * Used to check the runtime configuration of the application.
 *
 * @property errors List of errors found during the health check.
 * @property machineId The unique identifier of the machine running the application.
 * @property environment The [EnvironmentType] the application is running in.
 * @property developmentModeEnabled Flag indicating if development mode is enabled.
 * @property utc The current UTC timestamp.
 * @property local The current local timestamp.
 */
@HealthCheckApi
@Serializable
public data class RuntimeHealth private constructor(
    val errors: MutableList<String>,
    val machineId: Int,
    val environment: EnvironmentType,
    val developmentModeEnabled: Boolean,
    @Contextual val utc: Instant,
    val local: LocalDateTime,
) {
    internal constructor(call: ApplicationCall, settings: RuntimeSettings) : this(
        errors = mutableListOf(),
        machineId = settings.machineId,
        environment = settings.environment,
        developmentModeEnabled = call.application.developmentMode,
        utc = Instant.current(),
        local = LocalDateTime.current()
    )

    init {
        val className: String? = this::class.simpleName

        if (machineId == 0) {
            errors.add("$className. Machine ID is not set.")
        }

        if (environment == EnvironmentType.PROD && developmentModeEnabled) {
            errors.add("$className. Development mode flag enabled in $environment.")
        }
    }
}
