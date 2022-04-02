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
    maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
}

dependencies {
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.ktor:ktor-network-tls-certificates:2.0.0-beta-1")
    implementation("io.ktor:ktor-serialization-jackson-jvm:2.0.0-beta-1")
    implementation("io.ktor:ktor-server-call-logging-jvm:2.0.0-beta-1")
    implementation("io.ktor:ktor-server-compression-jvm:2.0.0-beta-1")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.0.0-beta-1")
    implementation("io.ktor:ktor-server-core-jvm:2.0.0-beta-1")
    implementation("io.ktor:ktor-server-default-headers-jvm:2.0.0-beta-1")
    implementation("io.ktor:ktor-server-locations-jvm:2.0.0-beta-1")
    implementation("io.ktor:ktor-server-netty-jvm:2.0.0-beta-1")
    testImplementation("io.ktor:ktor-server-tests-jvm:2.0.0-beta-1")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.6.20")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "16"
        targetCompatibility = "16"
    }

    withType<KotlinCompile>().configureEach { kotlinOptions { jvmTarget = "16" } }

    withType<Test> { useJUnitPlatform() }

    shadowJar { archiveName = "synergy.jar" }
}

application { mainClass.set("com.valaphee.synergy.MainKt") }

signing { useGpgCmd() }
