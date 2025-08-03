/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.database.column

import org.jetbrains.exposed.v1.core.*

/**
 * A custom column type for supporting PostgreSQL `citext` (case-insensitive text) data type in Exposed.
 *
 * This class extends `TextColumnType` to override the SQL type definition to `citext`,
 * allowing columns to store case-insensitive text values when used with a PostgreSQL database.
 */
private class CitextColumnType : TextColumnType() {
    override fun sqlType(): String = "citext"
}

/**
 * Adds a `citext` (case-insensitive text) column to a table in the database schema.
 * It is designed to work with PostgreSQL databases where `citext` is supported.
 *
 * #### Attention
 * Ensure the `citext` extension is installed in your PostgreSQL database before using this column type.
 * To enable the extension, run the following SQL:
 * ```
 * CREATE EXTENSION IF NOT EXISTS citext;
 * ```
 *
 * @param name The name of the column.
 * @return A column of type `citext`.
 */
public fun Table.citext(name: String): Column<String> {
    return registerColumn(name = name, type = CitextColumnType())
}
