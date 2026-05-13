package com.airbnb.android.showkase.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.airbnb.android.showkase.models.ShowkaseBrowserColor
import com.airbnb.android.showkase.models.ShowkaseBrowserComponent
import com.airbnb.android.showkase.models.ShowkaseBrowserScreenMetadata
import com.airbnb.android.showkase.models.ShowkaseBrowserTypography
import com.airbnb.android.showkase.models.ShowkaseCategory
import com.airbnb.android.showkase.models.ShowkaseCurrentScreen
import com.airbnb.android.showkase.models.insideGroup
import com.airbnb.android.showkase.resources.Res
import com.airbnb.android.showkase.resources.colors_category
import com.airbnb.android.showkase.resources.components_category
import com.airbnb.android.showkase.resources.search_label
import com.airbnb.android.showkase.resources.showkase_title
import com.airbnb.android.showkase.resources.typography_category
import com.airbnb.android.showkase.ui.SemanticsUtils.lineCountVal
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

@Suppress("LongMethod")
@Composable
internal fun ShowkaseBrowserApp(
    groupedComponentMap: Map<String, List<ShowkaseBrowserComponent>>,
    groupedColorsMap: Map<String, List<ShowkaseBrowserColor>>,
    groupedTypographyMap: Map<String, List<ShowkaseBrowserTypography>>,
    showkaseBrowserScreenMetadata: ShowkaseBrowserScreenMetadata,
    onUpdateShowkaseBrowserScreenMetadata: (ShowkaseBrowserScreenMetadata) -> Unit,
    onFinish: () -> Unit,
) {
    CompositionLocalProvider(
        LocalInspectionMode provides true,
    ) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute by remember(navBackStackEntry) {
            derivedStateOf { navBackStackEntry?.destination?.route }
        }
        Surface(
            color = Color.White
        ) {
            Scaffold(
                topBar = {
                    ShowkaseAppBar(
                        currentRoute = currentRoute,
                        showkaseBrowserScreenMetadata = showkaseBrowserScreenMetadata,
                        onSearchQueryChanged = {
                            onUpdateShowkaseBrowserScreenMetadata(
                                showkaseBrowserScreenMetadata.copy(searchQuery = it)
                            )
                        },
                        onClearSearch = {
                            onUpdateShowkaseBrowserScreenMetadata(
                                showkaseBrowserScreenMetadata.copy(searchQuery = "")
                            )
                        },
                        onActivateSearch = {
                            onUpdateShowkaseBrowserScreenMetadata(
                                showkaseBrowserScreenMetadata.copy(isSearchActive = true)
                            )
                        },
                        onCloseSearch = {
                            onUpdateShowkaseBrowserScreenMetadata(
                                showkaseBrowserScreenMetadata.copy(isSearchActive = false)
                            )
                        },

                        )
                },
                content = { contentPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color = SHOWKASE_COLOR_BACKGROUND)
                            .padding(contentPadding),
                    ) {
                        ShowkaseBodyContent(
                            navController,
                            groupedComponentMap,
                            groupedColorsMap,
                            groupedTypographyMap,
                            showkaseBrowserScreenMetadata,
                            onUpdateShowkaseBrowserScreenMetadata,
                            navigateTo = {
                                navController.navigate(it.name)
                            },
                            onFinish = onFinish,
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            )
        }
    }
}

