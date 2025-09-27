/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.base.settings.catalog.section.security.node

import kotlinx.serialization.Serializable

/**
 * Security constraints settings.
 *
 * @property publicApi Rate limit specification for the Public API endpoints.
 * @property privateApi Rate limit specification for the Private API endpoints.
 * @property newToken Rate limit specification for the New Authentication Token generation endpoint.
 */
@Serializable
public data class ConstraintsSettings internal constructor(
    val publicApi: LimitSpec,
    val privateApi: LimitSpec,
    val newToken: LimitSpec
) {

    /**
     * Rate limit specification.
     * For example, a limit of 10 requests per second would be represented as:
     * ```
     * LimitSpec(limit = 10, refillMs = 1000)
     * ```
     *
     * @property limit The maximum number of requests allowed within the refill period. Must be > 0.
     * @property refillMs The time period in milliseconds after which the limit is reset. Must be > 0.
     */
    @Serializable
    public data class LimitSpec internal constructor(
        val limit: Int,
        val refillMs: Long
    ) {
        init {
            require(limit > 0) { "Invalid rate limit. Must be > 0." }
            require(refillMs > 0L) { "Invalid rate refill. Must be > 0." }
        }
    }
}
