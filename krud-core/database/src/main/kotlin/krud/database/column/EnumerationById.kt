/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.database.column

import org.jetbrains.exposed.v1.core.*
import kotlin.enums.EnumEntries

/**
 * Creates a column in an `Exposed` table to store enum values by their integer unique IDs,
 * simplifying the process of defining a column in an `Exposed` table that corresponds
 * to an enum type, where the enum values are stored in the database as integers.
 * It uses a transformation approach to map integers to enum instances, leveraging the
 * integer column type in `Exposed`.
 *
 * #### Usage
 * ```
 * enum class SomeEnum(override val id: Int) : IEnumWithId {
 *     ELEMENT_A(100), ELEMENT_B(101), ...
 * }
 *
 * object SomeTable : Table() {
 *     val status: Column<SomeEnum> = enumerationById(
 *         name = "field_name",
 *         entries = SomeEnum.entries
 *     )
 *     ...
 * }
 * ```
 * @param E The enum class type. This class must implement the IEnumWithId interface.
 * @param name The name of the column in the database.
 * @param entries The list of enum entries to be stored in the database.
 * @return A Column<E> representing the enum in the `Exposed` table.
 * @throws IllegalArgumentException if an unknown enum id is encountered in the database.
 *
 * @see [IEnumWithId]
 */
internal fun <E> Table.enumerationById(name: String, entries: EnumEntries<E>): Column<E>
        where E : Enum<E>, E : IEnumWithId {
    return integer(name = name).transform(
        wrap = { dbEnumId ->
            entries.firstOrNull { enum -> enum.id == dbEnumId }
                ?: throw IllegalArgumentException("Unknown enum id: $dbEnumId")
        },
        unwrap = { enum -> enum.id }
    )
}

/**
 * Interface for enums that require a stored ID for persistence
 * in the database, instead of the enum name or ordinal.
 *
 * @property id The integer ID of the enum item. Expected to be unique across all enum items.
 *
 * @see [enumerationById]
 */
public interface IEnumWithId {
    public val id: Int
}
