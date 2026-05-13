package com.airbnb.android.showkase

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.airbnb.android.showkase.internal.loadShowkaseProvider
import com.airbnb.android.showkase.models.ShowkaseBrowserScreenMetadata
import com.airbnb.android.showkase.models.ShowkaseProvider
import com.airbnb.android.showkase.ui.ShowkaseBrowserApp
import com.airbnb.android.showkase.ui.ShowkaseErrorScreen

/**
 * Hosts the Showkase browser for the codegen class identified by [rootModuleCanonicalName].
 *
 * Use this overload on Android and Desktop, where the generated `${rootModuleCanonicalName}Codegen`
 * class is resolved via reflection. iOS callers must use the [ShowkaseBrowser] overload that takes
 * a concrete [ShowkaseProvider] instance — Kotlin/Native cannot do reflective class lookup.
 *
 * @param rootModuleCanonicalName Fully-qualified name of the `@ShowkaseRoot`-annotated class.
 * @param onFinish Invoked when the user attempts to back-press out of the browser's root screen.
 *                 On Android the [ShowkaseBrowserActivity] wires this to `finish()`. Other hosts
 *                 should dismiss / close the browser surface here.
 */
@Composable
fun ShowkaseBrowser(
    rootModuleCanonicalName: String,
    onFinish: () -> Unit = {},
) {
    val provider = remember(rootModuleCanonicalName) {
        loadShowkaseProvider(rootModuleCanonicalName)
    }
    ShowkaseBrowser(provider = provider, onFinish = onFinish)
}

/**
 * Hosts the Showkase browser using the supplied [provider]. This overload is required on iOS and
 * is also usable from Android/Desktop callers who already have a [ShowkaseProvider] instance.
 */
@Composable
fun ShowkaseBrowser(
    provider: ShowkaseProvider,
    onFinish: () -> Unit = {},
) {
    val metadata = remember(provider) { provider.metadata() }
    val componentList = metadata.componentList
    val colorList = metadata.colorList
    val typographyList = metadata.typographyList

    var showkaseBrowserScreenMetadata by remember {
        mutableStateOf(ShowkaseBrowserScreenMetadata())
    }
    when {
        componentList.isNotEmpty() || colorList.isNotEmpty() || typographyList.isNotEmpty() -> {
            ShowkaseBrowserApp(
                groupedComponentMap = componentList.groupBy { it.group },
                groupedColorsMap = colorList.groupBy { it.colorGroup },
                groupedTypographyMap = typographyList.groupBy { it.typographyGroup },
                showkaseBrowserScreenMetadata = showkaseBrowserScreenMetadata,
                onUpdateShowkaseBrowserScreenMetadata = { showkaseBrowserScreenMetadata = it },
                onFinish = onFinish,
            )
        }

        else -> {
            ShowkaseErrorScreen(
                errorText = "There were no elements that were annotated with either " +
                        "@ShowkaseComposable, @ShowkaseTypography or @ShowkaseColor. If you " +
                        "think this is a mistake, file an issue at " +
                        "https://github.com/airbnb/Showkase/issues"
            )
        }
    }
}
