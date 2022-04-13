/*
 * Copyright (c) 2022, Valaphee.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    id("com.github.johnrengelman.shadow") version "7.0.0"
    /*id("com.google.protobuf") version "0.8.18"*/
    id("com.palantir.git-version") version "0.12.3"
    kotlin("jvm") version "1.6.20"
    signing
}

group = "com.valaphee"
val versionDetails: groovy.lang.Closure<com.palantir.gradle.gitversion.VersionDetails> by extra
val details = versionDetails()
version = "${details.lastTag}.${details.commitDistance}"

repositories {
    mavenCentral()
    maven("https://repo.codemc.org/repository/maven-public")
    mavenLocal()
}

dependencies {
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.13.1")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("com.google.inject:guice:5.1.0")
    implementation("com.google.protobuf:protobuf-kotlin:3.20.0")
    implementation("com.hubspot.jackson:jackson-datatype-protobuf:0.9.12")
    implementation("com.nimbusds:srp6a:2.1.0")
    implementation("com.valaphee:netcode-mcbe:0.1.20")
    implementation("io.ktor:ktor-client-content-negotiation-jvm:2.0.0")
    implementation("io.ktor:ktor-client-okhttp:2.0.0")
    implementation("io.ktor:ktor-client-jackson:2.0.0")
    implementation("io.ktor:ktor-network-tls-certificates:2.0.0")
    implementation("io.ktor:ktor-serialization-jackson-jvm:2.0.0")
    implementation("io.ktor:ktor-server-call-logging-jvm:2.0.0")
    implementation("io.ktor:ktor-server-compression-jvm:2.0.0")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.0.0")
    implementation("io.ktor:ktor-server-core-jvm:2.0.0")
    implementation("io.ktor:ktor-server-default-headers-jvm:2.0.0")
    implementation("io.ktor:ktor-server-locations-jvm:2.0.0")
    implementation("io.ktor:ktor-server-netty-jvm:2.0.0")
    testImplementation("io.ktor:ktor-server-tests-jvm:2.0.0")
    implementation("io.ktor:ktor-server-websockets:2.0.0")
    implementation("io.netty:netty-tcnative:2.0.51.Final")
    implementation("io.netty:netty-tcnative-boringssl-static:2.0.51.Final")
    implementation("io.netty:netty-tcnative-boringssl-static:2.0.51.Final:windows-x86_64")
    implementation("net.java.dev.jna:jna:5.11.0")
    implementation("net.java.dev.jna:jna-platform:5.11.0")
    implementation("org.apache.logging.log4j:log4j-core:2.17.2")
    implementation("org.apache.logging.log4j:log4j-iostreams:2.17.2")
    implementation("org.apache.logging.log4j:log4j-jul:2.17.2")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.17.2")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")
    implementation("org.graalvm.js:js:22.0.0.2")
    implementation("org.graalvm.sdk:graal-sdk:22.0.0.2")
    implementation("org.graalvm.truffle:truffle-api:22.0.0.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.6.20")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.4")
}

java.sourceSets.getByName("main").java.srcDir("build/generated/source/proto/main/java")

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "16"
        targetCompatibility = "16"
    }

    withType<KotlinCompile>().configureEach { kotlinOptions { jvmTarget = "16" } }

    withType<Test> { useJUnitPlatform() }

    shadowJar { archiveName = "synergy.jar" }
}

/*protobuf { protobuf.protoc { artifact = "com.google.protobuf:protoc:3.19.4" } }*/

application { mainClass.set("com.valaphee.synergy.MainKt") }

signing { useGpgCmd() }
