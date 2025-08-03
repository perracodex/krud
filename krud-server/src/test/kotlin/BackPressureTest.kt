/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.test.dispatcher.*
import io.ktor.util.collections.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import krud.access.test.RbacTestUtils
import krud.base.test.TestUtils
import krud.database.test.DatabaseTestUtils
import krud.domain.employee.model.EmployeeRequest
import krud.domain.employee.test.EmployeeTestUtils
import org.koin.core.component.KoinComponent
import kotlin.system.measureTimeMillis
import kotlin.test.*

class BackPressureTest : KoinComponent {

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
     * Test the backpressure with controlled load increase
     * by sending a large number of concurrent requests to the server
     * and checking the average response time.
     */
    @Test
    fun testWithControlledLoadIncrease(): Unit = testApplication {
        startApplication()

        val authToken: String = RbacTestUtils.newAuthenticationToken()

        val startConcurrency = 100
        val maxConcurrency = 1000
        val step = 100
        val operationMix: List<String> = listOf("read", "write") // Simulate a mix of read and write operations.

        var totalAverageResponseTime = 0.0
        var largestAverageResponseTime = 0.0

        val writtenEmployeeIds = ConcurrentSet<String>()

        for (currentLoad in startConcurrency..maxConcurrency step step) {
            println("Testing with concurrency level: $currentLoad")

            val totalTimeMillis: Long = measureTimeMillis {
                testSuspend {
                    repeat(currentLoad) { index ->
                        launch {
                            val operationType: String = operationMix[index % operationMix.size]

                            if (operationType == "write") {
                                val employeeRequestJson: String = Json.encodeToString<EmployeeRequest>(
                                    value = EmployeeTestUtils.newEmployeeRequest()
                                )

                                val writeResponse: HttpResponse = client.post(urlString = "/api/v1/employees") {
                                    header(key = HttpHeaders.Authorization, value = "Bearer $authToken")
                                    contentType(type = ContentType.Application.Json)
                                    setBody(body = employeeRequestJson)
                                }

                                val writeResponseBody: String = writeResponse.bodyAsText()
                                val writeJsonElement: JsonElement = Json.parseToJsonElement(writeResponseBody)
                                val writeEmployeeId: String = getJsonContent(element = writeJsonElement, key = "id")
                                writtenEmployeeIds.add(writeEmployeeId)

                                assertEquals(HttpStatusCode.Created, writeResponse.status)
                            } else {
                                if (writtenEmployeeIds.isEmpty()) {
                                    // If no employees are written yet then using random employeeId for read operations.
                                    // Does not matter if it does not exist.
                                    val employeeId = "9965adab-21ac-4339-8cc0-8a44c2287c95"
                                    val readResponse: HttpResponse = client.get(urlString = "/api/v1/employees/$employeeId") {
                                        header(key = HttpHeaders.Authorization, value = "Bearer $authToken")
                                    }
                                    assertEquals(expected = HttpStatusCode.NotFound, actual = readResponse.status)
                                } else {
                                    val randomIndex: Int = (0 until writtenEmployeeIds.size).random()
                                    val employeeId: String = writtenEmployeeIds.elementAt(index = randomIndex)
                                    val readResponse: HttpResponse = client.get(urlString = "/api/v1/employees/$employeeId") {
                                        header(key = HttpHeaders.Authorization, value = "Bearer $authToken")
                                    }
                                    assertEquals(expected = HttpStatusCode.OK, actual = readResponse.status)
                                }
                            }

                            delay(timeMillis = 10L) // Simulate processing delay.
                        }
                    }
                }
            }

            val averageResponseTime: Double = totalTimeMillis.toDouble() / currentLoad
            println("Average response time at $currentLoad concurrency: $averageResponseTime ms")

            totalAverageResponseTime += averageResponseTime
            if (averageResponseTime > largestAverageResponseTime) {
                largestAverageResponseTime = averageResponseTime
            }

            // Check if average response time is within acceptable limits.
            assertTrue(message = "Average response time should be below 500ms") { averageResponseTime < 500L }
        }

        val overallAverageResponseTime: Double = totalAverageResponseTime / ((maxConcurrency - startConcurrency) / step + 1)
        println("Overall average response time: $overallAverageResponseTime ms")
        println("Largest average response time: $largestAverageResponseTime ms")
    }

    /**
     * Test the read backpressure operations by sending
     * a large number of concurrent read requests to the server.
     */
    @Test
    fun testConcurrentReads(): Unit = testApplication {
        // Start the application so that the Koin DI container is initialized.
        startApplication()

        // Prepare the employee request.
        val employeeRequest: EmployeeRequest = EmployeeTestUtils.newEmployeeRequest()
        val employeeRequestJson: String = Json.encodeToString<EmployeeRequest>(value = employeeRequest)

        // Generate the authentication token required for the request call.
        val authToken: String = RbacTestUtils.newAuthenticationToken()

        // Perform the backpressure test by sending a large number
        // of concurrent requests to the server.
        val totalCalls = 10000
        testSuspend {
            val writeResponse: HttpResponse = client.post(urlString = "/api/v1/employees") {
                header(key = HttpHeaders.Authorization, value = "Bearer $authToken")
                contentType(type = ContentType.Application.Json)
                setBody(body = employeeRequestJson)
                accept(contentType = ContentType.Application.Json)
            }

            val writeResponseBody: String = writeResponse.bodyAsText()
            val writeJsonElement: JsonElement = Json.parseToJsonElement(string = writeResponseBody)
            val writeEmployeeId: String = getJsonContent(element = writeJsonElement, key = "id")

            val jobs: List<Deferred<Unit>> = List(size = totalCalls) {
                async {
                    val readResponse: HttpResponse = client.get(urlString = "/api/v1/employees/$writeEmployeeId") {
                        header(key = HttpHeaders.Authorization, value = "Bearer $authToken")
                    }
                    assertEquals(expected = HttpStatusCode.OK, actual = readResponse.status)

                    val readResponseBody: String = readResponse.bodyAsText()
                    val readJsonElement: JsonElement = Json.parseToJsonElement(string = readResponseBody)
                    val readEmployeeId: String = getJsonContent(element = readJsonElement, key = "id")
                    assertEquals(expected = writeEmployeeId, actual = readEmployeeId)
                }
            }

            jobs.awaitAll()
        }
    }

