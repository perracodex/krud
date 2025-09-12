/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.access.domain.rbac.repository.field

import krud.access.domain.rbac.model.field.RbacFieldRuleRequest
import krud.database.schema.admin.rbac.RbacFieldRuleTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.Uuid

/**
 * Implementation of [IRbacFieldRuleRepository].
 * Manages the persistence of [RbacFieldRuleRequest] instances.
 *
 * @see [RbacFieldRuleRequest]
 */
internal class RbacFieldRuleRepository : IRbacFieldRuleRepository {

    override fun replace(scopeRuleId: Uuid, requestList: List<RbacFieldRuleRequest>?): Int {
        return transaction {
            RbacFieldRuleTable.deleteWhere {
                RbacFieldRuleTable.scopeRuleId eq scopeRuleId
            }

            requestList?.ifEmpty { null }?.let { requestList ->
                RbacFieldRuleTable.batchInsert(requestList) { scopeRule ->
                    this.toStatement(scopeRuleId = scopeRuleId, request = scopeRule)
                }.size
            } ?: 0
        }
    }

    /**
     * Populates an SQL [BatchInsertStatement] with data from an [RbacFieldRuleRequest] instance,
     * so that it can be used to update or create a database record.
     */
    private fun BatchInsertStatement.toStatement(scopeRuleId: Uuid, request: RbacFieldRuleRequest) {
        this[RbacFieldRuleTable.scopeRuleId] = scopeRuleId
        this[RbacFieldRuleTable.fieldName] = request.fieldName
        this[RbacFieldRuleTable.accessLevel] = request.accessLevel
    }
}
