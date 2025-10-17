/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.base.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.*
import krud.base.settings.AppSettings
import kotlin.time.Duration.Companion.milliseconds

/**
 * Configures the routes [RateLimit] plugin, which defines the maximum allowed calls per period.
 *
 * The [RateLimit] plugin allows limiting the number of requests a client can make within
 * a certain time period. Ktor provides different means for configuring rate limiting.
 * For example, enable rate limiting globally for a whole application or configure
 * different rate limits for different resources. Also, to configure rate limiting based
 * on specific request parameters: an IP address, an API key or access token, and so on.
 *
 * Rate limits must be applied when defining the routes by using the same scope key. Example:
 *```
 * routing {
 *      rateLimit(RateLimitName(RateLimitScope.PUBLIC_API)) {
 *           get("some_endpoint") { ... }
 *      }
 * }
 *```
 *
 * #### References
 * - [Ktor Rate Limit](https://ktor.io/docs/server-rate-limit.html)
 */
public fun Application.configureRateLimit() {

    install(plugin = RateLimit) {
        // Register the public API rate limit.
        register(RateLimitName(name = RateLimitScope.PUBLIC_API.key)) {
            rateLimiter(
                limit = AppSettings.security.constraints.publicApi.limit,
                refillPeriod = AppSettings.security.constraints.publicApi.refillMs.milliseconds
            )
        }

        // Register the private API rate limit.
        register(RateLimitName(name = RateLimitScope.PRIVATE_API.key)) {
            rateLimiter(
                limit = AppSettings.security.constraints.privateApi.limit,
                refillPeriod = AppSettings.security.constraints.privateApi.refillMs.milliseconds
            )
        }

        // Register the token generation rate limit.
        register(RateLimitName(name = RateLimitScope.NEW_AUTH_TOKEN.key)) {
            rateLimiter(
                limit = AppSettings.security.constraints.newToken.limit,
                refillPeriod = AppSettings.security.constraints.newToken.refillMs.milliseconds
            )
        }
    }
}

/**
 * The rate limit scopes used in the application.
 *
 * @property key The key that identifies the rate limit scope.
 */
public enum class RateLimitScope(public val key: String) {
    /** Scope key for authorization tokens. */
    NEW_AUTH_TOKEN(key = "new_auth_token"),

    /** Scope key for the public API. */
    PUBLIC_API(key = "public_api"),

    /** Scope key for the private API. */
    PRIVATE_API(key = "private_api")
}
