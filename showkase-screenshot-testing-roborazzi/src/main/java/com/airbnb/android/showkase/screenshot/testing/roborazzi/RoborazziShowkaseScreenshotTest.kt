package com.airbnb.android.showkase.screenshot.testing.roborazzi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.android.showkase.annotation.ScreenshotConfig
import com.airbnb.android.showkase.models.ShowkaseBrowserColor
import com.airbnb.android.showkase.models.ShowkaseBrowserComponent
import com.airbnb.android.showkase.models.ShowkaseBrowserTypography
import com.github.takahirom.roborazzi.RoborazziOptions

/**
 * Marker interface for the abstract class that the Showkase processor uses to generate a
 * [Roborazzi](https://github.com/takahirom/roborazzi)-backed screenshot test.
 *
 * Typical usage:
 *
 * ```
 * @ShowkaseScreenshot(rootShowkaseClass = MyRootModule::class)
 * abstract class MyRoborazziScreenshotTest : RoborazziShowkaseScreenshotTest {
 *     companion object : RoborazziShowkaseScreenshotTest.CompanionObject
 * }
 * ```
 *
 * The processor emits a `MyRoborazziScreenshotTestImpl` class wired to a Robolectric runner that
 * iterates every preview the Showkase root knows about and writes a PNG via Roborazzi's
 * `captureRoboImage`.
 *
 * Device profile / locale / RTL / dark-mode are controlled through Robolectric
 * `@Config(qualifiers = ...)` annotations on the test class, not via runtime API. Override
 * [CompanionObject.qualifiers] to change the device profile for the generated test.
 */
interface RoborazziShowkaseScreenshotTest {

    interface CompanionObject {

        /**
         * Robolectric `@Config(qualifiers = ...)` value applied to the generated test class.
         * Defaults to `RobolectricDeviceQualifiers.Pixel5`. See
         * [com.github.takahirom.roborazzi.RobolectricDeviceQualifiers] for available presets, and
         * append modifiers like `+night`, `+ldrtl`, `+fontScale1.5` as needed.
         */
        fun qualifiers(): String =
            com.github.takahirom.roborazzi.RobolectricDeviceQualifiers.Pixel5

        /**
         * Robolectric `@Config(sdk = [...])` SDK level applied to the generated test class.
         * Default 33 is a stable compromise between Compose support and Robolectric coverage.
         */
        fun sdk(): Int = 33

        /**
         * Roborazzi options for comparison. Default is pixel-perfect (`changeThreshold = 0f`).
         * Override to loosen the threshold for noisy renderings.
         */
        fun roborazziOptions(): RoborazziOptions = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0f),
        )
    }
}

/**
 * Common interface implemented by component, color, and typography wrappers so the generated
 * Roborazzi test can iterate everything Showkase knows about under one parameter type.
 */
interface RoborazziShowkaseTestPreview {
    @Composable
    fun Content()

    /** Stable identifier used as the snapshot file name. */
    val id: String

    val captureType: ScreenshotConfig
        get() = ScreenshotConfig.SingleStaticImage
}

private const val FILE_NAME_DELIM = "__"

private fun String.sanitizeForFileName(): String =
    replace(' ', '_').replace('/', '_').replace('\\', '_').replace(':', '_')

class ComponentRoborazziShowkaseTestPreview(
    private val showkaseBrowserComponent: ShowkaseBrowserComponent,
) : RoborazziShowkaseTestPreview {
    @Composable
    override fun Content() = showkaseBrowserComponent.component()

    override val captureType: ScreenshotConfig = showkaseBrowserComponent.screenshotConfig

    // componentKey is Showkase's guaranteed-unique identifier (it embeds the FQCN, group, name,
    // styleName, and parameter index for previews coming from a preview-parameter provider).
    override val id: String = showkaseBrowserComponent.componentKey.sanitizeForFileName()

    override fun toString(): String = id
}

class ColorRoborazziShowkaseTestPreview(
    private val showkaseBrowserColor: ShowkaseBrowserColor,
) : RoborazziShowkaseTestPreview {
    @Composable
    override fun Content() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(showkaseBrowserColor.color),
        )
    }

    override val id: String = listOf(
        showkaseBrowserColor.colorGroup,
        showkaseBrowserColor.colorName,
    ).joinToString(FILE_NAME_DELIM) { it.sanitizeForFileName() }

    override fun toString(): String = id
}

class TypographyRoborazziShowkaseTestPreview(
    private val showkaseBrowserTypography: ShowkaseBrowserTypography,
) : RoborazziShowkaseTestPreview {
    @Composable
    override fun Content() {
        BasicText(
            text = showkaseBrowserTypography.typographyName,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            style = showkaseBrowserTypography.textStyle,
        )
    }

    override val id: String = listOf(
        showkaseBrowserTypography.typographyGroup,
        showkaseBrowserTypography.typographyName,
    ).joinToString(FILE_NAME_DELIM) { it.sanitizeForFileName() }

    override fun toString(): String = id
}
