package com.airbnb.android.showkase.ui

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.backhandler.BackHandler
import com.airbnb.android.showkase.models.ShowkaseBrowserScreenMetadata
import com.airbnb.android.showkase.models.ShowkaseCategory
import com.airbnb.android.showkase.models.clear
import com.airbnb.android.showkase.models.clearActiveSearch

@Composable
internal fun ShowkaseCategoriesScreen(
    showkaseBrowserScreenMetadata: ShowkaseBrowserScreenMetadata,
    onUpdateShowkaseBrowserScreenMetadata: (ShowkaseBrowserScreenMetadata) -> Unit,
    categoryMetadataMap: Map<ShowkaseCategory, Int>,
    onNavigateToComponentGroups: () -> Unit,
    onNavigateToColorGroups: () -> Unit,
    onNavigateToTypographyGroups: () -> Unit,
    onFinish: () -> Unit,
) {
    LazyColumn {
        items(
            items = categoryMetadataMap.entries.toList(),
            key = { it.key.name },
            itemContent = { (category, categorySize) ->
                val title = category.name
                    .lowercase()
                    .replaceFirstChar { it.titlecaseChar() }

                SimpleTextCard(
                    text = "$title ($categorySize)",
                    onClick = {
                        onUpdateShowkaseBrowserScreenMetadata(
                            showkaseBrowserScreenMetadata.copy(
                                currentGroup = null,
                                isSearchActive = false,
                                searchQuery = null
                            )
                        )
                        when (category) {
                            ShowkaseCategory.COMPONENTS -> onNavigateToComponentGroups()
                            ShowkaseCategory.COLORS -> onNavigateToColorGroups()
                            ShowkaseCategory.TYPOGRAPHY -> onNavigateToTypographyGroups()
                        }
                    }
                )
            }
        )
    }
    BackHandler {
        goBackFromCategoriesScreen(
            onFinish,
            showkaseBrowserScreenMetadata,
            onUpdateShowkaseBrowserScreenMetadata
        )
    }
}

private fun goBackFromCategoriesScreen(
    onFinish: () -> Unit,
    showkaseBrowserScreenMetadata: ShowkaseBrowserScreenMetadata,
    onUpdateShowkaseBrowserScreenMetadata: (ShowkaseBrowserScreenMetadata) -> Unit
) {
    val isSearchActive = showkaseBrowserScreenMetadata.isSearchActive
    when {
        isSearchActive -> onUpdateShowkaseBrowserScreenMetadata(showkaseBrowserScreenMetadata.clearActiveSearch())
        else -> onFinish()
    }
}

internal fun goBackToCategoriesScreen(
    showkaseBrowserScreenMetadata: ShowkaseBrowserScreenMetadata,
    onUpdateShowkaseBrowserScreenMetadata: (ShowkaseBrowserScreenMetadata) -> Unit,
    onRootScreen: Boolean,
    onBackToCategories: () -> Unit,
    onBackPressOnRoot: () -> Unit,
) {
    when {
        showkaseBrowserScreenMetadata.isSearchActive ->
            onUpdateShowkaseBrowserScreenMetadata(showkaseBrowserScreenMetadata.clearActiveSearch())
        onRootScreen -> onBackPressOnRoot()
        else -> {
            onUpdateShowkaseBrowserScreenMetadata(showkaseBrowserScreenMetadata.clear())
            onBackToCategories()
        }
    }
}
