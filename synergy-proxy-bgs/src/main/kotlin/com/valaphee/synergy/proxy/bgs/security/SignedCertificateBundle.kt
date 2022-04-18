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

package com.valaphee.synergy.proxy.bgs.util

import com.fasterxml.jackson.module.kotlin.readValue
import com.valaphee.synergy.proxy.bgs.security.CertificateBundle
import com.valaphee.synergy.proxy.objectMapper
import com.valaphee.synergy.proxy.util.occurrencesOf
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.x509.X509CertificateStructure
import org.bouncycastle.util.encoders.Hex
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * @author Kevin Ludwig
 */
object SignedCertificateBundle {
    private val keyFactory = KeyFactory.getInstance("RSA")
    internal val key = keyFactory.generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlJgdPIKILnrsqpbKQjb62cMYlQ/BS7s2CzQAP0U8BPw6u5UrhgcuvyBX8DPkRXfuHKL1vKPCzM4r76ZpDUTZYk02oMpQUP35WVs9JO9/RPo/MjFS+Fw3LeCPt8YXdBUndp6E9UT1u65hiA8ggQhFZiXVN7GwqJtT4gObUfVQsubVi7yTdhDb/Rpe0oBce0Ffeirv8q4QhJMf1heIZpD3jKShrRI7mrX1jwU1snsr++cP6+Ubc7zKaQ4dsr2Zoj2gH/J1YZ3alZ8fmw6eKDh74xsJR/EY/cydy5js6/kVN1gZWFZYCxOvTRCIHgyz/+gxTvAbfLWkN/DU08Qz5xf/NQIDAQAB"))) as RSAPublicKey
    private val magic = "NGIS".toByteArray()
    internal val module = "Blizzard Certificate Bundle".toByteArray()

    fun sign(privateKey: PrivateKey, certificateBundle: CertificateBundle): ByteArray {
        val certificateBundleBytes = objectMapper.writeValueAsBytes(certificateBundle)
        val signedCertificateBundle = ByteArray(certificateBundleBytes.size + magic.size + 256)
        certificateBundleBytes.copyInto(signedCertificateBundle)
        magic.copyInto(signedCertificateBundle, certificateBundleBytes.size)
        Signature.getInstance("SHA256withRSA").apply {
            initSign(privateKey)
            update(certificateBundleBytes)
            update(module)
        }.sign().swap().copyInto(signedCertificateBundle, certificateBundleBytes.size + 4)
        return signedCertificateBundle
    }

    fun parse(signedCertificateBundle: ByteArray, publicKey: PublicKey = key): Pair<Boolean, CertificateBundle> {
        val signatureOffset = signedCertificateBundle.occurrencesOf(magic).single()
        val certificateBundle = signedCertificateBundle.copyOf(signatureOffset)
        return Signature.getInstance("SHA256withRSA").apply {
            initVerify(publicKey)
            update(certificateBundle)
            update(module)
        }.verify(signedCertificateBundle.copyOfRange(signatureOffset + magic.size, signedCertificateBundle.size).swap()) to objectMapper.readValue(certificateBundle)
    }
}

fun ASN1Sequence.hash() = Hex.toHexString(MessageDigest.getInstance("SHA256").digest(X509CertificateStructure.getInstance(this).subjectPublicKeyInfo.publicKeyData.bytes)).uppercase()

internal fun ByteArray.swap() = apply {
    repeat(size / 2) {
        val value = this[it]
        this[it] = this[size - it - 1]
        this[size - it - 1] = value
    }
}
