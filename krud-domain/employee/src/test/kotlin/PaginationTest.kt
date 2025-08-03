/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

import io.ktor.test.dispatcher.*
import io.mockk.mockk
import io.perracodex.exposed.pagination.Page
import io.perracodex.exposed.pagination.Pageable
import kotlinx.datetime.LocalDate
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
import krud.domain.employee.repository.IEmployeeRepository
import krud.domain.employee.service.EmployeeService
import krud.domain.employee.test.EmployeeTestUtils
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PaginationTest : KoinComponent {

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
    fun testEmptyPagination(): Unit = testSuspend {
        newSuspendedTransaction {
            val sessionContext: SessionContext = mockk<SessionContext>()

            val employeeService: EmployeeService by inject(
                parameters = { parametersOf(sessionContext) }
            )

            val totalRecords = 10

            // No records.
            employeeService.findAll(pageable = Pageable(page = 0, size = totalRecords)).also { page ->
                assertEquals(
                    expected = 0,
                    actual = page.details.totalPages,
                    message = "Total pages should be 0."
                )
                assertEquals(
                    expected = 0,
                    actual = page.details.totalElements,
                    message = "Total elements should be 0."
                )
                assertEquals(
                    expected = totalRecords,
                    actual = page.details.elementsPerPage,
                    message = "Elements per page should be $totalRecords."
                )
                assertEquals(
                    expected = 0,
                    actual = page.details.elementsInPage,
                    message = "Elements in page should be 0."
                )
                assertEquals(
                    expected = 0,
                    actual = page.details.pageIndex,
                    message = "Page index should be 0."
                )
                assertEquals(
                    expected = false,
                    actual = page.details.hasNext,
                    message = "Has next should be false."
                )
                assertEquals(
                    expected = false,
                    actual = page.details.hasPrevious,
                    message = "Has previous should be false."
                )
                assertEquals(
                    expected = true,
                    actual = page.details.isFirst,
                    message = "Is first should be true."
                )
                assertEquals(
                    expected = true,
                    actual = page.details.isLast,
                    message = "Is last should be true."
                )
            }
        }
    }

    @Test
    fun testEvenPagination(): Unit = testSuspend {
        newSuspendedTransaction {
            val sessionContext: SessionContext = mockk<SessionContext>()

            val employeeService: EmployeeService by inject(
                parameters = { parametersOf(sessionContext) }
            )

            val totalRecords = 10

            // Create test records.
            repeat(times = totalRecords) {
                val employeeRequest: EmployeeRequest = EmployeeTestUtils.newEmployeeRequest()
                employeeService.create(request = employeeRequest)
            }

            // 1 Page
            employeeService.findAll(pageable = Pageable(page = 0, size = 0)).also { page ->
                assertEquals(
                    expected = 1,
                    actual = page.details.totalPages,
                    message = "Total pages should be 1."
                )
                assertEquals(
                    expected = totalRecords,
                    actual = page.details.totalElements,
                    message = "Total elements should be $totalRecords."
                )
                assertEquals(
                    expected = totalRecords,
                    actual = page.details.elementsPerPage,
                    message = "Elements per page should be $totalRecords."
                )
                assertEquals(
                    expected = totalRecords,
                    actual = page.details.elementsInPage,
                    message = "Elements in page should be $totalRecords."
                )
                assertEquals(
                    expected = 0,
                    actual = page.details.pageIndex,
                    message = "Page index should be 0."
                )
                assertEquals(
                    expected = false,
                    actual = page.details.hasNext,
                    message = "Has next should be false."
                )
                assertEquals(
                    expected = false,
                    actual = page.details.hasPrevious,
                    message = "Has previous should be false."
                )
                assertEquals(
                    expected = true,
                    actual = page.details.isFirst,
                    message = "Is first should be true."
                )
                assertEquals(
                    expected = true,
                    actual = page.details.isLast,
                    message = "Is last should be true."
                )
            }

            // 2 pages.
            val halfPageCount: Int = totalRecords / 2
            employeeService.findAll(pageable = Pageable(page = 1, size = halfPageCount)).also { page ->
                assertEquals(
                    expected = 2,
                    actual = page.details.totalPages,
                    message = "Total pages should be 2."
                )

                assertEquals(
                    expected = totalRecords,
                    actual = page.details.totalElements,
                    message = "Total elements should be $totalRecords."
                )

                assertEquals(
                    expected = halfPageCount,
                    actual = page.details.elementsPerPage,
                    message = "Elements per page should be $halfPageCount."
                )

                assertEquals(
                    expected = halfPageCount,
                    actual = page.details.elementsInPage,
                    message = "Elements in page should be $halfPageCount."
                )

                assertEquals(
                    expected = 1,
                    actual = page.details.pageIndex,
                    message = "Page index should be 1."
                )

                assertEquals(
                    expected = false,
                    actual = page.details.hasNext,
                    message = "Has next should be true."
                )

                assertEquals(
                    expected = true,
                    actual = page.details.hasPrevious,
                    message = "Has previous should be false."
                )

                assertEquals(
                    expected = false,
                    actual = page.details.isFirst,
                    message = "Is first should be true."
                )

                assertEquals(
                    expected = true,
                    actual = page.details.isLast,
                    message = "Is last should be false."
                )
            }

            rollback()

            val page: Page<Employee> = employeeService.findAll()
            assertEquals(
                expected = 0, actual = page.details.totalElements,
                message = "After rollback total elements should be 0."
            )
        }
    }

    @Test
    fun testOddPagination(): Unit = testSuspend {
        newSuspendedTransaction {
            val sessionContext: SessionContext = mockk<SessionContext>()

            val employeeService: EmployeeService by inject(
                parameters = { parametersOf(sessionContext) }
            )

            val totalRecords = 12

            // Create test records.
            repeat(times = totalRecords) {
                val employeeRequest: EmployeeRequest = EmployeeTestUtils.newEmployeeRequest()
                employeeService.create(request = employeeRequest)
            }

            val elementsPerPage = 5

            (0..2).forEach { pageIndex ->
                employeeService.findAll(pageable = Pageable(page = pageIndex, size = elementsPerPage)).also { page ->
                    assertEquals(
                        expected = 3,
                        actual = page.details.totalPages,
                        message = "Total pages should be 3."
                    )
                    assertEquals(
                        expected = totalRecords,
                        actual = page.details.totalElements,
                        message = "Total elements should be $totalRecords."
                    )
                    assertEquals(
                        expected = elementsPerPage,
                        actual = page.details.elementsPerPage,
                        message = "Elements per page should be $elementsPerPage."
                    )

                    when (pageIndex) {
                        0 -> {
                            assertEquals(
                                expected = elementsPerPage,
                                actual = page.details.elementsInPage,
                                message = "Elements in page should be $elementsPerPage."
                            )
                            assertEquals(
                                expected = true,
                                actual = page.details.hasNext,
                                message = "Has next should be true."
                            )
                            assertEquals(
                                expected = false,
                                actual = page.details.hasPrevious,
                                message = "Has previous should be false."
                            )
                            assertEquals(
                                expected = true,
                                actual = page.details.isFirst,
                                message = "Is first should be true."
                            )
                            assertEquals(
                                expected = false,
                                actual = page.details.isLast,
                                message = "Is last should be false."
                            )
                        }

                        1 -> {
                            assertEquals(
                                expected = elementsPerPage,
                                actual = page.details.elementsInPage,
                                message = "Elements in page should be $elementsPerPage."
                            )
                            assertEquals(
                                expected = true,
                                actual = page.details.hasNext,
                                message = "Has next should be true."
                            )
                            assertEquals(
                                expected = true,
                                actual = page.details.hasPrevious,
                                message = "Has previous should be true."
                            )
                            assertEquals(
                                expected = false,
                                actual = page.details.isFirst,
                                message = "Is first should be false."
                            )
                            assertEquals(
                                expected = false,
                                actual = page.details.isLast,
                                message = "Is last should be false."
                            )
                        }

                        2 -> {
                            assertEquals(
                                expected = 2,
                                actual = page.details.elementsInPage,
                                message = "Elements in page should be 2."
                            )
                            assertEquals(
                                expected = false,
                                actual = page.details.hasNext,
                                message = "Has next should be false."
                            )
                            assertEquals(
                                expected = true,
                                actual = page.details.hasPrevious,
                                message = "Has previous should be true."
                            )
                            assertEquals(
                                expected = false,
                                actual = page.details.isFirst,
                                message = "Is first should be false."
                            )
                            assertEquals(
                                expected = true,
                                actual = page.details.isLast,
                                message = "Is last should be true."
                            )
                        }
                    }
                }
            }

            rollback()

            val page: Page<Employee> = employeeService.findAll()
            assertEquals(
                expected = 0,
                actual = page.details.totalElements,
                message = "After rollback total elements should be 0."
            )
        }
    }

    @Test
    fun testPaginationCount(): Unit = testSuspend {
        newSuspendedTransaction {
            val sessionContext: SessionContext = mockk<SessionContext>()

            val employeeRepository: IEmployeeRepository by inject(
                parameters = { parametersOf(sessionContext) }
            )

            val totalRecords = 10

            repeat(times = totalRecords) {
                val employeeRequest: EmployeeRequest = EmployeeTestUtils.newEmployeeRequest()
                employeeRepository.create(employeeRequest)
            }

            employeeRepository.findAll().also { page ->
                assertEquals(
                    expected = totalRecords,
                    actual = page.details.totalElements,
                    message = "There must be 10 employees in the database."
                )
                assertEquals(
                    expected = employeeRepository.count(),
                    actual = totalRecords,
                    message = "There must be 10 employees in the database."
                )
            }

            rollback()

            employeeRepository.findAll().also { page ->
                assertEquals(
                    expected = 0,
                    actual = page.details.totalElements,
                    message = "There must be 10 employees in the database."
                )
            }
        }
    }

    @Test
    fun testRandomPagination(): Unit = testSuspend {
        newSuspendedTransaction {
            val sessionContext: SessionContext = mockk<SessionContext>()

            val employeeService: EmployeeService by inject(
                parameters = { parametersOf(sessionContext) }
            )

            val totalRecords = 1000
            val names: List<String> = List(totalRecords) { "Employee_${it + 1}" } // Create identifiable names.

            // Create test records.
            names.forEachIndexed { index, name ->
                val employeeRequest = EmployeeRequest(
                    firstName = name,
                    lastName = "Surname_$index",
                    workEmail = "$name.surname_$index@work.com",
                    dob = LocalDate(year = 2000, month = 1, day = (index % 28) + 1), // Ensure valid day of month.
                    honorific = Honorific.entries.random(),
                    maritalStatus = MaritalStatus.entries.random()
                )

                employeeService.create(request = employeeRequest)
            }

            // Test random pagination.
            (0..25).forEach { _ ->
                val pageSize: Int = Random.nextInt(from = 1, until = 50)
                val pageIndex: Int = Random.nextInt(from = 0, until = (totalRecords / pageSize))

                employeeService.findAll(pageable = Pageable(page = pageIndex, size = pageSize)).also { page ->
                    val expectedTotalPages: Int = (totalRecords + pageSize - 1) / pageSize // Calculate expected total pages.
                    val startIndex: Int = pageIndex * pageSize // Calculate the start index of the records for the current page.

                    assertEquals(
                        expected = expectedTotalPages,
                        actual = page.details.totalPages,
                        message = "Total pages should be $expectedTotalPages."
                    )
                    assertEquals(
                        expected = totalRecords,
                        actual = page.details.totalElements,
                        message = "Total elements should be $totalRecords."
                    )
                    assertEquals(
                        expected = pageSize,
                        actual = page.details.elementsPerPage,
                        message = "Elements per page should be $pageSize."
                    )
                    assertEquals(
                        expected = minOf(pageSize, totalRecords - startIndex),
                        actual = page.details.elementsInPage,
                        message = "Elements in page should be ${minOf(pageSize, totalRecords - startIndex)}."
                    )
                    assertEquals(
                        expected = pageIndex,
                        actual = page.details.pageIndex,
                        message = "Page index should be $pageIndex."
                    )

                    // Verify the actual records on the page.
                    val expectedNames: List<String> = names.subList(
                        fromIndex = startIndex,
                        toIndex = minOf(a = startIndex + pageSize, b = totalRecords)
                    )
                    val actualNames: List<String> = page.content.map { it.firstName }
                    assertEquals(
                        expected = expectedNames,
                        actual = actualNames,
                        message = "Employee names on page $pageIndex should match expected."
                    )
                }
            }

            rollback()
        }
    }

    @Test
    fun testRandomPaginationWithSorting(): Unit = testSuspend {
        newSuspendedTransaction {
            val sessionContext: SessionContext = mockk<SessionContext>()

            val employeeService: EmployeeService by inject(
                parameters = { parametersOf(sessionContext) }
            )

            val totalRecords = 1000
            val names: List<String> = List(totalRecords) { "Employee_${it + 1}" }

            // Create test records and store them for later validation.
            val createdEmployees = mutableListOf<Employee>()
            names.forEachIndexed { index, name ->
                val employeeRequest = EmployeeRequest(
                    firstName = name,
                    lastName = "Surname_$index",
                    workEmail = "$name.surname_$index@work.com",
                    dob = LocalDate(year = 2000, month = 1, day = (index % 28) + 1),
                    honorific = Honorific.MS,
                    maritalStatus = MaritalStatus.SINGLE
                )
                val createdEmployee: Employee = employeeService.create(request = employeeRequest).getOrThrow()
                createdEmployees.add(createdEmployee)
            }

            val sortFields: List<String> = listOf("firstName", "lastName", "honorific", "maritalStatus", "dob")

            (0..25).forEach { _ ->
                val pageSize: Int = Random.nextInt(from = 1, until = 50)
                val pageIndex: Int = Random.nextInt(from = 0, until = (totalRecords / pageSize))
                val sortField: String = sortFields.random()
                val sortOrder: Pageable.PageDirection = Pageable.PageDirection.entries.random()

                val pageable = Pageable(
                    page = pageIndex,
                    size = pageSize,
                    sort = listOf(Pageable.PageSort(field = sortField, direction = sortOrder))
                )

                // Sort createdEmployees for validation according to the same criteria
                val sortedEmployees: List<Employee> = when (sortField) {
                    "firstName", "lastName" -> {
                        if (sortOrder == Pageable.PageDirection.ASC) {
                            createdEmployees.sortedBy { it.getSortValue(sortField) as String }
                        } else {
                            createdEmployees.sortedByDescending { it.getSortValue(sortField) as String }
                        }
                    }

                    "honorific", "maritalStatus" -> {
                        if (sortOrder == Pageable.PageDirection.ASC) {
                            createdEmployees.sortedBy { (it.getSortValue(sortField) as Enum<*>).name }
                        } else {
                            createdEmployees.sortedByDescending { (it.getSortValue(sortField) as Enum<*>).name }
                        }
                    }

                    "dob" -> {
                        if (sortOrder == Pageable.PageDirection.ASC) {
                            createdEmployees.sortedBy { it.getSortValue(sortField) as LocalDate }
                        } else {
                            createdEmployees.sortedByDescending { it.getSortValue(sortField) as LocalDate }
                        }
                    }

                    else -> throw IllegalArgumentException("Unknown sort field: $sortField")
                }

                employeeService.findAll(pageable = pageable).also { page ->
                    val expectedTotalPages: Int = (totalRecords + pageSize - 1) / pageSize
                    val expectedRecordsForPage: List<Employee> = sortedEmployees
                        .drop(n = pageIndex * pageSize)
                        .take(pageSize)

                    assertEquals(
                        expected = expectedTotalPages,
                        actual = page.details.totalPages,
                        message = "Total pages should be $expectedTotalPages."
                    )
                    assertEquals(
                        expected = totalRecords,
                        actual = page.details.totalElements,
                        message = "Total elements should be $totalRecords."
                    )
                    assertEquals(
                        expected = pageSize,
                        actual = page.details.elementsPerPage,
                        message = "Elements per page should be $pageSize."
                    )
                    assertEquals(
                        expected = minOf(pageSize, totalRecords - ((pageIndex - 1) * pageSize)),
                        actual = page.details.elementsInPage,
                        message = "Elements in page should be ${minOf(pageSize, totalRecords - ((pageIndex - 1) * pageSize))}."
                    )
                    assertEquals(
                        expected = pageIndex,
                        actual = page.details.pageIndex,
                        message = "Page index should be $pageIndex."
                    )

                    // Verify the actual records on the page are sorted and match the expected slice from sortedEmployees.
                    val actualValues: List<Comparable<*>> = page.content.map { it.getSortValue(sortField) }
                    val expectedValues: List<Comparable<*>> = expectedRecordsForPage.map { it.getSortValue(sortField) }

                    if (sortField == "dob") {
                        // Cast to LocalDate for comparison if the field is a date.
                        val actualDates: List<*> = actualValues as List<*>
                        val expectedDates: List<*> = expectedValues as List<*>
                        assertEquals(
                            expected = expectedDates,
                            actual = actualDates,
                            message = "Dates on page $pageIndex should match the expected dates."
                        )
                    } else {
                        // For string comparisons, including enums.
                        val actualStrings: List<String> = actualValues.map { it.toString() }
                        val expectedStrings: List<String> = expectedValues.map { it.toString() }
                        assertEquals(
                            expected = expectedStrings,
                            actual = actualStrings,
                            message = "Records on page $pageIndex should be correctly sorted by $sortField in ${sortOrder.name} " +
                                    "order and match the expected records."
                        )
                    }
                }
            }

            rollback()
        }
    }

    private fun Employee.getSortValue(sortField: String): Comparable<*> {
        return when (sortField) {
            "firstName" -> this.firstName
            "lastName" -> this.lastName
            "honorific" -> this.honorific
            "maritalStatus" -> this.maritalStatus
            "dob" -> this.dob
            else -> throw IllegalArgumentException("Unknown sort field: $sortField")
        }
    }
}
