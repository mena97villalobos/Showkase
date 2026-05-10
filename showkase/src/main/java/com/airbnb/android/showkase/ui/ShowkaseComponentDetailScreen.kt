package com.airbnb.android.showkase.ui

import android.content.Context
import android.content.res.Configuration
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import com.airbnb.android.showkase.R
import com.airbnb.android.showkase.models.ShowkaseBrowserComponent
import com.airbnb.android.showkase.models.ShowkaseBrowserScreenMetadata
import com.airbnb.android.showkase.models.ShowkaseCurrentScreen

@Composable
internal fun ShowkaseComponentDetailScreen(
    groupedComponentMap: Map<String, List<ShowkaseBrowserComponent>>,
    showkaseBrowserScreenMetadata: ShowkaseBrowserScreenMetadata,
    onUpdateShowkaseBrowserScreenMetadata: (ShowkaseBrowserScreenMetadata) -> Unit,
    navigateTo: (ShowkaseCurrentScreen) -> Unit,
) {
    val componentMetadataList =
        groupedComponentMap[showkaseBrowserScreenMetadata.currentGroup] ?: return
    val componentMetadata = componentMetadataList.find {
        it.componentKey == showkaseBrowserScreenMetadata.currentComponentKey
    } ?: return
    LazyColumn(
        modifier = Modifier.testTag("ShowkaseComponentDetailList")
    ) {
        items(
            items = listOf(componentMetadata),
            itemContent = { metadata ->
                ShowkaseComponentCardType.values().forEach { showkaseComponentCardType ->
                    when (showkaseComponentCardType) {
                        ShowkaseComponentCardType.BASIC -> {
                            if (metadata.componentKDoc.isNotBlank()) {
                                DocumentationPanel(metadata.componentKDoc)
                            }
                            BasicComponentCard(metadata)
                        }

                        ShowkaseComponentCardType.FONT_SCALE -> FontScaledComponentCard(metadata)
                        ShowkaseComponentCardType.DISPLAY_SCALED -> DisplayScaledComponentCard(
                            metadata
                        )

                        ShowkaseComponentCardType.RTL -> RTLComponentCard(metadata)
                        ShowkaseComponentCardType.DARK_MODE -> DarkModeComponentCard(metadata)
                    }
                }
            }
        )
    }
    BackHandler {
        back(
            onBackPressed = {
                onUpdateShowkaseBrowserScreenMetadata(
                    showkaseBrowserScreenMetadata.copy(
                        currentComponentStyleName = null,
                        isSearchActive = false,
                        searchQuery = null
                    )
                )
            },
            navigateTo = navigateTo
        )
    }
}

