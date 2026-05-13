package com.airbnb.android.showkasesample.ios

import androidx.compose.ui.window.ComposeUIViewController
import com.airbnb.android.showkase.ShowkaseBrowser
import com.airbnb.android.showkase.models.ShowkaseProvider

/**
 * Entry point consumed from Swift. The Xcode app target instantiates this view controller and
 * presents it. Generated `SampleRootCodegen` is referenced via the per-target [sampleProvider]
 * actual — Kotlin/Native cannot do reflective class lookup, and KSP-generated sources land in
 * each leaf source set (iosX64Main / iosArm64Main / iosSimulatorArm64Main) rather than in
 * iosMain, so the lookup is done by the actual closest to the generated code.
 */
fun MainViewController() = ComposeUIViewController {
    ShowkaseBrowser(provider = sampleProvider())
}

internal expect fun sampleProvider(): ShowkaseProvider
