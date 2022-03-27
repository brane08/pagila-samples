plugins {
    id("org.jetbrains.kotlin.jvm")
    id("io.ktor.plugin") version "2.3.5"
}

group = "com.github.brane08.pagila"
version = "1.0.0"

repositories {
    mavenLocal()
    mavenCentral()
}

val ktorVersion: String by project
val kotlinVersion: String by project
val kodeinVersion: String by project
val exposedVersion: String by project
val logbackVersion: String by project
val hikariVersion: String by project
val postgresVersion: String by project

dependencies {
    implementation(project(":core-api"))
    implementation(project(":data-exposed"))

    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-config-yaml:$ktorVersion")
    implementation("org.kodein.di:kodein-di-jvm:$kodeinVersion")
    implementation("org.kodein.di:kodein-di-framework-ktor-server-jvm:$kodeinVersion")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")
    runtimeOnly("org.postgresql:postgresql:$postgresVersion")

    testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}

kotlin {
    target {
        compilations.configureEach {
            kotlinOptions {
                jvmTarget = "20"
            }
        }
    }
}

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}