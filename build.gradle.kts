buildscript {
    dependencies {
        classpath("org.postgresql:postgresql:42.6.0")
    }
}

plugins {
    id("com.github.ben-manes.versions") version "0.49.0"
    id("org.flywaydb.flyway") version "9.22.3"
    id("org.jetbrains.kotlin.jvm") version "1.9.10" apply false
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
//
//flyway {
//    url = "jdbc:postgresql://localhost:5432/prore?currentSchema=academy"
//    user = "postgres"
//    schemas = ["pagila"]
//    locations = ["filesystem:database/*/migrations", "filesystem:database/*/test"]
//}
//
//idea {
//    module {
//        downloadSources = true
//    }
//}