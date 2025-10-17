/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.base.security.hash

import java.security.SecureRandom

/**
 * Represents a secure, randomly generated salt used in hashing operations.
 * This class is critical for adding additional security to hashed values by ensuring
 * that each hash is unique, even if the original values are identical.
 *
 * @property salt The binary representation of the salt.
 * @property length The specified length of the salt, in bytes.
 */
public data class SecureSalt private constructor(val salt: ByteArray, val length: Int) {

    /**
     * Converts the binary salt into a hexadecimal String representation.
     * This can be useful for storage or display purposes.
     *
     * @return A String representing the salt in hexadecimal format.
     */
    public fun saltValueToString(): String {
        return salt.joinToString(separator = "") { "%02x".format(it) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SecureSalt
        return (length == other.length) && salt.contentEquals(other.salt)
    }

    override fun hashCode(): Int = arrayOf(length, salt.contentHashCode()).contentHashCode()

    public companion object {
        /**
         * Factory method to generate a new [SecureSalt] of a specified length.
         * This method uses a cryptographically strong random number generator.
         *
         * @param length The desired length of the salt, in bytes. The default is 16-byte (128-bit).
         * @return A new instance of [SecureSalt] with the specified length.
         */
        public fun generate(length: Int = 16): SecureSalt {
            val salt = ByteArray(length)
            SecureRandom().nextBytes(salt)
            return SecureSalt(salt = salt, length = length)
        }
    }
}