@Composable
private fun DocumentationPanel(kDoc: String) {
    var showDocumentation by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val (buttonText, icon) = getCollabsableTextAndIcon(context, showDocumentation)
    val onClick = { showDocumentation = !showDocumentation }
    if (showDocumentation) {
        Text(
            modifier = Modifier.padding(start = padding4x, end = padding4x, top = padding2x),
            text = kDoc,
            style = TextStyle(
                color = Color.DarkGray,
                fontSize = 14.sp,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.W300
            )
        )
    }
    Row(
        modifier = Modifier
            .padding(start = padding4x, end = padding4x, top = padding2x)
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.labelLarge) {
            Text(
                text = buttonText,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Icon(imageVector = icon, contentDescription = buttonText)
    }
}

private fun getCollabsableTextAndIcon(
    context: Context,
    showDocumentation: Boolean
) = when (showDocumentation) {
    true -> context.getString(R.string.showkase_browser_hide_documentation) to Icons.Filled.KeyboardArrowUp
    false -> context.getString(R.string.showkase_browser_show_documentation) to Icons.Filled.KeyboardArrowDown
}

@Composable
private fun BasicComponentCard(metadata: ShowkaseBrowserComponent) {
    ComponentCardTitle("${metadata.componentName} [Basic Example]")
    ComponentCard(metadata)
}

@Composable
private fun FontScaledComponentCard(metadata: ShowkaseBrowserComponent) {
    val density = LocalDensity.current
    val customDensity = Density(fontScale = density.fontScale * 2, density = density.density)

    ComponentCardTitle("${metadata.componentName} [Font Scaled x 2]")
    CompositionLocalProvider(LocalDensity provides customDensity) {
        ComponentCard(metadata)
    }
}

@Composable
private fun DisplayScaledComponentCard(metadata: ShowkaseBrowserComponent) {
    val density = LocalDensity.current
    val customDensity = Density(density = density.density * 2f)

    ComponentCardTitle("${metadata.componentName} [Display Scaled x 2]")
    CompositionLocalProvider(LocalDensity provides customDensity) {
        ComponentCard(metadata)
    }
}

@Composable
private fun RTLComponentCard(metadata: ShowkaseBrowserComponent) {
    // This is added to make sure that the navigation of the ShowkaseBrowser does not break
    // when one of the previews has a back press handler in the implementation of the component.
    val backPressedDispatcherOwner = rememberOnBackPressedDispatcherOwner()
    CompositionLocalProvider(LocalOnBackPressedDispatcherOwner provides backPressedDispatcherOwner) {
        ComponentCardTitle("${metadata.componentName} [RTL]")
        val updatedModifier = Modifier.generateComposableModifier(metadata)
        Card(modifier = Modifier.fillMaxWidth()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(modifier = updatedModifier) {
                    metadata.component()
                }
            }
        }
    }
}

@Composable
private fun DarkModeComponentCard(metadata: ShowkaseBrowserComponent) {
    val darkModeConfiguration = Configuration(LocalConfiguration.current).apply {
        uiMode = Configuration.UI_MODE_NIGHT_YES
    }
    ComponentCardTitle("${metadata.componentName} [Dark Mode]")
    CompositionLocalProvider(LocalConfiguration provides darkModeConfiguration) {
        ComponentCard(
            metadata = metadata,
            darkMode = true
        )
    }
}

internal fun Modifier.generateComposableModifier(metadata: ShowkaseBrowserComponent) = composed {
    val baseModifier =
        this
            .padding(padding4x)
            .sizeIn(maxHeight = Dp(LocalConfiguration.current.screenHeightDp.toFloat()))
    val widthDp = metadata.widthDp
    val heightDp = metadata.heightDp
    when {
        heightDp != null && widthDp != null -> baseModifier.size(
            width = widthDp.dp,
            height = heightDp.dp
        )

        heightDp != null -> baseModifier.height(Dp(heightDp.toFloat()))
        widthDp != null -> baseModifier.width(Dp(widthDp.toFloat()))
        else -> baseModifier.fillMaxWidth()
    }
}

private fun back(
    onBackPressed: () -> Unit,
    navigateTo: (ShowkaseCurrentScreen) -> Unit,
) {
    onBackPressed()
    navigateTo(ShowkaseCurrentScreen.COMPONENT_STYLES)
}

/**
 * Returns an [OnBackPressedDispatcherOwner] backed by a fresh, unwired
 * [OnBackPressedDispatcher]. The dispatcher is intentionally not connected to the host activity's
 * lifecycle — its purpose is to **swallow** back-press handlers registered by preview composables
 * so they don't intercept browser-level back navigation. Provided through
 * [LocalOnBackPressedDispatcherOwner] in places like [ComponentCard] and the paparazzi wrapper.
 */
@Composable
internal fun rememberOnBackPressedDispatcherOwner(): OnBackPressedDispatcherOwner {
    val lifecycleOwner = LocalLifecycleOwner.current
    return remember(lifecycleOwner) {
        object : OnBackPressedDispatcherOwner {
            override val lifecycle: Lifecycle
                get() = lifecycleOwner.lifecycle
            override val onBackPressedDispatcher: OnBackPressedDispatcher
                get() = OnBackPressedDispatcher()
        }
    }
}
