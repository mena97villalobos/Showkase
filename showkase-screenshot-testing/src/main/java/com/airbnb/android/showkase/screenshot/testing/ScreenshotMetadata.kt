package com.airbnb.android.showkase.screenshot.testing

import android.graphics.Bitmap

/**
 * Metadata passed to [ShowkaseScreenshotTest.onScreenshot] when a screenshot has been captured.
 *
 * @param id A stable id for this screenshot. For composables, this is the component's
 * `componentKey`. For colors/typography, this is a composite of group and name.
 * @param name The name of the UI element.
 * @param group The group the element belongs to.
 * @param styleName The style name (Composables only; null for color/typography).
 * @param tags Tags declared on the `@ShowkaseComposable` annotation (Composables only).
 * @param extraMetadata Arbitrary extra metadata declared on the `@ShowkaseComposable` annotation.
 * @param screenshotType The type of UI element being captured.
 * @param screenshotBitmap The captured bitmap.
 */
public data class ScreenshotMetadata(
    val id: String,
    val name: String,
    val group: String,
    val styleName: String? = null,
    val tags: List<String> = emptyList(),
    val extraMetadata: List<String> = emptyList(),
    val screenshotType: ShowkaseScreenshotType,
    val screenshotBitmap: Bitmap,
)
