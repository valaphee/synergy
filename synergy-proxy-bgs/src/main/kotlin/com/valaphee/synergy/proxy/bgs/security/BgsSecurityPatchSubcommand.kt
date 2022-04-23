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

package com.valaphee.synergy.proxy.bgs.security

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.inject.Inject
import com.valaphee.synergy.ObjectMapper
import com.valaphee.synergy.proxy.bgs.util.occurrencesOf
import io.ktor.util.encodeBase64
import kotlinx.cli.ArgType
import kotlinx.cli.Subcommand
import kotlinx.cli.multiple
import okhttp3.internal.toHexString
import org.apache.logging.log4j.LogManager
import org.bouncycastle.asn1.ASN1Sequence
import java.io.File
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import kotlin.system.exitProcess

/**
 * @author Kevin Ludwig
 */
class BgsSecurityPatchSubcommand : Subcommand("bgs-security-patch", "Patch security") {
    private val input by argument(ArgType.String, "input", "Input file")
    private val output by argument(ArgType.String, "output", "Output file")
    private val aliases by option(ArgType.String, "alias", null, "Certificate alias").multiple()

    @Inject private lateinit var keyStore: KeyStore

    override fun execute() {
        val inputFile = File(input)
        if (inputFile.exists()) {
            log.info("Patching {}", inputFile)

            val bytes = inputFile.readBytes()
            bytes.occurrencesOf(SignedCertificateBundle.key.modulus.toByteArray().swap().copyOf(256)).singleOrNull()?.let { modulusIndex ->
                log.info("Modulus found at 0x{}", modulusIndex.toHexString().uppercase())
                bytes.occurrencesOf(prefix.toByteArray()).singleOrNull()?.let { certificateBundleIndex ->
                    log.info("Certificate bundle found at 0x{}", certificateBundleIndex.toHexString().uppercase())

                    val certificateBundleEnd = bytes.occurrencesOf(separator.toByteArray()).single() + 1
                    val certificateBundle = ObjectMapper.readValue<CertificateBundle>(bytes.copyOfRange(certificateBundleIndex, certificateBundleEnd))
                    val certificates = aliases.map { alias -> alias to checkNotNull(keyStore.getCertificate(alias)) }
                    val certificateBundleBytes = ObjectMapper.writeValueAsBytes(CertificateBundle(certificateBundle.created, certificates.map { CertificateBundle.UriKeyPair(it.first, (ASN1Sequence.fromByteArray(it.second.encoded) as ASN1Sequence).hash()) }, certificates.map { CertificateBundle.UriKeyPair(it.first, (ASN1Sequence.fromByteArray(it.second.encoded) as ASN1Sequence).hash()) }, listOf(CertificateBundle.RawCertificate("-----BEGIN CERTIFICATE-----${keyStore.getCertificate("synergy").encoded.encodeBase64()}-----END CERTIFICATE-----")), listOf((ASN1Sequence.fromByteArray(keyStore.getCertificate("synergy").encoded) as ASN1Sequence).hash())))
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
                            update(SignedCertificateBundle.module)
                        }.sign().swap().copyInto(bytes, certificateBundleEnd + 4)
                        log.info("Signed certificate bundle overwritten (size: {}, new size: {})", certificateBundleSize, certificateBundleBytes.size)
                    } else log.error("Patch too large")
                }
                File(output).writeBytes(bytes)
            } ?: log.error("Modulus not found")
        } else log.error("Input file {} not found", inputFile)

        exitProcess(0)
    }

    companion object {
        private val log = LogManager.getLogger(BgsSecurityPatchSubcommand::class.java)
        private const val prefix = "{\"Created\":"
        private const val separator = "}NGIS"
    }
}
