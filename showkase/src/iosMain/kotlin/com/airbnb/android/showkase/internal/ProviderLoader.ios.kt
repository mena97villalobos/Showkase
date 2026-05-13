package com.airbnb.android.showkase.internal

import com.airbnb.android.showkase.models.ShowkaseProvider

internal actual fun loadShowkaseProvider(classKey: String): ShowkaseProvider {
    throw UnsupportedOperationException(
        "ShowkaseBrowser(rootModuleCanonicalName) is not supported on iOS because " +
                "Kotlin/Native cannot reflectively load classes by name. Instead, call " +
                "ShowkaseBrowser(provider = ${'$'}{rootModuleCanonicalName}Codegen()) directly."
    )
}
