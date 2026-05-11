package com.airbnb.android.showkase.processor.logging

import com.airbnb.android.showkase.processor.exceptions.ShowkaseProcessorException
import com.google.devtools.ksp.processing.KSPLogger

internal class ShowkaseExceptionLogger {
    private val loggedExceptions: MutableList<Exception> = mutableListOf()
    private val loggedInfoMessage: MutableList<String> = mutableListOf()

    internal fun logErrorMessage(message: String) {
        logError(Exception(message))
    }

    internal fun logInfoMessage(message: String) {
        loggedInfoMessage += message
    }

    private fun logError(e: Exception) {
        loggedExceptions += e
    }

    internal fun publishMessages(logger: KSPLogger) {
        loggedExceptions.forEach {
            if (it is ShowkaseProcessorException && it.element != null) {
                logger.error("${it.message}", it.element)
            } else {
                logger.error("${it.message}")
            }
        }
        loggedInfoMessage.forEach { logger.info(it) }
    }
}
