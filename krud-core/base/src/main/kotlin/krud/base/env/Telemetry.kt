/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.base.env

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.config.MeterFilter
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

/**
 * Utility class for managing Micrometer metrics.
 *
 * The PrometheusMeterRegistry is a Micrometer registry that allows monitoring an application
 * using Prometheus, a popular open-source monitoring system and time series database.
 *
 * #### References
 * - [Prometheus](https://prometheus.io/)
 * - [Micrometer Prometheus](https://micrometer.io/docs/registry/prometheus)
 */
public object Telemetry {
    /**
     * Constant for the metric ID used to track server requests within Ktor applications.
     * This ID is used to identify metrics specifically for HTTP server requests in Prometheus.
     */
    private const val SERVER_REQUESTS_METRIC_ID_NAME: String = "ktor.http.server.requests"

    /**
     * Constant for the key used to retrieve the 'route' tag from a metric.
     * This tag helps identify the specific endpoint or route associated with a metric.
     */
    private const val KEY_TAG_ROUTE: String = "route"

    /**
     * Route prefix for RBAC endpoints.
     * Used to identify metrics related to RBAC (Role-Based Access Control)
     * to potentially exclude them from Prometheus metrics.
     */
    private const val RBAC_ROUTE: String = "/rbac/"

    /**
     * Provides a [PrometheusMeterRegistry] instance for managing application metrics.
     *
     * By default, excludes the metrics collection for the "/rbac" endpoint and any sub-paths.
     */
    public val registry: PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT).apply {
        config().meterFilter(
            MeterFilter.deny { metricId ->
                // Append "/" to ensure proper matching at the end.
                val route: String? = metricId.getTag(KEY_TAG_ROUTE)?.plus("/")

                // Deny rbac endpoint that either starts with the domain
                // or contains the domain modified by middlewares like a Rate Limiter.
                return@deny (metricId.name == SERVER_REQUESTS_METRIC_ID_NAME) &&
                        (route?.startsWith(prefix = RBAC_ROUTE) == true || route?.contains(other = RBAC_ROUTE) == true)
            }
        )
    }

    /**
     * Registers a new counter-metric.
     * Counters monitor monotonically increasing values. Counters may never be reset to a lesser value.
     * If it is necessary to track a value that goes up and down, then should use a Gauge instead.
     *
     * @param name The name of the counter.
     * @param description The description of the counter.
     * @return The registered counter.
     */
    public fun registerCounter(name: String, description: String): Counter =
        Counter.builder(name).description(description).register(registry)

    /**
     * Registers a new timer metric.
     * Timers are intended to track of a large number of short running events.
     * An example would be something like an HTTP request. Though "short running"
     * is a bit subjective, the assumption is that it should be under a minute.
     *
     * @param name The name of the timer.
     * @param description The description of the timer.
     * @return The registered timer.
     */
    public fun registerTimer(name: String, description: String): Timer =
        Timer.builder(name).description(description).register(registry)

    /**
     * Returns the metrics content in Prometheus text format for the response body
     * of an endpoint designated for Prometheus to scrape.
     */
    internal fun scrape(): String = registry.scrape()
}
