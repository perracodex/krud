/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kcrud.access.rbac.entity.role

import kcrud.access.rbac.entity.scope.RbacScopeRuleDto
import kcrud.access.rbac.entity.scope.RbacScopeRuleRequest
import kotlinx.serialization.Serializable

/**
 * Represents the request to create/update a concrete RBAC Role.
 *
 * @property roleName The unique role name.
 * @property description Optional description of the role.
 * @property isSuper Whether this is a super-role, in which case it has all permissions granted.
 * @property scopeRules The list of [RbacScopeRuleDto] entries for the role.
 */
@Serializable
public data class RbacRoleRequest(
    val roleName: String,
    val description: String? = null,
    val isSuper: Boolean,
    val scopeRules: List<RbacScopeRuleRequest>? = null
)
