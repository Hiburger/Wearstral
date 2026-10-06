package dev.wearstral.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import dev.wearstral.R
import dev.wearstral.chat.ChatError
import dev.wearstral.chat.ChatState
import dev.wearstral.chat.Conversation
import dev.wearstral.chat.Role
import dev.wearstral.voice.VoiceState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

private const val MAX_BLUR = 16f
private const val PANEL_MAX_ROWS = 4

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    state: ChatState,
    nostalgic: Boolean,
    history: List<Conversation>,
    activeId: Long,
    voiceState: VoiceState,
    voiceFinal: String?,
    onSend: (String) -> Unit,
    onNewChat: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenConversation: (Long) -> Unit,
    onVoiceTap: () -> Unit,
    onVoiceFinalConsumed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    var inputBeforeVoice by remember { mutableStateOf("") }

    var voiceHintRes by remember { mutableStateOf<Int?>(null) }

    val voicePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            inputBeforeVoice = input
            onVoiceTap()
        } else {
            voiceHintRes = R.string.chat_voice_denied
        }
    }

    val requestVoice: () -> Unit = {
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            inputBeforeVoice = input
            onVoiceTap()
        } else {
            voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val submit: () -> Unit = {
        if (input.isNotBlank() && !state.isSending) {
            onSend(input)
            input = ""
            keyboard?.hide()
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(voiceState) {
        val current = voiceState
        if (current is VoiceState.Listening) {
            input = current.partial
            voiceHintRes = null
        } else if (current is VoiceState.Failed) {
            voiceHintRes = when (current.reason) {
                VoiceState.Reason.NoModel -> R.string.chat_voice_not_setup
                VoiceState.Reason.MicError -> R.string.chat_voice_mic_error
            }
        }
    }

    LaunchedEffect(voiceHintRes) {
        if (voiceHintRes != null) {
            delay(3_000)
            voiceHintRes = null
        }
    }

    LaunchedEffect(voiceFinal) {
        voiceFinal?.let { text ->
            input = text.ifBlank { inputBeforeVoice }
            onVoiceFinalConsumed()
        }
    }

    var panelWidthPx by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val panelOffset = remember { Animatable(0f) }
    val panelScrollState = rememberScrollState()
    val progress = if (panelWidthPx > 0) {
        (panelOffset.value / panelWidthPx).coerceIn(0f, 1f)
    } else {
        0f
    }

    // route crown focus: the side panel takes it while open, the chat list
    // otherwise; re-grab after a send so the crown works again after typing
    val chatFocusRequester = remember { FocusRequester() }
    val panelFocusRequester = remember { FocusRequester() }
    val panelOpen = progress > 0.5f
    LaunchedEffect(panelOpen, state.isSending, state.messages.size) {
        (if (panelOpen) panelFocusRequester else chatFocusRequester).requestFocus()
    }

    fun closePanel() {
        scope.launch { panelOffset.animateTo(0f) }
    }

    fun settlePanel() {
        scope.launch {
            val target = if (panelOffset.value >= panelWidthPx / 4f) panelWidthPx.toFloat() else 0f
            panelOffset.animateTo(target)
        }
    }

    fun trackPanel(delta: Float) {
        scope.launch {
            panelOffset.snapTo(
                (panelOffset.value - delta).coerceIn(0f, panelWidthPx.toFloat())
            )
        }
    }

    BackHandler(enabled = progress > 0.01f) { closePanel() }

    // Rewind the panel list to the top once the panel is fully hidden, so it
    // always reappears from the beginning (buttons included) on the next open.
    LaunchedEffect(Unit) {
        snapshotFlow { panelOffset.value }.collect { offset ->
            if (offset < 1f && panelScrollState.value != 0) {
                panelScrollState.scrollTo(0)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { panelWidthPx = it.width }
    ) {
        AppScaffold(
            modifier = Modifier.then(
                if (progress > 0f) Modifier.blur(MAX_BLUR.dp * progress) else Modifier
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 44.dp, bottom = 34.dp, start = 24.dp, end = 24.dp)
                        .rotaryScrollable(
                            behavior = RotaryScrollableDefaults.behavior(scrollableState = listState),
                            focusRequester = chatFocusRequester,
                            // the chat list is reversed (newest at the bottom),
                            // so flip the crown direction to match every other
                            // scrollable in the app
                            reverseDirection = true
                        )
                        .focusable(),
                    reverseLayout = true,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom)
                ) {
                    item {
                        val focusRequester = remember { FocusRequester() }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 6.dp, end = 6.dp)
                                .height(42.dp)
                                .clip(CircleShape)
                                .background(SurfaceColor)
                                .border(1.dp, BorderColor, CircleShape)
                                .padding(start = 16.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .pointerInput(Unit) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(pass = PointerEventPass.Initial)
                                            var moved = 0f
                                            while (true) {
                                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                                val change = event.changes
                                                    .firstOrNull { it.id == down.id } ?: break
                                                if (!change.pressed) break
                                                moved = maxOf(
                                                    moved,
                                                    abs(change.position.x - down.position.x) +
                                                        abs(change.position.y - down.position.y)
                                                )
                                            }
                                            if (moved < 20f) focusRequester.requestFocus()
                                        }
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (input.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.chat_input_hint),
                                        style = TextStyle(fontSize = 13.sp, color = HintColor),
                                        maxLines = 1
                                    )
                                }
                                BasicTextField(
                                    value = input,
                                    onValueChange = { input = it },
                                    singleLine = true,
                                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = { submit() }),
                                    cursorBrush = SolidColor(Color.White),
                                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                                )
                            }
                            val listening = voiceState is VoiceState.Listening
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .alpha(if (state.isSending) 0.5f else 1f)
                                    .clip(CircleShape)
                                    .background(colorResource(R.color.mistral_orange))
                                    .pointerInput(listening) {
                                        detectTapGestures(
                                            onTap = { if (listening) onVoiceTap() else submit() },
                                            onLongPress = { if (!listening) requestVoice() }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (listening) {
                                    VoiceMicIcon()
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Send,
                                        contentDescription = stringResource(R.string.chat_send),
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                    if (state.isSending || state.error != null || voiceHintRes != null) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (state.isSending) {
                                        ThinkingDots()
                                    }
                                    state.error?.let { error ->
                                        Text(
                                            text = stringResource(error.displayRes()),
                                            style = TextStyle(fontSize = 12.sp, color = Color(0xFFFF8A65))
                                        )
                                    }
                                    voiceHintRes?.let { res ->
                                        Text(
                                            text = stringResource(res),
                                            style = TextStyle(fontSize = 12.sp, color = Color(0xFFFF8A65))
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (state.messages.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WavingHand,
                                    contentDescription = null,
                                    tint = colorResource(R.color.mistral_orange),
                                    modifier = Modifier.size(46.dp)
                                )
                                Text(
                                    text = stringResource(R.string.chat_hint),
                                    style = TextStyle(fontSize = 13.sp, color = MutedColor),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                )
                            }
                        }
                    }
                    items(state.messages.size) { index ->
                        val message = state.messages[state.messages.size - 1 - index]
                        val isUser = message.role == Role.User
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                        ) {
                            Text(
                                text = stringResource(
                                    if (isUser) R.string.chat_role_you
                                    else if (nostalgic) R.string.chat_role_ai_nostalgic
                                    else R.string.chat_role_ai
                                ),
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    color = if (isUser) {
                                        colorResource(R.color.mistral_orange)
                                    } else {
                                        MutedColor
                                    }
                                )
                            )
                            Text(
                                text = message.content.toChatMarkdown(),
                                style = TextStyle(fontSize = 14.sp, color = Color.White),
                                textAlign = if (isUser) TextAlign.End else TextAlign.Start
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Black,
                                    0.3f to Color.Black,
                                    1f to Color.Transparent
                                )
                            )
                        )
                )
            }
        }

        if (progress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(progress)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .pointerInput(panelWidthPx) {
                        detectTapGestures { closePanel() }
                    }
            )
        }

        Box(
            modifier = Modifier
                .offset { IntOffset((panelWidthPx - panelOffset.value).roundToInt(), 0) }
                .fillMaxSize()
                .background(PanelColor)
                .pointerInput(panelWidthPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        var panelDrag: Boolean? = null
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: break
                            if (!change.pressed) break
                            if (change.isConsumed) continue
                            val delta = change.positionChange()
                            if (panelDrag == null && abs(delta.x) + abs(delta.y) > 4f) {
                                panelDrag = abs(delta.x) > abs(delta.y)
                            }
                            if (panelDrag == true) {
                                trackPanel(delta.x)
                                change.consume()
                            }
                        }
                        if (panelDrag == true) {
                            settlePanel()
                        }
                    }
                }
        ) {
            val archived = history.filterNot { it.id == activeId }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(panelScrollState)
                    .rotaryScrollable(
                        behavior = RotaryScrollableDefaults.behavior(scrollableState = panelScrollState),
                        focusRequester = panelFocusRequester
                    )
                    .focusable()
                    .padding(top = 44.dp, start = 24.dp, end = 24.dp, bottom = 44.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PanelButton(
                        icon = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.chat_new),
                        onClick = {
                            scope.launch { panelOffset.animateTo(0f) }
                            onNewChat()
                        }
                    )
                    PanelButton(
                        icon = Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.chat_settings),
                        onClick = {
                            scope.launch { panelOffset.snapTo(0f) }
                            onOpenSettings()
                        }
                    )
                }
                if (archived.isEmpty()) {
                    Text(
                        text = stringResource(R.string.chat_panel_history_hint),
                        style = TextStyle(fontSize = 12.sp, color = HintColor),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                    )
                } else {
                    archived.take(PANEL_MAX_ROWS - 1).forEach { conversation ->
                        PanelConversationRow(
                            title = conversation.title,
                            onClick = {
                                closePanel()
                                onOpenConversation(conversation.id)
                            }
                        )
                    }
                    PanelActionRow(
                        icon = Icons.AutoMirrored.Filled.List,
                        label = stringResource(R.string.history_open_all),
                        onClick = {
                            closePanel()
                            onOpenHistory()
                        }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(20.dp)
                .pointerInput(panelWidthPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) break
                            trackPanel(change.positionChange().x)
                            change.consume()
                        }
                        settlePanel()
                    }
                }
        )
    }
}

