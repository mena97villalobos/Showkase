package com.airbnb.android.showkase.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

@Composable
internal actual fun screenHeightDp(): Int {
    val density = LocalDensity.current
    val heightPx = LocalWindowInfo.current.containerSize.height
    return (heightPx / density.density).toInt()
}
