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

package com.valaphee.synergy.bnet

import com.fasterxml.jackson.module.kotlin.readValue
import com.valaphee.synergy.keyStore
import okhttp3.internal.toHexString
import org.bouncycastle.asn1.ASN1Sequence
import org.slf4j.LoggerFactory
import java.io.File
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.RSAPublicKey

/**
 * @author Kevin Ludwig
 */
object Security {
    fun patch(file: File) {
        log.info("Patching {}", file)
        val bytes = file.readBytes()
        bytes.occurrencesOf(blizzardKey.modulus.toByteArray().swap().copyOf(256)).singleOrNull()?.let { modulusIndex ->
            log.info("Modulus found at 0x{}", modulusIndex.toHexString().uppercase())
            bytes.occurrencesOf(prefix.toByteArray()).singleOrNull()?.let { certificateBundleIndex ->
                log.info("Certificate bundle found at 0x{}", certificateBundleIndex.toHexString().uppercase())

                val certificateBundleEnd = bytes.occurrencesOf(infix.toByteArray()).single() + 1
                val certificateBundle = objectMapper.readValue<CertificateBundle>(bytes.copyOfRange(certificateBundleIndex, certificateBundleEnd))
                val certificateBundleBytes = objectMapper.writeValueAsBytes(CertificateBundle(certificateBundle.created, certificateBundle.certificates.map { if (it.uri == "eu.actual.battle.net") CertificateBundle.UriKeyPair(it.uri, (ASN1Sequence.fromByteArray(keyStore.getCertificate("eu.actual.battle.net").encoded) as ASN1Sequence).hash()) else it }, certificateBundle.publicKeys.map { if (it.uri == "eu.actual.battle.net") CertificateBundle.UriKeyPair(it.uri, (ASN1Sequence.fromByteArray(keyStore.getCertificate("eu.actual.battle.net").encoded) as ASN1Sequence).hash()) else it }, certificateBundle.signingCertificates, certificateBundle.rootCaPublicKeyShas))
                val certificateBundleSize = certificateBundleEnd - certificateBundleIndex
                if (certificateBundleSize >= certificateBundleBytes.size) {
                    val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
                    val publicKey = keyPair.public as RSAPublicKey
                    publicKey.modulus.toByteArray().swap().copyInto(bytes, modulusIndex, endIndex = 256)
                    publicKey.publicExponent.toByteArray().swap().copyInto(bytes, modulusIndex + 256)
                    log.info("Modulus and exponent overwritten")

                    certificateBundleBytes.copyInto(bytes, certificateBundleIndex)
                    repeat(certificateBundleSize - certificateBundleBytes.size) { bytes[certificateBundleIndex + certificateBundleBytes.size + it] = ' '.code.toByte() }
                    Signature.getInstance("SHA256withRSA").apply {
                        initSign(keyPair.private)
                        update(certificateBundleBytes)
                        update(module.toByteArray())
                    }.sign().swap().copyInto(bytes, certificateBundleEnd + 4)
                    log.info("Signed certificate bundle overwritten (size: {}, new size: {})", certificateBundleSize, certificateBundleBytes.size)
                } else log.warn("Unable to patch, too large")
            }
            file.writeBytes(bytes)
        } ?: log.warn("Unable to find modulus")
    }

    private val log = LoggerFactory.getLogger(Security::class.java)
    private const val prefix = "{\"Created\":"
    private const val infix = "}NGIS"
}
