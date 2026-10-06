package dev.wearstral.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable

/**
 * Makes a scrollable container respond to the watch crown / rotary bezel.
 * The container grabs focus on entry so crown events are routed to it.
 */
@Composable
fun Modifier.rotaryScroll(scrollState: ScrollableState): Modifier {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    return this
        .rotaryScrollable(
            behavior = RotaryScrollableDefaults.behavior(scrollableState = scrollState),
            focusRequester = focusRequester
        )
        .focusable()
}
