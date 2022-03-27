plugins {
    `java-library`
}

group = "com.github.brane08.pagila"
version = "1.0.0"

repositories {
    mavenLocal()
    mavenCentral()
}

val hibernateVersion: String by project
val mapstructVersion: String by project

dependencies {
    api("org.hibernate:hibernate-core:${hibernateVersion}")

    implementation(project(":core-api"))
    implementation("org.mapstruct:mapstruct:${mapstructVersion}")

    annotationProcessor("org.mapstruct:mapstruct-processor:${mapstructVersion}")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
