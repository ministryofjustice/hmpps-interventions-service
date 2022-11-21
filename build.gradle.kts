plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.6.0"
  kotlin("plugin.spring") version "1.7.21"
  id("org.jetbrains.kotlin.plugin.jpa") version "1.7.21"
  id("jacoco")
}

repositories {
  mavenCentral()
}

configurations {
  testImplementation {
    exclude(group = "org.junit.vintage")
  }
}

jacoco {
  toolVersion = "0.8.7"
}

tasks {
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
          minimum = "0.8".toBigDecimal()
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

dependencies {
  // batch processing
  implementation("org.springframework.boot:spring-boot-starter-batch")

  // monitoring and logging
  implementation("io.micrometer:micrometer-registry-prometheus")
  implementation("io.sentry:sentry-spring-boot-starter:6.8.0")
  implementation("io.sentry:sentry-logback:6.7.1")
  implementation("io.github.microutils:kotlin-logging-jvm:3.0.4")
  implementation("net.logstash.logback:logstash-logback-encoder:7.2")
  runtimeOnly("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.0") // needed for OffsetDateTime for AppInsights

  // openapi
  implementation("org.springdoc:springdoc-openapi-ui:1.6.12")

  // notifications
  implementation("uk.gov.service.notify:notifications-java-client:3.19.0-RELEASE")

  // aws
  implementation("software.amazon.awssdk:sns:2.18.9")
  implementation("software.amazon.awssdk:s3:2.18.9")

  // security
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("com.nimbusds:oauth2-oidc-sdk:10.1")

  // database
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.hibernate:hibernate-core:5.6.14.Final")
  implementation("com.vladmihalcea:hibernate-types-55:2.20.0")
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:postgresql:42.5.0")

  // json and csv
  implementation("com.github.java-json-tools:json-patch:1.13")
  implementation("org.apache.commons:commons-csv:1.9.0")

  testImplementation("au.com.dius.pact.provider:junit5spring:4.3.17")
  testImplementation("com.squareup.okhttp3:okhttp:4.10.0")
  testImplementation("com.squareup.okhttp3:mockwebserver:4.10.0")
  testImplementation("org.mockito:mockito-inline:4.9.0")
}
