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
import com.valaphee.netcode.mcbe.pack.Manifest
import com.valaphee.synergy.ObjectMapper
import io.netty.buffer.Unpooled
import kotlinx.cli.ArgType
import kotlinx.cli.Subcommand
import kotlinx.cli.default
import kotlinx.cli.multiple
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.RandomAccessFile
import java.security.SecureRandom
import java.util.Random
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.system.exitProcess

/**
 * @author Kevin Ludwig
 */
class McbePackEncryptSubcommand : Subcommand("mcbe-pack-encrypt", "Encrypt pack") {
    private val random = SecureRandom()

    private val input by argument(ArgType.String, "input", "i", "Input path")
    private val output by argument(ArgType.String, "output", "o", "Output path")
    private val key by option(ArgType.String, "key", null, "Key").default(random.nextKey())
    private val ignore by option(ArgType.String, "ignore", null, "Ignore files").multiple().default(listOf("manifest.json", "pack_icon.png", "bug_pack_icon.png"))

    override fun execute() {
        val inputPath = File(input)
        if (inputPath.exists()) {
            val manifest = ObjectMapper.readValue<Manifest>(File(inputPath, "manifest.json"))

            val outputPath = File(output)
            if (!outputPath.exists()) outputPath.mkdirs()

            val content = Content(1, inputPath.walkTopDown().filter { it.isFile }.map {
                val file = it.relativeTo(inputPath).path.replace('\\', '/')
                Content.Entry(file, if (ignore.contains(file)) null else random.nextKey().toByteArray())
            }.toList())
            content.content.forEach {
                it.key?.let { _ -> log.info("Encrypting {}, using key {}", it.path, it.key) } ?: log.info("Copying {}", it.path)
                File(inputPath, it.path).inputStream().use { inputStream ->
                    val outputFile = File(outputPath, it.path)
                    if (!outputFile.parentFile.exists()) outputFile.parentFile.mkdirs()
                    outputFile.outputStream().use { outputStream ->
                        val cipher = it.key?.let { Cipher.getInstance("AES/CFB8/NoPadding").apply { init(Cipher.ENCRYPT_MODE, SecretKeySpec(it, "AES"), IvParameterSpec(it.copyOf(16))) } }
                        val buffer = ByteArray(64)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) cipher?.let { outputStream.write(it.update(buffer, 0, bytesRead)) } ?: outputStream.write(buffer, 0, bytesRead)
                        cipher?.let { outputStream.write(it.doFinal()) }
                    }
                }
            }

            log.info("Encrypting contents.json, using key {}", key)
            val outputBuffer = Unpooled.buffer()
            outputBuffer.writeIntLE(0)
            outputBuffer.writeIntLE(0x9BCFB9FC.toInt())
            outputBuffer.writerIndex(0x10)
            outputBuffer.writeByte(manifest.header.id.toString().length)
            outputBuffer.writeBytes(manifest.header.id.toString().toByteArray())
            outputBuffer.writerIndex(0x100)
            ObjectMapper.writeValue(File(inputPath, "contents.json"), content)
            File(inputPath, "contents.json").inputStream().use { inputStream ->
                val cipher = key.toByteArray().let { Cipher.getInstance("AES/CFB8/NoPadding").apply { init(Cipher.ENCRYPT_MODE, SecretKeySpec(it, "AES"), IvParameterSpec(it.copyOf(16))) } }
                val buffer = ByteArray(64)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) outputBuffer.writeBytes(cipher.update(buffer, 0, bytesRead))
                outputBuffer.writeBytes(cipher.doFinal())
            }
            RandomAccessFile(File(outputPath, "contents.json"), "rw").use {
                it.setLength(0)
                it.channel.use { outputBuffer.readBytes(it, outputBuffer.readableBytes()) }
            }
            outputBuffer.release()
        } else log.error("Input path {} not found", inputPath)

        exitProcess(0)
    }

    companion object {
        private val log = LogManager.getLogger(McbePackEncryptSubcommand::class.java)

        private fun Random.nextKey() = ints(48, 122 + 1).filter { (it <= 57 || it >= 65) && (it <= 90 || it >= 97) }.limit(32).collect(::StringBuilder, StringBuilder::appendCodePoint, StringBuilder::append).toString()
    }
}
