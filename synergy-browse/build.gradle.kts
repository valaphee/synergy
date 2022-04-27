/*
 * Copyright (c) 2020-2022, GrieferGames, Valaphee.
 * All rights reserved.
 */

plugins {
    id("com.github.johnrengelman.shadow")
    id("org.openjfx.javafxplugin")
}

dependencies {
    implementation(project(":synergy"))
    implementation(project(":synergy-ngdp"))
    implementation("de.codecentric.centerdevice:javafxsvg:1.3.0")
    implementation("no.tornado:tornadofx:1.7.20")
    implementation("org.apache.commons:commons-vfs2:2.9.0")
    implementation("org.controlsfx:controlsfx:11.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.6.0")
    implementation("org.jfxtras:jmetro:11.6.15")
}

tasks {
    jar { manifest { attributes(mapOf("Main-Class" to "com.valaphee.synergy.MainKt")) } }

    shadowJar { archiveName = "synergy-browse.jar" }
}

javafx { modules("javafx.controls", "javafx.graphics") }
