/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import krud.access.domain.rbac.model.base.BaseRbac
import krud.access.domain.rbac.service.RbacFieldAnonymization
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BaseRbacTest {

    @Test
    fun testFields() {
        data class TestClass(
            val id: Int,
            val name: String
        ) : BaseRbac()

        val instance = TestClass(
            id = 1,
            name = "name"
        )

        // Verify no anonymization.
        assertEquals(expected = 1, actual = instance.id)
        assertEquals(expected = "name", actual = instance.name)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(instance.id), actual = false)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(instance.name), actual = false)

        // Verify single field anonymization.
        val singleFieldProcessed: TestClass = instance.anonymize(fields = listOf("name"))
        assertEquals(expected = 1, actual = singleFieldProcessed.id)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(singleFieldProcessed.id), actual = false)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(singleFieldProcessed.name), actual = true)

        // Verify multiple field anonymization.
        val allFieldsProcessed: TestClass = instance.anonymize(fields = listOf("id", "name"))
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(allFieldsProcessed.id), actual = true)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(allFieldsProcessed.name), actual = true)
    }

    @Test
    fun testNestedInstances() {
        data class ChildClass(
            val id: Int,
            val name: String,
            val nested: ChildClass? = null
        ) : BaseRbac()

        data class ParentClass(
            val id: Int,
            val name: String,
            val nested: ChildClass? = null
        ) : BaseRbac()

        val instance = ParentClass(
            id = 1,
            name = "name",
            nested = ChildClass(
                id = 2,
                name = "nested",
                nested = ChildClass(
                    id = 3,
                    name = "nested",
                    nested = ChildClass(
                        id = 4,
                        name = "nested"
                    )
                )
            )
        )

        val processed: ParentClass = instance.anonymize(
            fields = listOf(
                "id", "name", "nested.id", "nested.id", "nested.name", "nested.nested.name"
            )
        )

        assertNotNull(actual = processed.nested)
        assertNotNull(actual = processed.nested.nested)
        assertNotNull(actual = processed.nested.nested.nested)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(processed.id), actual = true)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(processed.name), actual = true)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(processed.nested.id), actual = true)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(processed.nested.name), actual = true)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(processed.nested.nested.id), actual = false)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(processed.nested.nested.name), actual = true)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(processed.nested.nested.nested.id), actual = false)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(processed.nested.nested.nested.name), actual = false)
    }

    @Test
    fun testDates() {
        data class TestClass(
            val id: Int,
            val date: LocalDate,
            val time: LocalTime,
            val dateTime: LocalDateTime
        ) : BaseRbac()

        val instance = TestClass(
            id = 1,
            date = LocalDate(year = 2021, month = 1, day = 1),
            time = LocalTime(hour = 12, minute = 0, second = 0, nanosecond = 0),
            dateTime = LocalDateTime(
                date = LocalDate(year = 2021, month = 1, day = 1),
                time = LocalTime(hour = 12, minute = 0, second = 0, nanosecond = 0)
            )
        )

        // Verify no anonymization.
        assertEquals(expected = 1, actual = instance.id)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(instance.id), actual = false)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(instance.date), actual = false)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(instance.time), actual = false)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(instance.dateTime), actual = false)

        // Verify anonymization.
        val anonymized: TestClass = instance.anonymize(fields = listOf("date", "time", "dateTime"))
        assertEquals(expected = 1, actual = anonymized.id)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(anonymized.id), actual = false)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(anonymized.date), actual = true)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(anonymized.time), actual = true)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(anonymized.dateTime), actual = true)
    }

    @Test
    fun testNumbers() {
        data class TestClass(
            val id: Int,
            val double: Double,
            val float: Float,
            val long: Long,
            val int: Int
        ) : BaseRbac()

        val instance = TestClass(
            id = 1,
            double = 1.0,
            float = 1.0f,
            long = 1L,
            int = 1
        )

        // Verify no anonymization.
        assertEquals(expected = 1, actual = instance.id)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(instance.id), actual = false)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(instance.double), actual = false)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(instance.float), actual = false)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(instance.long), actual = false)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(instance.int), actual = false)

        // Verify anonymization.
        val anonymized: TestClass = instance.anonymize(fields = listOf("double", "float", "long", "int"))
        assertEquals(expected = 1, actual = anonymized.id)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(anonymized.id), actual = false)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(anonymized.double), actual = true)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(anonymized.float), actual = true)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(anonymized.long), actual = true)
        assertEquals(expected = RbacFieldAnonymization.isAnonymized(anonymized.int), actual = true)
    }
}
