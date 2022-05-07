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

import com.igormaznitsa.jbbp.JBBPParser
import com.igormaznitsa.jbbp.compiler.conversion.JBBPToJavaConverter

plugins {
    id("com.github.johnrengelman.shadow")
    id("org.openjfx.javafxplugin")
}

dependencies {
    implementation(project(":synergy-ngdp"))
    implementation("com.google.inject:guice:5.1.0")
    implementation("com.igormaznitsa:jbbp:2.0.3")
    implementation("de.codecentric.centerdevice:javafxsvg:1.3.0")
    implementation("io.ktor:ktor-client-content-negotiation:2.0.1")
    implementation("io.ktor:ktor-client-okhttp:2.0.1")
    implementation("io.ktor:ktor-serialization-jackson:2.0.1")
    implementation("io.netty:netty-buffer:4.1.76.Final")
    implementation("no.tornado:tornadofx:1.7.20")
    implementation("org.controlsfx:controlsfx:11.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.6.1")
    implementation("org.jfxtras:jmetro:11.6.15")
}

java.sourceSets.getByName("main").java.srcDir("build/generated/sources/jbbp/main/java")

tasks {
    register("jbbp") {
        doLast {
            val inputDir = File(projectDir, "src/main/jbbp")
            val outputDir = File(buildDir, "generated/sources/jbbp/main/java")
            File(inputDir, "com/valaphee/synergy/ngdp/tank/data").walk().forEach {
                if (it.isFile) {
                    val name = it.relativeTo(inputDir).path.removeSuffix(".hxx").replace('\\', '/').replace('/', '.')
                    val classPackage = name.substring(0, name.lastIndexOf('.'))
                    val className = name.substring(name.lastIndexOf('.') + 1)
                    File(outputDir, "${name.replace('.', '/')}.java").also { it.parentFile.mkdirs() }.writeText(
                        JBBPToJavaConverter.makeBuilder(JBBPParser.prepare(Runtime.getRuntime().exec("""gcc -E -P "${it.path}"""").inputReader().readText()))
                            .setMainClassPackage(classPackage)
                            .setMainClassName(className)
                            .setSuperClass("com.valaphee.synergy.ngdp.tank.Data")
                            .build()
                            .convert()
                    )
                }
            }
        }
    }

    compileJava { dependsOn("jbbp") }

    jar { manifest { attributes(mapOf("Main-Class" to "com.valaphee.synergy.MainKt")) } }

    shadowJar { archiveName = "synergy.jar" }
}

javafx { modules("javafx.controls", "javafx.graphics") }
