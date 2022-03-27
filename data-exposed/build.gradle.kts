plugins {
    id("org.jetbrains.kotlin.jvm")
}

group = "com.github.brane08.pagila"
version = "1.0.0"

repositories {
    mavenLocal()
    mavenCentral()
}

val exposedVersion: String by project
val junitVersion: String by project

dependencies {
    implementation(project(":core-api"))

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    testImplementation(platform("org.junit:junit-bom:${junitVersion}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
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

tasks {
    test {
        useJUnitPlatform()
    }
}