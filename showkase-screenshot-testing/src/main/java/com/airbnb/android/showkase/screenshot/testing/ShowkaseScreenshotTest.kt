package com.airbnb.android.showkase.screenshot.testing

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.airbnb.android.showkase.annotation.ShowkaseScreenshot
import com.airbnb.android.showkase.models.ShowkaseBrowserColor
import com.airbnb.android.showkase.models.ShowkaseBrowserComponent
import com.airbnb.android.showkase.models.ShowkaseBrowserTypography
import org.junit.Rule
import java.util.Locale

private val componentPadding = 16.dp

/**
 *
 * Interface used to provide the logic needed for enabling screenshot test support in your repository.
 * This is always used along with the [ShowkaseScreenshot] annotation. You would typically add
 * a class that implements this interface in your root module that has access to all your UI elements.
 *
 * <p>
 * Here's an example of how you would typically use it:
 *
 * @ShowkaseScreenshotTest
 * abstract class MyScreenshotTest: ShowkaseScreenshotTest {
 *   override fun onScreenshot(metadata: ScreenshotMetadata) {
 *       // Here you do the action you want to take with the screenshot.
 *   }
 * }
 *
 * </p>
 *
 * Note: you should add this class to the androidTest sourceSet as that's where your testing
 * dependencies will exist otherwise the generated test won't compile. Additionally, it's important
 * that the class you annotate with [ShowkaseScreenshot] is either abstract or open as Showkase
 * generates a class that extends this class in order to get access to the onScreenshot method.
 */
@Suppress("Detekt.TooGenericExceptionCaught", "Detekt.TooGenericExceptionThrown")
interface ShowkaseScreenshotTest {
    @get:Rule
    val composeTestRule: ComposeContentTestRule

    /**
     * Called during the execution of each screenshot test after the screenshot has been captured.
     * Implementations typically save the bitmap to disk, compare it against a golden, or upload
     * it to a screenshot service.
     */
    fun onScreenshot(metadata: ScreenshotMetadata) {
        @Suppress("DEPRECATION")
        onScreenshot(
            id = metadata.id,
            name = metadata.name,
            group = metadata.group,
            styleName = metadata.styleName,
            tags = metadata.tags,
            extraMetadata = metadata.extraMetadata,
            screenshotType = metadata.screenshotType,
            screenshotBitmap = metadata.screenshotBitmap,
        )
    }

    /**
     * Legacy 8-parameter overload kept for backwards compatibility with subclasses written against
     * the pre-refactor signature. New implementations should override [onScreenshot] above instead.
     */
    @Suppress("LongParameterList")
    @Deprecated(
        message = "Override the single-parameter onScreenshot(ScreenshotMetadata) instead.",
        replaceWith = ReplaceWith("onScreenshot(metadata)"),
        level = DeprecationLevel.WARNING,
    )
    fun onScreenshot(
        id: String,
        name: String,
        group: String,
        styleName: String? = null,
        tags: List<String> = emptyList(),
        extraMetadata: List<String> = emptyList(),
        screenshotType: ShowkaseScreenshotType,
        screenshotBitmap: Bitmap,
    ) {
        throw NotImplementedError(
            "ShowkaseScreenshotTest implementations must override onScreenshot(ScreenshotMetadata)."
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun takeComposableScreenshot(
        showkaseBrowserComponent: ShowkaseBrowserComponent,
    ) {
        try {
            // Disable animations for screenshots to make them deterministic
            composeTestRule.mainClock.autoAdvance = false
            composeTestRule.setContent { showkaseBrowserComponent.component() }
            val bitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
            onScreenshot(
                ScreenshotMetadata(
                    id = showkaseBrowserComponent.componentKey,
                    name = showkaseBrowserComponent.componentName,
                    group = showkaseBrowserComponent.group,
                    styleName = showkaseBrowserComponent.styleName,
                    tags = showkaseBrowserComponent.tags,
                    extraMetadata = showkaseBrowserComponent.extraMetadata,
                    screenshotType = ShowkaseScreenshotType.Composable,
                    screenshotBitmap = bitmap,
                )
            )
        } catch (e: Throwable) {
            throw RuntimeException(
                "Failure while screenshotting component $showkaseBrowserComponent",
                e,
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun takeTypographyScreenshot(
        showkaseBrowserTypography: ShowkaseBrowserTypography,
    ) {
        try {
            composeTestRule.mainClock.autoAdvance = false
            composeTestRule.setContent {
                BasicText(
                    text = showkaseBrowserTypography.typographyName.replaceFirstChar {
                        it.titlecase(Locale.getDefault())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(componentPadding),
                    style = showkaseBrowserTypography.textStyle,
                )
            }
            val bitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
            onScreenshot(
                ScreenshotMetadata(
                    id = "${showkaseBrowserTypography.typographyGroup}_${showkaseBrowserTypography.typographyName}",
                    name = showkaseBrowserTypography.typographyName,
                    group = showkaseBrowserTypography.typographyGroup,
                    screenshotType = ShowkaseScreenshotType.Typography,
                    screenshotBitmap = bitmap,
                )
            )
        } catch (e: Throwable) {
            throw RuntimeException(
                "Failure while screenshotting typography $showkaseBrowserTypography",
                e,
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun takeColorScreenshot(
        showkaseBrowserColor: ShowkaseBrowserColor,
    ) {
        try {
            composeTestRule.mainClock.autoAdvance = false
            composeTestRule.setContent {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .background(showkaseBrowserColor.color)
                )
            }
            val bitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
            onScreenshot(
                ScreenshotMetadata(
                    id = "${showkaseBrowserColor.colorGroup}_${showkaseBrowserColor.colorName}",
                    name = showkaseBrowserColor.colorName,
                    group = showkaseBrowserColor.colorGroup,
                    screenshotType = ShowkaseScreenshotType.Color,
                    screenshotBitmap = bitmap,
                )
            )
        } catch (e: Throwable) {
            throw RuntimeException("Failure while screenshotting color $showkaseBrowserColor", e)
        }
    }
}
