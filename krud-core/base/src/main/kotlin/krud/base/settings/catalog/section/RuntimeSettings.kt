/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.base.settings.catalog.section

import kotlinx.serialization.Serializable
import krud.base.env.EnvironmentType

/**
 * Contains settings related to server runtime.
 *
 * @property developmentMode Whether running in development mode.
 * @property machineId The unique machine ID. Used for generating unique IDs for call traceability.
 * @property environment The environment type. Not to be confused with the development mode flag.
 * @property doubleReceiveEnvironments The list of environments where the double receive plugin is enabled.
 * @property workingDir The working directory where files are stored.
 */
@Serializable
public data class RuntimeSettings internal constructor(
    val developmentMode: Boolean = false,
    val machineId: Int,
    val environment: EnvironmentType,
    val doubleReceiveEnvironments: List<EnvironmentType>,
    val workingDir: String
)