@Composable
internal fun ShowkaseAppBar(
    showkaseBrowserScreenMetadata: ShowkaseBrowserScreenMetadata,
    currentRoute: String?,
    onSearchQueryChanged: (String) -> Unit,
    onCloseSearch: () -> Unit,
    onActivateSearch: () -> Unit,
    onClearSearch: () -> Unit,
) {
    Surface(
        shadowElevation = 4.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(padding2x),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShowkaseAppBarTitle(
                isSearchActive = showkaseBrowserScreenMetadata.isSearchActive,
                currentGroup = showkaseBrowserScreenMetadata.currentGroup,
                currentComponentName = showkaseBrowserScreenMetadata.currentComponentName,
                currentComponentStyleName = showkaseBrowserScreenMetadata.currentComponentStyleName,
                currentRoute = currentRoute,
                searchQuery = showkaseBrowserScreenMetadata.searchQuery,
                searchQueryValueChange = {
                    onSearchQueryChanged(it)
                },
                modifier = Modifier.fillMaxWidth(0.75f),
                onCloseSearchFieldClick = {
                    onCloseSearch()
                },
                onClearSearchField = {
                    onClearSearch()
                }
            )
            ShowkaseAppBarActions(
                isActive = showkaseBrowserScreenMetadata.isSearchActive,
                onActionClicked = {
                    onActivateSearch()
                },
                currentRoute = currentRoute,
                modifier = Modifier.fillMaxWidth(0.25f)
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun ShowkaseAppBarTitle(
    isSearchActive: Boolean,
    currentGroup: String?,
    currentComponentName: String?,
    currentComponentStyleName: String?,
    currentRoute: String?,
    searchQuery: String?,
    searchQueryValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onCloseSearchFieldClick: () -> Unit,
    onClearSearchField: () -> Unit,
) {

    AnimatedVisibility(
        visible = isSearchActive,
        enter = expandHorizontally(),
        exit = shrinkHorizontally()
    ) {
        ShowkaseSearchField(
            searchQuery = searchQuery,
            searchQueryValueChange = searchQueryValueChange,
            onCloseSearchFieldClick = onCloseSearchFieldClick,
            onClearSearchField = onClearSearchField,
        )
    }
    AnimatedVisibility(
        visible = !isSearchActive,
        enter = slideInHorizontally() + expandIn()
    ) {
        AppBarTitle(
            currentRoute = currentRoute,
            modifier = modifier,
            currentGroup = currentGroup,
            currentComponentName = currentComponentName,
            currentComponentStyleName = currentComponentStyleName
        )
    }
}

@Composable
private fun AppBarTitle(
    modifier: Modifier,
    currentRoute: String?,
    currentGroup: String?,
    currentComponentName: String?,
    currentComponentStyleName: String?
) {
    when {
        currentRoute == ShowkaseCurrentScreen.SHOWKASE_CATEGORIES.name -> {
            ToolbarTitle(stringResource(Res.string.showkase_title), modifier)
        }

        currentRoute == ShowkaseCurrentScreen.COMPONENT_GROUPS.name -> {
            ToolbarTitle(stringResource(Res.string.components_category), modifier)
        }

        currentRoute == ShowkaseCurrentScreen.COLOR_GROUPS.name -> {
            ToolbarTitle(stringResource(Res.string.colors_category), modifier)
        }

        currentRoute == ShowkaseCurrentScreen.TYPOGRAPHY_GROUPS.name -> {
            ToolbarTitle(stringResource(Res.string.typography_category), modifier)
        }

        currentRoute.insideGroup() -> {
            ToolbarTitle(currentGroup ?: "currentGroup", modifier)
        }

        currentRoute == ShowkaseCurrentScreen.COMPONENT_STYLES.name -> {
            ToolbarTitle(currentComponentName.orEmpty(), modifier)
        }

        currentRoute == ShowkaseCurrentScreen.COMPONENT_DETAIL.name -> {
            val styleName = currentComponentStyleName?.let { "[$it]" }.orEmpty()
            ToolbarTitle(
                "${currentComponentName.orEmpty()} $styleName",
                modifier
            )
        }
    }
}

@Composable
private fun ToolbarTitle(
    string: String,
    modifier: Modifier
) {
    val lineCount = remember {
        mutableStateOf(0)
    }

    Text(
        text = string,
        modifier = modifier then Modifier
            .padding(vertical = verticalToolbarPadding)
            .semantics {
                lineCountVal = lineCount.value
            },
        style = TextStyle(
            fontSize = 20.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        ),
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = {
            lineCount.value = it.lineCount
        }
    )
}

@Composable
internal fun ShowkaseSearchField(
    searchQuery: String?,
    searchQueryValueChange: (String) -> Unit,
    onCloseSearchFieldClick: () -> Unit,
    onClearSearchField: () -> Unit,
) {
    var localSearchQuery by remember { mutableStateOf(searchQuery.orEmpty()) }
    LaunchedEffect(localSearchQuery) {
        delay(300) // Debounce delay
        searchQueryValueChange(localSearchQuery)
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery != localSearchQuery) {
            localSearchQuery = searchQuery.orEmpty()
        }
    }

    TextField(
        value = localSearchQuery,
        // Update value of textValue with the latest value of the text field
        onValueChange = { localSearchQuery = it },
        label = {
            Text(text = stringResource(Res.string.search_label))
        },
        textStyle = TextStyle(
            color = Color.Black,
            fontFamily = FontFamily.Default,
            fontSize = 18.sp,
            fontWeight = FontWeight.W500
        ),
        modifier = Modifier
            .testTag("SearchTextField")
            .fillMaxWidth(),
        leadingIcon = {
            IconButton(
                onClick = onCloseSearchFieldClick,
                modifier = Modifier.testTag("close_search_bar_tag")
            ) {
                Icon(imageVector = Icons.Filled.Search, contentDescription = "Search Icon")
            }
        },
        colors = TextFieldDefaults.colors(),
        trailingIcon = {
            IconButton(
                onClick = {
                    localSearchQuery = ""
                    onClearSearchField()
                },
                modifier = Modifier.testTag("clear_search_field"),
                enabled = localSearchQuery.isNotEmpty()
            ) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear Search Field")
            }
        }
    )
}

@Composable
private fun ShowkaseAppBarActions(
    isActive: Boolean,
    onActionClicked: () -> Unit,
    currentRoute: String?,
    modifier: Modifier = Modifier
) {
    when {
        isActive -> {
        }

        currentRoute == ShowkaseCurrentScreen.COMPONENT_DETAIL.name ||
                currentRoute == ShowkaseCurrentScreen.SHOWKASE_CATEGORIES.name -> {
        }

        else -> {
            IconButton(
                modifier = modifier.testTag("SearchIcon"),
                onClick = {
                    onActionClicked()
                }
            ) {
                Icon(imageVector = Icons.Filled.Search, contentDescription = "Search Icon")
            }
        }
    }
}

@Composable
internal fun ShowkaseBodyContent(
    navController: NavHostController,
    groupedComponentMap: Map<String, List<ShowkaseBrowserComponent>>,
    groupedColorsMap: Map<String, List<ShowkaseBrowserColor>>,
    groupedTypographyMap: Map<String, List<ShowkaseBrowserTypography>>,
    showkaseBrowserScreenMetadata: ShowkaseBrowserScreenMetadata,
    onUpdateShowkaseBrowserScreenMetadata: (ShowkaseBrowserScreenMetadata) -> Unit,
    navigateTo: (ShowkaseCurrentScreen) -> Unit,
    onFinish: () -> Unit,
) {
    val startDestination = remember(groupedColorsMap, groupedTypographyMap, groupedComponentMap) {
        startDestination(
            groupedColorsMap,
            groupedTypographyMap,
            groupedComponentMap,
        )
    }
    NavHost(
        navController = navController,
        startDestination = startDestination,
        builder = {
            navGraph(
                showkaseBrowserScreenMetadata,
                onUpdateShowkaseBrowserScreenMetadata,
                groupedColorsMap,
                groupedTypographyMap,
                groupedComponentMap,
                onRootScreen = navController.currentDestination?.id == navController.graph.startDestinationId,
                navigateTo = navigateTo,
                onFinish = onFinish,
            )
        }
    )
}

internal fun startDestination(
    groupedColorsMap: Map<String, List<ShowkaseBrowserColor>>,
    groupedTypographyMap: Map<String, List<ShowkaseBrowserTypography>>,
    groupedComponentMap: Map<String, List<ShowkaseBrowserComponent>>
) = when {
    groupedComponentMap.isOnlyCategory(groupedColorsMap, groupedTypographyMap) ->
        ShowkaseCurrentScreen.COMPONENT_GROUPS.name

    groupedColorsMap.isOnlyCategory(groupedTypographyMap, groupedComponentMap) ->
        ShowkaseCurrentScreen.COLOR_GROUPS.name

    groupedTypographyMap.isOnlyCategory(groupedColorsMap, groupedComponentMap) ->
        ShowkaseCurrentScreen.TYPOGRAPHY_GROUPS.name

    else ->
        ShowkaseCurrentScreen.SHOWKASE_CATEGORIES.name
}

private fun NavGraphBuilder.navGraph(
    showkaseBrowserScreenMetadata: ShowkaseBrowserScreenMetadata,
    onUpdateShowkaseBrowserScreenMetadata: (ShowkaseBrowserScreenMetadata) -> Unit,
    groupedColorsMap: Map<String, List<ShowkaseBrowserColor>>,
    groupedTypographyMap: Map<String, List<ShowkaseBrowserTypography>>,
    groupedComponentMap: Map<String, List<ShowkaseBrowserComponent>>,
    onRootScreen: Boolean,
    navigateTo: (ShowkaseCurrentScreen) -> Unit,
    onFinish: () -> Unit,
) = when {
    groupedComponentMap.isOnlyCategory(groupedColorsMap, groupedTypographyMap) ->
        componentsNavGraph(
            groupedComponentMap = groupedComponentMap,
            showkaseBrowserScreenMetadata = showkaseBrowserScreenMetadata,
            onRootScreen = onRootScreen,
            onUpdateShowkaseBrowserScreenMetadata = onUpdateShowkaseBrowserScreenMetadata,
            navigateTo = navigateTo,
            onFinish = onFinish,
        )

    groupedColorsMap.isOnlyCategory(groupedTypographyMap, groupedComponentMap) ->
        colorsNavGraph(
            groupedColorsMap = groupedColorsMap,
            showkaseBrowserScreenMetadata = showkaseBrowserScreenMetadata,
            onRootScreen = onRootScreen,
            onUpdateShowkaseBrowserScreenMetadata = onUpdateShowkaseBrowserScreenMetadata,
            navigateTo = navigateTo,
            onFinish = onFinish,
        )

    groupedTypographyMap.isOnlyCategory(groupedColorsMap, groupedComponentMap) ->
        typographyNavGraph(
            groupedTypographyMap = groupedTypographyMap,
            showkaseBrowserScreenMetadata = showkaseBrowserScreenMetadata,
            onRootScreen = onRootScreen,
            onUpdateShowkaseBrowserScreenMetadata = onUpdateShowkaseBrowserScreenMetadata,
            navigateTo = navigateTo,
            onFinish = onFinish,
        )

    else ->
        fullNavGraph(
            groupedComponentMap = groupedComponentMap,
            groupedColorsMap = groupedColorsMap,
            groupedTypographyMap = groupedTypographyMap,
            onRootScreen = onRootScreen,
            showkaseBrowserScreenMetadata = showkaseBrowserScreenMetadata,
            onUpdateShowkaseBrowserScreenMetadata = onUpdateShowkaseBrowserScreenMetadata,
            navigateTo = navigateTo,
            onFinish = onFinish,
        )
}

internal fun Map<String, List<*>>.isOnlyCategory(
    otherCategoryMap1: Map<String, List<*>>,
    otherCategoryMap2: Map<String, List<*>>
) = this.values.isNotEmpty() && otherCategoryMap1.isEmpty() && otherCategoryMap2.isEmpty()

private fun NavGraphBuilder.componentsNavGraph(
    groupedComponentMap: Map<String, List<ShowkaseBrowserComponent>>,
    showkaseBrowserScreenMetadata: ShowkaseBrowserScreenMetadata,
    onRootScreen: Boolean,
    onUpdateShowkaseBrowserScreenMetadata: (ShowkaseBrowserScreenMetadata) -> Unit,
    navigateTo: (ShowkaseCurrentScreen) -> Unit,
    onFinish: () -> Unit,
) {
    composable(ShowkaseCurrentScreen.COMPONENT_GROUPS.name) {
        ShowkaseComponentGroupsScreen(
            groupedComponentMap = groupedComponentMap,
            showkaseBrowserScreenMetadata = showkaseBrowserScreenMetadata,
            onUpdateShowkaseBrowserScreenMetadata = onUpdateShowkaseBrowserScreenMetadata,
            onRootScreen = onRootScreen,
            navigateTo = navigateTo,
            onFinish = onFinish,
        )
    }
    composable(ShowkaseCurrentScreen.COMPONENTS_IN_A_GROUP.name) {
        ShowkaseComponentsInAGroupScreen(
            groupedComponentMap = groupedComponentMap,
            showkaseBrowserScreenMetadata = showkaseBrowserScreenMetadata,
            onUpdateShowkaseBrowserScreenMetadata = onUpdateShowkaseBrowserScreenMetadata,
            navigateTo = navigateTo,
        )
    }
    composable(ShowkaseCurrentScreen.COMPONENT_STYLES.name) {
        ShowkaseComponentStylesScreen(
            groupedComponentMap = groupedComponentMap,
            showkaseBrowserScreenMetadata = showkaseBrowserScreenMetadata,
            onUpdateShowkaseBrowserScreenMetadata = onUpdateShowkaseBrowserScreenMetadata,
            navigateTo = navigateTo,
        )
    }
    composable(ShowkaseCurrentScreen.COMPONENT_DETAIL.name) {
        ShowkaseComponentDetailScreen(
            groupedComponentMap = groupedComponentMap,
            showkaseBrowserScreenMetadata = showkaseBrowserScreenMetadata,
            onUpdateShowkaseBrowserScreenMetadata = onUpdateShowkaseBrowserScreenMetadata,
            navigateTo = navigateTo,

        )
    }
}

private fun NavGraphBuilder.colorsNavGraph(
    groupedColorsMap: Map<String, List<ShowkaseBrowserColor>>,
    showkaseBrowserScreenMetadata: ShowkaseBrowserScreenMetadata,
    onRootScreen: Boolean,
    onUpdateShowkaseBrowserScreenMetadata: (ShowkaseBrowserScreenMetadata) -> Unit,
    navigateTo: (ShowkaseCurrentScreen) -> Unit,
    onFinish: () -> Unit,
) {
    composable(ShowkaseCurrentScreen.COLOR_GROUPS.name) {
        ShowkaseColorGroupsScreen(
            groupedColorsMap = groupedColorsMap,
            showkaseBrowserScreenMetadata = showkaseBrowserScreenMetadata,
            onRootScreen = onRootScreen,
            onUpdateShowkaseBrowserScreenMetadata = onUpdateShowkaseBrowserScreenMetadata,
            navigateTo = navigateTo,
            onFinish = onFinish,
        )
    }
    composable(ShowkaseCurrentScreen.COLORS_IN_A_GROUP.name) {
        ShowkaseColorsInAGroupScreen(
            groupedColorsMap = groupedColorsMap,
            showkaseBrowserScreenMetadata = showkaseBrowserScreenMetadata,
            onUpdateShowkaseBrowserScreenMetadata = onUpdateShowkaseBrowserScreenMetadata,
            navigateTo = navigateTo
        )
    }
}

private fun NavGraphBuilder.typographyNavGraph(
    groupedTypographyMap: Map<String, List<ShowkaseBrowserTypography>>,
    showkaseBrowserScreenMetadata: ShowkaseBrowserScreenMetadata,
    onRootScreen: Boolean,
    onUpdateShowkaseBrowserScreenMetadata: (ShowkaseBrowserScreenMetadata) -> Unit,
    navigateTo: (ShowkaseCurrentScreen) -> Unit,
    onFinish: () -> Unit,
) {
    composable(ShowkaseCurrentScreen.TYPOGRAPHY_GROUPS.name) {
        ShowkaseTypographyGroupsScreen(
            groupedTypographyMap = groupedTypographyMap,
            showkaseBrowserScreenMetadata = showkaseBrowserScreenMetadata,
            onRootScreen = onRootScreen,
            onUpdateShowkaseBrowserScreenMetadata = onUpdateShowkaseBrowserScreenMetadata,
            navigateTo = navigateTo,
            onFinish = onFinish,
        )
    }
    composable(ShowkaseCurrentScreen.TYPOGRAPHY_IN_A_GROUP.name) {
        ShowkaseTypographyInAGroupScreen(
            groupedTypographyMap = groupedTypographyMap,
            showkaseBrowserScreenMetadata = showkaseBrowserScreenMetadata,
            onUpdateShowkaseBrowserScreenMetadata = onUpdateShowkaseBrowserScreenMetadata,
            onRootScreen = onRootScreen,
            navigateTo = navigateTo,
            onFinish = onFinish,
        )
    }
}

private fun NavGraphBuilder.fullNavGraph(
    groupedComponentMap: Map<String, List<ShowkaseBrowserComponent>>,
    groupedColorsMap: Map<String, List<ShowkaseBrowserColor>>,
    groupedTypographyMap: Map<String, List<ShowkaseBrowserTypography>>,
    onRootScreen: Boolean,
    showkaseBrowserScreenMetadata: ShowkaseBrowserScreenMetadata,
    onUpdateShowkaseBrowserScreenMetadata: (ShowkaseBrowserScreenMetadata) -> Unit,
    navigateTo: (ShowkaseCurrentScreen) -> Unit,
    onFinish: () -> Unit,
) {
    composable(ShowkaseCurrentScreen.SHOWKASE_CATEGORIES.name) {
        ShowkaseCategoriesScreen(
            showkaseBrowserScreenMetadata,
            onUpdateShowkaseBrowserScreenMetadata,
            getCategoryMetadataMap(
                groupedComponentMap,
                groupedColorsMap,
                groupedTypographyMap,
            ),
            onNavigateToComponentGroups = {
                navigateTo(ShowkaseCurrentScreen.COMPONENT_GROUPS)
            },
            onNavigateToColorGroups = {
                navigateTo(ShowkaseCurrentScreen.COLOR_GROUPS)
            },
            onNavigateToTypographyGroups = {
                navigateTo(ShowkaseCurrentScreen.TYPOGRAPHY_GROUPS)
            },
            onFinish = onFinish,
        )
    }
    componentsNavGraph(
        groupedComponentMap,
        showkaseBrowserScreenMetadata,
        onRootScreen = onRootScreen,
        onUpdateShowkaseBrowserScreenMetadata,
        navigateTo,
        onFinish = onFinish,
    )
    colorsNavGraph(
        groupedColorsMap,
        showkaseBrowserScreenMetadata,
        onRootScreen = onRootScreen,
        onUpdateShowkaseBrowserScreenMetadata,
        navigateTo,
        onFinish = onFinish,
    )
    typographyNavGraph(
        groupedTypographyMap,
        showkaseBrowserScreenMetadata,
        onRootScreen = onRootScreen,
        onUpdateShowkaseBrowserScreenMetadata,
        navigateTo,
        onFinish = onFinish,
    )
}

private fun getCategoryMetadataMap(
    groupedComponentMap: Map<String, List<ShowkaseBrowserComponent>>,
    groupedColorsMap: Map<String, List<ShowkaseBrowserColor>>,
    groupedTypographyMap: Map<String, List<ShowkaseBrowserTypography>>,
) = mapOf(
    ShowkaseCategory.COMPONENTS to groupedComponentMap.flatComponentCount(),
    ShowkaseCategory.COLORS to groupedColorsMap.flatCount(),
    ShowkaseCategory.TYPOGRAPHY to groupedTypographyMap.flatCount()
)

private fun Map<String, List<*>>.flatCount() = flatMap { it.value }.count()

private fun Map<String, List<ShowkaseBrowserComponent>>.flatComponentCount() = flatMap { entry ->
    // Only group name and component name is taken into account for the count to ensure that the
    // styles of the same component aren't added  in this calculation.
    entry.value.distinctBy { "${it.group}_${it.componentName}" }
}.count()

/**
 * Helper function to navigate to the passed [ShowkaseCurrentScreen]
 */
internal fun NavHostController.navigate(destinationScreen: ShowkaseCurrentScreen) =
    navigate(destinationScreen.name)

private val verticalToolbarPadding = 16.dp
