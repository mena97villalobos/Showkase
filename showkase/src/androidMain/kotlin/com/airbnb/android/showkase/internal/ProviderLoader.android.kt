package com.airbnb.android.showkase.internal

import com.airbnb.android.showkase.exceptions.ShowkaseException
import com.airbnb.android.showkase.models.ShowkaseProvider

internal actual fun loadShowkaseProvider(classKey: String): ShowkaseProvider {
    val codegenClassName = "${classKey}Codegen"
    return runCatching {
        Class.forName(codegenClassName).getDeclaredConstructor().newInstance() as ShowkaseProvider
    }.getOrElse { e ->
        throw ShowkaseException(
            "Failed to load generated Showkase provider for $codegenClassName. " +
                    "This usually means the processor didn't run on the module that contains your " +
                    "@ShowkaseRoot, or the generated class is out of sync with the runtime. " +
                    "Rebuild the project and verify your @ShowkaseRoot setup.",
            cause = e,
        )
    }
}
