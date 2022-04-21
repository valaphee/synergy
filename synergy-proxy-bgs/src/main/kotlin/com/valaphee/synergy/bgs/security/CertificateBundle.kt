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

package com.valaphee.synergy.bgs.security

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @author Kevin Ludwig
 */
data class CertificateBundle(
    @get:JsonProperty("Created") val created: Long,
    @get:JsonProperty("Certificates") val certificates: List<UriKeyPair>,
    @get:JsonProperty("PublicKeys") val publicKeys: List<UriKeyPair>,
    @get:JsonProperty("SigningCertificates") val signingCertificates: List<RawCertificate>,
    @get:JsonProperty("RootCAPublicKeys") val rootCaPublicKeyShas: List<String>
) {
    data class UriKeyPair(
        @get:JsonProperty("Uri") val uri: String,
        @get:JsonProperty("ShaHashPublicKeyInfo") val hash: String
    )

    data class RawCertificate(
        @get:JsonProperty("RawData") val data: String
    )
}

