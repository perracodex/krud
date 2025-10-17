/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.base.util

import kotlinx.serialization.SerialName
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation

/**
 * Retrieves the fully qualified name of a property path.
 *
 * Infers the root class from the first property, and appends subsequent property names,
 * respecting any `@SerialName` annotations present on classes or properties.
 *
 * #### Usage
 * ```kotlin
 * @Serializable
 * @SerialName("EmploymentRequestEntity")
 * data class EmploymentRequest(
 *     val period: Period,
 *     val status: EmploymentStatus,
 * )
 *
 * @Serializable
 * data class Period(
 *     val isActive: Boolean,
 *     @SerialName("end_date") val endDate: LocalDate?
 * )
 *
 * // Retrieving "EmploymentRequestEntity.period.end_date"
 * val fieldFullName = getPropertyName(
 *     EmploymentRequest::period,
 *     Period::endDate
 * )
 * ```
 *
 * @param root The first [KProperty1] reference, used to infer the root class.
 * @param remaining The subsequent [KProperty1] references representing the path.
 * @return The fully qualified name of the property as a [String], formatted as `"ClassName.property1.property2...propertyN"`.
 *         If `@SerialName` is present on any class or property in the path, the annotated names are used instead.
 *
 * @throws IllegalArgumentException If the property path is invalid, or if class/property names cannot be determined.
 */
public fun getPropertyName(
    root: KProperty1<*, *>,
    vararg remaining: KProperty1<*, *>
): String {
    // Determine the root class from the first property.
    val rootClass: KClass<out Any> = root.parameters.firstOrNull()?.type?.classifier as? KClass<*>
        ?: root.returnType.classifier as? KClass<*>
        ?: throw IllegalArgumentException("Unable to determine the root class from the first property.")

    val allProperties: List<KProperty1<*, *>> = listOf(root) + remaining

    val fieldNames: MutableList<String> = mutableListOf()

    // Get the root class serial name.
    val rootClassSerialName: String = rootClass.findAnnotation<SerialName>()?.value ?: rootClass.simpleName
    ?: throw IllegalArgumentException("Unable to determine the class name for '${rootClass.simpleName}'.")

    fieldNames.add(rootClassSerialName)

    var currentClass: KClass<*> = rootClass

    for (property in allProperties) {
        // Ensure the property belongs to the current class.
        val declaringClass: KClass<out Any>? = property.parameters.firstOrNull()?.type?.classifier as? KClass<*>
            ?: property.returnType.classifier as? KClass<*>

        require(declaringClass == currentClass) {
            "Property '${property.name}' does not belong to class '${currentClass.simpleName}'."
        }

        // Get @SerialName if present on the property.
        val propertySerialName: String = property.findAnnotation<SerialName>()?.value ?: property.name

        fieldNames.add(propertySerialName)

        // Update current class.
        val nextClass: KClass<*> = property.returnType.classifier as? KClass<*>
            ?: throw IllegalArgumentException("Property '${property.name}' does not have a valid class type.")

        currentClass = nextClass
    }

    return fieldNames.joinToString(".")
}
