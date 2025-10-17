/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.server.health.check

import io.ktor.server.application.*
import io.ktor.server.request.*
import kotlinx.serialization.Serializable
import krud.base.env.EnvironmentType
import krud.base.env.HealthCheckApi
import krud.base.settings.AppSettings
import krud.base.util.NetworkUtils

/**
 * Used to check the deployment configuration of the application.
 *
 * @property errors List of errors found during the health check.
 * @property configured The [ConfiguredDeployment] settings.
 * @property serverSpec The [ServerSpec] configuration of the server.
 * @property connectors The connectors used by the application.
 */
@HealthCheckApi
@Serializable
public data class DeploymentHealth private constructor(
    val errors: MutableList<String>,
    val configured: ConfiguredDeployment,
    val serverSpec: ServerSpec,
    val connectors: MutableMap<String, List<String>>,
) {
    internal companion object {
        /**
         * Creates a new [DeploymentHealth] instance.
         * We need to use a suspendable factory method as getting the connectors requires a suspend function.
         */
        suspend fun create(call: ApplicationCall): DeploymentHealth {
            val connectors: MutableMap<String, List<String>> = NetworkUtils.getConnectors(
                application = call.application
            )

            return DeploymentHealth(
                errors = mutableListOf(),
                configured = ConfiguredDeployment(),
                serverSpec = ServerSpec(request = call.request),
                connectors = connectors
            )
        }
    }

    /**
     * Contains the deployment settings.
     *
     * @property protocol The network protocol used by the application, such as "http" or "https".
     * @property port The network port the server listens to on.
     * @property sslPort The network port the server listens on for secure connections.
     * @property host The network address the server is bound to.
     * @property allowedHosts The list of allowed hosts configured in CORS.
     */
    @Serializable
    public data class ConfiguredDeployment internal constructor(
        val protocol: String = NetworkUtils.getProtocol().name,
        val port: Int = AppSettings.deployment.port,
        val sslPort: Int = AppSettings.deployment.sslPort,
        val host: String = NetworkUtils.getServerUrl().toString(),
        val allowedHosts: List<String> = AppSettings.cors.allowedHosts,
    )

    /**
     * Contains the server configuration.
     *
     * @property serverHost The host to which the request are sent.
     * @property serverPort Port to which the request are sent, for example, 80 or 443.
     * @property localHost The host on which the request are received.
     * @property localPort The port on which the request are received, for example, 80 or 443.
     * @property remoteHostHost Client address or host name if it can be resolved.
     * @property remoteAddress Client address.
     * @property remotePort Client port.
     * @property httpVersion The HTTP version of the request.
     * @property scheme The scheme of the request, for example, "http" or "https".
     */
    @Serializable
    public data class ServerSpec private constructor(
        val serverHost: String?,
        val serverPort: Int?,
        val localHost: String?,
        val localPort: Int?,
        val remoteHostHost: String?,
        val remoteAddress: String?,
        val remotePort: Int?,
        val httpVersion: String?,
        val scheme: String?
    ) {
        internal constructor(request: ApplicationRequest) : this(
            serverHost = request.local.serverHost,
            serverPort = request.local.serverPort,
            localHost = request.local.localHost,
            localPort = request.local.localPort,
            remoteHostHost = request.local.remoteHost,
            remoteAddress = request.local.remoteAddress,
            remotePort = request.local.remotePort,
            httpVersion = request.httpVersion,
            scheme = request.local.scheme
        )
    }

    init {
        val className: String? = this::class.simpleName
        val environment: EnvironmentType = AppSettings.runtime.environment

        if (connectors.isEmpty()) {
            errors.add("$className. No connectors detected.")
        }

        if (environment == EnvironmentType.PROD) {
            if (AppSettings.cors.allowAllHosts()) {
                errors.add("$className. Allowing all hosts. '$environment'.")
            }

            if (!NetworkUtils.isSecureProtocol(protocol = configured.protocol)) {
                errors.add(
                    "$className. Configured insecure '${configured.protocol}' protocol. '$environment'."
                )
            }

            serverSpec.scheme?.let { scheme ->
                if (!NetworkUtils.isSecureProtocol(protocol = scheme)) {
                    errors.add(
                        "$className. Running with insecure '$scheme' protocol. '$environment'."
                    )
                }
            }

            if (configured.port == configured.sslPort) {
                errors.add(
                    "$className. Secure and insecure ports are the same: ${configured.port}. " +
                            "$environment."
                )
            }

            if (AppSettings.security.useSecureConnection) {
                if (!NetworkUtils.isSecurePort(port = configured.sslPort)) {
                    errors.add(
                        "$className. Configured SSL port is not secure or not set. " +
                                "Port: ${configured.sslPort}. $environment."
                    )
                }

                val runtimePorts: List<Int?> = listOf(
                    serverSpec.serverPort,
                    serverSpec.localPort,
                    serverSpec.remotePort
                )
                if (!NetworkUtils.isSecurePort(ports = runtimePorts)) {
                    errors.add(
                        "$className. Runtime ports are not secure or not set. " +
                                "Ports: $runtimePorts. $environment."
                    )
                }
            } else {
                if (configured.port == 0) {
                    errors.add("$className. Insecure port is not set. $environment.")
                }
            }
        }
    }
}
