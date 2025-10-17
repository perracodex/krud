/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.database.util

import java.sql.Connection

/**
 * Represents the isolation level for database transactions.
 *
 * @property id The id of the isolation level.
 *
 * @see [java.sql.Connection]
 */
internal enum class IsolationLevel(val id: Int) {

    /**
     * The most lenient isolation level. Allows uncommitted changes from one transaction to affect a read
     * in another transaction (a "dirty read").
     * Dirty reads, non-repeatable reads, and phantom reads can occur.
     * This level allows a row changed by one transaction to be read by another transaction before
     * any changes in that row have been committed (a "dirty read"). If any of the changes are rolled back,
     * the second transaction will have retrieved an invalid row.
     */
    TRANSACTION_READ_UNCOMMITTED(id = Connection.TRANSACTION_READ_UNCOMMITTED),

    /**
     * Prevents dirty reads from occurring but still allows non-repeatable reads and phantom reads to occur.
     * A non-repeatable read is when a transaction ("Transaction A") reads a row from the database,
     * another transaction ("Transaction B") changes the row, and ("Transaction A") reads the row again,
     * resulting in an inconsistency.
     * This level only prohibits a transaction from reading a row with uncommitted changes in it.
     */
    TRANSACTION_READ_COMMITTED(id = Connection.TRANSACTION_READ_COMMITTED),

    /**
     * The default setting for Exposed transactions.
     * Prevents both dirty and non-repeatable reads but still allows for phantom reads.
     * A phantom read is when a transaction ("Transaction A") selects a list of rows through a ("WHERE") clause,
     * another transaction ("Transaction B") performs an ("INSERT") or ("DELETE") with a row that satisfies
     * ("Transaction A")'s ("WHERE") clause, and ("Transaction A") selects using the same ("WHERE") clause again,
     * resulting in an inconsistency.
     * This level prohibits a transaction from reading a row with uncommitted changes in it. It also
     * prohibits the situation where one transaction reads a row, a second transaction alters the row,
     * and the first transaction rereads the row, getting different values the second time (a "non-repeatable read").
     */
    TRANSACTION_REPEATABLE_READ(id = Connection.TRANSACTION_REPEATABLE_READ),

    /**
     * The strictest setting. Prevents dirty reads, non-repeatable reads, and phantom reads.
     * This level includes the prohibitions in [TRANSACTION_REPEATABLE_READ] and further prohibits
     * the situation where one transaction reads all rows that satisfy a ("WHERE") condition,
     * a second transaction inserts a row that satisfies that condition, and the first transaction
     * re-reads for the same condition, retrieving the additional "phantom" row in the second read.
     */
    TRANSACTION_SERIALIZABLE(id = Connection.TRANSACTION_SERIALIZABLE)
}
