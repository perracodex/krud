/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

import io.ktor.test.dispatcher.*
import krud.access.domain.actor.di.ActorDomainInjection
import krud.access.domain.actor.model.Actor
import krud.access.domain.actor.model.ActorCredentials
import krud.access.domain.actor.model.ActorRequest
import krud.access.domain.actor.service.ActorService
import krud.access.domain.rbac.di.RbacDomainInjection
import krud.access.domain.rbac.model.role.RbacRole
import krud.access.domain.rbac.model.role.RbacRoleRequest
import krud.access.domain.rbac.model.scope.RbacScopeRuleRequest
import krud.access.domain.rbac.service.RbacService
import krud.base.test.TestUtils
import krud.database.schema.admin.rbac.type.RbacAccessLevel
import krud.database.schema.admin.rbac.type.RbacScope
import krud.database.test.DatabaseTestUtils
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.test.*
import kotlin.uuid.Uuid

/**
 * Test for the [RbacService].
 * Not for RBAC permission checks, but instead for the service interface.
 * For an example of RBAC permission checks, see the RBAC tests in the application service.
 */
class RbacActorTest : KoinComponent {

    @BeforeTest
    fun setUp() {
        TestUtils.loadSettings()
        DatabaseTestUtils.setupDatabase()
        TestUtils.setupKoin(
            modules = listOf(
                RbacDomainInjection.get(),
                ActorDomainInjection.get()
            )
        )
    }

    @AfterTest
    fun tearDown() {
        DatabaseTestUtils.closeDatabase()
        TestUtils.tearDown()
    }

    @Test
    fun testActor(): Unit = testSuspend {
        val roleName = "any_role_name"
        val description = "Any role description"

        val roleRequest = RbacRoleRequest(
            roleName = roleName,
            description = description,
            isSuper = false,
            scopeRules = listOf(
                RbacScopeRuleRequest(
                    scope = RbacScope.SYSTEM,
                    accessLevel = RbacAccessLevel.FULL
                )
            )
        )

        val rbacService: RbacService by inject()

        // Create the role.
        val rbacRole: RbacRole = rbacService.createRole(roleRequest = roleRequest)

        // Create the actor.
        val actorService: ActorService by inject()

        val username = "any_username"
        val password = "any_password"
        val actorId: Uuid = actorService.create(
            actorRequest = ActorRequest(
                roleId = rbacRole.id,
                username = username,
                password = password,
                isLocked = false
            )
        )

        var actor: Actor? = actorService.findByUsername(username = username)
        assertNotNull(actual = actor, message = "The actor was not found in the database after it was created.")
        assertEquals(expected = username, actual = actor.username)

        actor = actorService.findById(actorId = actorId)
        assertNotNull(actual = actor, message = "The actor was not found in the database after it was created.")
        assertEquals(expected = username, actual = actor.username)

        val actorCredentials: ActorCredentials? = actorService.findCredentials(actorId = actorId)
        assertNotNull(actual = actorCredentials, message = "The actor credentials was not found in the database after it was created.")
        assertEquals(expected = username, actual = actorCredentials.username)
        assertEquals(expected = password, actual = actorCredentials.password)

        // Try to create the same actor again.
        // This should fail because the username is unique.
        assertFailsWith<ExposedSQLException> {
            actorService.create(
                actorRequest = ActorRequest(
                    roleId = rbacRole.id,
                    username = username,
                    password = password,
                    isLocked = false
                )
            )
        }

        // Find a role by Actor ID.
        val roleByActor: RbacRole? = rbacService.findRoleByActorId(actorId = actorId)
        assertNotNull(actual = roleByActor, message = "The role was not found in the database after it was created.")
        assertEquals(expected = roleName, actual = roleByActor.roleName)
        assertEquals(expected = description, actual = roleByActor.description)
    }
}
