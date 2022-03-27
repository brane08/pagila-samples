plugins {
    `java-library`
    id("io.ebean") version "13.23.0"
}

group = "com.github.brane08.pagila"
version = "1.0.0"

repositories {
    mavenLocal()
    mavenCentral()
}

val ebeanVersion: String by project
val mapstructVersion: String by project
val junitVersion: String by project

dependencies {
    api("jakarta.transaction:jakarta.transaction-api:2.0.1")
    api("io.ebean:ebean-api:${ebeanVersion}")

    implementation(project(":core-api"))
    implementation("org.mapstruct:mapstruct:${mapstructVersion}")

    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")

    annotationProcessor("org.mapstruct:mapstruct-processor:${mapstructVersion}")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks {
    test {
        useJUnitPlatform()
    }
}