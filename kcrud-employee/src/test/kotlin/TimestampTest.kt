/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

import io.ktor.test.dispatcher.*
import io.mockk.every
import io.mockk.mockk
import kcrud.base.database.schema.employee.types.Honorific
import kcrud.base.database.schema.employee.types.MaritalStatus
import kcrud.base.env.CallContext
import kcrud.base.persistence.serializers.OffsetTimestamp
import kcrud.base.utils.KLocalDate
import kcrud.base.utils.TestUtils
import kcrud.domain.employee.di.EmployeeDomainInjection
import kcrud.domain.employee.model.Employee
import kcrud.domain.employee.model.EmployeeRequest
import kcrud.domain.employee.repository.IEmployeeRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import kotlin.test.*


class TimestampTest : KoinComponent {

    @BeforeTest
    fun setUp() {
        TestUtils.loadSettings()
        TestUtils.setupDatabase()
        TestUtils.setupKoin(modules = listOf(EmployeeDomainInjection.get()))
    }

    @AfterTest
    fun tearDown() {
        TestUtils.tearDown()
    }

    @Test
    fun testTimestamp(): Unit = testSuspend {
        val callContext: CallContext = mockk<CallContext>()
        every { callContext.schema } returns null

        val employeeRepository: IEmployeeRepository by inject(
            parameters = { parametersOf(callContext) }
        )

        val employeeRequest = EmployeeRequest(
            firstName = "AnyName",
            lastName = "AnySurname",
            dob = KLocalDate(year = 2000, monthNumber = 1, dayOfMonth = 1),
            honorific = Honorific.MR,
            maritalStatus = MaritalStatus.SINGLE
        )

        val employee: Employee = employeeRepository.create(request = employeeRequest)

        // Assert that both timestamps are the same after creation.
        assertEquals(
            expected = employee.meta.createdAt,
            actual = employee.meta.updatedAt
        )

        val createdAt: OffsetTimestamp = employee.meta.createdAt
        val updatedAt: OffsetTimestamp = employee.meta.updatedAt
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
