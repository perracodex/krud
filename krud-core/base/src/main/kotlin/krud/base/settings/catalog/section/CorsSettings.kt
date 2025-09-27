/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.base.settings.catalog.section

import kotlinx.serialization.Serializable

/**
 * Contains settings related to CORS.
 *
 * Hosts should be in the format:
 *      "host|comma-delimited-schemes|optional comma-delimited-subdomains".
 * Example:
 * ```
 * 	 	"example.com|http,https|api,admin",
 * 	 	"potato.com|https|api",
 * 	 	"somewhere.com|https|"
 * ```
 *
 * If empty list or any of the hosts is '*', then the default is to allow all hosts,
 * in which case schemes and subdomains are ignored even if defined, in addition of
 * any other hosts.
 *
 * @property allowedHosts The list of allowed hosts used in CORS.
 */
@Serializable
public data class CorsSettings internal constructor(
    val allowedHosts: List<String>
) {
    /**
     * Represents a single host configuration.
     *
     * @property host The host, for example "potato.com".
     * @property schemes The allowed schemes in the host, such as "http" and/or "https".
     * @property subDomains The allowed subdomains, such as "api","admin", etc.
     */
    public data class HostConfig internal constructor(
        val host: String,
        val schemes: List<String>,
        val subDomains: List<String>
    )

    /**
     * Returns true if the allowed hosts list is empty
     * or any of the hosts is/or starts with a wildcard.
     */
    public fun allowAllHosts(): Boolean {
        return allowedHosts.isEmpty() or
                (allowedHosts.any { it.startsWith(prefix = "*") })
    }

    internal companion object {
        /**
         * The delimiter used to separate the host, schemes, and subdomains.
         *
         * #### Example
         * "example.com;http,https;api,admin" will be split into 3 sections:
         * - host: "example.com"
         * - schemes: "http,https"
         * - subdomains: "api,admin"
         */
        private const val SECTION_DELIMITER: Char = ';'

        /**
         * The delimiter used to separate multiple values within a section.
         *
         * #### Example
         * "example.com;http,https;api,admin" will split the schemes and subdomains sections into the values:
         * - schemes: "http", "https"
         * - subdomains: "api", "admin"
         */
        private const val VALUE_DELIMITER: Char = ','

        /**
         * Parses a host configuration from a string.
         *
         * @param spec The string to parse.
         *
         * @see [CorsSettings]
         */
        fun parse(spec: String): HostConfig {
            var host = ""
            var schemes: List<String> = emptyList()
            var subDomains: List<String> = emptyList()

            spec.split(SECTION_DELIMITER).forEachIndexed { index, part ->
                when (index) {
                    0 -> host = part.trim()
                    1 -> schemes = part.split(VALUE_DELIMITER).filterNot { it.isBlank() }.map { it.trim() }
                    2 -> subDomains = part.split(VALUE_DELIMITER).filterNot { it.isBlank() }.map { it.trim() }
                }
            }

            return HostConfig(
                host = host,
                schemes = schemes,
                subDomains = subDomains
            )
        }
    }
}
