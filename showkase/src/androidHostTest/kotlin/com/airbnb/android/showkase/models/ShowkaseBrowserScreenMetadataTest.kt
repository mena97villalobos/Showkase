package com.airbnb.android.showkase.models

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Covers the `clear()` and `clearActiveSearch()` extension functions on
 * [ShowkaseBrowserScreenMetadata]. These are the helpers behind the three previously-broken
 * back-press state-reset bugs — the tests here guard against regressions.
 */
class ShowkaseBrowserScreenMetadataTest {

    @Test
    fun `clear resets every field on the metadata`() {
        val populated = ShowkaseBrowserScreenMetadata(
            currentGroup = "G",
            currentComponentName = "Comp",
            currentComponentStyleName = "Style",
            currentComponentKey = "G-Comp-Style",
            isSearchActive = true,
            searchQuery = "needle",
        )

        val cleared = populated.clear()

        assertThat(cleared.currentGroup).isNull()
        assertThat(cleared.currentComponentName).isNull()
        assertThat(cleared.currentComponentStyleName).isNull()
        assertThat(cleared.currentComponentKey).isNull()
        assertThat(cleared.isSearchActive).isFalse()
        assertThat(cleared.searchQuery).isNull()
    }

    @Test
    fun `clearActiveSearch only resets isSearchActive and searchQuery`() {
        val populated = ShowkaseBrowserScreenMetadata(
            currentGroup = "G",
            currentComponentName = "Comp",
            currentComponentStyleName = "Style",
            currentComponentKey = "G-Comp-Style",
            isSearchActive = true,
            searchQuery = "needle",
        )

        val cleared = populated.clearActiveSearch()

        assertThat(cleared.currentGroup).isEqualTo("G")
        assertThat(cleared.currentComponentName).isEqualTo("Comp")
        assertThat(cleared.currentComponentStyleName).isEqualTo("Style")
        assertThat(cleared.currentComponentKey).isEqualTo("G-Comp-Style")
        assertThat(cleared.isSearchActive).isFalse()
        assertThat(cleared.searchQuery).isNull()
    }

    @Test
    fun `clear returns a new instance and does not mutate the original`() {
        val original = ShowkaseBrowserScreenMetadata(currentGroup = "G", searchQuery = "q")
        val cleared = original.clear()
        assertThat(original.currentGroup).isEqualTo("G")
        assertThat(original.searchQuery).isEqualTo("q")
        assertThat(cleared).isNotEqualTo(original)
    }

    @Test
    fun `insideGroup is true for the three in-group routes`() {
        assertThat(ShowkaseCurrentScreen.COMPONENTS_IN_A_GROUP.name.insideGroup()).isTrue()
        assertThat(ShowkaseCurrentScreen.COLORS_IN_A_GROUP.name.insideGroup()).isTrue()
        assertThat(ShowkaseCurrentScreen.TYPOGRAPHY_IN_A_GROUP.name.insideGroup()).isTrue()
    }

    @Test
    fun `insideGroup is false for other routes and null`() {
        assertThat(ShowkaseCurrentScreen.SHOWKASE_CATEGORIES.name.insideGroup()).isFalse()
        assertThat(ShowkaseCurrentScreen.COMPONENT_GROUPS.name.insideGroup()).isFalse()
        assertThat(ShowkaseCurrentScreen.COMPONENT_DETAIL.name.insideGroup()).isFalse()
        assertThat((null as String?).insideGroup()).isFalse()
    }
}
