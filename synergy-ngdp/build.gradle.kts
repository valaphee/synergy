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
import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo
import com.igormaznitsa.jbbp.compiler.conversion.JBBPToJavaConverter
import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPFieldTypeParameterContainer
import com.igormaznitsa.jbbp.io.JBBPBitInputStream
import com.igormaznitsa.jbbp.io.JBBPBitOrder
import com.igormaznitsa.jbbp.model.JBBPAbstractField

dependencies {
    implementation("com.google.guava:guava:31.1-jre")
    implementation("com.igormaznitsa:jbbp:2.0.3")
    implementation("io.netty:netty-buffer:4.1.76.Final")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")
}

java.sourceSets.getByName("main").java.srcDir("build/generated/source/jbbp/main/java")

tasks {
    register("jbbp") {
        doLast {
            val inputDir = File(projectDir, "src/main/jbbp")
            val outputDir = File(buildDir, "generated/source/jbbp/main/java")
            File(inputDir, "com/valaphee/synergy/ngdp").walk().forEach {
                if (it.isFile) {
                    val name = it.relativeTo(inputDir).path.removeSuffix(".hxx").replace('\\', '/').replace('/', '.')
                    val classPackage = name.substring(0, name.lastIndexOf('.'))
                    val className = name.substring(name.lastIndexOf('.') + 1)
                    File(outputDir, "${name.replace('.', '/')}.java").also { it.parentFile.mkdirs() }.writeText(
                        JBBPToJavaConverter.makeBuilder(JBBPParser.prepare(Runtime.getRuntime().exec("""gcc -E -P "${it.path}"""").inputReader().readText()))
                            .setMainClassPackage(classPackage)
                            .setMainClassName(className)
                            .build()
                            .convert()
                    )
                }
            }
        }
    }

    compileJava { dependsOn("jbbp") }
}

private class Int24CustomTypeProcessor : com.igormaznitsa.jbbp.JBBPCustomFieldTypeProcessor {
    override fun getCustomFieldTypes(): Array<String> {
        TODO("Not yet implemented")
    }

    override fun isAllowed(fieldType: JBBPFieldTypeParameterContainer?, fieldName: String?, extraData: Int, isArray: Boolean): Boolean {
        TODO("Not yet implemented")
    }

    override fun readCustomFieldType(`in`: JBBPBitInputStream?, bitOrder: JBBPBitOrder?, parserFlags: Int, customTypeFieldInfo: JBBPFieldTypeParameterContainer?, fieldName: JBBPNamedFieldInfo?, extraData: Int, readWholeStream: Boolean, arrayLength: Int): JBBPAbstractField {
        TODO("Not yet implemented")
    }
}
