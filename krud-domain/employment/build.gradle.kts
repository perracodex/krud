/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

group = "krud.employment"
version = "1.0.0"

dependencies {
    implementation(project(":krud-core"))

    detektPlugins(libs.detekt.formatting)

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.pagination)

    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    implementation(libs.koin.test)

    implementation(libs.kopapi)

    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization)

    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.core)

    implementation(libs.shared.commons.codec)

    implementation(libs.test.kotlin.junit)
    implementation(libs.test.mockk)
    implementation(libs.test.mockito.kotlin)
}
