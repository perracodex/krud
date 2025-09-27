pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // https://github.com/gradle/foojay-toolchains
    // https://github.com/gradle/foojay-toolchains/tags
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "Krud"

include("krud-core:base")
include("krud-core:database")
include("krud-core:access")
include("krud-core")
include("krud-domain:employee")
include("krud-domain:employment")
include("krud-domain")
include("krud-server")

// includeBuild("../Kopapi") {
//    dependencySubstitution {
//        substitute(module("io.github.perracodex:kopapi")).using(project(":"))
//    }
// }

// includeBuild("../ExposedPagination") {
//    dependencySubstitution {
//        substitute(module("io.github.perracodex:exposed-pagination")).using(project(":"))
//    }
// }

