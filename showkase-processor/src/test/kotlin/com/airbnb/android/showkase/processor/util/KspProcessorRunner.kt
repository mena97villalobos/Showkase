package com.airbnb.android.showkase.processor.util

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class)
internal fun runKspProcessorTest(
    sources: List<SourceFile>,
    block: (Resolver) -> Unit,
) {
    val provider = object : SymbolProcessorProvider {
        override fun create(env: SymbolProcessorEnvironment): SymbolProcessor =
            object : SymbolProcessor {
                override fun process(resolver: Resolver): List<KSAnnotated> {
                    block(resolver)
                    return emptyList()
                }
            }
    }

    val compilation = KotlinCompilation().apply {
        this.sources = sources
        inheritClassPath = true
        configureKsp {
            symbolProcessorProviders += provider
        }
    }

    val result = compilation.compile()
    check(result.exitCode == KotlinCompilation.ExitCode.OK) {
        "Compilation failed:\n${result.messages}"
    }
}
