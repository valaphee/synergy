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

package com.valaphee.synergy.bgs

import com.fasterxml.jackson.module.kotlin.readValue
import com.valaphee.synergy.keyStore
import com.valaphee.synergy.util.occurrencesOf
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.cli.multiple
import kotlinx.cli.required
import okhttp3.internal.toHexString
import org.apache.logging.log4j.LogManager
import org.bouncycastle.asn1.ASN1Sequence
import java.io.File
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import java.util.Base64

/**
 * @author Kevin Ludwig
 */
@OptIn(ExperimentalCli::class)
object PatchSecuritySubcommand : Subcommand("bgs-patch-security", "Patches the security module, and updates the certificate bundle") {
    private val input by option(ArgType.String, "input", "i", "Input file").required()
    private val output by option(ArgType.String, "output", "o", "Output file")
    private val aliases by option(ArgType.String, "alias", "a", "Certificate alias").multiple()

    override fun execute() {
        val inputFile = File(input)
        log.info("Patching {}", inputFile)

        val serverCertificates = aliases.mapNotNull { alias -> keyStore.getCertificate(alias)?.let { alias to it } }

        val bytes = inputFile.readBytes()
        bytes.occurrencesOf(key.modulus.toByteArray().swap().copyOf(256)).singleOrNull()?.let { modulusIndex ->
            log.info("Modulus found at 0x{}", modulusIndex.toHexString().uppercase())
            bytes.occurrencesOf(prefix.toByteArray()).singleOrNull()?.let { certificateBundleIndex ->
                log.info("Certificate bundle found at 0x{}", certificateBundleIndex.toHexString().uppercase())

                val certificateBundleEnd = bytes.occurrencesOf(infix.toByteArray()).single() + 1
                val certificateBundle = objectMapper.readValue<CertificateBundle>(bytes.copyOfRange(certificateBundleIndex, certificateBundleEnd))
                val certificateBundleBytes = objectMapper.writeValueAsBytes(CertificateBundle(certificateBundle.created, serverCertificates.map { CertificateBundle.UriKeyPair(it.first, (ASN1Sequence.fromByteArray(it.second.encoded) as ASN1Sequence).hash()) }, serverCertificates.map { CertificateBundle.UriKeyPair(it.first, (ASN1Sequence.fromByteArray(it.second.encoded) as ASN1Sequence).hash()) }, listOf(CertificateBundle.RawCertificate("-----BEGIN CERTIFICATE-----${base64Encoder.decode(keyStore.getCertificate("synergy").encoded)}-----END CERTIFICATE-----")), listOf((ASN1Sequence.fromByteArray(keyStore.getCertificate("synergy").encoded) as ASN1Sequence).hash())))
                val certificateBundleSize = certificateBundleEnd - certificateBundleIndex
                if (certificateBundleSize >= certificateBundleBytes.size) {
                    val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
                    val publicKey = keyPair.public as RSAPublicKey
                    publicKey.modulus.toByteArray().swap().copyInto(bytes, modulusIndex, endIndex = 256)
                    publicKey.publicExponent.toByteArray().swap().copyInto(bytes, modulusIndex + 256)
                    log.info("Modulus and exponent overwritten")

                    val certificateBundleBytesPadded = ByteArray(certificateBundleSize) { 0x20 }
                    certificateBundleBytes.copyInto(certificateBundleBytesPadded)
                    certificateBundleBytesPadded.copyInto(bytes, certificateBundleIndex)

                    Signature.getInstance("SHA256withRSA").apply {
                        initSign(keyPair.private)
                        update(certificateBundleBytesPadded)
                        update(module.toByteArray())
                    }.sign().swap().copyInto(bytes, certificateBundleEnd + 4)
                    log.info("Signed certificate bundle overwritten (size: {}, new size: {})", certificateBundleSize, certificateBundleBytes.size)
                } else log.warn("Unable to patch, too large")
            }
            File(output ?: input).writeBytes(bytes)
        } ?: log.warn("Unable to find modulus")
    }

    private val log = LogManager.getLogger(PatchSecuritySubcommand::class.java)
    private const val prefix = "{\"Created\":"
    private const val infix = "}NGIS"
    private val base64Encoder = Base64.getDecoder()
}
