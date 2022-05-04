/*
 * Copyright (c) 2020-2022, GrieferGames, Valaphee.
 * All rights reserved.
 */

plugins {
    id("com.github.johnrengelman.shadow")
    id("org.openjfx.javafxplugin")
}

dependencies {
    implementation(project(":synergy-ngdp"))
    implementation("com.google.inject:guice:5.1.0")
    implementation("de.codecentric.centerdevice:javafxsvg:1.3.0")
    implementation("io.ktor:ktor-client-content-negotiation:2.0.1")
    implementation("io.ktor:ktor-client-okhttp:2.0.1")
    implementation("io.ktor:ktor-serialization-jackson:2.0.1")
    implementation("io.netty:netty-buffer:4.1.76.Final")
    implementation("no.tornado:tornadofx:1.7.20")
    implementation("org.controlsfx:controlsfx:11.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.6.0")
    implementation("org.jfxtras:jmetro:11.6.15")
}

tasks {
    jar { manifest { attributes(mapOf("Main-Class" to "com.valaphee.synergy.MainKt")) } }

    shadowJar { archiveName = "synergy.jar" }
}

javafx { modules("javafx.controls", "javafx.graphics") }
