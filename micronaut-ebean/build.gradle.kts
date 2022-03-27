plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.micronaut.application") version "4.1.1"
    id("io.micronaut.test-resources") version "4.1.1"
    id("io.micronaut.aot") version "4.1.1"
}

version = "0.1"
group = "com.github.brane08.pagila"

repositories {
    mavenLocal()
    mavenCentral()
}

val ebeanVersion: String by project

dependencies {
    annotationProcessor("io.micronaut.serde:micronaut-serde-processor")
    annotationProcessor("io.micronaut.jaxrs:micronaut-jaxrs-processor")
    annotationProcessor("io.micronaut.validation:micronaut-validation-processor")

    implementation(project(":core-api"))
    implementation(project(":data-ebean"))
    annotationProcessor("io.micronaut.serde:micronaut-serde-processor")
    implementation("io.micronaut.beanvalidation:micronaut-hibernate-validator")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("io.micronaut.sql:micronaut-jdbc-hikari")
    implementation("io.micronaut.jaxrs:micronaut-jaxrs-server")
    implementation("io.micronaut.validation:micronaut-validation")
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("io.ebean:ebean-postgres:${ebeanVersion}")
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.yaml:snakeyaml")
    testImplementation("io.micronaut:micronaut-http-client")
}

application {
    mainClass.set("com.github.brane08.pagila.Application")
}

java {
    sourceCompatibility = JavaVersion.toVersion("20")
    targetCompatibility = JavaVersion.toVersion("20")
}

graalvmNative.toolchainDetection.set(false)
micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("com.github.brane08.pagila.*")
    }
    testResources {
        additionalModules.add("jdbc-postgresql")
    }
    aot {
        // Please review carefully the optimizations enabled below
        // Check https://micronaut-projects.github.io/micronaut-aot/latest/guide/ for more details
        optimizeServiceLoading.set(false)
        convertYamlToJava.set(false)
        precomputeOperations.set(true)
        cacheEnvironment.set(true)
        optimizeClassLoading.set(true)
        deduceEnvironment.set(true)
        optimizeNetty.set(true)
    }
}



