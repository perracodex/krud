/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

group = "krud.base"
version = "1.0.0"

dependencies {
    detektPlugins(libs.detekt.formatting)

    implementation(libs.exposed.core)
    implementation(libs.exposed.crypt)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.exposed.pagination)

    implementation(libs.google.phonenumber)

    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    implementation(libs.koin.test)

    implementation(libs.kopapi)

    implementation(libs.kotlinx.atomicfu)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization)

    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.autoHeadResponse)
    implementation(libs.ktor.server.caching.headers)
    implementation(libs.ktor.server.call.id)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.compression)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.defaultHeaders)
    implementation(libs.ktor.server.doubleReceive)
    implementation(libs.ktor.server.forwardedHeader)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.html.builder)
    implementation(libs.ktor.server.http.redirect)
    implementation(libs.ktor.server.hsts)
    implementation(libs.ktor.server.rateLimit)
    implementation(libs.ktor.server.sse)
    implementation(libs.ktor.server.statusPages)
    implementation(libs.ktor.server.tests)
    implementation(libs.ktor.server.thymeleaf)

    implementation(libs.ktor.config)

    implementation(libs.logback.classic)

    implementation(libs.micrometer.metrics)
    implementation(libs.micrometer.metrics.prometheus)

    implementation(libs.shared.commons.codec)

    implementation(libs.typesafe.config)

    testImplementation(libs.test.kotlin.junit)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.mockito.kotlin)
}
