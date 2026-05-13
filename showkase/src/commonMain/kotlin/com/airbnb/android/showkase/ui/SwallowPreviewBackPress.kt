package com.airbnb.android.showkase.ui

import androidx.compose.runtime.Composable

/**
 * Wraps a previewed component so its own back-press handlers don't escape and steal the browser's
 * BackHandler. On Android this installs a fresh unwired [OnBackPressedDispatcherOwner] into
 * [LocalOnBackPressedDispatcherOwner] so the browser activity's dispatcher remains the
 * back-press authority. On other targets there is no global back-press dispatcher to hijack, so
 * the actual is a no-op that just calls [content].
 */
@Composable
internal expect fun SwallowPreviewBackPress(content: @Composable () -> Unit)
