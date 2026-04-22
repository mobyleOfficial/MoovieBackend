plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
    id("io.ktor.plugin") version "3.1.1"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:2.3.0")
    implementation("io.ktor:ktor-server-netty:2.3.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.0")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.0")
    implementation("io.ktor:ktor-server-status-pages:2.3.0")
    implementation("io.ktor:ktor-server-cors:2.3.0")
    implementation("io.ktor:ktor-server-auth:2.3.0")
    implementation("ch.qos.logback:logback-classic:1.2.10")

    // Ktor Client (for TMDB API calls)
    implementation("io.ktor:ktor-client-core:2.3.0")
    implementation("io.ktor:ktor-client-cio:2.3.0")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.0")
    implementation("io.ktor:ktor-client-logging:2.3.0")

    // Koin for Dependency Injection
    implementation("io.insert-koin:koin-core:3.5.6")
    implementation("io.insert-koin:koin-ktor:3.5.6")
    implementation("io.insert-koin:koin-logger-slf4j:3.5.6")

    testImplementation("io.ktor:ktor-server-tests:2.3.0")
}

configurations.all {
    resolutionStrategy {
        force("io.netty:netty-codec-http:4.1.129.Final")
        force("io.netty:netty-handler:4.1.129.Final")
        force("io.netty:netty-codec:4.1.129.Final")
        force("io.netty:netty-transport:4.1.129.Final")
        force("io.netty:netty-buffer:4.1.129.Final")
        force("io.netty:netty-common:4.1.129.Final")
    }
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("org.example.MainKt")
}
