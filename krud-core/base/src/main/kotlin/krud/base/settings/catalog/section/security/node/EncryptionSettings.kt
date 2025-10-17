/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.base.settings.catalog.section.security.node

import kotlinx.serialization.Serializable
import krud.base.settings.catalog.section.security.SecuritySettings

/**
 * Encryption key settings.
 *
 * @property atRest Settings related to encryption at rest.
 * @property atTransit Settings related to encryption in transit.
 */
@Serializable
public data class EncryptionSettings internal constructor(
    val atRest: Spec,
    val atTransit: Spec
) {

    /**
     * Configuration settings for specific encryption.
     *
     * @property algorithm Algorithm used for encrypting/decrypting data.
     * @property salt Salt used for encrypting/decrypting data.
     * @property key Secret key for encrypting/decrypting data.
     * @property sign Signature key to sign the encrypted data.
     */
    @Serializable
    public data class Spec internal constructor(
        val algorithm: String,
        val salt: String,
        val key: String,
        val sign: String
    ) {
        init {
            require(algorithm.isNotBlank()) { "Missing encryption algorithm." }
            require(salt.isNotBlank()) { "Missing encryption salt." }
            require(key.isNotBlank() && (key.length >= SecuritySettings.MIN_KEY_LENGTH)) {
                "Invalid encryption key. Must be >= ${SecuritySettings.MIN_KEY_LENGTH} characters long."
            }
            require(sign.isNotBlank() && (sign.length >= SecuritySettings.MIN_KEY_LENGTH)) {
                "Invalid sign key. Must be >= ${SecuritySettings.MIN_KEY_LENGTH} characters long."
            }
        }
    }
}
