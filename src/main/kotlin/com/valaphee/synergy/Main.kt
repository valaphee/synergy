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

import com.valaphee.synergy.bnet.BNetProxy
import com.valaphee.synergy.tcp.TcpProxy
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.ExtendedKeyUsage
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.KeyPurposeId
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Security
import java.security.cert.X509Certificate
import java.util.Calendar
import kotlin.random.asKotlinRandom

internal lateinit var rootCertificate: X509Certificate
internal lateinit var rootContentSigner: ContentSigner

fun main() {
    Security.addProvider(BouncyCastleProvider())

    val path = File(System.getProperty("user.home"), ".valaphee/synergy").also(File::mkdirs)
    val rootFile = File(path, "root.pfx")
    if (!rootFile.exists()) {
        val rootKeyPair = KeyPairGenerator.getInstance("RSA", "BC").apply { initialize(2048) }.generateKeyPair()
        rootCertificate = JcaX509CertificateConverter().setProvider("BC").getCertificate(JcaX509v3CertificateBuilder(X500Name("CN=Synergy Root CA"), BigInteger(SecureRandom().asKotlinRandom().nextBytes(8)), Calendar.getInstance().apply { add(Calendar.DATE, -1) }.time, Calendar.getInstance().apply { add(Calendar.YEAR, 1) }.time, X500Name("CN=Synergy Root CA"), rootKeyPair.public).apply {
            addExtension(Extension.keyUsage, true, KeyUsage(KeyUsage.cRLSign or KeyUsage.keyCertSign or KeyUsage.keyAgreement or KeyUsage.digitalSignature))
            addExtension(Extension.basicConstraints, true, BasicConstraints(true))
            addExtension(Extension.authorityKeyIdentifier, false, JcaX509ExtensionUtils().createAuthorityKeyIdentifier(rootKeyPair.public))
            addExtension(Extension.subjectKeyIdentifier, false, JcaX509ExtensionUtils().createSubjectKeyIdentifier(rootKeyPair.public))
            addExtension(Extension.extendedKeyUsage, false, ExtendedKeyUsage(arrayOf(KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth)))
        }.build(JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(rootKeyPair.private)))
        KeyStore.getInstance("PKCS12", "BC").apply {
            load(null, null)
            setKeyEntry("synergy-root", rootKeyPair.private, null, arrayOf(rootCertificate))
            rootFile.outputStream().use { store(it, "".toCharArray()) }
        }
        rootContentSigner = JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(rootKeyPair.private)
    } else KeyStore.getInstance("PKCS12", "BC").apply {
        rootFile.inputStream().use { load(it, "".toCharArray()) }
        rootCertificate = getCertificate("synergy-root") as X509Certificate
        rootContentSigner = JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(getKey("synergy-root", null) as PrivateKey)
    }

    val proxies = mutableMapOf<String, Proxy>()
    embeddedServer(Netty) {
        install(ContentNegotiation) { jackson() }

        routing {
            post("/proxy/tcp") {
                val proxy = call.receive<TcpProxy>()
                if (proxies.putIfAbsent(proxy.id, proxy) != null) call.respond(HttpStatusCode.BadRequest)
                if (call.request.queryParameters["autoStart"] == "true") proxy.start()
                call.respond(HttpStatusCode.OK)
            }
            post("/proxy/bnet") {
                val proxy = call.receive<BNetProxy>()
                if (proxies.putIfAbsent(proxy.id, proxy) != null) call.respond(HttpStatusCode.BadRequest)
                if (call.request.queryParameters["autoStart"] == "true") proxy.start()
                call.respond(HttpStatusCode.OK)
            }
            delete("/proxy/{id}") {
                proxies.remove(call.parameters["id"])?.stop() ?: call.respond(HttpStatusCode.NotFound)
                call.respond(HttpStatusCode.OK)
            }
            get("/proxy") { call.respond(proxies.values) }
            get("/proxy/{id}/start") {
                proxies[call.parameters["id"]]?.start() ?: call.respond(HttpStatusCode.NotFound)
                call.respond(HttpStatusCode.OK)
            }
            get("/proxy/{id}/stop") {
                proxies[call.parameters["id"]]?.stop() ?: call.respond(HttpStatusCode.NotFound)
                call.respond(HttpStatusCode.OK)
            }
        }
    }.start(true)
}
