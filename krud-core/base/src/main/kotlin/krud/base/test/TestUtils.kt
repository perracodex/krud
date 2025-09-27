/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.base.test

import io.ktor.server.config.*
import io.ktor.server.testing.*
import krud.base.settings.AppSettings
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import java.io.File

/**
 * Common utilities for unit testing.
 */
public object TestUtils {

    /**
     * Loads the application settings for testing.
     */
    public fun loadSettings() {
        testApplication {
            environment {
                config = ApplicationConfig(configPath = "application.conf")
            }
            application {
                AppSettings.load(this)
            }
        }
    }

    /**
     * Sets up Koin for testing.
     *
     * @param modules The modules to load.
     */
    public fun setupKoin(modules: List<Module> = emptyList()) {
        GlobalContext.startKoin {
            modules(modules)
        }
    }

    /**
     * Tears down the testing environment.
     */
    public fun tearDown() {
        stopKoin()

        val tempRuntime = File(AppSettings.runtime.workingDir)
        if (tempRuntime.exists()) {
            tempRuntime.deleteRecursively()
        }
    }
}
