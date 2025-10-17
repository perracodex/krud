/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.access.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import krud.access.context.SessionContextFactory
import krud.access.domain.rbac.annotation.RbacApi
import krud.access.domain.rbac.view.RbacLoginView
import krud.base.context.clearSessionContext
import krud.base.context.sessionContext

/**
 * Refreshes the default actors and configures the RBAC from login authentication.
 *
 * Demonstrates how to use form-based authentication, in which case
 * principal is not propagated across different requests, so we
 * must use [Sessions] to store the actor information.
 *
 * #### References
 * - [Basic Authentication Documentation](https://ktor.io/docs/server-basic-auth.html)
 */
@OptIn(RbacApi::class)
public fun Application.configureRbac() {

    // Configure the RBAC from login authentication.
    authentication {
        form(name = RbacLoginView.RBAC_LOGIN_PATH) {
            userParamName = RbacLoginView.KEY_USERNAME
            passwordParamName = RbacLoginView.KEY_PASSWORD

            challenge {
                call.clearSessionContext()
                call.respondRedirect(url = RbacLoginView.RBAC_LOGIN_PATH)
            }

            validate { credential ->
                SessionContextFactory.from(credential = credential)?.let { sessionContext ->
                    this.sessionContext = sessionContext
                    return@validate sessionContext
                }

                this.clearSessionContext()
                return@validate null
            }
        }
    }
}
