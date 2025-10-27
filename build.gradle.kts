/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

plugins {
    application // Required to enable packaging and running the Ktor server as an executable JAR.
    alias(libs.plugins.dokka) // Required for generating documentation.
    alias(libs.plugins.kotlin.jvm) // Required for Kotlin JVM development.
    alias(libs.plugins.ktor) // Required for Ktor server development.
    alias(libs.plugins.kotlin.serialization) apply false // Required for Kotlin Serialization support.
    alias(libs.plugins.detekt) // Required for static code analysis.
}

group = "krud"
version = "1.0.0"

// Ktor plugin configuration for creating a fat JAR.
// A fat JAR packages all dependencies, including the server and external libraries, into a single JAR file.
// This simplifies the process of deploying and running the application, which is why the application plugin is utilized.
ktor {
    fatJar {
        // Name of the output JAR file, reflecting the project group and version.
        archiveFileName.set("$group-$version-all.jar")
    }
}

application {
    // Specify the fully qualified name of the main class for the application.
    // This setting is used to define the entry point for the executable JAR generated
    // by Gradle, which is essential for running the application with 'java -jar' command.
    mainClass.set("krud.server.ApplicationKt")

    // Configure detailed coroutine debug logging.
    // https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-debug/
    // https://github.com/Kotlin/kotlinx.coroutines/blob/master/docs/topics/debugging.md
    // Defined in 'gradle.properties' file.
    val enhanceCoroutinesDebugging: Boolean = project.findProperty("enhanceCoroutinesDebugging")?.toString().toBoolean()
    if (enhanceCoroutinesDebugging) {
        applicationDefaultJvmArgs = listOf("-Dkotlinx.coroutines.debug=on")
    }
}

// Configuration block for all projects in this multi-project build.
allprojects {

    // Define repositories where dependencies are fetched from.
    repositories {
        // Use Maven Central as the primary repository for fetching dependencies.
        mavenCentral()

        // Used to include locally published libraries. Useful for testing libraries
        // that are built and published locally.
        mavenLocal()
    }
}

// Defined in 'gradle.properties' file.
val disableOptimizations: Boolean = project.findProperty("disableOptimizations")?.toString().toBoolean()

// Configuration block applied to all subprojects within the multi-project setup.
subprojects {

    // Apply common plugins necessary for Kotlin development to all subprojects.
    // This includes the Kotlin JVM and Kotlin Serialization plugins, which are
    // retrieved dynamically from the version catalog defined in the root project.
    apply {
        plugin(rootProject.libs.plugins.dokka.get().pluginId)
        plugin(rootProject.libs.plugins.kotlin.jvm.get().pluginId)
        plugin(rootProject.libs.plugins.kotlin.serialization.get().pluginId)
        plugin(rootProject.libs.plugins.detekt.get().pluginId)
    }

    // Configure the Kotlin JVM toolchain for all subprojects to use JDK version 17.
    // This ensures that all Kotlin compilations in subprojects use the specified JDK version.
    kotlin {
        jvmToolchain(jdkVersion = 17)

        // Enable explicit API mode for all subprojects.
        // https://github.com/Kotlin/KEEP/blob/master/proposals/explicit-api-mode.md
        // https://kotlinlang.org/docs/whatsnew14.html#explicit-api-mode-for-library-authors
        explicitApi()

        compilerOptions {
            if (disableOptimizations) {
                // Add '-Xdebug' flag to disable local variable optimizations when debugging.
                // WARNING: Never add this flag in production as it can cause memory leaks.
                freeCompilerArgs.add("-Xdebug")
            }

            extraWarnings.set(true)
            freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
            freeCompilerArgs.add("-opt-in=kotlin.uuid.ExperimentalUuidApi")
            freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
        }
    }

    // Configure Detekt for static code analysis.
    detekt {
        buildUponDefaultConfig = true
        allRules = true
        config.setFrom("$rootDir/config/detekt/detekt.yml")
    }

    // Configure Detekt task reports.
    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        reports {
            html.required.set(true)
            xml.required.set(false)
            sarif.required.set(true)
        }
    }
}

dependencies {
    implementation(project(":krud-server"))
}

/** Part of the fat JAR workflow: Task to copy the SSL keystore file for secure deployment. */
val copyKeystoreTask: TaskProvider<Copy> by tasks.registering(Copy::class) {
    from(".certificate/ktor.localhost.p12")
    into("build/libs")
    doFirst {
        println(
            "Copying keystore from " +
                    "${project.layout.projectDirectory}/.certificate/ktor.localhost.p12 " +
                    "to ${project.layout.buildDirectory}/libs."
        )
    }
}

/** Part of the fat JAR workflow: Ensures the SSL keystore file is placed in the build output after the fat JAR creation. */
tasks.named("buildFatJar") {
    finalizedBy(copyKeystoreTask)
    doLast {
        println("Finished building fat JAR.")
    }
}

// Part of the fat JAR workflow: Ensures the keystore copying task is completed before running the fat JAR.
tasks.named("startShadowScripts") {
    dependsOn(copyKeystoreTask)
}
tasks.named("startScripts") {
    dependsOn(copyKeystoreTask)
}
