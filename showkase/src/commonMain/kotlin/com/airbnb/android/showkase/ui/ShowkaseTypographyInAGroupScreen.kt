package com.airbnb.android.showkase.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.airbnb.android.showkase.models.ShowkaseBrowserScreenMetadata
import com.airbnb.android.showkase.models.ShowkaseBrowserTypography
import com.airbnb.android.showkase.models.ShowkaseCurrentScreen
import com.airbnb.android.showkase.models.clear
import com.airbnb.android.showkase.models.clearActiveSearch

@Composable
internal fun ShowkaseTypographyInAGroupScreen(
    groupedTypographyMap: Map<String, List<ShowkaseBrowserTypography>>,
    showkaseBrowserScreenMetadata: ShowkaseBrowserScreenMetadata,
    onRootScreen: Boolean,
    onUpdateShowkaseBrowserScreenMetadata: (ShowkaseBrowserScreenMetadata) -> Unit,
    navigateTo: (ShowkaseCurrentScreen) -> Unit,
    onFinish: () -> Unit,
) {
    val groupTypographyList = remember(groupedTypographyMap, showkaseBrowserScreenMetadata.currentGroup) {
        groupedTypographyMap[showkaseBrowserScreenMetadata.currentGroup]
            ?.sortedBy { it.typographyName }
    } ?: return
    val filteredList = remember(
        groupTypographyList,
        showkaseBrowserScreenMetadata.isSearchActive,
        showkaseBrowserScreenMetadata.searchQuery
    ) {
        getFilteredSearchList(groupTypographyList, showkaseBrowserScreenMetadata)
    }
    LazyColumn(
        modifier = Modifier
            .background(Color.White)
            .fillMaxSize()
            .testTag("TypographyInAGroupList")
    ) {
        items(
            items = filteredList,
            key = { "${it.typographyGroup}_${it.typographyName}" },
            itemContent = { groupTypographyMetadata ->
                Text(
                    text = groupTypographyMetadata.typographyName.replaceFirstChar {
                        it.titlecaseChar()
                    },
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .padding(padding4x),
                    style = groupTypographyMetadata.textStyle
                )
                HorizontalDivider()
            }
        )
    }
    BackHandler {
        goBackFromTypographyInAGroupScreen(
            showkaseBrowserScreenMetadata,
            onUpdateShowkaseBrowserScreenMetadata,
            noGroups = groupedTypographyMap.size == 1,
            onRootScreen = onRootScreen,
            navigateTo = navigateTo,
            onBackPressOnRoot = onFinish,
        )
    }
}

private fun goBackFromTypographyInAGroupScreen(
    showkaseBrowserScreenMetadata: ShowkaseBrowserScreenMetadata,
    onUpdateShowkaseBrowserScreenMetadata: (ShowkaseBrowserScreenMetadata) -> Unit,
    noGroups: Boolean,
    onRootScreen: Boolean,
    navigateTo: (ShowkaseCurrentScreen) -> Unit,
    onBackPressOnRoot: () -> Unit,
) {
    val isSearchActive = showkaseBrowserScreenMetadata.isSearchActive
    when {
        isSearchActive -> onUpdateShowkaseBrowserScreenMetadata(showkaseBrowserScreenMetadata.clearActiveSearch())
        noGroups -> {
            onUpdateShowkaseBrowserScreenMetadata(showkaseBrowserScreenMetadata.clear())
            if (onRootScreen) {
                onBackPressOnRoot()
            } else {
                navigateTo(ShowkaseCurrentScreen.SHOWKASE_CATEGORIES)
            }
        }
        else -> {
            onUpdateShowkaseBrowserScreenMetadata(showkaseBrowserScreenMetadata.clear())
            navigateTo(ShowkaseCurrentScreen.TYPOGRAPHY_GROUPS)
        }
    }
}

private fun getFilteredSearchList(
    list: List<ShowkaseBrowserTypography>,
    showkaseBrowserScreenMetadata: ShowkaseBrowserScreenMetadata
) =
    when (showkaseBrowserScreenMetadata.isSearchActive) {
        false -> list
        !showkaseBrowserScreenMetadata.searchQuery.isNullOrBlank() -> {
            list.filter {
                matchSearchQuery(
                    showkaseBrowserScreenMetadata.searchQuery!!,
                    it.typographyName
                )
            }
        }
        else -> list
    }
