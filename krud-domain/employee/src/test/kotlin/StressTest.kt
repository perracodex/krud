/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */
import io.ktor.test.dispatcher.*
import io.mockk.mockk
import io.perracodex.exposed.pagination.Page
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import krud.access.domain.actor.di.ActorDomainInjection
import krud.access.domain.rbac.di.RbacDomainInjection
import krud.base.context.SessionContext
import krud.base.test.TestUtils
import krud.database.schema.employee.type.Honorific
import krud.database.schema.employee.type.MaritalStatus
import krud.database.test.DatabaseTestUtils
import krud.domain.employee.di.EmployeeDomainInjection
import krud.domain.employee.model.Employee
import krud.domain.employee.model.EmployeeRequest
import krud.domain.employee.service.EmployeeService
import krud.domain.employee.test.EmployeeTestUtils
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class StressTest : KoinComponent {

    @BeforeTest
    fun setUp() {
        TestUtils.loadSettings()
        DatabaseTestUtils.setupDatabase()
        TestUtils.setupKoin(
            modules = listOf(
                RbacDomainInjection.get(),
                ActorDomainInjection.get(),
                EmployeeDomainInjection.get()
            )
        )
    }

    @AfterTest
    fun tearDown() {
        DatabaseTestUtils.closeDatabase()
        TestUtils.tearDown()
    }

    @Test
    fun largeConcurrentSet(): Unit = testSuspend {
        val sessionContext: SessionContext = mockk<SessionContext>()

        val employeeService: EmployeeService by inject(
            parameters = { parametersOf(sessionContext) }
        )

        val employeeRequest: EmployeeRequest = EmployeeTestUtils.newEmployeeRequest()
        val totalElements = 10000

        val jobs: List<Deferred<Employee>> = List(size = totalElements) { index ->
            val randomYears: Int = (20..65).random()
            val randomMonths: Int = (1..12).random()
            val randomDays: Int = (1..28).random()
            val randomChars: String = List(size = 2) { "abc0123".random() }.joinToString(separator = "")

            val request: EmployeeRequest = employeeRequest.copy(
                firstName = "${employeeRequest.firstName}_$index",
                maritalStatus = MaritalStatus.entries.random(),
                honorific = Honorific.entries.random(),
                dob = employeeRequest.dob.minus(randomYears, DateTimeUnit.YEAR)
                    .minus(randomMonths, DateTimeUnit.MONTH)
                    .minus(randomDays, DateTimeUnit.DAY),
                contact = employeeRequest.contact?.let { contactRequest ->
                    contactRequest.copy(email = "${randomChars}_${contactRequest.email}")
                }
            )

            async {
                employeeService.create(request = request).getOrThrow()
            }
        }

        // Await all the Deferred objects to ensure all async operations complete.
        jobs.awaitAll()

        // Verify all employees after all insertions are complete.
        val employees: Page<Employee> = employeeService.findAll()
        assertEquals(expected = totalElements, actual = employees.content.size)
        assertEquals(expected = totalElements, actual = employees.details.totalElements)

        employeeService.deleteAll()
    }

    @Test
    fun largeEmployeeSet(): Unit = testSuspend {
        val sessionContext: SessionContext = mockk<SessionContext>()

        val employeeService: EmployeeService by inject(
            parameters = { parametersOf(sessionContext) }
        )

        val employeeRequest: EmployeeRequest = EmployeeTestUtils.newEmployeeRequest()
        val totalElements = 10000

        (1..totalElements).forEach { index ->
            val randomYears: Int = (20..65).random()
            val randomMonths: Int = (1..12).random()
            val randomDays: Int = (1..28).random()
            val randomChars: String = List(size = 2) { "abc0123".random() }.joinToString(separator = "")

            val request: EmployeeRequest = employeeRequest.copy(
                firstName = "${employeeRequest.firstName}_$index",
                maritalStatus = MaritalStatus.entries.random(),
                honorific = Honorific.entries.random(),
                dob = employeeRequest.dob.minus(randomYears, DateTimeUnit.YEAR)
                    .minus(randomMonths, DateTimeUnit.MONTH)
                    .minus(randomDays, DateTimeUnit.DAY),
                contact = employeeRequest.contact?.let { contactRequest ->
                    contactRequest.copy(email = "${randomChars}_${contactRequest.email}")
                }
            )

            employeeService.create(request = request).getOrThrow()
        }

        // Verify all employees.
        val employees: Page<Employee> = employeeService.findAll()
        assertEquals(expected = totalElements, actual = employees.content.size)
        assertEquals(expected = totalElements, actual = employees.details.totalElements)
    }
}
