package com.airbnb.android.showkase.processor.exceptions

import com.google.devtools.ksp.symbol.KSNode

internal class ShowkaseProcessorException(message: String, val element: KSNode? = null) : Exception(message)
