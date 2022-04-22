/*
 * Copyright (c) 2020-2022, GrieferGames, Valaphee.
 * All rights reserved.
 */

dependencies {
    implementation(project(":synergy-api"))
    implementation("com.google.protobuf:protobuf-kotlin:3.20.0")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")
}

java.sourceSets.getByName("main").java.srcDir("build/generated/source/proto/main/java")

/*protobuf { protobuf.protoc { artifact = "com.google.protobuf:protoc:3.19.4" } }*/
