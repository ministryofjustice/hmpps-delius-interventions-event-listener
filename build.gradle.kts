plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "3.3.14"
  kotlin("plugin.spring") version "1.6.0"
}

dependencies {
  // web & security
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  // bumps for security, until bumped in upstream
  implementation("io.netty:netty-codec:4.1.72.Final")
  implementation("org.apache.logging.log4j:log4j-api:2.15.0")

  // aws
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:1.0.3")
  implementation("software.amazon.awssdk:sns:2.17.16")

  // monitoring & logging
  implementation("io.github.microutils:kotlin-logging-jvm:2.1.16")

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
