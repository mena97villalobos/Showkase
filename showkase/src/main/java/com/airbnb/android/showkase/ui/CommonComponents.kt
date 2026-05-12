package com.airbnb.android.showkase.ui

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.airbnb.android.showkase.R
import com.airbnb.android.showkase.models.ShowkaseBrowserComponent

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
    // This is added to make sure that the navigation of the ShowkaseBrowser does not break
    // when one of the previews has a back press handler in the implementation of the component.
    val backPressedDispatcherOwner = rememberOnBackPressedDispatcherOwner()
    CompositionLocalProvider(LocalOnBackPressedDispatcherOwner provides backPressedDispatcherOwner) {
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
                    // Need to add this as part of the stack so that we can intercept the touch of the
                    // component when we are on the "Group components" screen. If
                    // composableContainerModifier does not have any clickable modifiers, this column has no
                    // impact and the touches go through to the component(this happens in the "Component
                    // Detail" screen.
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
    val context = LocalContext.current
    val showLabel = metadata.dialogButtonText
        .ifEmpty { context.getString(R.string.showkase_browser_show_dialog) }
    val hideLabel = metadata.dialogHideButtonText
        .ifEmpty { context.getString(R.string.showkase_browser_hide_dialog) }
    Button(onClick = { show = !show }) {
        Text(text = if (show) hideLabel else showLabel)
    }
    if (show) metadata.component()
}
