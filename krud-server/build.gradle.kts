/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

group = "krud.server"
version = "1.0.0"

dependencies {
    implementation(project(":krud-system:core"))
    implementation(project(":krud-system:database"))
    implementation(project(":krud-system:access"))
    implementation(project(":krud-domain:employee"))
    implementation(project(":krud-domain:employment"))

    detektPlugins(libs.detekt.formatting)

    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization)

    implementation(libs.exposed.core)
    implementation(libs.exposed.pagination)

    implementation(libs.kopapi)

    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.html.builder)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.rateLimit)
    implementation(libs.ktor.server.tests)

    implementation(libs.ktor.config)

    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    implementation(libs.koin.test)

    implementation(libs.shared.commons.codec)

    testImplementation(libs.test.kotlin.junit)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.mockito.kotlin)
}