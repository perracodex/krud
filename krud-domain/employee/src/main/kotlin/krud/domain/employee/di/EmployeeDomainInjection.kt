/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.domain.employee.di

import krud.domain.contact.repository.ContactRepository
import krud.domain.contact.repository.IContactRepository
import krud.domain.employee.repository.EmployeeRepository
import krud.domain.employee.repository.IEmployeeRepository
import krud.domain.employee.service.EmployeeService
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.scopedOf
import org.koin.dsl.module
import org.koin.ktor.plugin.RequestScope

/**
 * Employee domain dependency injection module.
 */
public object EmployeeDomainInjection {

    /**
     * Get the dependency injection module for the Employee domain.
     */
    public fun get(): Module {
        return module {
            // Scoped definitions within RequestScope for a single request lifecycle.
            // Services receive the SessionContext as a parameter. Repositories
            // which should only be accessed by "services" do not receive it directly.

            scope<RequestScope> {
                scopedOf(::ContactRepository) {
                    bind<IContactRepository>()
                }

                scopedOf(::EmployeeRepository) {
                    bind<IEmployeeRepository>()
                }

                scopedOf(::EmployeeService)
            }

            // Definitions for non-scoped (global) access.

            factoryOf(::ContactRepository) {
                bind<IContactRepository>()
            }

            factoryOf(::EmployeeRepository) {
                bind<IEmployeeRepository>()
            }

            factoryOf(::EmployeeService)
        }
    }
}
