package com.airbnb.android.showkase.models

import androidx.compose.runtime.Composable
import com.airbnb.android.showkase.annotation.ScreenshotConfig

data class ShowkaseBrowserComponent(
    val componentKey: String,
    val group: String,
    val componentName: String,
    val componentKDoc: String,
    val component: @Composable () -> Unit,
    val styleName: String? = null,
    val isDefaultStyle: Boolean = false,
    val widthDp: Int? = null,
    val heightDp: Int? = null,
    val tags: List<String> = emptyList(),
    val extraMetadata: List<String> = emptyList(),
    val screenshotConfig: ScreenshotConfig = ScreenshotConfig.SingleStaticImage,
    val isDialog: Boolean = false,
    val dialogButtonText: String = "",
    val dialogHideButtonText: String = "",
    )
