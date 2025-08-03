/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

import krud.base.test.TestUtils
import krud.database.test.DatabaseTestUtils
import org.jetbrains.exposed.v1.core.Schema
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.system.measureTimeMillis
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SchemaChangeTest {

    @BeforeTest
    fun setUp() {
        TestUtils.loadSettings()
        DatabaseTestUtils.setupDatabase()
    }

    @AfterTest
    fun tearDown() {
        DatabaseTestUtils.closeDatabase()
        TestUtils.tearDown()
    }

    /**
     * Test schema changes timing.
     * - Create 20 different schemas.
     * - Change between schemas 500 times, that would be equivalent to change
     *   between schemas 10_000 times within the scope of a single request call.
     */
    @Test
    fun testMultipleSchemaChangesTiming() {
        val schemas: List<Schema> = List(size = 20) { Schema(name = "SCHEMA_$it") } // Create 10 different schemas.

        var tested = 0

        val duration: Long = measureTimeMillis {
            transaction {
                schemas.forEach { schema ->
                    SchemaUtils.createSchema(schema)
                }

                repeat(times = 500) {
                    schemas.forEach { schema ->
                        tested++
                        println("Testing schema: ${schema.identifier}")
                        SchemaUtils.setSchema(schema = schema)
                        assertEquals(expected = schema.identifier, actual = TransactionManager.current().connection.schema)
                    }
                }
            }
        }

        println("Total time for schema changes: ${duration}ms. Tested $tested schema changes.")
    }
}
