package com.airbnb.android.showkase.processor.utils

import com.airbnb.android.showkase.processor.exceptions.ShowkaseProcessorException
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import kotlin.reflect.KClass

internal fun KSAnnotation.argByNameOrNull(name: String): Any? {
    val argument = arguments.firstOrNull { it.name?.asString() == name }
        ?: defaultArguments.firstOrNull { it.name?.asString() == name }
        ?: return null
    return argument.value
}

internal fun KSAnnotation.argByName(name: String): Any =
    argByNameOrNull(name) ?: throw ShowkaseProcessorException(
        "Annotation @${shortName.asString()} is missing required argument '$name'"
    )

internal fun KSAnnotation.getAsString(name: String): String = argByName(name) as String

internal fun KSAnnotation.getAsInt(name: String): Int = argByName(name) as Int

internal fun KSAnnotation.getAsBoolean(name: String): Boolean = argByName(name) as Boolean

@Suppress("UNCHECKED_CAST")
internal fun KSAnnotation.getAsStringList(name: String): List<String> {
    return when (val value = argByNameOrNull(name)) {
        null -> emptyList()
        is List<*> -> value as List<String>
        is Array<*> -> value.map { it as String }
        else -> error("Expected list of strings for $name, got ${value::class}")
    }
}

@Suppress("UNCHECKED_CAST")
internal fun KSAnnotation.getAsIntList(name: String): List<Int> {
    return when (val value = argByNameOrNull(name)) {
        null -> emptyList()
        is List<*> -> value as List<Int>
        is IntArray -> value.toList()
        is Array<*> -> value.map { it as Int }
        else -> error("Expected list of ints for $name, got ${value::class}")
    }
}

internal fun KSAnnotation.getAsAnnotation(name: String): KSAnnotation = argByName(name) as KSAnnotation

internal fun KSAnnotation.getAsType(name: String): KSType = argByName(name) as KSType

@Suppress("UNCHECKED_CAST")
internal fun KSAnnotation.getAsTypeList(name: String): List<KSType> {
    return when (val value = argByNameOrNull(name)) {
        null -> emptyList()
        is List<*> -> value as List<KSType>
        is Array<*> -> value.map { it as KSType }
        else -> error("Expected list of types for $name, got ${value::class}")
    }
}

internal inline fun <reified E : Enum<E>> KSAnnotation.getAsEnum(name: String): E {
    val value = argByName(name)
    val entryName = when (value) {
        is KSType -> value.declaration.simpleName.asString()
        is KSClassDeclaration -> value.simpleName.asString()
        else -> value.toString()
    }
    return enumValueOf<E>(entryName)
}

internal fun KSAnnotated.findAnnotationBySimpleName(simpleName: String): KSAnnotation? {
    return annotations.firstOrNull { it.shortName.asString() == simpleName }
}

internal fun KSAnnotated.requireAnnotationBySimpleName(simpleName: String): List<KSAnnotation> {
    return annotations.filter { it.shortName.asString() == simpleName }.toList()
}

internal fun KSAnnotated.getAnnotation(kclass: KClass<out Annotation>): KSAnnotation? =
    findAnnotationBySimpleName(kclass.simpleName!!)

internal fun KSAnnotated.getAnnotations(kclass: KClass<out Annotation>): List<KSAnnotation> =
    requireAnnotationBySimpleName(kclass.simpleName!!)

internal fun KSAnnotated.requireAnnotation(kclass: KClass<out Annotation>): KSAnnotation =
    findAnnotationBySimpleName(kclass.simpleName!!)
        ?: error("Annotation @${kclass.simpleName} not found on $this")

internal fun KSAnnotation.annotationDeclaration(): KSClassDeclaration =
    annotationType.resolve().declaration as KSClassDeclaration

internal fun KSAnnotated.containingFileOrNull(): KSFile? =
    (this as? KSDeclaration)?.containingFile

internal fun KSType.isSameTypeAs(other: KSType): Boolean {
    val thisName = this.declaration.qualifiedName?.asString()
    val otherName = other.declaration.qualifiedName?.asString()
    return thisName != null &&
        otherName != null &&
        thisName == otherName &&
        this.arguments.size == other.arguments.size &&
        this.arguments.zip(other.arguments).all { (a, b) ->
            a.type?.resolve()?.declaration?.qualifiedName?.asString() ==
                b.type?.resolve()?.declaration?.qualifiedName?.asString()
        }
}

/**
 * The order of symbols returned by KSP2 differs from that returned by KSP1.
 * This workaround ensures that the order of symbols is consistent across both KSP versions.
 *
 * @see [https://github.com/google/ksp/issues/1719]
 */
internal fun <T : KSAnnotated> Collection<T>.ensureConsistentOrdering(): Sequence<T> {
    return this.asSequence()
        .sortedWith(
            compareBy { element ->
                when (element) {
                    is KSClassDeclaration -> 0
                    is KSFunctionDeclaration -> 1
                    else -> 2
                }
            }
        )
}
