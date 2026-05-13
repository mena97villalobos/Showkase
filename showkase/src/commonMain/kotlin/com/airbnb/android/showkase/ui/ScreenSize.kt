package com.airbnb.android.showkase.ui

import androidx.compose.runtime.Composable

/**
 * Logical screen height in dp. On Android this resolves to
 * `LocalConfiguration.current.screenHeightDp`; on Desktop/iOS it is derived from the active
 * compose window size.
 */
@Composable
internal expect fun screenHeightDp(): Int
