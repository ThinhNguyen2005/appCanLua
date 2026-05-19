package com.GiaThinh.canlua.ui.screen.aichat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.GiaThinh.canlua.data.model.ChatSession
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.util.parseInlineMarkdown
import com.GiaThinh.canlua.ui.viewmodel.AiChatViewModel
import com.GiaThinh.canlua.ui.viewmodel.UiMessage
import com.GiaThinh.canlua.ui.viewmodel.VoiceState
import com.GiaThinh.canlua.util.SpeechRecognizerHelper
import kotlinx.coroutines.launch

private val PRESETS = listOf(
    "Lúa bị đạo ôn cổ bông xử lý sao?",
    "Khi nào nên bón đợt 2 cho ST25?",
    "Rầy nâu phát triển mạnh, cách phòng?",
    "Nước ruộng ngập 5 ngày có sao không?"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiChatScreen(
    viewModel: AiChatViewModel = hiltViewModel(),
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
) {
    val state by viewModel.state.collectAsState()
    val voice by viewModel.voiceState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val imeVisible = WindowInsets.isImeVisible

    // Auto-scroll xuống tin nhắn cuối khi list grow hoặc khi keyboard mở/đóng.
    // Thêm imeVisible vào key để khi user bắt đầu gõ, list tự cuộn lại đúng vị trí
    // (không bị input bar che mất tin nhắn vừa gửi).
    LaunchedEffect(state.messages.size, state.currentSessionId, imeVisible) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatSessionsDrawer(
                sessions = state.sessions,
                currentId = state.currentSessionId,
                onNewSession = {
                    viewModel.newSession()
                    scope.launch { drawerState.close() }
                },
                onSelectSession = { id ->
                    viewModel.switchSession(id)
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Surface)
                // imePadding ở root → toàn bộ content (LazyColumn + InputBar) co theo IME
                // đồng bộ với keyboard animation. Kết hợp với MainScreen ẩn bottom bar khi
                // imeVisible → không còn gap thừa giữa Input và keyboard.
                .imePadding()
        ) {

            // Preset prompts (chỉ hiển thị khi mới mở chat)
            AnimatedVisibility(visible = state.messages.size <= 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(PRESETS) { preset ->
                        PresetChip(text = preset, onClick = { viewModel.usePresetPrompt(preset) })
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.messages, key = { it.id }) { msg ->
                    MessageBubble(msg)
                }
                if (state.isStreaming) {
                    item { TypingIndicator() }
                }
            }

            // Voice error banner — auto dismiss khi state IDLE
            AnimatedVisibility(visible = voice.error != null) {
                VoiceErrorBanner(
                    message = voice.error.orEmpty(),
                    onDismiss = viewModel::clearVoiceError
                )
            }

            InputBar(
                value = state.input,
                partial = voice.partial,
                voice = voice,
                onValueChange = viewModel::onInputChange,
                onSend = viewModel::send,
                onStartVoice = viewModel::startVoice,
                onStopVoice = viewModel::stopVoice,
                onCancelVoice = viewModel::cancelVoice,
                isStreaming = state.isStreaming
            )
        }
    }
}

@Composable
private fun ChatSessionsDrawer(
    sessions: List<ChatSession>,
    currentId: String?,
    onNewSession: () -> Unit,
    onSelectSession: (String) -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = AppColors.Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .statusBarsPadding()
        ) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Lịch sử trò chuyện",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.GreenSurface)
                    .clickable(onClick = onNewSession)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = AppColors.GreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    "Phiên chat mới",
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColors.GreenPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    SessionRow(
                        session = session,
                        isCurrent = session.id == currentId,
                        onClick = { onSelectSession(session.id) }
                    )
                }
            }
            Text(
                "Lịch sử chỉ giữ đến khi bạn tắt ứng dụng.",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextHint,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun SessionRow(
    session: ChatSession,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isCurrent) AppColors.GreenSurface else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.SmartToy,
            contentDescription = null,
            tint = if (isCurrent) AppColors.GreenPrimary else AppColors.TextHint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = session.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isCurrent) AppColors.GreenPrimary else AppColors.TextPrimary,
            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}


