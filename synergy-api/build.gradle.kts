/*
 * Copyright (c) 2020-2022, GrieferGames, Valaphee.
 * All rights reserved.
 */

plugins { id("com.github.johnrengelman.shadow") }

dependencies {
    implementation(project(":synergy-component"))
    implementation(project(":synergy-component-cv"))
    implementation(project(":synergy-component-input"))
    implementation(project(":synergy-component-proxy"))
    /*implementation(project(":synergy-component-proxy-bgs"))*/
    implementation(project(":synergy-component-proxy-http"))
    implementation(project(":synergy-component-proxy-mcbe"))
    implementation(project(":synergy-component-proxy-tcp"))
    implementation("com.fasterxml.jackson.module:jackson-module-guice:2.13.2")
    implementation("com.hubspot.jackson:jackson-datatype-protobuf:0.9.12")
    implementation("io.ktor:ktor-server-call-logging:2.0.1")
    implementation("io.ktor:ktor-server-compression:2.0.1")
    implementation("io.ktor:ktor-server-content-negotiation:2.0.1")
    implementation("io.ktor:ktor-server-core:2.0.1")
    implementation("io.ktor:ktor-server-default-headers:2.0.1")
    implementation("io.ktor:ktor-server-locations:2.0.1")
    implementation("io.ktor:ktor-server-netty:2.0.1")
    implementation("io.ktor:ktor-server-websockets:2.0.1")
    implementation("io.netty:netty-tcnative:2.0.51.Final")
    implementation("io.netty:netty-tcnative-boringssl-static:2.0.51.Final")
    implementation("io.netty:netty-tcnative-boringssl-static:2.0.51.Final:windows-x86_64")
    implementation("org.apache.logging.log4j:log4j-core:2.17.2")
    implementation("org.apache.logging.log4j:log4j-iostreams:2.17.2")
    implementation("org.apache.logging.log4j:log4j-jul:2.17.2")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.17.2")
    implementation("org.graalvm.js:js:22.0.0.2")
    implementation("org.graalvm.sdk:graal-sdk:22.0.0.2")
    implementation("org.graalvm.truffle:truffle-api:22.0.0.2")
}

tasks {
    jar { manifest { attributes(mapOf("Main-Class" to "com.valaphee.synergy.MainKt")) } }

    shadowJar { archiveName = "synergy.jar" }
}
