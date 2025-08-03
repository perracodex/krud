/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.database.extensions

import org.jetbrains.exposed.v1.jdbc.Query

/**
 * Extension function to check if any row exists that matches the query conditions
 *
 * @return `true` if any row exists, `false` otherwise.
 */
public fun Query.exists(): Boolean = this.count() > 0
