/*
 * Copyright (c) 2020-2022, GrieferGames, Valaphee.
 * All rights reserved.
 */

plugins { id("com.github.johnrengelman.shadow") }

dependencies {
    implementation(project(":synergy-api"))
    implementation(project(":synergy-bgs"))
    implementation(project(":synergy-cv"))
    implementation(project(":synergy-http"))
    implementation(project(":synergy-input"))
    implementation(project(":synergy-mcbe"))
    implementation(project(":synergy-tcp"))
    implementation("com.hubspot.jackson:jackson-datatype-protobuf:0.9.12")
    implementation("io.ktor:ktor-server-call-logging-jvm:2.0.0")
    implementation("io.ktor:ktor-server-compression-jvm:2.0.0")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.0.0")
    implementation("io.ktor:ktor-server-core-jvm:2.0.0")
    implementation("io.ktor:ktor-server-default-headers-jvm:2.0.0")
    implementation("io.ktor:ktor-server-locations-jvm:2.0.0")
    implementation("io.ktor:ktor-server-netty-jvm:2.0.0")
    implementation("io.ktor:ktor-server-websockets:2.0.0")
    implementation("io.netty:netty-tcnative:2.0.51.Final")
    implementation("io.netty:netty-tcnative-boringssl-static:2.0.51.Final")
    implementation("io.netty:netty-tcnative-boringssl-static:2.0.51.Final:windows-x86_64")
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
