/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.base.util

/**
 * Trims a nullable string, returning the trimmed result if it is not blank,
 * or `null` if the string is `null` or the trimmed result is empty.
 *
 * @return The trimmed string if is not blank, or `null` if the original string is `null` or blank after trimming.
 */
public fun String?.trimOrNull(): String? {
    return this?.trim()?.takeIf { it.isNotBlank() }
}

/**
 * Trims a nullable string, returning the trimmed result if it is not blank,
 * or a specified default value if the string is `null` or blank after trimming.
 *
 * @param defaultValue The default value to return if the string is `null` or blank after trimming.
 * @return The trimmed string if not blank, or the specified default value.
 */
public fun String?.trimOrDefault(defaultValue: String): String {
    return this?.trimOrNull() ?: defaultValue
}
