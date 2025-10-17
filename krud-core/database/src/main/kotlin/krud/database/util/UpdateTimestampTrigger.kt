/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.database.util

import org.h2.tools.TriggerAdapter
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp

/**
 * Utility class for updating the `updated_at` column in an H2 database.
 * This class is specifically required for H2 databases, which do not support
 * automatic timestamp updates natively through SQL or configuration.
 *
 * In more advanced databases such as `PostgreSQL`, this utility class is not
 * necessary as `PostgreSQL` supports automatic updates of timestamp columns
 * using triggers or the `DEFAULT` keyword with expressions.
 */
public class UpdateTimestampTrigger : TriggerAdapter() {
    @Suppress("KDocMissingDocumentation")
    @Throws(SQLException::class)
    override fun fire(conn: Connection, oldRow: ResultSet?, newRow: ResultSet?) {
        newRow?.updateTimestamp("updated_at", Timestamp(System.currentTimeMillis()))
    }
}
