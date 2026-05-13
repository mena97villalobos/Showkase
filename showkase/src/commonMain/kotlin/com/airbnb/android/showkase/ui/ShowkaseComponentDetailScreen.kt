package com.airbnb.android.showkase.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.android.showkase.models.ShowkaseBrowserComponent
import com.airbnb.android.showkase.models.ShowkaseBrowserScreenMetadata
import com.airbnb.android.showkase.models.ShowkaseCurrentScreen
import com.airbnb.android.showkase.resources.Res
import com.airbnb.android.showkase.resources.showkase_browser_hide_documentation
import com.airbnb.android.showkase.resources.showkase_browser_show_documentation
import org.jetbrains.compose.resources.stringResource

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
    val buttonText = if (showDocumentation) {
        stringResource(Res.string.showkase_browser_hide_documentation)
    } else {
        stringResource(Res.string.showkase_browser_show_documentation)
    }
    val icon = if (showDocumentation) {
        Icons.Filled.KeyboardArrowUp
    } else {
        Icons.Filled.KeyboardArrowDown
    }
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
    // Wrap the previewed component so its own back-press handlers don't escape and steal the
    // browser's BackHandler. On Android this installs a fake OnBackPressedDispatcher; on other
    // targets this is a no-op (there's no global back-press dispatcher to hijack).
    SwallowPreviewBackPress {
        ComponentCardTitle("${metadata.componentName} [RTL]")
        val updatedModifier = Modifier.generateComposableModifier(metadata)
        Card(modifier = Modifier.fillMaxWidth()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(modifier = updatedModifier) {
                    DialogAwareComponent(metadata)
                }
            }
        }
    }
}

@Composable
private fun DarkModeComponentCard(metadata: ShowkaseBrowserComponent) {
    ComponentCardTitle("${metadata.componentName} [Dark Mode]")
    ComponentCard(
        metadata = metadata,
        darkMode = true
    )
}

internal fun Modifier.generateComposableModifier(metadata: ShowkaseBrowserComponent) = composed {
    val baseModifier =
        this
            .padding(padding4x)
            .sizeIn(maxHeight = Dp(screenHeightDp().toFloat()))
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
