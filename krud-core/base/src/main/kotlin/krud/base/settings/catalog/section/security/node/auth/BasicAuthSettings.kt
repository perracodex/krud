/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.base.settings.catalog.section.security.node.auth

import kotlinx.serialization.Serializable

/**
 * Configuration parameters for HTTP authentication mechanisms.
 *
 * @property providerName Name of the authentication provider.
 * @property realm Security realm for the HTTP authentication, used to differentiate between protection spaces.
 */
@Serializable
public data class BasicAuthSettings internal constructor(
    val providerName: String,
    val realm: String,
)
