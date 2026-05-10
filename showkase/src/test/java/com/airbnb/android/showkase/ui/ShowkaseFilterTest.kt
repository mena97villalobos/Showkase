package com.airbnb.android.showkase.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.airbnb.android.showkase.models.ShowkaseBrowserColor
import com.airbnb.android.showkase.models.ShowkaseBrowserComponent
import com.airbnb.android.showkase.models.ShowkaseBrowserTypography
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for the pure-function filter and helper utilities in the showkase browser. These
 * cover [matchSearchQuery], [getNumOfUIElements], and the various `getFilteredSearchList` helpers
 * across the UI screens — all of which previously had zero coverage.
 */
class ShowkaseFilterTest {

    private fun component(name: String, group: String = "G", style: String? = null) =
        ShowkaseBrowserComponent(
            componentKey = "$group-$name-${style ?: ""}",
            group = group,
            componentName = name,
            componentKDoc = "",
            component = {},
            styleName = style,
        )

    private fun color(name: String, group: String = "G") =
        ShowkaseBrowserColor(group, name, "", Color.Red)

    private fun typography(name: String, group: String = "G") =
        ShowkaseBrowserTypography(group, name, "", TextStyle())

    // matchSearchQuery — defined in ShowkaseComponentStylesScreen.kt:122-125
    @Test
    fun `matchSearchQuery finds substring match in single property`() {
        assertThat(matchSearchQuery("alpha", "AlphaButton")).isTrue()
    }

    @Test
    fun `matchSearchQuery is case insensitive`() {
        assertThat(matchSearchQuery("ALPHA", "alphabutton")).isTrue()
        assertThat(matchSearchQuery("alpha", "AlphaButton")).isTrue()
    }

    @Test
    fun `matchSearchQuery searches all provided properties`() {
        assertThat(matchSearchQuery("dark", "PrimaryButton", "Dark theme variant")).isTrue()
        assertThat(matchSearchQuery("missing", "PrimaryButton", "Dark theme variant")).isFalse()
    }

    @Test
    fun `matchSearchQuery returns false when no property matches`() {
        assertThat(matchSearchQuery("zzz", "AlphaButton", "BetaCard")).isFalse()
    }

    @Test
    fun `matchSearchQuery treats empty query as matching any property containing empty string`() {
        // String.contains("") is true for every non-null string — this captures intentional behavior.
        assertThat(matchSearchQuery("", "anything")).isTrue()
    }

    // getNumOfUIElements — defined in ShowkaseGroupsScreen.kt:73-79
    @Test
    fun `getNumOfUIElements dedupes components by componentName`() {
        val list = listOf(
            component("Button", style = "light"),
            component("Button", style = "dark"),
            component("Card"),
        )
        // Two unique component names: Button, Card.
        assertThat(getNumOfUIElements(list)).isEqualTo(2)
    }

    @Test
    fun `getNumOfUIElements returns raw size for non-component lists`() {
        val colors = listOf(color("Red"), color("Blue"), color("Green"))
        assertThat(getNumOfUIElements(colors)).isEqualTo(3)
    }

    @Test
    fun `getNumOfUIElements returns 0 for empty list`() {
        assertThat(getNumOfUIElements(emptyList<Any>())).isEqualTo(0)
    }

    // getFilteredSearchList (generic Map variant) — defined in ShowkaseGroupsScreen.kt:81-97
    @Test
    fun `getFilteredSearchList passes through map when search inactive`() {
        val map = mapOf("a" to listOf(1), "b" to listOf(2, 3))
        val result = getFilteredSearchList(map, isSearchActive = false, searchQuery = "any")
        assertThat(result).isEqualTo(map)
    }

    @Test
    fun `getFilteredSearchList filters by key when search active with query`() {
        val map = mapOf("alpha" to listOf(1), "beta" to listOf(2), "gamma" to listOf(3))
        val result = getFilteredSearchList(map, isSearchActive = true, searchQuery = "et")
        assertThat(result.keys).containsExactly("beta")
    }

    @Test
    fun `getFilteredSearchList passes through when search active but query blank`() {
        val map = mapOf("alpha" to listOf(1), "beta" to listOf(2))
        val result = getFilteredSearchList(map, isSearchActive = true, searchQuery = "")
        assertThat(result).isEqualTo(map)
    }

    @Test
    fun `getFilteredSearchList passes through when search active but query null`() {
        val map = mapOf("alpha" to listOf(1), "beta" to listOf(2))
        val result = getFilteredSearchList(map, isSearchActive = true, searchQuery = null)
        assertThat(result).isEqualTo(map)
    }

    // isOnlyCategory — defined in ShowkaseBrowserApp.kt:532-535
    @Test
    fun `isOnlyCategory is true when receiver has entries and others are empty`() {
        val components = mapOf("g" to listOf(component("c")))
        val emptyColors = emptyMap<String, List<ShowkaseBrowserColor>>()
        val emptyTypo = emptyMap<String, List<ShowkaseBrowserTypography>>()
        assertThat(components.isOnlyCategory(emptyColors, emptyTypo)).isTrue()
    }

    @Test
    fun `isOnlyCategory is false when receiver is empty`() {
        val empty = emptyMap<String, List<Any>>()
        val nonEmpty = mapOf("g" to listOf(1))
        assertThat(empty.isOnlyCategory(nonEmpty, nonEmpty)).isFalse()
    }

    @Test
    fun `isOnlyCategory is false when another category is non-empty`() {
        val components = mapOf("g" to listOf(component("c")))
        val nonEmptyColors = mapOf("g" to listOf(color("c")))
        val emptyTypo = emptyMap<String, List<ShowkaseBrowserTypography>>()
        assertThat(components.isOnlyCategory(nonEmptyColors, emptyTypo)).isFalse()
    }
}
