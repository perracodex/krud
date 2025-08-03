/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.database.schema.base

import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.datetime.date

/**
 * Base class for database tables that manage temporal data modelling. Designed to be extended,
 * this class provides foundational columns for tracking the active status and temporal boundaries
 * of periods associated with various entries.
 * This structure supports applications needing to capture historical data or manage ongoing events.
 */
public abstract class PeriodTable(name: String) : BaseTable(name = name) {

    /** Whether the period is currently active. */
    public val isActive: Column<Boolean> = bool(
        name = "is_active"
    )

    /** The date when the period started. */
    public val startDate: Column<LocalDate> = date(
        name = "start_date"
    )

    /** Optional date when the period ended; null if ongoing. */
    public val endDate: Column<LocalDate?> = date(
        name = "end_date"
    ).nullable()

    /** Optional notes for additional context or details about the period. */
    public val comments: Column<String?> = varchar(
        name = "comments",
        length = 512
    ).nullable()
}
