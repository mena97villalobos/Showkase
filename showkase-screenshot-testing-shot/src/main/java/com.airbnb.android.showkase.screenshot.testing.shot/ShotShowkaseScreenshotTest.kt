package com.airbnb.android.showkase.screenshot.testing.shot

import android.os.Build
import androidx.annotation.RequiresApi
import com.airbnb.android.showkase.screenshot.testing.ScreenshotMetadata
import com.airbnb.android.showkase.screenshot.testing.ShowkaseScreenshotTest
import com.karumi.shot.ScreenshotTest

abstract class ShotShowkaseScreenshotTest : ShowkaseScreenshotTest, ScreenshotTest {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onScreenshot(metadata: ScreenshotMetadata) {
        compareScreenshot(composeTestRule)
    }
}
