/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.base.error

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.serialization.json.Json

/**
 * Extension function to build the error detail from
 * the given [AppException] instance, and respond it to a client.
 *
 * @param cause The [AppException] instance to respond with.
 */
public suspend fun ApplicationCall.respondError(cause: AppException) {
    // Set the ETag header with the error code.
    this.response.header(name = HttpHeaders.ETag, value = cause.errorCode)

    // Serialize the error response.
    val json: String = Json.encodeToString<AppException.ErrorResponse>(value = cause.toResponse())

    // Send the serialized error response.
    this.respondText(
        text = json,
        contentType = ContentType.Application.Json,
        status = cause.statusCode
    )
}

/**
 * Extension function to build the error details from the multiple
 * [AppException] instances encapsulated by the given [CompositeAppException],
 * and respond it to a client.
 *
 * @param cause The [CompositeAppException] instance containing multiple errors to respond with.
 */
public suspend fun ApplicationCall.respondError(cause: CompositeAppException) {
    // Set the ETag header with the error codes.
    val etagValue: String = cause.errors.joinToString(separator = ";") { it.errorCode }
    this.response.header(name = HttpHeaders.ETag, value = etagValue)

    val responses: CompositeAppException.ErrorResponses = CompositeAppException.ErrorResponses(
        errors = cause.errors.map { it.toResponse() }
    )

    // Serialize the error responses.
    val json: String = Json.encodeToString(responses)

    // Although each error has its own status code, we respond with a 400 Bad Request.
    // Clients can inspect the individual error codes in the response body.
    this.respondText(
        text = json,
        contentType = ContentType.Application.Json,
        status = HttpStatusCode.BadRequest
    )
}
