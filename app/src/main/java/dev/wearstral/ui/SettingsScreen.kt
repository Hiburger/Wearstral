package dev.wearstral.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import dev.wearstral.R

@Composable
fun SettingsScreen(
    nostalgic: Boolean,
    onToggleNostalgic: (Boolean) -> Unit,
    onOpenApiKey: () -> Unit,
    onOpenVoice: () -> Unit,
    onClearHistory: () -> Unit,
    onOpenInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmClear by remember { mutableStateOf(false) }
    var clearedFlash by remember { mutableStateOf(false) }
    LaunchedEffect(confirmClear) {
        if (confirmClear) {
            delay(4_000)
            confirmClear = false
        }
    }
    LaunchedEffect(clearedFlash) {
        if (clearedFlash) {
            delay(2_500)
            clearedFlash = false
        }
    }
    AppScaffold(timeText = { TimeText() }, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 44.dp, start = 24.dp, end = 24.dp, bottom = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = TextStyle(fontSize = 15.sp, color = MutedColor)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceColor)
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                    .clickable { onOpenApiKey() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Key,
                    contentDescription = null,
                    tint = colorResource(R.color.mistral_orange),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = stringResource(R.string.settings_api_key),
                    style = TextStyle(fontSize = 15.sp, color = Color.White)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceColor)
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                    .clickable { onOpenVoice() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = null,
                    tint = colorResource(R.color.mistral_orange),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = stringResource(R.string.settings_voice),
                    style = TextStyle(fontSize = 15.sp, color = Color.White)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceColor)
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                    .clickable { onToggleNostalgic(!nostalgic) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = colorResource(R.color.mistral_orange),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = stringResource(R.string.settings_nostalgic),
                    style = TextStyle(fontSize = 15.sp, color = Color.White),
                    modifier = Modifier.weight(1f)
                )
                MiniToggle(checked = nostalgic)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceColor)
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                    .clickable { onOpenInfo() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = colorResource(R.color.mistral_orange),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = stringResource(R.string.settings_info),
                    style = TextStyle(fontSize = 15.sp, color = Color.White)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceColor)
                    .border(
                        1.dp,
                        if (confirmClear) Color(0xFFFF8A65) else BorderColor,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable {
                        if (confirmClear) {
                            confirmClear = false
                            onClearHistory()
                            clearedFlash = true
                        } else {
                            confirmClear = true
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (clearedFlash) Icons.Filled.Check else Icons.Filled.Delete,
                    contentDescription = null,
                    tint = if (clearedFlash) colorResource(R.color.mistral_orange) else Color(0xFFFF8A65),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = stringResource(
                        when {
                            clearedFlash -> R.string.settings_cleared
                            confirmClear -> R.string.settings_clear_confirm
                            else -> R.string.settings_clear_history
                        }
                    ),
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = when {
                            clearedFlash -> colorResource(R.color.mistral_orange)
                            confirmClear -> Color(0xFFFF8A65)
                            else -> Color.White
                        }
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun MiniToggle(checked: Boolean) {
    Box(
        modifier = Modifier
            .width(36.dp)
            .height(20.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (checked) colorResource(R.color.mistral_orange) else BorderColor),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
