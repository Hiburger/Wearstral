package dev.wearstral.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import dev.wearstral.R
import dev.wearstral.voice.VoiceLanguageUi

@Composable
fun VoiceScreen(
    rows: List<VoiceLanguageUi>,
    onLanguageTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // keep the screen awake during downloads
    val view = LocalView.current
    val downloading = rows.any { it.progress != null }
    DisposableEffect(downloading) {
        view.keepScreenOn = downloading
        onDispose { view.keepScreenOn = false }
    }
    AppScaffold(timeText = { TimeText() }, modifier = modifier) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .rotaryScroll(scrollState)
                .padding(top = 44.dp, start = 24.dp, end = 24.dp, bottom = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.voice_title),
                style = TextStyle(fontSize = 15.sp, color = MutedColor)
            )
            rows.forEach { row ->
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
                        .clickable { onLanguageTap(row.language.id) }
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
