/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kcrud.base.scheduler.api.tasks.delete

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kcrud.base.scheduler.api.SchedulerRouteAPI
import kcrud.base.scheduler.service.core.SchedulerService

/**
 * Deletes all the scheduler tasks.
 */
@SchedulerRouteAPI
internal fun Route.deleteAllSchedulerTasksRoute() {
    /**
     * Deletes all scheduler tasks.
     * @OpenAPITag Scheduler
     */
    delete("scheduler/task") {
        val deletedCount: Int = SchedulerService.tasks.deleteAll()
        call.respond(status = HttpStatusCode.OK, message = deletedCount)
    }
}