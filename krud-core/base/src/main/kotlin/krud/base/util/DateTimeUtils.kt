/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.base.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlinx.serialization.Serializable
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import java.time.Instant as JavaInstant

/**
 * Singleton providing time-related utility functions.
 */
public object DateTimeUtils {

    /**
     * Enum class for date formats.
     *
     * @property pattern The pattern to format the date.
     */
    public enum class Format(public val pattern: String) {
        /** Format: yyyy-MM-dd, e.g. 2024-04-01 */
        YYYY_MM_DD("yyyy-MM-dd"),

        /** Format: yyyy-MMM-dd, e.g., 2024-APR-01 */
        YYYY_MMM_DD("yyyy-MMM-dd")
    }

    /**
     * Calculates an integer age considering the date.
     */
    public fun LocalDate.age(): Int {

        // Get today's date based on the system clock and timezone.
        val currentDate: LocalDate = LocalDate.current()

        // Calculate the difference in years.
        val age: Int = currentDate.year - this.year

        val birthdayAlreadyPassed: Boolean = (month.number < currentDate.month.number) ||
                (month.number == currentDate.month.number && day <= currentDate.day)

        // Adjust the age if the birthday hasn't occurred this year yet.
        return age.takeIf { birthdayAlreadyPassed } ?: (age - 1)
    }

    /**
     * Represents a time interval.
     *
     * @property days The number of days in the interval.
     * @property hours The number of hours in the interval.
     * @property minutes The number of minutes in the interval.
     */
    @Serializable
    public data class Interval(val days: UInt = 0u, val hours: UInt = 0u, val minutes: UInt = 0u) {
        /**
         * Converts the overall interval into a total number of minutes.
         */
        public fun toTotalMinutes(): UInt {
            return (days * 24u * 60u) + (hours * 60u) + minutes
        }
    }

    /**
     * Formats a LocalDateTime to a consistent ISO 8601 string with configurable fractional second precision.
     *
     * Fractional seconds are displayed to nanosecond precision (9 digits),
     * which represents the full precision of the LocalDateTime.
     *
     * @param timeDelimiter The delimiter to use between the date and time components. Defaults to "T".
     * @param precision The number of digits for fractional seconds (0 to 9). Defaults to 9 (nanoseconds).
     *                  Values outside this range are coerced to 0 or 9.
     * @return A string representation of the LocalDateTime in ISO 8601 format with the specified fractional precision.
     */
    public fun LocalDateTime.formatted(timeDelimiter: String = "T", precision: Int = 9): String {
        val year: String = "%04d".format(this.year)
        val month: String = "%02d".format(this.month)
        val day: String = "%02d".format(day)
        val hour: String = "%02d".format(this.hour)
        val minute: String = "%02d".format(this.minute)
        val second: String = "%02d".format(this.second)

        // If no fractional seconds are needed, return the basic timestamp.
        val adjustedPrecision: Int = precision.coerceIn(minimumValue = 0, maximumValue = 9)
        if (adjustedPrecision == 0) {
            return "$year-$month-$day$timeDelimiter$hour:$minute:$second"
        }

        // Extract the desired number of digits for fractional seconds.
        val nanosecond: String = "%09d".format(this.nanosecond)
        val fractionalPrecision: String = nanosecond.take(n = adjustedPrecision)

        // Assemble the final formatted string
        return "$year-$month-$day$timeDelimiter$hour:$minute:$second.$fractionalPrecision"
    }

    /**
     * Returns the system's default timezone.
     *
     * Equivalent to [TimeZone.currentSystemDefault].
     */
    public fun TimeZone.Companion.current(): TimeZone {
        return currentSystemDefault()
    }

    /**
     * Returns the current date-time in the system's default time zone.
     */
    public fun LocalDateTime.Companion.current(): LocalDateTime {
        return Clock.System.now().toLocalDateTime(timeZone = TimeZone.currentSystemDefault())
    }

    /**
     * Returns the current date in the system's default time zone.
     */
    private fun LocalDate.Companion.current(): LocalDate {
        return Clock.System.todayIn(timeZone = TimeZone.currentSystemDefault())
    }

    /**
     * Returns the current date-time in UTC.
     */
    public fun Instant.Companion.current(): Instant = Clock.System.now()

    /**
     * Formats a date using the given [Format] pattern.
     */
    public fun LocalDate.format(pattern: Format): String {
        return DateTimeFormatter.ofPattern(pattern.pattern).format(this.toJavaLocalDate())
    }

    /**
     * Formats a date-time using the given [Format] pattern.
     */
    public fun LocalDateTime.format(pattern: Format): String {
        return DateTimeFormatter.ofPattern(pattern.pattern).format(this.toJavaLocalDateTime())
    }

    /**
     * Converts a Kotlin [LocalDateTime] to a Java [Date].
     */
    public fun LocalDateTime.toJavaDate(): Date {
        this.toInstant(timeZone = TimeZone.current()).toJavaInstant().let { instant ->
            return Date.from(instant)
        }
    }

    /**
     * Converts a Java [Date] to a Kotlin [LocalDateTime].
     *
     * @param zoneId The java time zone to apply during the conversion. Defaults to the system's default time zone.
     */
    public fun Date.toKotlinLocalDateTime(zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime {
        return this.toInstant()
            .atZone(zoneId)
            .toLocalDateTime()
            .toKotlinLocalDateTime()
    }

    /**
     * Converts a Kotlin [Duration] to a Java [Instant].
     */
    public fun Duration.toJavaInstant(): JavaInstant {
        return JavaInstant.ofEpochMilli(
            System.currentTimeMillis() + this.toLong(unit = DurationUnit.MILLISECONDS)
        )
    }

    /**
     * Extension function flattening the time to 00:00:00.000000000.
     */
    public fun LocalDateTime.flattenTime(): LocalDateTime {
        return LocalDateTime(
            year = year,
            month = month,
            day = day,
            hour = 0,
            minute = 0,
            second = 0,
            nanosecond = 0
        )
    }

    /**
     * Extension function filling the time with 23:59:59.999999999.
     */
    public fun LocalDateTime.fillTime(): LocalDateTime {
        return LocalDateTime(
            year = year,
            month = month,
            day = day,
            hour = 59,
            minute = 59,
            second = 59,
            nanosecond = 999999999
        )
    }
}
