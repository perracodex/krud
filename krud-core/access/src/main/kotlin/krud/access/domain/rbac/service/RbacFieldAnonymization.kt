/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.access.domain.rbac.service

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import krud.access.domain.rbac.model.base.BaseRbac

/**
 * Utility class to anonymize fields based on their type.
 *
 * Enum types are not directly supported for anonymization due to the lack of a standardized approach.
 * A recommended practice is to include an `ANONYMIZED` value within all enum types to facilitate their anonymization.
 *
 * Lists are not supported, as for anonymization the list itself is replaced with an empty list,
 * and we cannot determine the original type of the list's elements. Another approach would
 * have been to anonymize the list's elements, but this would require a recursive approach to
 * anonymize nested lists.
 *
 * Special consideration must be given to the anonymization of time values.
 * The anonymization approach uses "00:00:00.000" as the placeholder for time values.
 * To distinguish between actual instances of midnight and anonymized values, we should consider implementing
 * an additional utility method that would adjust genuine "00:00:00.000" time values, whenever encountered,
 * to a slightly different time (e.g., "00:00:00.001") to prevent confusion with the anonymized time placeholder.
 * Another approach could be to just change Time fields to Text fields, and anonymize them as strings.
 *
 * @see [BaseRbac]
 */
public object RbacFieldAnonymization {
    private const val ANONYMIZED_STRING: String = "##########"
    private const val ANONYMIZED_INT: Int = Int.MIN_VALUE
    private const val ANONYMIZED_LONG: Long = Long.MIN_VALUE
    private const val ANONYMIZED_DOUBLE: Double = Double.MIN_VALUE
    private const val ANONYMIZED_FLOAT: Float = Float.MIN_VALUE
    private val ANONYMIZED_DATE: LocalDate = LocalDate(year = 1900, month = 1, day = 1)
    private val ANONYMIZED_TIME: LocalTime = LocalTime(hour = 0, minute = 0, second = 0, nanosecond = 0)
    private val ANONYMIZED_DATE_TIME: LocalDateTime = LocalDateTime(date = ANONYMIZED_DATE, time = ANONYMIZED_TIME)

    /**
     * Internal method to replace the value of a field with a placeholder based on the field's type.
     *
     * @param value The original value to be anonymized.
     * @return The anonymized value, or the original value if the field type does not match any handled type.
     */
    public fun anonymize(value: Any?): Any? {
        return when (value) {
            is String -> ANONYMIZED_STRING
            is Int -> ANONYMIZED_INT
            is Long -> ANONYMIZED_LONG
            is Double -> ANONYMIZED_DOUBLE
            is Float -> ANONYMIZED_FLOAT
            is LocalDate -> ANONYMIZED_DATE
            is LocalTime -> ANONYMIZED_TIME
            is LocalDateTime -> ANONYMIZED_DATE_TIME
            is List<*> -> emptyList<Any?>() // Return an empty list regardless of the element type.
            else -> value
        }
    }

    /**
     * Checks if the given value has been anonymized.
     *
     * @param value The value to check for anonymization.
     * @return True if the value is anonymized, false otherwise.
     */
    public fun isAnonymized(value: Any?): Boolean {
        return when (value) {
            is String -> value == ANONYMIZED_STRING
            is Int -> value == ANONYMIZED_INT
            is Long -> value == ANONYMIZED_LONG
            is Double -> value == ANONYMIZED_DOUBLE
            is Float -> value == ANONYMIZED_FLOAT
            is LocalDate -> value == ANONYMIZED_DATE
            is LocalTime -> value == ANONYMIZED_TIME
            is LocalDateTime -> value == ANONYMIZED_DATE_TIME
            else -> false
        }
    }
}
