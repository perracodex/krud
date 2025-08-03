/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

group = "krud.access"
version = "1.0.0"

dependencies {
    implementation(project(":krud-core:base"))
    implementation(project(":krud-core:database"))

    detektPlugins(libs.detekt.formatting)

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)

    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    implementation(libs.koin.test)

    implementation(libs.kopapi)

    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.serialization)

    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.html.builder)
    implementation(libs.ktor.server.rateLimit)
    implementation(libs.ktor.server.sessions)
    implementation(libs.ktor.server.tests)

    implementation(libs.shared.commons.codec)

    testImplementation(libs.test.kotlin.junit)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.mockito.kotlin)
}