@Composable
private fun PanelConversationRow(
    title: String,
    muted: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceColor)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = title,
            style = TextStyle(fontSize = 12.sp, color = if (muted) MutedColor else Color.White),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private val inlineMarkdown = Regex(
    pattern = "\\*\\*(.+?)\\*\\*|__(.+?)__|~~(.+?)~~|`([^`]+)`|\\*(.+?)\\*|_(.+?)_"
)

private val bulletLine = Regex("^([ \\t]*)[-*][ \\t]+(.*)$")

private val ruleLine = Regex("^[ \\t]*([-*]|[_]){3,}[ \\t]*$")

/** Renders the lightweight markdown the model replies with (bold, italic, code, strike, bullets). */
private fun String.toChatMarkdown(): AnnotatedString = buildAnnotatedString {
    lines().forEachIndexed { lineIndex, line ->
        if (lineIndex > 0) append('\n')
        if (ruleLine.matches(line)) {
            if (lineIndex > 0) append('\n')
            return@forEachIndexed
        }
        val bullet = bulletLine.find(line)
        val content = if (bullet != null) {
            append(bullet.groupValues[1])
            append("\u2022  ")
            bullet.groupValues[2]
        } else {
            line
        }
        var last = 0
        for (match in inlineMarkdown.findAll(content)) {
            append(content.substring(last, match.range.first))
            val groups = match.groupValues
            val style = when {
                groups[1].isNotEmpty() || groups[2].isNotEmpty() ->
                    SpanStyle(fontWeight = FontWeight.Bold)
                groups[3].isNotEmpty() ->
                    SpanStyle(textDecoration = TextDecoration.LineThrough)
                groups[4].isNotEmpty() ->
                    SpanStyle(fontFamily = FontFamily.Monospace, background = SurfaceColor)
                else -> SpanStyle(fontStyle = FontStyle.Italic)
            }
            withStyle(style) {
                append(groups.drop(1).first { it.isNotEmpty() })
            }
            last = match.range.last + 1
        }
        append(content.substring(last))
    }
}

