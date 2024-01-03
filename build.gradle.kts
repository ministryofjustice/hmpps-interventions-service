import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.13.0"
  kotlin("plugin.spring") version "1.9.22"
  id("org.jetbrains.kotlin.plugin.jpa") version "1.9.22"
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
  toolVersion = "0.8.11"
}

tasks {

  withType<KotlinCompile> {
    kotlinOptions {
      jvmTarget = JavaVersion.VERSION_17.toString()
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

dependencyManagement {
  dependencies {
    dependency("net.minidev:json-smart:2.5.0")
  }
}

dependencies {
  // batch processing
  implementation("org.springframework.boot:spring-boot-starter-batch")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.3") // also needed runtime for AppInsights

  // monitoring and logging
  implementation("io.micrometer:micrometer-registry-prometheus")
  implementation("io.sentry:sentry-spring-boot-starter:7.1.0")
  implementation("io.sentry:sentry-logback:7.1.0")
  implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
  implementation("net.logstash.logback:logstash-logback-encoder:7.3")

  runtimeOnly("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.1") // needed for OffsetDateTime for AppInsights

  // openapi
  implementation("org.springdoc:springdoc-openapi-ui:1.7.0")

  // notifications
  implementation("uk.gov.service.notify:notifications-java-client:4.1.1-RELEASE")
  implementation("org.json:json") {
    version {
      strictly("20231013")
    }
  }

  // aws
  implementation("software.amazon.awssdk:sns:2.22.9")
  implementation("software.amazon.awssdk:s3:2.22.9")
  implementation("software.amazon.awssdk:sts:2.22.9")

  // security
  implementation("org.springframework.boot:spring-boot-starter-webflux:3.2.1")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server:3.2.1")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client:3.2.1")
  implementation("com.nimbusds:oauth2-oidc-sdk:11.9")
  implementation("org.springframework.boot:spring-boot-starter-webflux:3.2.1")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server:3.2.1")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client:3.2.1")
  implementation("com.nimbusds:oauth2-oidc-sdk:11.9")

  // database
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.hibernate:hibernate-core:5.6.15.Final")
  implementation("com.vladmihalcea:hibernate-types-55:2.21.1")
  implementation("com.h2database:h2:2.2.224")
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:postgresql:42.7.1")

  // json and csv
  implementation("com.github.java-json-tools:json-patch:1.13")
  implementation("org.apache.commons:commons-csv:1.10.0")

  testImplementation("au.com.dius.pact.provider:junit5spring:4.6.4")
  testImplementation("com.squareup.okhttp3:okhttp:4.12.0")
  testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("org.springframework.batch:spring-batch-test")
  testImplementation("com.github.tomakehurst:wiremock-jre8:3.0.1")
}
