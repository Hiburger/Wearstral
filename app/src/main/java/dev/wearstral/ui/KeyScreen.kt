package dev.wearstral.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import dev.wearstral.R
import dev.wearstral.settings.KeyEntryServer

@Composable
fun KeyScreen(
    currentKey: String?,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    var input by rememberSaveable { mutableStateOf("") }
    var revealed by rememberSaveable { mutableStateOf(false) }
    val currentOnSave by rememberUpdatedState(onSave)
    val server = remember { KeyEntryServer { currentOnSave(it) } }
    val serverState by server.state.collectAsState()
    DisposableEffect(server) {
        onDispose { server.stop() }
    }
    // keep the screen awake while a session runs: Wear OS freezes the app
    // (and the server) as soon as the screen dims, and the user needs the
    // screen on anyway to read the URL and PIN
    val view = LocalView.current
    DisposableEffect(serverState is KeyEntryServer.State.Running) {
        view.keepScreenOn = serverState is KeyEntryServer.State.Running
        onDispose { view.keepScreenOn = false }
    }
    val listState = rememberScalingLazyListState()

    AppScaffold(modifier = modifier) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .rotaryScroll(listState),
            contentPadding = PaddingValues(top = 32.dp, start = 24.dp, end = 24.dp, bottom = 44.dp),
            autoCentering = null,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            scalingParams = ScalingLazyColumnDefaults.scalingParams(edgeAlpha = 1f)
        ) {
            item {
                Text(
                    text = if (currentKey == null) {
                        stringResource(R.string.key_title_new)
                    } else {
                        stringResource(R.string.key_title_saved, currentKey.takeLast(4))
                    },
                    textAlign = TextAlign.Center
                )
            }
            when (val s = serverState) {
                is KeyEntryServer.State.Running -> {
                    item {
                        Text(
                            text = stringResource(R.string.key_remote_hint),
                            style = TextStyle(fontSize = 12.sp, color = MutedColor),
                            textAlign = TextAlign.Center
                        )
                    }
                    item {
                        Text(
                            text = s.url,
                            style = TextStyle(
                                fontSize = 13.sp,
                                color = colorResource(R.color.mistral_orange)
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                    item {
                        Text(
                            text = stringResource(R.string.key_remote_pin, s.pin),
                            style = TextStyle(fontSize = 22.sp, color = Color.White),
                            textAlign = TextAlign.Center
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceColor)
                                .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                                .clickable { server.stop() }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                tint = colorResource(R.color.mistral_orange),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = stringResource(R.string.key_remote_stop),
                                style = TextStyle(fontSize = 14.sp, color = Color.White),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                KeyEntryServer.State.Done -> {
                    item {
                        Text(
                            text = stringResource(R.string.key_remote_saved),
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = colorResource(R.color.mistral_orange)
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1E1E22))
                                .border(1.dp, Color(0xFF44474B), RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BasicTextField(
                                    value = input,
                                    onValueChange = { input = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 14.sp
                                    ),
                                    visualTransformation = if (revealed) {
                                        VisualTransformation.None
                                    } else {
                                        PasswordVisualTransformation()
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    cursorBrush = SolidColor(Color.White),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 4.dp)
                                )
                                Icon(
                                    imageVector = if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = stringResource(if (revealed) R.string.key_hide else R.string.key_show),
                                    tint = Color.White,
                                    modifier = Modifier
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { revealed = !revealed }
                                        .padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                                )
                            }
                        }
                    }
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CompactButton(
                                onClick = {
                                    onSave(input)
                                    input = ""
                                },
                                enabled = input.isNotBlank(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorResource(R.color.mistral_orange),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.key_save),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (currentKey != null) {
                                CompactButton(
                                    onClick = {
                                        input = ""
                                        onClear()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Text(
                                        text = stringResource(R.string.key_clear),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceColor)
                                .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                                .clickable { server.start() }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhoneAndroid,
                                contentDescription = null,
                                tint = colorResource(R.color.mistral_orange),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = stringResource(R.string.key_remote_button),
                                style = TextStyle(fontSize = 14.sp, color = Color.White)
                            )
                        }
                    }
                    if (serverState is KeyEntryServer.State.Failed) {
                        item {
                            Text(
                                text = stringResource(
                                    if ((serverState as KeyEntryServer.State.Failed).reason ==
                                        KeyEntryServer.State.Reason.NoWifi
                                    ) {
                                        R.string.key_remote_nowifi
                                    } else {
                                        R.string.key_remote_error
                                    }
                                ),
                                style = TextStyle(fontSize = 11.sp, color = Color(0xFFFF8A65)),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