@Composable
private fun PresetChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.GreenSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = AppColors.GreenPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MessageBubble(msg: UiMessage) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (msg.isError) AppColors.Error.copy(alpha = 0.15f) else AppColors.GreenSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.SmartToy,
                    contentDescription = null,
                    tint = if (msg.isError) AppColors.Error else AppColors.GreenPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.size(8.dp))
        }
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .background(
                    when {
                        msg.isError -> AppColors.Error.copy(alpha = 0.1f)
                        isUser -> AppColors.GreenPrimary
                        else -> AppColors.CardBg
                    }
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = if (msg.role == "assistant") parseInlineMarkdown(msg.content)
                       else androidx.compose.ui.text.AnnotatedString(msg.content),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    msg.isError -> AppColors.Error
                    isUser -> Color.White
                    else -> AppColors.TextPrimary
                }
            )
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(AppColors.GreenSurface),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = AppColors.GreenPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.size(8.dp))
        Text(
            "Đang soạn câu trả lời...",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextHint,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun VoiceErrorBanner(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.Error.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Mic,
            contentDescription = null,
            tint = AppColors.Error,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.size(8.dp))
        Text(
            message,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.Error
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Đóng",
                tint = AppColors.Error,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun InputBar(
    value: String,
    partial: String,
    voice: VoiceState,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onCancelVoice: () -> Unit,
    isStreaming: Boolean
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isListening = voice.state == SpeechRecognizerHelper.State.LISTENING ||
        voice.state == SpeechRecognizerHelper.State.PROCESSING

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onStartVoice()
    }

    fun requestVoice() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onStartVoice()
        } else {
            permLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Column outer ở AiChatScreen đã có imePadding() → InputBar tự push lên theo IME.
    // Khi keyboard tắt: paddingValues của Scaffold đã reserve bottom bar height (80dp)
    //   và bottom bar đã tự vẽ với navigationBars inset → InputBar dán sát bottom bar.
    // Không cần add độc lập navigationBarsPadding/imePadding ở đây — tránh cộng dồn.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Khi đang lắng nghe → hiển thị partial text dưới dạng placeholder mờ
        val displayValue = if (isListening && partial.isNotBlank() && value.isBlank()) partial else value
        val displayPlaceholder = when {
            isListening -> "Đang nghe..."
            else -> "Hỏi điều bạn muốn biết..."
        }

        OutlinedTextField(
            value = displayValue,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(displayPlaceholder, color = AppColors.TextHint) },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isListening) AppColors.Error else AppColors.GreenPrimary,
                unfocusedBorderColor = if (isListening) AppColors.Error else AppColors.Divider,
                focusedTextColor = AppColors.TextPrimary,
                unfocusedTextColor = AppColors.TextPrimary,
                cursorColor = AppColors.GreenPrimary
            ),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Send,
                capitalization = KeyboardCapitalization.Sentences
            ),
            maxLines = 4,
            readOnly = isListening
        )
        Spacer(Modifier.size(8.dp))

        // Smart action button: Mic | Stop | Send
        ActionButton(
            value = value,
            isStreaming = isStreaming,
            isListening = isListening,
            voiceAvailable = voice.available,
            onMic = ::requestVoice,
            onStop = onStopVoice,
            onCancel = onCancelVoice,
            onSend = onSend
        )
    }
}

@Composable
private fun ActionButton(
    value: String,
    isStreaming: Boolean,
    isListening: Boolean,
    voiceAvailable: Boolean,
    onMic: () -> Unit,
    onStop: () -> Unit,
    onCancel: () -> Unit,
    onSend: () -> Unit
) {
    val showSend = value.isNotBlank() && !isListening
    val showMic = !showSend && voiceAvailable

    // Pulse khi đang nghe
    val infiniteTransition = rememberInfiniteTransition(label = "mic-pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic-pulse-scale"
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .scale(if (isListening) pulseScale else 1f)
            .clip(CircleShape)
            .background(
                when {
                    isListening -> AppColors.Error
                    showSend && !isStreaming -> AppColors.GreenPrimary
                    else -> AppColors.SurfaceContainer
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            isListening -> {
                IconButton(onClick = onStop) {
                    Icon(
                        Icons.Filled.Stop,
                        contentDescription = "Dừng nghe",
                        tint = Color.White
                    )
                }
            }
            showSend -> {
                IconButton(
                    onClick = onSend,
                    enabled = !isStreaming
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Gửi",
                        tint = if (isStreaming) AppColors.TextHint else Color.White
                    )
                }
            }
            showMic -> {
                IconButton(onClick = onMic, enabled = !isStreaming) {
                    Icon(
                        Icons.Filled.Mic,
                        contentDescription = "Nhập bằng giọng nói",
                        tint = if (isStreaming) AppColors.TextHint else AppColors.GreenPrimary
                    )
                }
            }
            else -> {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = AppColors.TextHint
                )
            }
        }
    }
}
