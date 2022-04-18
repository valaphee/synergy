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

package com.valaphee.synergy

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.name.Named
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder
import java.io.File
import java.math.BigInteger
import java.net.Socket
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Principal
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Calendar
import javax.net.ssl.ExtendedSSLSession
import javax.net.ssl.KeyManager
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedKeyManager
import kotlin.random.asKotlinRandom

/**
 * @author Kevin Ludwig
 */
class SecurityModule(
    private val keyStoreFile: File
) : AbstractModule() {
    @Singleton
    @Named("key-store")
    @Provides
    fun keyStoreFile() = keyStoreFile

    @Singleton
    @Provides
    fun keyStore(@Named("key-store") keyStoreFile: File): KeyStore = if (!keyStoreFile.exists()) {
        val rootKeyPair = KeyPairGenerator.getInstance("RSA", "BC").apply { initialize(2048) }.generateKeyPair()
        val rootCertificate = JcaX509CertificateConverter().setProvider("BC").getCertificate(JcaX509v3CertificateBuilder(X500Name("CN=Synergy"), BigInteger(random.nextBytes(8)), Calendar.getInstance().apply { add(Calendar.DATE, -1) }.time, Calendar.getInstance().apply { add(Calendar.YEAR, 1) }.time, X500Name("CN=Synergy"), rootKeyPair.public).apply {
            addExtension(Extension.basicConstraints, true, BasicConstraints(true))
            addExtension(Extension.subjectKeyIdentifier, false, JcaX509ExtensionUtils().createSubjectKeyIdentifier(rootKeyPair.public))
        }.build(JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(rootKeyPair.private)))
        KeyStore.getInstance("PKCS12", "BC").apply {
            load(null, null)
            setKeyEntry("synergy", rootKeyPair.private, null, arrayOf(rootCertificate))
            keyStoreFile.outputStream().use { store(it, "".toCharArray()) }
        }
    } else KeyStore.getInstance("PKCS12", "BC").apply { keyStoreFile.inputStream().use { load(it, "".toCharArray()) } }

    @Singleton
    @Provides
    fun keyManager(@Named("key-store") keyStoreFile: File, keyStore: KeyStore): KeyManager = object : X509ExtendedKeyManager() {
        override fun getClientAliases(keyType: String, issuers: Array<out Principal>?) = null

        override fun chooseClientAlias(keyType: Array<out String>, issuers: Array<out Principal>?, socket: Socket?) = null

        override fun getServerAliases(keyType: String, issuers: Array<out Principal>?) = null

        override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal>?, socket: Socket?) = null

        override fun chooseEngineServerAlias(keyType: String, issuers: Array<out Principal>?, engine: SSLEngine): String? {
            val session = engine.handshakeSession
            return if (session is ExtendedSSLSession) (session.requestedServerNames.first() as SNIHostName).asciiName else null
        }

        override fun getCertificateChain(alias: String): Array<X509Certificate> {
            val rootCertificate = keyStore.getCertificate("synergy") as X509Certificate
            val serverCertificate = keyStore.getCertificate(alias) as X509Certificate? ?: run {
                val serverKeyPair = KeyPairGenerator.getInstance("RSA", "BC").apply { initialize(2048) }.generateKeyPair()
                val rootSigner = JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(keyStore.getKey("synergy", null) as PrivateKey)
                val serverCsr = JcaPKCS10CertificationRequestBuilder(X500Name("CN=$alias"), serverKeyPair.public).build(rootSigner)
                val serverCertificate = JcaX509CertificateConverter().setProvider("BC").getCertificate(X509v3CertificateBuilder(JcaX509CertificateHolder(rootCertificate).subject, BigInteger(random.nextBytes(8)), Calendar.getInstance().apply { add(Calendar.DATE, -1) }.time, Calendar.getInstance().apply { add(Calendar.YEAR, 1) }.time, serverCsr.subject, serverCsr.subjectPublicKeyInfo).apply {
                    addExtension(Extension.basicConstraints, true, BasicConstraints(false))
                    addExtension(Extension.authorityKeyIdentifier, false, JcaX509ExtensionUtils().createAuthorityKeyIdentifier(rootCertificate))
                    addExtension(Extension.subjectKeyIdentifier, false, JcaX509ExtensionUtils().createSubjectKeyIdentifier(serverCsr.subjectPublicKeyInfo))
                    addExtension(Extension.subjectAlternativeName, false, GeneralNames(GeneralName(GeneralName.dNSName, alias)))
                }.build(rootSigner))
                keyStore.setKeyEntry(alias, serverKeyPair.private, null, arrayOf(serverCertificate))
                keyStoreFile.outputStream().use { keyStore.store(it, "".toCharArray()) }
                serverCertificate
            }
            return arrayOf(serverCertificate, rootCertificate)
        }

        override fun getPrivateKey(alias: String?) = keyStore.getKey(alias, "".toCharArray()) as PrivateKey
    }

    companion object {
        private val random = SecureRandom().asKotlinRandom()
    }
}
