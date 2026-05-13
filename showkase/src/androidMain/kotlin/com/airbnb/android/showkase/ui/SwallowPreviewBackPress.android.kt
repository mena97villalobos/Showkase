package com.airbnb.android.showkase.ui

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
internal actual fun SwallowPreviewBackPress(content: @Composable () -> Unit) {
    val dispatcherOwner = rememberOnBackPressedDispatcherOwner()
    CompositionLocalProvider(LocalOnBackPressedDispatcherOwner provides dispatcherOwner) {
        content()
    }
}

@Composable
private fun rememberOnBackPressedDispatcherOwner(): OnBackPressedDispatcherOwner {
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
