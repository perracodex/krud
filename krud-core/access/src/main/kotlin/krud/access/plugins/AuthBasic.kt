/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.access.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import krud.access.context.SessionContextFactory
import krud.base.context.clearSessionContext
import krud.base.context.sessionContext
import krud.base.settings.AppSettings

/**
 * Configures the Basic authentication.
 *
 * The Basic authentication scheme is a part of the HTTP framework used for access control and authentication.
 * In this scheme, actor credentials are transmitted as username/password pairs encoded using Base64.
 *
 * #### References
 * - [Basic Authentication Documentation](https://ktor.io/docs/server-basic-auth.html)
 */
public fun Application.configureBasicAuthentication() {

    authentication {
        basic(name = AppSettings.security.basicAuth.providerName) {
            realm = AppSettings.security.basicAuth.realm

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
