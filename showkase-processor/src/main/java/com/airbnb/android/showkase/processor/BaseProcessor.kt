package com.airbnb.android.showkase.processor

import com.airbnb.android.showkase.processor.exceptions.ShowkaseProcessorException
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated

abstract class BaseProcessor(
    val kspEnvironment: SymbolProcessorEnvironment,
) : SymbolProcessor {

    val codeGenerator: CodeGenerator = kspEnvironment.codeGenerator
    val logger: KSPLogger = kspEnvironment.logger
    val options: Map<String, String> = kspEnvironment.options

    private var roundNumber = 1

    init {
        initOptions(options)
    }

    /**
     * Unified place to handle any compiler processor options that are passed to the KSP processor,
     * before any rounds are processed.
     */
    open fun initOptions(options: Map<String, String>) {}

    final override fun process(resolver: Resolver): List<KSAnnotated> {
        val timer = Timer("${this.javaClass.simpleName} [Round $roundNumber]")
        timer.start()

        tryOrPrintError {
            processRound(resolver)
        }

        timer.finishAndPrint(logger)
        roundNumber++
        return emptyList()
    }

    private inline fun tryOrPrintError(block: () -> Unit) {
        @Suppress("Detekt.TooGenericExceptionCaught")
        try {
            block()
        } catch (e: Throwable) {
            // Errors thrown from within KSP can get lost, making the root cause of an issue hidden.
            // This helps to surface all thrown errors.
            if (e is ShowkaseProcessorException && e.element != null) {
                logger.error(e.stackTraceToString(), e.element)
            } else {
                logger.error(e.stackTraceToString())
            }
        }
    }

    abstract fun processRound(resolver: Resolver)
}
