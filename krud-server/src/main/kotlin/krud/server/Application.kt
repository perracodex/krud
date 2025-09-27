/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.server

import io.ktor.server.application.*
import io.ktor.server.netty.*
import krud.access.plugins.*
import krud.base.plugins.*
import krud.base.settings.AppSettings
import krud.database.plugins.configureDatabase
import krud.server.plugins.configureKoin
import krud.server.plugins.configureRoutes
import krud.server.util.ApplicationsUtils

/**
 * Application main entry point.
 * Launches the Ktor server using Netty as the application engine.
 *
 * #### Continuous Compilation
 * - Command: `./gradlew -t build -x test -i`
 * - [Auto-Reload](https://ktor.io/docs/server-auto-reload.html)
 *
 * #### References
 * - [Choosing an engine](https://ktor.io/docs/server-engines.html)
 * - [Configure an engine](https://ktor.io/docs/server-engines.html#configure-engine)
 * - [Application Monitoring With Server Events](https://ktor.io/docs/server-events.html)
 *
 * @param args Command line arguments passed to the application.
 */
public fun main(args: Array<String>) {
    EngineMain.main(args)
}

/**
 * Application configuration module, responsible for setting up the server with various plugins.
 *
 * #### Attention
 * The order of execution is vital, as certain configurations depend on the initialization
 * of previous plugins. Incorrect ordering can lead to runtime errors or configuration issues.
 *
 * #### References
 * - [Modules](https://ktor.io/docs/server-modules.html)
 * - [Plugins](https://ktor.io/docs/server-plugins.html)
 */
internal fun Application.krudModule() {

    AppSettings.load(application = this)

    configureKoin()

    configureDatabase()

    configureCors()

    configureSecureConnection()

    configureHeaders()

    configureHttp()

    configureSse()

    configureCallLogging()

    configureSerialization()

    configureRateLimit()

    configureBasicAuthentication()

    configureJwtAuthentication()

    configureOAuthAuthentication()

    configureRbac()

    configureSessions()

    configureApiSchema()

    configureRoutes()

    configureMicroMeterMetrics()

    configureStatusPages()

    configureDoubleReceive()

    configureThymeleaf()

    ApplicationsUtils.completeServerConfiguration(application = this)
}
