/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.base.settings.catalog.section.security.node.auth

import kotlinx.serialization.Serializable

/**
 * OAuth-authentication settings.
 *
 * @property providerName Name of the OAuth provider.
 * @property redirectCallbackUrl Redirect URL opened when authorization is completed. Must be registered in the OAuth provider.
 * @property authorizeUrl OAuth server authorization page URL. Provided by the OAuth server vendor.
 * @property accessTokenUrl OAuth server access token request domain URL. Provided by the OAuth server vendor.
 * @property clientId Client id parameter. Provided by the OAuth server vendor.
 * @property clientSecret client secret parameter. Provided by the OAuth server vendor.
 * @property defaultScopes List of OAuth scopes used by default. Provided by the OAuth server vendor.
 */
@Serializable
public data class OAuthSettings internal constructor(
    val providerName: String,
    val redirectCallbackUrl: String,
    val authorizeUrl: String,
    val accessTokenUrl: String,
    val clientId: String,
    val clientSecret: String,
    val defaultScopes: List<String>
) {
    init {
        require(providerName.isNotBlank()) { "Missing OAuth provider name." }
        require(redirectCallbackUrl.isNotBlank()) { "Missing OAuth redirect callback Url." }
        require(authorizeUrl.isNotBlank()) { "Missing OAuth authorize Url." }
        require(accessTokenUrl.isNotBlank()) { "Missing OAuth token Url." }
        require(clientId.isNotBlank()) { "Missing OAuth client Id." }
        require(clientSecret.isNotBlank()) { "Missing OAuth client secret." }
        require(defaultScopes.isNotEmpty()) { "Missing OAuth default scopes." }
    }
}
