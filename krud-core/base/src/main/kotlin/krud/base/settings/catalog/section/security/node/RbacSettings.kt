/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.base.settings.catalog.section.security.node

/**
 * RBAC settings.
 *
 * @property isEnabled Flag to enable/disable RBAC authentication.
 */
public data class RbacSettings internal constructor(
    val isEnabled: Boolean
)
