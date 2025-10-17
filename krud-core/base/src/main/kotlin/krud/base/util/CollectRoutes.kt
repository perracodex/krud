/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.base.util

import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

/**
 * Extension function traverse and collect all routes.
 *
 * @return A list of all found routes.
 */
public fun Application.collectRoutes(): List<RouteInfo> {
    val routes: MutableList<RouteInfo> = mutableListOf()

    // A helper function to recursively traverse and collect routes.
    fun RoutingNode.collect() {
        if (this.selector is HttpMethodRouteSelector) {
            val method: String = (this.selector as HttpMethodRouteSelector).method.value
            val path: String = this.extractRoutePath()
            val routeInfo = RouteInfo(path = path, method = method)
            routes.add(routeInfo)
        }
        this.children.forEach { it.collect() }
    }

    // Start collecting from the root route.
    this.routing { }.children.forEach { it.collect() }

    return routes.sortedWith(
        compareBy(
            { it.path },
            { it.method }
        )
    )
}

/**
 * Holds information about a registered route.
 *
 * @property path The path of the route.
 * @property method The HTTP method of the route.
 */
@Serializable
public data class RouteInfo internal constructor(
    val path: String,
    val method: String
)

/**
 * Constructs the full path of a route by aggregating path segments from the current route up to the root,
 * by traversing the parent chain of the current route and collects segments defined by various
 * types of [RouteSelector] types.
 *
 * It builds a full path by piecing together these segments in the order from the root to the current route.
 * The method is designed to ignore segments that do not directly contribute to the path structure,
 * such as HTTP method selectors and trailing slashes.
 *
 * @return A string representing the full path from the root to the current route, starting with a `/`.
 * If the current route is at the root or no path segments are defined, the function returns just `/`.
 */
private fun RoutingNode.extractRoutePath(): String {
    val segments: MutableList<String> = mutableListOf()
    var currentRoute: Route? = this

    // Traverse the parent chain of the current route and collect path segments.
    while (currentRoute is RoutingNode) {
        val selector: RouteSelector = currentRoute.selector

        when (selector) {
            is PathSegmentConstantRouteSelector -> selector.value
            is PathSegmentParameterRouteSelector -> "{${selector.name}}"
            is PathSegmentOptionalParameterRouteSelector -> "{${selector.name}?}"
            is PathSegmentWildcardRouteSelector -> "*"
            is TrailingSlashRouteSelector -> ""  // Ignored for path construction
            is HttpMethodRouteSelector -> ""     // Ignored for path construction
            else -> ""
        }.let { segment ->
            if (segment.isNotEmpty()) {
                segments.add(segment)
            }
        }

        currentRoute = currentRoute.parent
    }

    return segments.asReversed().joinToString(separator = "/", prefix = "/").trimEnd('/')
}
