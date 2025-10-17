/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.base.settings.catalog.section

import kotlinx.serialization.Serializable

/**
 * Contains settings related to how the application is deployed.
 *
 * @property port The network port the server listens to on.
 * @property sslPort The network port the server listens on for secure connections.
 * @property host The network address the server is bound to.
 */
@Serializable
public data class DeploymentSettings internal constructor(
    val port: Int,
    val sslPort: Int,
    val host: String,
)
