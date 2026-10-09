package dev.wearstral.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import dev.wearstral.R
import dev.wearstral.voice.VoiceLanguageUi

// quite a few imports eh

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VoiceScreen(
    rows: List<VoiceLanguageUi>,
    onLanguageTap: (String) -> Unit,
    onDeleteLanguage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    var selected by remember { mutableStateOf<VoiceLanguageUi?>(null) }
    // keep the screen awake during downloads
    val view = LocalView.current
    val context = LocalContext.current
    val downloading = rows.any { it.progress != null }
    DisposableEffect(downloading) {
        view.keepScreenOn = downloading
        onDispose { view.keepScreenOn = false }
    }

    var prevRowStates by remember { mutableStateOf(mapOf<String, Pair<Int?, Boolean>>()) }
    LaunchedEffect(rows) {
        val next = rows.associate { it.language.id to (it.progress to it.failed) }
        next.forEach { (id, rowState) ->
            val before = prevRowStates[id] ?: return@forEach
            if (before.first != null && rowState.first == null && !rowState.second) {
                context.haptic(Haptic.Click)
            }
            if (!before.second && rowState.second) {
                context.haptic(Haptic.HeavyClick)
            }
        }
        prevRowStates = next
    }
    AppScaffold(timeText = { TimeText() }, modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            val listState = rememberScalingLazyListState()
            ScalingLazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .rotaryScroll(listState),
                contentPadding = PaddingValues(top = 32.dp, start = 24.dp, end = 24.dp, bottom = 44.dp),
                autoCentering = null,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                scalingParams = ScalingLazyColumnDefaults.scalingParams(edgeAlpha = 1f)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.voice_title),
                        style = TextStyle(fontSize = 15.sp, color = MutedColor)
                    )
                }
                items(rows) { row ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceColor)
                            .border(
                                1.dp,
                                if (row.active) colorResource(R.color.mistral_orange) else BorderColor,
                                RoundedCornerShape(14.dp)
                            )
                            .combinedClickable(
                                onClick = { onLanguageTap(row.language.id) },
                                onLongClick = {
                                    if (row.downloaded) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        selected = row
                                    }
                                }
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = row.language.name,
                            style = TextStyle(fontSize = 13.sp, color = Color.White)
                        )
                        StatusText(row)
                        if (row.progress != null) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .fillMaxWidth(row.progress / 100f)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(colorResource(R.color.mistral_orange))
                            )
                        }
                    }
                }
            }

            selected?.let { row ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { selected = null }
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceColor)
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                        // swallow taps on the card itself so they do not
                        // fall through to the dismiss overlay behind it
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { }
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = row.language.name,
                        style = TextStyle(fontSize = 12.sp, color = MutedColor),
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(BorderColor)
                    )
                    OptionsRow(
                        icon = Icons.Filled.Delete,
                        label = stringResource(R.string.voice_delete_model),
                        onClick = {
                            onDeleteLanguage(row.language.id)
                            selected = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusText(row: VoiceLanguageUi) {
    val text = when {
        row.progress != null -> stringResource(R.string.voice_downloading, row.progress)
        row.failed -> stringResource(R.string.voice_failed)
        row.active -> stringResource(R.string.voice_active)
        row.downloaded -> stringResource(R.string.voice_downloaded)
        else -> stringResource(R.string.voice_tap_download)
    }
    Text(
        text = text,
        style = TextStyle(
            fontSize = 11.sp,
            color = when {
                row.failed -> Color(0xFFFF8A65)
                row.active -> colorResource(R.color.mistral_orange)
                else -> MutedColor
            }
        )
    )
}

@Composable
private fun OptionsRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(PanelColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colorResource(R.color.mistral_orange),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = TextStyle(fontSize = 12.sp, color = Color.White)
        )
    }
}
