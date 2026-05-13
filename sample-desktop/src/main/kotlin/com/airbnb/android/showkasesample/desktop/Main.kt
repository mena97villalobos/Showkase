package com.airbnb.android.showkasesample.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.airbnb.android.showkase.ShowkaseBrowser

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Showkase Browser",
    ) {
        ShowkaseBrowser(
            rootModuleCanonicalName = "com.airbnb.android.showkasesample.desktop.SampleRoot",
            onFinish = ::exitApplication,
        )
    }
}
