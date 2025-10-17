/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.domain.employment.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import krud.database.model.Period
import krud.database.schema.employment.type.EmploymentStatus
import krud.database.schema.employment.type.WorkModality

/**
 * Represents the request to create/update an `employment`.
 *
 * @property period The employment's period details.
 * @property status The [EmploymentStatus] to set.
 * @property probationEndDate Optional employment's probation end date.
 * @property workModality The employment's [WorkModality] to set.
 * @property sensitiveData Optional sensitive data. Demonstrates encrypted columns.
 */
@Serializable
public data class EmploymentRequest public constructor(
    val period: Period,
    val status: EmploymentStatus,
    val probationEndDate: LocalDate? = null,
    val workModality: WorkModality,
    val sensitiveData: String? = null
)
