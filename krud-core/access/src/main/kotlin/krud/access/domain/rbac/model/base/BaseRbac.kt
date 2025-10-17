/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.access.domain.rbac.model.base

import krud.access.domain.rbac.service.RbacFieldAnonymization
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * Base class for classes that should support field level anonymization.
 *
 * It provides functionality to anonymize specified fields using reflection,
 * creating a new instance with certain fields replaced by predefined placeholders.
 *
 * While this class defines the core functionality for anonymization, it still requires concrete
 * implementations to use the [anonymize] method to specify which fields should be anonymized.
 *
 * Using placeholders instead of enforcing the target to use null arguments ensures that
 * the anonymized instance remains consistent with its original structure, which is important for
 * serialization and maintains immutability.
 *
 * This class has scope for improvement, such as handling nested classes and caching the reflection
 * results for performance optimization.
 *
 * @see [RbacFieldAnonymization]
 */
public abstract class BaseRbac {

    /**
     * Anonymizes specified fields by using reflection to create a new class instance
     * with the fields anonymized.
     *
     * Anonymized fields are replaced with predefined placeholders based on their type.
     * Fields of nested instances are also supported.
     *
     * #### Usage
     * ```
     * data class Sample(val name: String, val phone: String): BaseRbac()
     * val instance = Sample(...)
     * val anonymized = instance.anonymize(listOf("phone"))
     * ```
     *
     * @param T The target type, automatically inferred from the inheriting class.
     * @param fields A list of strings indicating which fields should be anonymized.
     *               The fields are case-insensitive and should match the exact name of the property.
     *               If null or empty, the original instance is returned without changes.
     * @return A new instance with specified fields anonymized.
     */
    public inline fun <reified T : BaseRbac> anonymize(fields: List<String>?): T {
        // Delegate to the internal, non-inline anonymization method while preserving the type information.
        return internalAnonymize(fields = fields, clazz = T::class) as T
    }

    /**
     * Handles the actual anonymization logic, capable of handling nesting. This method is not inline,
     * enabling it to be recursively called without causing issues related to inline recursion.
     *
     * @param fields A list of strings representing the fields to be anonymized, supporting nested notation.
     * @param clazz The KClass instance of the class type, used for reflection.
     * @return A new instance (or nested instance) with specified fields anonymized.
     */
    @Suppress("UNCHECKED_CAST")
    public fun <T : BaseRbac> internalAnonymize(fields: List<String>?, clazz: KClass<T>): BaseRbac {
        if (fields.isNullOrEmpty()) {
            // If no fields are specified for anonymization, return the instance as is.
            return this
        }

        // Separate the fields into top-level fields and nested fields based on dot notation.
        val topLevelFields: List<String> = fields.filter {
            !it.contains(char = '.')
        }
        val nestedFields: Map<String, List<String>> = fields.filter {
            it.contains(char = '.')
        }.groupBy {
            it.substringBefore(delimiter = '.')
        }

        // Retrieve the primary constructor of the class.
        val constructor: KFunction<T> = clazz.primaryConstructor
            ?: throw IllegalArgumentException("Primary constructor not found for class: ${clazz.simpleName}")

        // Create a map of constructor parameters and their corresponding anonymized values where applicable.
        val arguments: Map<KParameter, Any?> = constructor.parameters.associateWith { parameter ->
            // Find the corresponding property for each constructor parameter.
            val property: KProperty1<T, *>? = clazz.memberProperties.firstOrNull { it.name == parameter.name }

            if (property == null) {
                // Return null for non-existent properties. Unlikely with well-formed data.
                return@associateWith null
            } else if (property.name in topLevelFields) {
                // Anonymize top-level fields directly.
                return@associateWith RbacFieldAnonymization.anonymize(value = property.get(this as T))
            } else if (property.name in nestedFields.keys) {
                // Handle nested fields: find the nested class and recursively anonymize it.

                val nestedInstance: BaseRbac? = property.get(this as T) as? BaseRbac
                val newFields: List<String>? = nestedFields[property.name]?.map { it.substringAfter(delimiter = '.') }

                return@associateWith nestedInstance?.internalAnonymize(
                    fields = newFields,
                    clazz = nestedInstance::class
                ) ?: property.get(this) // If the nested instance is null, keep the original value.
            } else {
                return@associateWith property.get(this as T)
            }
        }

        // Construct and return the new instance of the class with anonymized fields.
        return constructor.callBy(args = arguments)
    }
}
