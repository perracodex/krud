/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.database.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import krud.database.schema.base.BaseTable
import krud.database.schema.base.TimestampedTable
import org.jetbrains.exposed.v1.core.ResultRow
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Represents the metadata of a record.
 *
 * @property createdAt The timestamp when the record was created, in UTC.
 * @property createdBy The actor who created the record.
 * @property updatedAt The timestamp when the record was last updated, in UTC.
 * @property updatedBy The actor who last modified the record.
 */
@Serializable
public data class Meta(
    @Contextual val createdAt: Instant,
    val createdBy: Uuid? = null,
    @Contextual val updatedAt: Instant,
    val updatedBy: Uuid? = null
) {
    public companion object {
        /**
         * Maps a [ResultRow] to a [Meta] instance, converting timestamps to UTC.
         * This conversion ensures that the timestamps are timezone-agnostic
         * and can be consistently interpreted in any geographical location.
         *
         * @param row The [ResultRow] to map.
         * @param table The [TimestampedTable] from which the [ResultRow] was obtained.
         * @return The mapped [Meta] instance with timestamps in UTC.
         */
        public fun from(row: ResultRow, table: TimestampedTable): Meta {
            val createdAtJava = row[table.createdAt].toInstant()
            val updatedAtJava = row[table.updatedAt].toInstant()

            return Meta(
                createdAt = Instant.fromEpochSeconds(epochSeconds = createdAtJava.epochSecond, nanosecondAdjustment = createdAtJava.nano),
                updatedAt = Instant.fromEpochSeconds(epochSeconds = updatedAtJava.epochSecond, nanosecondAdjustment = updatedAtJava.nano)
            )
        }

        /**
         * Maps a [ResultRow] to a [Meta] instance, converting timestamps to UTC.
         * This conversion ensures that the timestamps are timezone-agnostic
         * and can be consistently interpreted in any geographical location.
         *
         * @param row The [ResultRow] to map.
         * @param table The [BaseTable] from which the [ResultRow] was obtained.
         * @return The mapped [Meta] instance with timestamps in UTC.
         */
        public fun from(row: ResultRow, table: BaseTable): Meta {
            val createdAtJava = row[table.createdAt].toInstant()
            val updatedAtJava = row[table.updatedAt].toInstant()

            return Meta(
                createdAt = Instant.fromEpochSeconds(epochSeconds = createdAtJava.epochSecond, nanosecondAdjustment = createdAtJava.nano),
                createdBy = row[table.createdBy],
                updatedAt = Instant.fromEpochSeconds(epochSeconds = updatedAtJava.epochSecond, nanosecondAdjustment = updatedAtJava.nano),
                updatedBy = row[table.updatedBy]
            )
        }
    }
}
