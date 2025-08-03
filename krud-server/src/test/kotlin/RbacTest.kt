/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.test.dispatcher.*
import io.mockk.mockk
import kotlinx.serialization.json.Json
import krud.access.test.RbacTestUtils
import krud.base.context.SessionContext
import krud.base.test.TestUtils
import krud.database.schema.admin.rbac.type.RbacAccessLevel
import krud.database.test.DatabaseTestUtils
import krud.domain.employee.model.Employee
import krud.domain.employee.model.EmployeeRequest
import krud.domain.employee.service.EmployeeService
import krud.domain.employee.test.EmployeeTestUtils
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.test.*
import kotlin.uuid.Uuid

class RbacTest : KoinComponent {

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
     * Tests Role-Based Access Control (RBAC) enforcement by systematically tests different combinations
     * of access levels against various API operations.
     * Verifies that each API endpoint honors the correct access level by either permitting or denying requests.
     */
    @Test
    fun testAccess(): Unit = testApplication {
        startApplication()

        // Prepare a generic employee request used in POST and PUT endpoints.
        val employeeRequest: EmployeeRequest = EmployeeTestUtils.newEmployeeRequest()
        val employeeRequestJson: String = Json.encodeToString<EmployeeRequest>(value = employeeRequest)

        // List defining the expected outcomes for various operations based on different RBAC levels.
        // Placeholder {employee_id} will be replaced with an actual employee's ID during tests.
        // 'first' represents the API endpoint for creating an employee.
        // 'second' is the HTTP method used for this action.
        // 'third' is a map of RBACAccessLevel to the expected HttpStatusCode for that level of access.
        val endpointsAndExpectedOutcomes = listOf(
            // Creating an employee.
            Triple(
                first = "/v1/employees",
                second = "POST",
                third = mapOf(
                    RbacAccessLevel.NONE to HttpStatusCode.Forbidden, // No access should result in Forbidden.
                    RbacAccessLevel.VIEW to HttpStatusCode.Forbidden, // View-only access should still forbid creation.
                    RbacAccessLevel.FULL to HttpStatusCode.Created // Full access allows creating, should return Created status.
                )
            ),
            // Getting all employees.
            Triple(
                first = "/v1/employees",
                second = "GET",
                third = mapOf(
                    RbacAccessLevel.NONE to HttpStatusCode.Forbidden, // No access results in Forbidden status.
                    RbacAccessLevel.VIEW to HttpStatusCode.OK, // View access should successfully retrieve the list.
                    RbacAccessLevel.FULL to HttpStatusCode.OK // Full access should also successfully retrieve the list.
                )
            ),
            // Getting a single employee by ID:
            Triple(
                first = "/v1/employees/{employee_id}",
                second = "GET",
                third = mapOf(
                    RbacAccessLevel.NONE to HttpStatusCode.Forbidden, // No access results in Forbidden.
                    RbacAccessLevel.VIEW to HttpStatusCode.OK, // View access allows retrieving individual details.
                    RbacAccessLevel.FULL to HttpStatusCode.OK // Full access allows for the same.
                )
            ),
            // Updating an employee by ID.
            Triple(
                first = "/v1/employees/{employee_id}",
                second = "PUT",
                third = mapOf(
                    RbacAccessLevel.NONE to HttpStatusCode.Forbidden, // No access should result in Forbidden.
                    RbacAccessLevel.VIEW to HttpStatusCode.Forbidden, // View access does not permit updating.
                    RbacAccessLevel.FULL to HttpStatusCode.OK // Full access should allow updating.
                )
            ),
            // Deleting an employee by ID.
            Triple(
                first = "/v1/employees/{employee_id}",
                second = "DELETE",
                third = mapOf(
                    RbacAccessLevel.NONE to HttpStatusCode.Forbidden, // No access should result in Forbidden.
                    RbacAccessLevel.VIEW to HttpStatusCode.Forbidden, // View access does not permit deletion.
                    RbacAccessLevel.FULL to HttpStatusCode.OK // Full access should allow deletion.
                )
            ),
            // Deleting all employees.
            Triple(
                first = "/v1/employees",
                second = "DELETE",
                third = mapOf(
                    RbacAccessLevel.NONE to HttpStatusCode.Forbidden, // No access should result in Forbidden.
                    RbacAccessLevel.VIEW to HttpStatusCode.Forbidden, // View access does not permit bulk deletion.
                    RbacAccessLevel.FULL to HttpStatusCode.OK // Full access should allow bulk deletion.
                )
            )
        )

        var testIteration = 0

        testSuspend {
            // Create an employee to be used in the test cases for individual operations.
            val createdEmployeeId: Uuid = createEmployee().id

            // Iterate over each endpoint and method pair, along with their expected outcomes.
            for (endpointData in endpointsAndExpectedOutcomes) {
                val (
                    endpointTemplate: String,
                    requestMethod: String,
                    testOutcomes: Map<RbacAccessLevel, HttpStatusCode>
                ) = endpointData

                // Update the target endpoint by replacing its placeholder with the actual employee ID.
                // This is necessary for the GET, PUT, and DELETE operations.
                // If the endpoint does not contain the placeholder, it will remain the same.
                val endpoint: String = endpointTemplate.replace(
                    oldValue = "{employee_id}",
                    newValue = createdEmployeeId.toString()
                )

                testOutcomes.forEach { (accessLevel, expectedStatus) ->

                    // Create a unique Actor for each test case to ensure clear RBAC role assignments.
                    val authToken: String = RbacTestUtils.newAuthenticationToken(
                        accessLevel = accessLevel,
                        testIteration = testIteration
                    )
                    testIteration++
                    assertNotNull(actual = authToken, message = "Auth token should not be null")

                    // Make the request call.
                    val response: HttpResponse = when (requestMethod) {
                        "POST" -> client.post(endpoint) {
                            header(key = HttpHeaders.Authorization, value = "Bearer $authToken")
                            contentType(type = ContentType.Application.Json)
                            setBody(body = employeeRequestJson)
                        }

                        "GET" -> client.get(endpoint) {
                            header(key = HttpHeaders.Authorization, value = "Bearer $authToken")
                        }

                        "PUT" -> client.put(endpoint) {
                            header(key = HttpHeaders.Authorization, value = "Bearer $authToken")
                            contentType(type = ContentType.Application.Json)
                            setBody(body = employeeRequestJson)
                        }

                        "DELETE" -> client.delete(endpoint) {
                            header(key = HttpHeaders.Authorization, value = "Bearer $authToken")
                        }

                        else -> throw IllegalArgumentException("Unsupported request method: $requestMethod")
                    }

                    // Assert the response status.
                    assertEquals(expected = expectedStatus, actual = response.status)
                }
            }
        }
    }

    private suspend fun createEmployee(): Employee {
        val employeeRequest: EmployeeRequest = EmployeeTestUtils.newEmployeeRequest()
        val sessionContext: SessionContext = mockk<SessionContext>()

        val employeeService: EmployeeService by inject(
            parameters = { org.koin.core.parameter.parametersOf(sessionContext) }
        )

        return employeeService.create(request = employeeRequest).getOrThrow()
    }
}
