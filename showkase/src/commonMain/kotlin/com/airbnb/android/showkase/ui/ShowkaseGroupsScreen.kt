package com.airbnb.android.showkase.ui

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.backhandler.BackHandler
import com.airbnb.android.showkase.models.ShowkaseBrowserColor
import com.airbnb.android.showkase.models.ShowkaseBrowserComponent
import com.airbnb.android.showkase.models.ShowkaseBrowserScreenMetadata
import com.airbnb.android.showkase.models.ShowkaseBrowserTypography
import com.airbnb.android.showkase.models.ShowkaseCurrentScreen

@Composable
internal fun ShowkaseGroupsScreen(
    groupedTypographyMap: Map<String, List<*>>,
    showkaseBrowserScreenMetadata: ShowkaseBrowserScreenMetadata,
    onRootScreen: Boolean,
    onUpdateShowkaseBrowserScreenMetadata: (ShowkaseBrowserScreenMetadata) -> Unit,
    navigateToShowkaseCategories: () -> Unit,
    onFinish: () -> Unit,
    onGroupClicked: () -> Unit,
) {
    val filteredMap = remember(
        groupedTypographyMap,
        showkaseBrowserScreenMetadata.isSearchActive,
        showkaseBrowserScreenMetadata.searchQuery
    ) {
        getFilteredSearchList(
            groupedTypographyMap.toList().sortedBy { it.first }.toMap(),
            showkaseBrowserScreenMetadata.isSearchActive,
            showkaseBrowserScreenMetadata.searchQuery
        )
    }

    LazyColumn {
        items(
            items = filteredMap.entries.toList(),
            key = { it.key },
            itemContent = { (group, list) ->
                val size = getNumOfUIElements(list)
                SimpleTextCard(
                    text = "$group ($size)",
                    onClick = {
                        onUpdateShowkaseBrowserScreenMetadata(
                            showkaseBrowserScreenMetadata.copy(
                                currentGroup = group,
                                isSearchActive = false,
                                searchQuery = null
                            )
                        )
                        onGroupClicked()
                    }
                )
            }
        )
    }
    BackHandler {
        goBackToCategoriesScreen(
            showkaseBrowserScreenMetadata,
            onUpdateShowkaseBrowserScreenMetadata,
            onRootScreen = onRootScreen,
            onBackToCategories = {
                navigateToShowkaseCategories()
            },
            onBackPressOnRoot = onFinish,
        )
    }
}

internal fun getNumOfUIElements(list: List<*>): Int {
    val isComponentList = list.filterIsInstance<ShowkaseBrowserComponent>()
    return when {
        isComponentList.isNotEmpty() -> isComponentList.distinctBy { it.componentName }.size
        else -> list.size
    }
}

internal fun <T> getFilteredSearchList(
    map: Map<String, List<T>>,
    isSearchActive: Boolean,
    searchQuery: String?,
) =
    when (isSearchActive) {
        false -> map
        !searchQuery.isNullOrBlank() -> {
            map.filter {
                matchSearchQuery(
                    searchQuery!!,
                    it.key
                )
            }
        }
        else -> map
    }

@Composable
internal fun ShowkaseComponentGroupsScreen(
    groupedComponentMap: Map<String, List<ShowkaseBrowserComponent>>,
    showkaseBrowserScreenMetadata: ShowkaseBrowserScreenMetadata,
    onRootScreen: Boolean,
    onUpdateShowkaseBrowserScreenMetadata: (ShowkaseBrowserScreenMetadata) -> Unit,
    navigateTo: (ShowkaseCurrentScreen) -> Unit,
    onFinish: () -> Unit,
) {
    ShowkaseGroupsScreen(
        groupedTypographyMap = groupedComponentMap,
        showkaseBrowserScreenMetadata = showkaseBrowserScreenMetadata,
        onRootScreen = onRootScreen,
        onUpdateShowkaseBrowserScreenMetadata = onUpdateShowkaseBrowserScreenMetadata,
        navigateToShowkaseCategories = {
            navigateTo(ShowkaseCurrentScreen.SHOWKASE_CATEGORIES)
        },
        onFinish = onFinish,
    ) {
        navigateTo(ShowkaseCurrentScreen.COMPONENTS_IN_A_GROUP)
    }
}

@Composable
internal fun ShowkaseColorGroupsScreen(
    groupedColorsMap: Map<String, List<ShowkaseBrowserColor>>,
    showkaseBrowserScreenMetadata: ShowkaseBrowserScreenMetadata,
    onRootScreen: Boolean,
    onUpdateShowkaseBrowserScreenMetadata: (ShowkaseBrowserScreenMetadata) -> Unit,
    navigateTo: (ShowkaseCurrentScreen) -> Unit,
    onFinish: () -> Unit,
) {
    ShowkaseGroupsScreen(
        groupedTypographyMap = groupedColorsMap,
        showkaseBrowserScreenMetadata = showkaseBrowserScreenMetadata,
        onRootScreen = onRootScreen,
        onUpdateShowkaseBrowserScreenMetadata = onUpdateShowkaseBrowserScreenMetadata,
        navigateToShowkaseCategories = {
            navigateTo(ShowkaseCurrentScreen.SHOWKASE_CATEGORIES)
        },
        onFinish = onFinish,
    ) {
        navigateTo(ShowkaseCurrentScreen.COLORS_IN_A_GROUP)
    }
}

@Composable
internal fun ShowkaseTypographyGroupsScreen(
    groupedTypographyMap: Map<String, List<ShowkaseBrowserTypography>>,
    showkaseBrowserScreenMetadata: ShowkaseBrowserScreenMetadata,
    onRootScreen: Boolean,
    onUpdateShowkaseBrowserScreenMetadata: (ShowkaseBrowserScreenMetadata) -> Unit,
    navigateTo: (ShowkaseCurrentScreen) -> Unit,
    onFinish: () -> Unit,
) {
    val singleGroup = groupedTypographyMap.entries.singleOrNull()
    LaunchedEffect(singleGroup) {
        if (singleGroup != null && showkaseBrowserScreenMetadata.currentGroup != singleGroup.key) {
            onUpdateShowkaseBrowserScreenMetadata(
                showkaseBrowserScreenMetadata.copy(currentGroup = singleGroup.key)
            )
        }
    }
    if (singleGroup != null) {
        ShowkaseTypographyInAGroupScreen(
            groupedTypographyMap = groupedTypographyMap,
            showkaseBrowserScreenMetadata = showkaseBrowserScreenMetadata,
            onRootScreen = onRootScreen,
            onUpdateShowkaseBrowserScreenMetadata = onUpdateShowkaseBrowserScreenMetadata,
            navigateTo = navigateTo,
            onFinish = onFinish,
        )
    } else {
        ShowkaseGroupsScreen(
            groupedTypographyMap = groupedTypographyMap,
            showkaseBrowserScreenMetadata = showkaseBrowserScreenMetadata,
            onRootScreen = onRootScreen,
            onUpdateShowkaseBrowserScreenMetadata = onUpdateShowkaseBrowserScreenMetadata,
            navigateToShowkaseCategories = {
                navigateTo(ShowkaseCurrentScreen.SHOWKASE_CATEGORIES)
            },
            onFinish = onFinish,
        ) {
            navigateTo(ShowkaseCurrentScreen.TYPOGRAPHY_IN_A_GROUP)
        }
    }
}
