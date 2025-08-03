/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

import io.ktor.test.dispatcher.*
import io.mockk.mockk
import kotlin.time.Instant
import krud.base.context.SessionContext
import krud.base.test.TestUtils
import krud.database.test.DatabaseTestUtils
import krud.domain.employee.di.EmployeeDomainInjection
import krud.domain.employee.model.Employee
import krud.domain.employee.model.EmployeeRequest
import krud.domain.employee.repository.IEmployeeRepository
import krud.domain.employee.test.EmployeeTestUtils
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import kotlin.test.*

class TimestampTest : KoinComponent {

    @BeforeTest
    fun setUp() {
        TestUtils.loadSettings()
        DatabaseTestUtils.setupDatabase()
        TestUtils.setupKoin(modules = listOf(EmployeeDomainInjection.get()))
    }

    @AfterTest
    fun tearDown() {
        DatabaseTestUtils.closeDatabase()
        TestUtils.tearDown()
    }

    @Test
    fun testTimestamp(): Unit = testSuspend {
        val sessionContext: SessionContext = mockk<SessionContext>()

        val employeeRepository: IEmployeeRepository by inject(
            parameters = { parametersOf(sessionContext) }
        )

        val employeeRequest: EmployeeRequest = EmployeeTestUtils.newEmployeeRequest()
        val employee: Employee = employeeRepository.create(request = employeeRequest)

        // Assert that both timestamps are the same after creation.
        assertEquals(
            expected = employee.meta.createdAt,
            actual = employee.meta.updatedAt
        )

        val createdAt: Instant = employee.meta.createdAt
        val updatedAt: Instant = employee.meta.updatedAt
        employeeRepository.update(employeeId = employee.id, request = employeeRequest)
        val updatedEmployee: Employee = employeeRepository.findById(employeeId = employee.id)
            ?: fail("Employee not found.")

        // The createdAt timestamp should not change.
        assertEquals(
            expected = createdAt,
            actual = updatedEmployee.meta.createdAt
        )

        // The updatedAt timestamp should change.
        assertNotEquals(
            illegal = updatedAt,
            actual = updatedEmployee.meta.updatedAt
        )
    }
}
