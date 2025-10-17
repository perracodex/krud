/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.base.error.validator

import krud.base.error.validator.base.IValidator
import krud.base.error.validator.base.ValidationException

/**
 * Verifies if an email address is in the correct format.
 * It adheres to the RFC 5321 specification, which defines the standard format for email addresses.
 *
 * The top-level domain (TLD) must be at least two characters long.
 * The total length of the email address must not exceed 254 characters (as per RFC 5321).
 *
 * The local part of the email (before the '@') allows:
 * ```
 *      • Uppercase and lowercase letters (A-Z, a-z)
 *      • Digits (0-9)
 *      • Characters: dot (.), underscore (_), percent (%), plus (+), hyphen (-)
 *      • Maximum length of 64 characters (as per RFC 5321)
 * ```
 * The domain part of the email (after the '@') can include:
 * ```
 *      • Letters (A-Z, a-z)
 *      • Digits (0-9)
 *      • Hyphens (-)
 * ```
 *
 * Examples of valid email formats:
 * ```
 *      • example@email.com
 *      • user.name+tag+sorting@example.co.uk
 *      • user_name@example.org
 *      • username@example.travel
 *      • user1234@example-company.com
 * ```
 * Examples of invalid email formats:
 * ```
 *      • any-plain-text
 *      • @no-local-part.com
 *      • .email@example.com (local part starts with a dot)
 *      • email.@example.com (local part ends with a dot)
 *      • email..email@example.com (local part has consecutive dots)
 *      • email@example (no top-level domain)
 *      • email@example...com (top-level domain has consecutive dots)
 * ```
 *
 * @see [IValidator]
 * @see [ValidationException]
 */
public object EmailValidator : IValidator<String> {
    /** The maximum length of an email address (as per RFC 5321). */
    public const val MAX_EMAIL_LENGTH: Int = 254

    /** The maximum length of the local part of an email address (as per RFC 5321). */
    private const val MAX_LOCAL_PART_LENGTH: Int = 64

    private const val DOMAIN_SEPARATOR: String = "@"
    private val EMAIL_REGEX: Regex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}$".toRegex()

    public override fun verify(value: String): Result<String> {
        // Check for the maximum length of the entire email address (254 characters).
        if (value.length > MAX_EMAIL_LENGTH) {
            return Result.failure(
                ValidationException(
                    code = "EMAIL_LENGTH_EXCEEDED",
                    message = "Email exceeds the maximum length of $MAX_EMAIL_LENGTH characters: $value"
                )
            )
        }

        // Validate the email address format using a regular expression.
        if (!value.matches(regex = EMAIL_REGEX)) {
            return Result.failure(
                ValidationException(
                    code = "INVALID_EMAIL_FORMAT",
                    message = "Email does not match the required format: $value"
                )
            )
        }

        // Splitting local and domain parts to apply specific checks.
        val parts: List<String> = value.split(DOMAIN_SEPARATOR)
        val localPart: String = parts[0]
        val domainPart: String = parts[1]

        // Check for the maximum length of the local part (64 characters).
        if (localPart.length > MAX_LOCAL_PART_LENGTH) {
            return Result.failure(
                ValidationException(
                    code = "EMAIL_LOCAL_PART_LENGTH_EXCEEDED",
                    message = "Email local part exceeds the maximum length of $MAX_LOCAL_PART_LENGTH characters: $value"
                )
            )
        }

        // Ensure the domain part does not have consecutive dots.
        if (domainPart.contains(other = "..")) {
            return Result.failure(
                ValidationException(
                    code = "EMAIL_DOMAIN_CONSECUTIVE_DOTS",
                    message = "Email domain part contains consecutive dots: $value"
                )
            )
        }

        // Check if the local part starts or ends with a dot or contains consecutive dots.
        if (localPart.startsWith(prefix = ".") || localPart.endsWith(suffix = ".") || localPart.contains(other = "..")) {
            return Result.failure(
                ValidationException(
                    code = "EMAIL_LOCAL_PART_CONSECUTIVE_DOTS",
                    message = "Email local part contains consecutive dots: $value"
                )
            )
        }

        return Result.success(value)
    }
}