@Composable
private fun PanelActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MutedColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = TextStyle(fontSize = 12.sp, color = MutedColor)
        )
    }
}

@Composable
private fun VoiceMicIcon() {
    val pulse by rememberInfiniteTransition(label = "voicePulse").animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voicePulseAlpha"
    )
    Icon(
        imageVector = Icons.Filled.Mic,
        contentDescription = stringResource(R.string.chat_voice),
        tint = Color.White,
        modifier = Modifier
            .size(20.dp)
            .alpha(pulse)
    )
}

@Composable
private fun ThinkingDots(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "thinking")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing)
        ),
        label = "thinkingProgress"
    )
    Row(
        modifier = modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        repeat(3) { index ->
            val phase = (progress + index / 3f) % 1f
            val lift = sin(phase * PI).toFloat()
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, (-4.dp.toPx() * lift).roundToInt()) }
                    .alpha(0.35f + 0.65f * lift)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(colorResource(R.color.mistral_orange))
            )
        }
    }
}

@Composable
private fun PanelButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(SurfaceColor)
            .border(1.dp, BorderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colorResource(R.color.mistral_orange),
            modifier = Modifier.size(22.dp)
        )
    }
}

private fun ChatError.displayRes(): Int = when (this) {
    ChatError.InvalidKey -> R.string.chat_error_invalid_key
    ChatError.RateLimited -> R.string.chat_error_rate_limited
    ChatError.Server -> R.string.chat_error_server
    ChatError.Network -> R.string.chat_error_network
    ChatError.Unknown -> R.string.chat_error_unknown
}
