import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("plugin.spring") version "2.1.20"
  id("org.jetbrains.kotlin.plugin.jpa") version "2.1.20"
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "9.0.2"
  id("jacoco")
  id("project-report")
}

ext["hibernate.version"] = "6.5.3.Final"

configurations {
  testImplementation {
    exclude(group = "org.junit.vintage")
  }
}

jacoco {
  toolVersion = "0.8.13"
}

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

tasks {
  withType<KotlinCompile> {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_21)
    }
  }
  test {
    useJUnitPlatform {
      exclude("**/*PactTest*")
    }
    finalizedBy(jacocoTestReport)
    minHeapSize = "128m"
    maxHeapSize = "512m"
  }

  jacocoTestReport {
    dependsOn(test)

    reports {
      html.required.set(true)
    }
  }

  jacocoTestCoverageVerification {
    violationRules {
      rule {
        limit {
          minimum = "0.7".toBigDecimal()
        }
      }
    }
  }

  register<Test>("pactTestPublish") {
    description = "Run and publish Pact provider tests"
    group = "verification"

    systemProperty("pact.provider.tag", System.getenv("PACT_PROVIDER_TAG"))
    systemProperty("pact.provider.version", System.getenv("PACT_PROVIDER_VERSION"))
    systemProperty("pact.verifier.publishResults", System.getenv("PACT_PUBLISH_RESULTS") ?: "false")

    useJUnitPlatform {
      include("**/*PactTest*")
    }
  }
}

dependencyManagement {
  dependencies {
    dependency("net.minidev:json-smart:2.5.2")
  }
}

repositories {
  maven { url = uri("https://repo.spring.io/milestone") }
  mavenCentral()
}
dependencies {
  // batch processing
  implementation("org.springframework.boot:spring-boot-starter-batch")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.0") // also needed runtime for AppInsights

  // monitoring and logging
  implementation("io.micrometer:micrometer-registry-prometheus")
  implementation("io.sentry:sentry-spring-boot-starter:8.11.1")
  implementation("io.sentry:sentry-logback:8.11.1")
  implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
  implementation("net.logstash.logback:logstash-logback-encoder:8.1")

  runtimeOnly("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.0") // needed for OffsetDateTime for AppInsights

  // openapi
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.8")
  implementation("javax.xml.bind:jaxb-api:2.3.1")
  // notifications
  implementation("uk.gov.service.notify:notifications-java-client:5.2.1-RELEASE")
  implementation("org.json:json") {
    version {
      strictly("20231013")
    }
  }

  // aws
  implementation("software.amazon.awssdk:sns:2.31.36")
  implementation("software.amazon.awssdk:s3:2.31.36")
  implementation("software.amazon.awssdk:sts:2.31.36")

  // security
  implementation("org.springframework.boot:spring-boot-starter-webflux:3.5.0")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server:3.5.0")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client:3.5.0")
  implementation("org.springframework.security:spring-security-crypto:6.5.0")
  implementation("com.nimbusds:oauth2-oidc-sdk:11.25")

  // database
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("com.h2database:h2:2.3.232")
  implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.9.10")

  runtimeOnly("org.postgresql:postgresql:42.7.7")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")

  // json and csv
  implementation("com.github.java-json-tools:json-patch:1.13")
  implementation("org.apache.commons:commons-csv:1.14.0")

  testImplementation("au.com.dius.pact.provider:junit5spring:4.6.17")
  testImplementation("com.squareup.okhttp3:okhttp:5.1.0")
  testImplementation("com.squareup.okhttp3:mockwebserver:5.1.0")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("org.springframework.batch:spring-batch-test")
  testImplementation("org.wiremock:wiremock:3.13.1")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.1")
}
