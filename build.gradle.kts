import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.kotlin.dsl.register

plugins {
    java
    application

    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("io.freefair.lombok") version "9.5.0"
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.1.2")
    }
}

project.group = "app.oengus"
// Version code is year.month.(release num)
project.version = "2026.03.1"

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

application {
    mainClass.set("${project.group}.OengusApplication")
}

repositories {
    mavenCentral()
}

val snippetsDir = file("build/generated-snippets")

extra["snippetsDir"] = snippetsDir

val sentryVersion = "8.29.0"
val mapstructVersion = "1.6.3"

// TODO: gradle version catalog
dependencies {
    // SPRING
    implementation(group = "org.springframework.boot", name = "spring-boot-starter-validation")
    implementation(group = "org.springframework.boot", name = "spring-boot-starter-actuator")
    implementation(group = "org.springframework.boot", name = "spring-boot-starter-data-jpa")
    implementation(group = "org.springframework.boot", name = "spring-boot-starter-security")
    implementation(group = "org.springframework.boot", name = "spring-boot-starter-mail")
    implementation(group = "org.springframework.boot", name = "spring-boot-starter-thymeleaf")
    implementation(group = "org.springframework.boot", name = "spring-boot-starter-web") {
        // remove tomcat as we replace it with undertow
//        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
    }
//    implementation(group = "org.springframework.boot", name = "spring-boot-starter-undertow")
    implementation(group = "org.springframework.boot", name = "spring-boot-starter-cache")
    implementation(group = "org.liquibase", name = "liquibase-core")
    implementation(group = "io.micrometer", name = "micrometer-registry-prometheus")

    // Authentication + OTP
    implementation(group = "com.google.zxing", name = "core", version = "3.5.4")
    implementation(group = "de.taimos", name = "totp", version = "1.0")

    // POSTGRESQL
    implementation(group = "org.postgresql", name = "postgresql", version = "42.7.13")

    // JWT
    implementation(group = "io.jsonwebtoken", name = "jjwt-api", version = "0.13.0")
    runtimeOnly(group = "io.jsonwebtoken", name = "jjwt-impl", version = "0.13.0")
    runtimeOnly(group = "io.jsonwebtoken", name = "jjwt-jackson", version = "0.13.0")

    // APACHE
    implementation(group = "org.apache.commons", name = "commons-lang3", version = "3.20.0")
    implementation(group = "org.apache.commons", name = "commons-csv", version = "1.14.1")

    // FEIGN
    implementation(group = "org.springframework.cloud", name = "spring-cloud-starter-openfeign", version = "5.0.2")

    // JACKSON
    implementation(group = "tools.jackson.core", name = "jackson-core", version = "3.2.2")
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-annotations", version = "2.22")
    implementation(group = "com.fasterxml.jackson.datatype", name = "jackson-datatype-jsr310", version = "2.22.1")

    // documentation
    implementation(group = "org.springdoc", name = "springdoc-openapi-starter-webmvc-api", version = "3.0.3")

    // iCal4J
    implementation(group = "org.mnode.ical4j", name = "ical4j", version = "4.3.0")

    // Paypal
    implementation(group = "com.paypal.sdk", name = "checkout-sdk", version = "2.0.0")

    // OKHTTP
    implementation(group = "com.squareup.okhttp3", name = "okhttp", version = "5.4.0")

    // COUNTRY SUPPORT
    implementation(group = "com.neovisionaries", name = "nv-i18n", version = "1.29")

    // Sentry
    implementation(group = "io.sentry", name = "sentry-spring-boot-4", version = sentryVersion)
    implementation(group = "io.sentry", name = "sentry-logback", version = sentryVersion)

    // security and shit
    implementation("org.apache.logging.log4j:log4j-to-slf4j:2.26.1")
    implementation("org.passay:passay:2.0.0")

    // idk
    implementation("org.javassist:javassist:3.32.0-GA")
    implementation("com.google.code.findbugs:jsr305:3.0.2")

    implementation(group = "com.rabbitmq", name = "amqp-client", version = "5.34.0")
    implementation("org.mapstruct:mapstruct:$mapstructVersion")

    // development tools, will be removed in production builds
//    developmentOnly("org.springframework.boot:spring-boot-devtools")

//    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

    // And of course, we are going to write unit tests.
    testImplementation(group = "org.springframework.boot", name = "spring-boot-starter-validation-test")
    testImplementation(group = "org.springframework.boot", name = "spring-boot-starter-data-jpa-test")
    testImplementation(group = "org.springframework.boot", name = "spring-boot-starter-mail-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(group = "org.springframework.boot", name = "spring-boot-starter-cache-test")
    testImplementation("net.datafaker:datafaker:2.7.0")
    testAnnotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
    testRuntimeOnly("com.h2database:h2:2.4.240")
}

val wrapper: Wrapper by tasks
val compileJava: JavaCompile by tasks

val sourcesForRelease = tasks.register<Copy>("sourcesForRelease") {
    from("src/main/java") {
        include("**/CoreConfiguration.java")

        val items = mapOf(
            "OENGUS_VERSION" to project.version
        )

        filter<ReplaceTokens>(mapOf("tokens" to items))
    }

    into("build/filteredSrc")

    includeEmptyDirs = false
}

val generateJavaSources = tasks.register<SourceTask>("generateJavaSources") {
    val javaSources = sourceSets["main"].allJava.filter {
        !arrayOf("CoreConfiguration.java").contains(it.name)
    }.asFileTree

    source = javaSources + fileTree(sourcesForRelease.get().destinationDir)

    dependsOn(sourcesForRelease)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.test {
    outputs.dir(snippetsDir)

    testLogging {
        showStandardStreams = System.getenv("CI") == null
        exceptionFormat = TestExceptionFormat.FULL
    }
}

compileJava.apply {
    source = generateJavaSources.get().source

    dependsOn(generateJavaSources)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.isIncremental = true
    // NOTE: use -java-parameters for the kotlin compiler
    // see https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-6.1-Release-Notes
    options.compilerArgs = listOf("-Xlint:deprecation", "-Xlint:unchecked", "-parameters")
}

wrapper.apply {
    gradleVersion = "9.3.1"
    distributionType = Wrapper.DistributionType.BIN
}
