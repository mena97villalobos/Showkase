package com.airbnb.android.showkase.screenshot.testing.roborazzi.sample

import com.airbnb.android.showkase.annotation.ShowkaseScreenshot
import com.airbnb.android.showkase.screenshot.testing.roborazzi.RoborazziShowkaseScreenshotTest

/**
 * Marker class for the Showkase processor. The accompanying companion object is what supplies the
 * runtime Robolectric qualifiers and Roborazzi comparison options to the auto-generated
 * `MyRoborazziShowkaseScreenshotTestImpl`.
 */
@ShowkaseScreenshot(rootShowkaseClass = RoborazziSampleRootModule::class)
abstract class MyRoborazziShowkaseScreenshotTest : RoborazziShowkaseScreenshotTest {
    companion object : RoborazziShowkaseScreenshotTest.CompanionObject
}
