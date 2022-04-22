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

package com.valaphee.synergy.proxy.mcbe.pack

import com.fasterxml.jackson.module.kotlin.readValue
import com.valaphee.netcode.mcbe.pack.Content
import com.valaphee.synergy.objectMapper
import kotlinx.cli.ArgType
import kotlinx.cli.Subcommand
import kotlinx.cli.required
import org.apache.logging.log4j.LogManager
import java.io.File
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.system.exitProcess

/**
 * @author Kevin Ludwig
 */
class McbePackDecryptSubcommand : Subcommand("mcbe-pack-decrypt", "Decrypt pack") {
    private val input by argument(ArgType.String, "input", "i", "Input path")
    private val output by argument(ArgType.String, "output", "o", "Output path")
    private val key by option(ArgType.String, "key", null, "Key").required()

    override fun execute() {
        val inputPath = File(input)
        if (inputPath.exists()) {
            val outputPath = File(output)
            if (!outputPath.exists()) outputPath.mkdirs()

            log.info("Decrypting contents.json, using key {}", key)
            File(inputPath, "contents.json").inputStream().use { inputStream ->
                inputStream.skip(0x100)
                File(outputPath, "contents.json").outputStream().use { outputStream ->
                    val cipher = key.encodeToByteArray().let { Cipher.getInstance("AES/CFB8/NoPadding").apply { init(Cipher.DECRYPT_MODE, SecretKeySpec(it, "AES"), IvParameterSpec(it.copyOf(16))) } }
                    val buffer = ByteArray(64)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) cipher?.let { outputStream.write(it.update(buffer, 0, bytesRead)) } ?: outputStream.write(buffer, 0, bytesRead)
                    cipher?.let { outputStream.write(it.doFinal()) }
                }
            }

            objectMapper.readValue<Content>(File(outputPath, "contents.json")).content.forEach {
                it.key?.let { _ -> log.info("Decrypting {}, using key {}", it.path, it.key) } ?: log.info("Copying {}", it.path)
                File(inputPath, it.path).inputStream().use { inputStream ->
                    val outputFile = File(outputPath, it.path)
                    if (!outputFile.parentFile.exists()) outputFile.parentFile.mkdirs()
                    outputFile.outputStream().use { outputStream ->
                        val cipher = it.key?.let { Cipher.getInstance("AES/CFB8/NoPadding").apply { init(Cipher.DECRYPT_MODE, SecretKeySpec(it, "AES"), IvParameterSpec(it.copyOf(16))) } }
                        val buffer = ByteArray(64)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) cipher?.let { outputStream.write(it.update(buffer, 0, bytesRead)) } ?: outputStream.write(buffer, 0, bytesRead)
                        cipher?.let { outputStream.write(it.doFinal()) }
                    }
                }
            }
        } else log.error("Input path {} not found", inputPath)

        exitProcess(0)
    }

    companion object {
        private val log = LogManager.getLogger(McbePackDecryptSubcommand::class.java)
    }
}
