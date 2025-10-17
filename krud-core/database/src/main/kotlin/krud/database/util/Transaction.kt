/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.database.util

import krud.base.context.SessionContext
import org.jetbrains.exposed.v1.core.Schema
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Creates a transaction then calls the [statement] block with this transaction as its receiver and returns the result.
 *
 * **Note** If the database value [db] is not set, the value used will be either the last Database instance created
 * or the value associated with the parent transaction (if this function is invoked in an existing transaction).
 *
 * The transaction takes into account the specified [sessionContext] instance, which may include, for example,
 * a database connection, a schema name, or other session-context-specific information.
 *
 * #### References
 * - [Transactions](https://jetbrains.github.io/Exposed/transactions.html)
 * - [Schema Tests](https://github.com/JetBrains/Exposed/blob/main/exposed-tests/src/test/kotlin/org/jetbrains/exposed/sql/tests/shared/SchemaTests.kt)
 *
 * @receiver [Transaction] The transaction instance to be used for the transaction.
 *
 * @param sessionContext The [SessionContext] instance to be used for the transaction.
 * @return The final result of the [statement] block.
 */
public fun <T> transaction(
    sessionContext: SessionContext,
    statement: Transaction.() -> T
): T {
    return transaction(db = sessionContext.db) scope@{
        val auditor = AuditInterceptor(sessionContext = sessionContext)
        registerInterceptor(interceptor = auditor)

        try {
            if (sessionContext.schema.isNullOrBlank()) {
                // Directly proceed with the transaction if no schema is specified.
                return@scope statement()
            }

            TransactionManager.current().connection.schema.let { originalSchema ->
                val changeSchema: Boolean = (sessionContext.schema != originalSchema)

                // Change the schema only if it's different from the current one.
                if (changeSchema) {
                    val schemaName: String? = sessionContext.schema
                    checkNotNull(schemaName) { "Schema name must not be null." }
                    SchemaUtils.setSchema(schema = Schema(name = schemaName))
                }

                try {
                    return@scope statement()
                } finally {
                    // Restore the original schema if it was changed.
                    if (changeSchema) {
                        SchemaUtils.setSchema(schema = Schema(name = originalSchema))
                    }
                }
            }
        } finally {
            unregisterInterceptor(interceptor = auditor)
        }
    }
}
