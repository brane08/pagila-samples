plugins {
    `java-library`
}

group = "com.github.brane08.pagila"
version = "1.0.0"

repositories {
    mavenLocal()
    mavenCentral()
}

val junitVersion: String by project
val mapstructVersion: String by project
val commonsLangVersion: String by project

dependencies {
    api("org.mapstruct:mapstruct:${mapstructVersion}")
    implementation("org.apache.commons:commons-lang3:${commonsLangVersion}")

    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testCompileOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
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
