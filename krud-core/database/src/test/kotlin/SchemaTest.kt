/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

import krud.base.test.TestUtils
import krud.database.test.DatabaseTestUtils
import org.jetbrains.exposed.v1.core.Schema
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SchemaTest {

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

    @Test
    fun testSchemaCreation() {
        val schemaA = Schema(name = "SCHEMA_A")
        val schemaB = Schema(name = "SCHEMA_B")

        transaction {
            SchemaUtils.createSchema(schemaA, schemaB)

            SchemaUtils.setSchema(schema = schemaA)
            assertEquals(expected = schemaA.identifier, actual = TransactionManager.current().connection.schema)

            SchemaUtils.setSchema(schema = schemaB)
            assertEquals(expected = schemaB.identifier, actual = TransactionManager.current().connection.schema)
        }
    }

    @Test
    fun testNestedSchema() {
        val schemaA = Schema(name = "SCHEMA_A")
        val schemaB = Schema(name = "SCHEMA_B")

        transaction {
            SchemaUtils.createSchema(schemaA, schemaB)

            // Start with schemaA.
            SchemaUtils.setSchema(schema = schemaA)
            assertEquals(expected = schemaA.identifier, actual = TransactionManager.current().connection.schema)

            transaction {
                // Change to schemaB.
                SchemaUtils.setSchema(schema = schemaB)
                assertEquals(expected = schemaB.identifier, actual = TransactionManager.current().connection.schema)
                SchemaUtils.setSchema(schema = schemaA)
            }

            // Should be back to schemaA.
            assertEquals(expected = schemaA.identifier, actual = TransactionManager.current().connection.schema)
        }
    }
}
