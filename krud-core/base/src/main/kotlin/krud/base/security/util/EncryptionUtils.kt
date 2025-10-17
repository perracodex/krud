/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.base.security.util

import krud.base.settings.AppSettings
import krud.base.settings.catalog.section.security.node.EncryptionSettings
import org.jetbrains.exposed.v1.crypt.*

/**
 * Utility class for database field encryption.
 */
public object EncryptionUtils {
    private enum class AlgorithmName {
        AES_256_PBE_CBC,
        AES_256_PBE_GCM,
        BLOW_FISH,
        TRIPLE_DES
    }

    /**
     * The type of encryption to use.
     */
    public enum class Type {
        /** Stable encryption for data at rest, such as encrypted database fields. */
        AT_REST,

        /** Transient encryption for data in transit, such as encrypted URLs. */
        AT_TRANSIT
    }

    /**
     * Get the [Encryptor] based on the encryption configuration settings.
     * Used, for example, to encrypt database fields.
     *
     * @param type The target [EncryptionUtils.Type] of encryption to use.
     */
    public fun getEncryptor(type: Type): Encryptor {
        val encryptionSpec: EncryptionSettings.Spec = when (type) {
            Type.AT_REST -> AppSettings.security.encryption.atRest
            Type.AT_TRANSIT -> AppSettings.security.encryption.atTransit
        }

        val algorithm: AlgorithmName = AlgorithmName.valueOf(encryptionSpec.algorithm)
        val key: String = encryptionSpec.key
        val salt: String = encryptionSpec.salt

        return when (algorithm) {
            AlgorithmName.AES_256_PBE_CBC -> Algorithms.AES_256_PBE_CBC(password = key, salt = salt)
            AlgorithmName.AES_256_PBE_GCM -> Algorithms.AES_256_PBE_GCM(password = key, salt = salt)
            AlgorithmName.BLOW_FISH -> Algorithms.BLOW_FISH(key = key)
            AlgorithmName.TRIPLE_DES -> Algorithms.TRIPLE_DES(secretKey = key)
        }
    }

    /**
     * Converts a hexadecimal string to a ByteArray of a specified length.
     * Pads with zeros or truncates as necessary, suitable for key generation in encryption.
     *
     * @param length The desired length of the key in bytes.
     * @return A ByteArray of the specified length.
     */
    public fun String.toByteKey(length: Int): ByteArray {
        val requiredHexLength: Int = length * 2 // Each byte is represented by two hex characters.
        return (if (this.length < requiredHexLength) padEnd(requiredHexLength, padChar = '0') else take(requiredHexLength))
            .chunked(size = 2)
            .map { it.toInt(radix = 16).toByte() }
            .toByteArray()
    }
}
