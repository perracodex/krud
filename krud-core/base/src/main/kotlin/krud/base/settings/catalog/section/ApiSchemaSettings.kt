/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.base.settings.catalog.section

import kotlinx.serialization.Serializable
import krud.base.env.EnvironmentType

/**
 * Contains settings related to Swagger and OpenAPI.
 *
 * @property environments The list of environments under which the documentation is enabled.
 * @property schemaRoot The root endpoint path for API schema documentation.
 * @property openApiEndpoint The endpoint to the OpenAPI specification.
 * @property swaggerEndpoint The endpoint to the Swagger UI.
 * @property redocEndpoint The endpoint path to the Redoc UI.
 */
@Serializable
public data class ApiSchemaSettings internal constructor(
    val environments: List<EnvironmentType>,
    val schemaRoot: String,
    val openApiEndpoint: String,
    val swaggerEndpoint: String,
    val redocEndpoint: String
)
