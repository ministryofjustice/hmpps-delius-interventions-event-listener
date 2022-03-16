plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.0.4"
  kotlin("plugin.spring") version "1.6.10"
}

dependencies {
  // web & security
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

  // aws
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:1.1.1")
  implementation("software.amazon.awssdk:sns:2.17.16")

  // monitoring & logging
  implementation("io.github.microutils:kotlin-logging-jvm:2.1.21")

  testImplementation("org.awaitility:awaitility-kotlin:4.1.1")
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(16))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "16"
    }
  }
}
