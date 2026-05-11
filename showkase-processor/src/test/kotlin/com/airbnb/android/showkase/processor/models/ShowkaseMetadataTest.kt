package com.airbnb.android.showkase.processor.models

import com.airbnb.android.showkase.processor.util.runKspProcessorTest
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.symbol.FunctionKind
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.tschuchort.compiletesting.SourceFile
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import strikt.assertions.single

class ShowkaseMetadataTest {

    @Test
    fun isTopLevelFunction() {
        val libSource = SourceFile.kotlin(
            "lib.kt",
            """
            @com.airbnb.android.showkase.processor.models.MyAnnotation
            fun foo() {}

            class Bar {
                fun enclosedFoo() {}
            }
            """.trimIndent()
        )
        runKspProcessorTest(listOf(libSource)) { resolver ->
            val barClass = resolver.getClassDeclarationByName(resolver.getKSNameFromString("Bar"))!!
            val enclosedFn = barClass.getDeclaredFunctions()
                .single { it.simpleName.asString() == "enclosedFoo" }

            expectThat(enclosedFn.functionKind == FunctionKind.TOP_LEVEL).isFalse()

            expectThat(resolver.getSymbolsWithAnnotation(MyAnnotation::class.qualifiedName!!).toList())
                .single()
                .isA<KSFunctionDeclaration>()
                .get { functionKind == FunctionKind.TOP_LEVEL }
                .isTrue()
        }
    }

    @Test
    fun isTopLevelFunctionProperty() {
        val libSource = SourceFile.kotlin(
            "lib.kt",
            """
            @com.airbnb.android.showkase.processor.models.MyAnnotation
            val foo: Int = 0

            class Bar {
                val enclosedFoo: Int = 0
            }
            """.trimIndent()
        )
        runKspProcessorTest(listOf(libSource)) { resolver ->
            val barClass = resolver.getClassDeclarationByName(resolver.getKSNameFromString("Bar"))!!
            val enclosedProp = barClass.getDeclaredProperties()
                .single { it.simpleName.asString() == "enclosedFoo" }

            expectThat(enclosedProp.parentDeclaration == null).isFalse()

            expectThat(resolver.getSymbolsWithAnnotation(MyAnnotation::class.qualifiedName!!).toList())
                .single()
                .isA<KSPropertyDeclaration>()
                .get { parentDeclaration == null }
                .isTrue()
        }
    }
}

annotation class MyAnnotation
