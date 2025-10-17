/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.domain.employment.repository

import io.perracodex.exposed.pagination.Page
import io.perracodex.exposed.pagination.Pageable
import krud.domain.employment.model.Employment
import krud.domain.employment.model.EmploymentRequest
import kotlin.uuid.Uuid

/**
 * Responsible for managing employment data.
 */
internal interface IEmploymentRepository {

    /**
     * Retrieves all employments.
     *
     * @param pageable The pagination options to be applied, or null for a single all-in-one page.
     * @return List of [Employment] entries.
     */
    fun findAll(pageable: Pageable? = null): Page<Employment>

    /**
     * Retrieves an `employment` by its ID.
     *
     * @param employeeId The ID of the employee associated with the employment.
     * @param employmentId The ID of the employment to be retrieved.
     * @return The resolved [Employment] if found, null otherwise.
     */
    fun findById(employeeId: Uuid, employmentId: Uuid): Employment?

    /**
     * Retrieves all employment entries for a given employee.
     *
     * @param employeeId The ID of the employee associated with the employment.
     * @return List of [Employment] entries.
     */
    fun findByEmployeeId(employeeId: Uuid): List<Employment>

    /**
     * Creates a new `employment`.
     *
     * @param employeeId The employee ID associated with the employment.
     * @param request The [EmploymentRequest] to be created.
     * @return The created [Employment], or null if the employee was not found.
     */
    fun create(employeeId: Uuid, request: EmploymentRequest): Employment?

    /**
     * Updates an employment's details.
     *
     * @param employeeId The employee ID associated with the employment.
     * @param employmentId The ID of the employment to be updated.
     * @param request The new [EmploymentRequest] to update.
     * @return The updated [Employment], or null if the employment was not found.
     */
    fun update(employeeId: Uuid, employmentId: Uuid, request: EmploymentRequest): Employment?

    /**
     * Deletes an `employment` using the provided ID.
     *
     * @param employmentId The ID of the employment to be deleted.
     * @return The number of delete records.
     */
    fun delete(employmentId: Uuid): Int

    /**
     * Deletes all the employments for the given employee ID.
     *
     * @param employeeId The ID of the employee to delete all its employments.
     * @return The number of delete records.
     */
    fun deleteAll(employeeId: Uuid): Int

    /**
     * Retrieves the total count of employments.
     *
     * @param employeeId The ID of the employee to count its employments, or null to count all employments.
     * @return The total count of existing records.
     */
    fun count(employeeId: Uuid? = null): Int
}
