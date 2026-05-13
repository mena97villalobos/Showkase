package com.airbnb.android.showkase.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.airbnb.android.showkase.models.ShowkaseBrowserComponent
import com.airbnb.android.showkase.resources.Res
import com.airbnb.android.showkase.resources.showkase_browser_hide_dialog
import com.airbnb.android.showkase.resources.showkase_browser_show_dialog
import org.jetbrains.compose.resources.stringResource

private val LightColors = lightColorScheme()
private val DarkColors = darkColorScheme()

@Composable
internal fun SimpleTextCard(
    text: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = padding4x, end = padding4x, top = padding2x, bottom = padding2x)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(padding4x),
            style = TextStyle(
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
internal fun ComponentCardTitle(componentName: String) {
    Text(
        text = componentName,
        modifier = Modifier.padding(
            start = padding4x, end = padding4x, top = padding8x,
            bottom = padding1x,
        ),
        style = TextStyle(
            fontSize = 16.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
        ),
    )
}

@Composable
internal fun ComponentCard(
    metadata: ShowkaseBrowserComponent,
    onClick: (() -> Unit)? = null,
    darkMode: Boolean = false,
) {
    // Wrap the previewed component so its own back-press handlers don't escape and steal the
    // browser's BackHandler. On Android this installs a fake OnBackPressedDispatcher; on other
    // targets this is a no-op.
    SwallowPreviewBackPress {
        val composableModifier = Modifier.generateComposableModifier(metadata)
        val composableContainerModifier = Modifier.generateContainerModifier(onClick)
        MaterialTheme(
            colorScheme = if (darkMode) DarkColors else LightColors,
        ) {
            Card(
                shape = MaterialTheme.shapes.large,
            ) {
                Box {
                    Column(modifier = composableModifier) {
                        DialogAwareComponent(metadata)
                    }
                    // Stacked sibling that intercepts touches when there's an onClick (used on the
                    // "Group components" screen). When onClick is null the column is empty and the
                    // touches pass through.
                    Column(
                        modifier = Modifier
                            .matchParentSize()
                            .then(composableContainerModifier),
                    ) {}
                }
            }
        }
    }
}

private fun Modifier.generateContainerModifier(onClick: (() -> Unit)?) =
    onClick?.let {
        fillMaxWidth()
            .clickable(onClick = onClick)
    } ?: fillMaxWidth()

@Composable
internal fun DialogAwareComponent(metadata: ShowkaseBrowserComponent) {
    if (!metadata.isDialog) {
        metadata.component()
        return
    }
    var show by remember { mutableStateOf(false) }
    val showLabel = metadata.dialogButtonText
        .ifEmpty { stringResource(Res.string.showkase_browser_show_dialog) }
    val hideLabel = metadata.dialogHideButtonText
        .ifEmpty { stringResource(Res.string.showkase_browser_hide_dialog) }
    Button(onClick = { show = !show }) {
        Text(text = if (show) hideLabel else showLabel)
    }
    if (show) metadata.component()
}
