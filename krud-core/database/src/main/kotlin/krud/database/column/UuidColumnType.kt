/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.database.column

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.Table.Dual.clientDefault
import org.jetbrains.exposed.v1.core.statements.api.RowApi
import org.jetbrains.exposed.v1.core.vendors.MariaDBDialect
import org.jetbrains.exposed.v1.core.vendors.currentDialect
import java.util.*
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

/**
 * Custom column type for handling Kotlin's [Uuid] in Exposed database operations.
 * Kotlin introduced native [Uuid] support in version 2.0.20, which differs from Java's UUID type.
 * This class facilitates the use of Kotlin's [Uuid] in the Exposed framework, reducing the need
 * for conversions between Java's UUID and Kotlin's Uuid and streamlining database operations.
 *
 * Note that `Exposed` framework team has opened a ticket regarding this issue.
 *
 * #### Issues
 * - [EXPOSED-507](https://youtrack.jetbrains.com/issue/EXPOSED-507/Support-kotlin.uuid.Uuid-as-a-column-type)
 */
internal class UuidColumnType : ColumnType<Uuid>() {
    override fun sqlType(): String = currentDialect.dataTypeProvider.uuidType()

    override fun valueFromDB(value: Any): Uuid = when (value) {
        is Uuid -> value
        is UUID -> value.toKotlinUuid()
        is ByteArray -> Uuid.fromByteArray(byteArray = value)
        is String if value.matches(regex = uuidRegexp) -> Uuid.parse(uuidString = value)
        is String -> Uuid.fromByteArray(byteArray = value.toByteArray())
        else -> error("Unexpected value of type Uuid: $value of ${value::class.qualifiedName}")
    }

    override fun notNullValueToDB(value: Uuid): Any {
        return currentDialect.dataTypeProvider.uuidToDB(value = value.toJavaUuid())
    }

    override fun nonNullValueToString(value: Uuid): String = "'$value'"

    override fun readObject(rs: RowApi, index: Int): Any? {
        val raw: Any? = rs.getObject(index)
        return when {
            currentDialect is MariaDBDialect -> when (raw) {
                is ByteArray -> Uuid.fromByteArray(byteArray = raw)
                is UUID -> raw.toKotlinUuid()
                else -> error("Unexpected MariaDB UUID value: $raw")
            }

            raw is UUID -> raw.toKotlinUuid()
            raw is ByteArray -> Uuid.fromByteArray(byteArray = raw)
            raw is String && uuidRegexp.matches(input = raw) -> Uuid.parse(uuidString = raw)
            else -> super.readObject(rs, index)
        }
    }

    companion object {
        private val uuidRegexp =
            Regex(
                pattern = "[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}",
                option = RegexOption.IGNORE_CASE
            )
    }
}

/**
 * Extension function for defining a column with a Kotlin [Uuid] type in an Exposed table.
 * Named `kotlinUuid` to avoid conflicts with Exposed `uuid` built-in function.
 */
internal fun Table.kotlinUuid(name: String): Column<Uuid> {
    return registerColumn(name = name, type = UuidColumnType())
}

/**
 * Extension function to enable auto-generation of Kotlin [Uuid] values for a column.
 */
@JvmName("autoGenerateKotlinUuid")
internal fun Column<Uuid>.autoGenerate(): Column<Uuid> = clientDefault { Uuid.random() }

/**
 * Extension function to establish foreign key relationships using Kotlin's [Uuid] columns.
 */
internal fun Column<Uuid>.references(
    ref: Column<Uuid>,
    onDelete: ReferenceOption,
    onUpdate: ReferenceOption,
    fkName: String
): Column<Uuid> {
    this.foreignKey = ForeignKeyConstraint(
        target = ref,
        from = this,
        onUpdate = onUpdate,
        onDelete = onDelete,
        name = fkName
    )
    return this
}
