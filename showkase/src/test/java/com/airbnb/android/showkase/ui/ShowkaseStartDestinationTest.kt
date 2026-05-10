package com.airbnb.android.showkase.ui

import com.airbnb.android.showkase.models.ShowkaseBrowserColor
import com.airbnb.android.showkase.models.ShowkaseBrowserComponent
import com.airbnb.android.showkase.models.ShowkaseBrowserTypography
import com.airbnb.android.showkase.models.ShowkaseCurrentScreen
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * Covers the decision logic in [startDestination] (`ShowkaseBrowserApp.kt:466-482`) — when only
 * one category has entries we jump straight to its groups screen; otherwise we land on the
 * categories selector.
 */
class ShowkaseStartDestinationTest {

    private val componentMap = mapOf(
        "g" to listOf(
            ShowkaseBrowserComponent(
                componentKey = "g-c",
                group = "g",
                componentName = "c",
                componentKDoc = "",
                component = {},
            )
        )
    )
    private val colorMap = mapOf("g" to listOf(ShowkaseBrowserColor("g", "red", "", Color.Red)))
    private val typoMap = mapOf("g" to listOf(ShowkaseBrowserTypography("g", "h1", "", TextStyle())))
    private val emptyComponentMap = emptyMap<String, List<ShowkaseBrowserComponent>>()
    private val emptyColorMap = emptyMap<String, List<ShowkaseBrowserColor>>()
    private val emptyTypoMap = emptyMap<String, List<ShowkaseBrowserTypography>>()

    @Test
    fun `start destination is COMPONENT_GROUPS when only components are present`() {
        assertThat(startDestination(emptyColorMap, emptyTypoMap, componentMap))
            .isEqualTo(ShowkaseCurrentScreen.COMPONENT_GROUPS.name)
    }

    @Test
    fun `start destination is COLOR_GROUPS when only colors are present`() {
        assertThat(startDestination(colorMap, emptyTypoMap, emptyComponentMap))
            .isEqualTo(ShowkaseCurrentScreen.COLOR_GROUPS.name)
    }

    @Test
    fun `start destination is TYPOGRAPHY_GROUPS when only typography is present`() {
        assertThat(startDestination(emptyColorMap, typoMap, emptyComponentMap))
            .isEqualTo(ShowkaseCurrentScreen.TYPOGRAPHY_GROUPS.name)
    }

    @Test
    fun `start destination is SHOWKASE_CATEGORIES when multiple categories are present`() {
        assertThat(startDestination(colorMap, typoMap, componentMap))
            .isEqualTo(ShowkaseCurrentScreen.SHOWKASE_CATEGORIES.name)
    }

    @Test
    fun `start destination is SHOWKASE_CATEGORIES when all categories empty`() {
        assertThat(startDestination(emptyColorMap, emptyTypoMap, emptyComponentMap))
            .isEqualTo(ShowkaseCurrentScreen.SHOWKASE_CATEGORIES.name)
    }
}
