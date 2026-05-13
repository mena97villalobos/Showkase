package com.airbnb.android.showkase.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
internal actual fun screenHeightDp(): Int = LocalConfiguration.current.screenHeightDp