    /**
     * Test the write backpressure operations by sending
     * a large number of concurrent write requests to the server.
     */
    @Test
    fun testConcurrentWrites(): Unit = testApplication {
        // Start the application so that the Koin DI container is initialized.
        startApplication()

        // Prepare the employee request.
        val employeeRequest: EmployeeRequest = EmployeeTestUtils.newEmployeeRequest()
        val employeeRequestJson: String = Json.encodeToString<EmployeeRequest>(value = employeeRequest)

        // Generate the authentication token required for the request call.
        val authToken: String = RbacTestUtils.newAuthenticationToken()

        // Perform the backpressure test by sending a large number
        // of concurrent requests to the server.
        val totalCalls = 10000
        testSuspend {
            val jobs: List<Deferred<Unit>> = List(size = totalCalls) {
                async {
                    val response: HttpResponse = client.post(urlString = "/api/v1/employees") {
                        header(key = HttpHeaders.Authorization, value = "Bearer $authToken")
                        contentType(type = ContentType.Application.Json)
                        setBody(body = employeeRequestJson)
                    }
                    assertEquals(expected = HttpStatusCode.Created, actual = response.status)
                }
            }

            jobs.awaitAll()
        }
    }

    /**
     * Test the concurrent read and write operations by sending
     * a large number of concurrent read and write requests to the server.
     */
    @Test
    fun testConcurrentReadWrite(): Unit = testApplication {
        // Start the application so that the Koin DI container is initialized.
        startApplication()

        // Generate the authentication token required for the request call.
        val authToken: String = RbacTestUtils.newAuthenticationToken()

        // Concurrently send write and read requests.
        val totalCalls = 10000
        val writtenEmployeeIds = ConcurrentSet<String>()

        testSuspend {
            val jobs: List<Deferred<Unit>> = List(size = totalCalls) { index ->

                // Alternate between write and read to simulate concurrent operations.
                if (index % 2 == 0) {
                    async {
                        // Prepare a unique employee request for write operations.
                        val employeeRequest: EmployeeRequest = EmployeeTestUtils.newEmployeeRequest()
                        val employeeRequestJson = Json.encodeToString<EmployeeRequest>(value = employeeRequest)
                        val writeResponse: HttpResponse = client.post(urlString = "/api/v1/employees") {
                            header(key = HttpHeaders.Authorization, value = "Bearer $authToken")
                            contentType(type = ContentType.Application.Json)
                            setBody(body = employeeRequestJson)
                        }

                        val writeResponseBody: String = writeResponse.bodyAsText()
                        val writeJsonElement: JsonElement = Json.parseToJsonElement(string = writeResponseBody)
                        val writeEmployeeId: String = getJsonContent(element = writeJsonElement, key = "id")
                        writtenEmployeeIds.add(writeEmployeeId)

                        assertEquals(expected = HttpStatusCode.Created, actual = writeResponse.status)
                    }
                } else {
                    if (writtenEmployeeIds.isEmpty()) {
                        delay(timeMillis = 10L)
                    }

                    async {
                        if (writtenEmployeeIds.isEmpty()) {
                            // If no employees are written yet then using random employeeId for read operations.
                            // Does not matter if it does not exist.
                            val employeeId = "9965adab-21ac-4339-8cc0-8a44c2287c95"
                            val readResponse: HttpResponse = client.get(urlString = "/api/v1/employees/$employeeId") {
                                header(key = HttpHeaders.Authorization, value = "Bearer $authToken")
                            }
                            assertEquals(expected = HttpStatusCode.NotFound, actual = readResponse.status)
                        } else {
                            val randomIndex: Int = (0 until writtenEmployeeIds.size).random()
                            val employeeId: String = writtenEmployeeIds.elementAt(index = randomIndex)
                            val readResponse: HttpResponse = client.get(urlString = "/api/v1/employees/$employeeId") {
                                header(key = HttpHeaders.Authorization, value = "Bearer $authToken")
                            }
                            assertEquals(expected = HttpStatusCode.OK, actual = readResponse.status)
                        }
                    }
                }
            }

            // Await completion of all jobs.
            jobs.awaitAll()
        }
    }

    @Suppress("SameParameterValue")
    private fun getJsonContent(element: JsonElement, key: String): String {
        val primitive: JsonPrimitive? = element.jsonObject[key]?.jsonPrimitive
        assertNotNull(primitive)
        return primitive.content
    }
}
