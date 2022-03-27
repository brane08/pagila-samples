pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        id("io.quarkus") version "3.4.3"
    }
}

rootProject.name = "pagila-samples"

include("core-api")
include("data-hibernate")
include("data-ebean")
include("quarkus-hibernate")
include("quarkus-ebean")
include("micronaut-ebean")
include("data-exposed")
include("ktor-exposed")
