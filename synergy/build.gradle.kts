/*
 * Copyright (c) 2020-2022, GrieferGames, Valaphee.
 * All rights reserved.
 */

plugins { id("com.github.johnrengelman.shadow") }

dependencies {
    implementation(project(":synergy-api"))
    implementation(project(":synergy-component-cv"))
    implementation(project(":synergy-component-hid"))
    implementation(project(":synergy-proxy-bgs"))
    implementation(project(":synergy-proxy-mcbe"))
    implementation("com.hubspot.jackson:jackson-datatype-protobuf:0.9.12")
    implementation("io.ktor:ktor-server-call-logging-jvm:2.0.0")
    implementation("io.ktor:ktor-server-compression-jvm:2.0.0")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.0.0")
    implementation("io.ktor:ktor-server-core-jvm:2.0.0")
    implementation("io.ktor:ktor-server-default-headers-jvm:2.0.0")
    implementation("io.ktor:ktor-server-locations-jvm:2.0.0")
    implementation("io.ktor:ktor-server-netty-jvm:2.0.0")
    implementation("io.ktor:ktor-server-websockets:2.0.0")
    implementation("org.apache.logging.log4j:log4j-core:2.17.2")
    implementation("org.apache.logging.log4j:log4j-iostreams:2.17.2")
    implementation("org.apache.logging.log4j:log4j-jul:2.17.2")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.17.2")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")
    implementation("org.graalvm.js:js:22.0.0.2")
    implementation("org.graalvm.sdk:graal-sdk:22.0.0.2")
    implementation("org.graalvm.truffle:truffle-api:22.0.0.2")
}

tasks {
    jar { manifest { attributes(mapOf("Main-Class" to "com.valaphee.synergy.MainKt")) } }

    shadowJar { archiveName = "synergy.jar" }
}
