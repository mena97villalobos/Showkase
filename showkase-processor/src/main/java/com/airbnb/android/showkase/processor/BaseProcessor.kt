package com.airbnb.android.showkase.processor

import androidx.room.compiler.processing.XFiler
import androidx.room.compiler.processing.XMessager
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XRoundEnv
import com.airbnb.android.showkase.processor.exceptions.ShowkaseProcessorException
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import javax.tools.Diagnostic

/**
 * Base class for the KSP-backed Showkase processor. Provides a single uniform processing pipeline
 * driven by Room's [XProcessingEnv] abstraction.
 */
abstract class BaseProcessor(
    val kspEnvironment: SymbolProcessorEnvironment,
) : SymbolProcessor {

    lateinit var environment: XProcessingEnv
        private set

    val messager: XMessager
        get() = environment.messager

    val filer: XFiler
        get() = environment.filer

    private var roundNumber = 1

    init {
        initOptions(kspEnvironment.options)
    }

    /**
     * Unified place to handle any compiler processor options that are passed to the KSP processor,
     * before any rounds are processed.
     */
    open fun initOptions(options: Map<String, String>) {}

    final override fun process(resolver: Resolver): List<KSAnnotated> {
        environment = XProcessingEnv.create(
            kspEnvironment,
            resolver,
        )
        internalProcess(environment, XRoundEnv.create(environment))
        return emptyList()
    }

    private fun internalProcess(
        environment: XProcessingEnv,
        round: XRoundEnv,
    ) {
        val timer = Timer("${this.javaClass.simpleName} [Round $roundNumber]")
        timer.start()

        tryOrPrintError {
            process(environment, round)
        }

        timer.finishAndPrint(messager)
        roundNumber++
    }

    private inline fun tryOrPrintError(block: () -> Unit) {
        @Suppress("Detekt.TooGenericExceptionCaught")
        try {
            block()
        } catch (e: Throwable) {
            // Errors thrown from within KSP can get lost, making the root cause of an issue hidden.
            // This helps to surface all thrown errors.
            if (e is ShowkaseProcessorException && e.element != null) {
                messager.printMessage(Diagnostic.Kind.ERROR, e.stackTraceToString(), e.element)
            } else {
                messager.printMessage(Diagnostic.Kind.ERROR, e.stackTraceToString())
            }
        }
    }

    abstract fun process(
        environment: XProcessingEnv,
        round: XRoundEnv,
    )
}
