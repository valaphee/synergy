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

plugins { id("com.github.johnrengelman.shadow") }

dependencies {
    implementation(project(":synergy-module"))
    implementation(project(":synergy-module-cv"))
    implementation(project(":synergy-module-input"))
    implementation(project(":synergy-module-proxy"))
    /*implementation(project(":synergy-module-proxy-bgs"))*/
    implementation(project(":synergy-module-proxy-http"))
    implementation(project(":synergy-module-proxy-mcbe"))
    implementation(project(":synergy-module-proxy-tcp"))
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
