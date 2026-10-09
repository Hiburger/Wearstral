package dev.wearstral.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import dev.wearstral.R

@Composable
fun InfoScreen(
    version: String,
    modelName: String,
    nostalgic: Boolean,
    voiceModel: String?,
    modifier: Modifier = Modifier
) {
    AppScaffold(timeText = { TimeText() }, modifier = modifier) {
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
                    text = stringResource(R.string.info_title),
                    style = TextStyle(fontSize = 15.sp, color = MutedColor)
                )
            }
            item {
                InfoCard(
                    icon = Icons.Filled.ChatBubble,
                    title = "Wearstral",
                    value = stringResource(R.string.info_version, version)
                )
            }
            item {
                InfoCard(
                    icon = Icons.Filled.Memory,
                    title = stringResource(R.string.info_model),
                    value = modelName
                )
            }
            item {
                InfoCard(
                    icon = Icons.Filled.Mic,
                    title = stringResource(R.string.info_voice),
                    value = voiceModel ?: stringResource(R.string.info_voice_none)
                )
            }
            item {
                InfoCard(
                    icon = Icons.Filled.Favorite,
                    title = stringResource(R.string.info_nostalgic),
                    value = stringResource(
                        if (nostalgic) R.string.info_on else R.string.info_off
                    )
                )
            }
        }
    }
}

@Composable
private fun InfoCard(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceColor)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colorResource(R.color.mistral_orange),
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = title,
                style = TextStyle(fontSize = 13.sp, color = Color.White)
            )
            Text(
                text = value,
                style = TextStyle(fontSize = 11.sp, color = MutedColor)
            )
        }
    }
}
