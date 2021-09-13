plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "3.3.7"
  kotlin("plugin.spring") version "1.5.30"
}

dependencies {
  // web & security
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

  // aws
  implementation("io.awspring.cloud:spring-cloud-starter-aws-messaging")

  // monitoring & logging
  implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")
}

dependencyManagement {
  imports {
    mavenBom("io.awspring.cloud:spring-cloud-aws-messaging:2.3.2")
  }
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
