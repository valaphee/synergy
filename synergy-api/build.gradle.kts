/*
 * Copyright (c) 2020-2022, GrieferGames, Valaphee.
 * All rights reserved.
 */

dependencies {
    implementation("com.google.guava:guava:31.1-jre")
    api("com.google.inject:guice:5.1.0")
    api("com.valaphee:foundry-math:1.4.0")
    api("io.ktor:ktor-client-content-negotiation-jvm:2.0.0")
    api("io.ktor:ktor-client-okhttp:2.0.0")
    api("io.ktor:ktor-client-jackson:2.0.0")
    api("io.ktor:ktor-network-tls-certificates:2.0.0")
    api("io.ktor:ktor-serialization-jackson-jvm:2.0.0")
    api("io.netty:netty-all:4.1.74.Final")
    implementation("io.netty:netty-tcnative:2.0.51.Final")
    implementation("io.netty:netty-tcnative-boringssl-static:2.0.51.Final")
    implementation("io.netty:netty-tcnative-boringssl-static:2.0.51.Final:windows-x86_64")
    api("net.java.dev.jna:jna:5.11.0")
    api("net.java.dev.jna:jna-platform:5.11.0")
    api("org.apache.logging.log4j:log4j-api:2.17.2")
    api("org.jetbrains.kotlinx:kotlinx-cli:0.3.4")
}
