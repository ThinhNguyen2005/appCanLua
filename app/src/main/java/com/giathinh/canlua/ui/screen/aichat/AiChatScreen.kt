package com.giathinh.canlua.ui.screen.aichat

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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
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
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.util.AiMarkdownText
import com.giathinh.canlua.ui.viewmodel.AiChatViewModel
import com.giathinh.canlua.ui.viewmodel.UiMessage
import com.giathinh.canlua.ui.viewmodel.VoiceState
import com.giathinh.canlua.util.TrackScreenRender
import com.giathinh.canlua.util.SpeechRecognizerHelper

private val PRESETS = listOf(
    "Lúa bị đạo ôn cổ bông xử lý sao?",
    "Khi nào nên bón đợt 2 cho ST25?",
    "Rầy nâu phát triển mạnh, cách phòng?",
    "Nước ruộng ngập 5 ngày có sao không?"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiChatScreen(
    viewModel: AiChatViewModel = hiltViewModel()
) {
    TrackScreenRender("ai_chat")
    val state by viewModel.state.collectAsStateWithLifecycle()
    val voice by viewModel.voiceState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val imeVisible = WindowInsets.isImeVisible
    val isGuestMode by viewModel.isGuestMode.collectAsStateWithLifecycle(initialValue = false)

    if (isGuestMode) {
        AiChatLockedScreen(onLoginClick = viewModel::disableGuestMode)
    } else {
        // Auto-scroll xuống tin nhắn cuối khi list grow hoặc khi keyboard mở/đóng.
        // Thêm imeVisible vào key để khi user bắt đầu gõ, list tự cuộn lại đúng vị trí
        // (không bị input bar che mất tin nhắn vừa gửi).
        LaunchedEffect(state.messages.size, imeVisible) {
            if (state.messages.isNotEmpty()) {
                listState.animateScrollToItem(state.messages.size - 1)
            }
        }

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
                items(PRESETS, key = { it }) { preset ->
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

        // Khi IME đóng, BottomBar overlay (capsule + nav inset) đang chiếm đáy →
        // InputBar phải nâng 88dp để không bị che. IME mở → BottomBar ẩn ở
        // MainScreen, imePadding của Column đã đẩy InputBar dán sát bàn phím.
        val imeOpen = WindowInsets.isImeVisible
        val bottomPadding = if (imeOpen) {
            0.dp
        } else {
            80.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        }
        Box(
            modifier = Modifier.padding(bottom = bottomPadding)
        ) {
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
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
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
            val textColor = when {
                msg.isError -> AppColors.Error
                isUser -> Color.White
                else -> AppColors.TextPrimary
            }
            if (msg.role == "assistant" && !msg.isError) {
                AiMarkdownText(
                    markdown = msg.content,
                    textColor = textColor,
                    linkColor = AppColors.GreenPrimary,
                    textSizeSp = 14f
                )
            } else {
                Text(
                    text = msg.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp))
                .background(AppColors.CardBg)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                color = AppColors.GreenPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(14.dp)
            )
            Text(
                "Đang soạn câu trả lời...",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextHint,
                fontWeight = FontWeight.Medium
            )
        }
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

@Composable
private fun AiChatLockedScreen(onLoginClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Surface)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(AppColors.OrangeSurface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = AppColors.Orange,
                    modifier = Modifier.size(40.dp)
                )
            }

            // Text
            Text(
                text = "Trợ lý AI Cân Lúa 🤖",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )

            Text(
                text = "Tính năng Hỏi đáp AI chỉ dành cho thành viên đã đăng nhập. Hãy đăng ký ngay để mở khóa các đặc quyền hấp dẫn!",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Comparison Table
            ComparisonTable()

            // Early Adopter Promo Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.GreenSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = AppColors.GreenPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Đăng nhập sớm trước ngày 01/07/2026 để nhận ngay gói Premium Early Adopter miễn phí trọn đời!",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.GreenPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Login button
            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.GreenPrimary)
            ) {
                Text(
                    text = "Đăng Nhập / Đăng Ký Ngay",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ComparisonTable() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AppColors.Divider.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.SurfaceContainer, RoundedCornerShape(8.dp))
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Loại", modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                Text(text = "Hỏi đáp AI", modifier = Modifier.weight(1.3f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                Text(text = "Quảng cáo", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                Text(text = "Sao lưu", modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                Text(text = "Ưu đãi sớm", modifier = Modifier.weight(1.3f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Khách Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Khách", modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = AppColors.TextSecondary)
                Text(text = "Khóa 🔒", modifier = Modifier.weight(1.3f), style = MaterialTheme.typography.bodySmall, color = AppColors.Error)
                Text(text = "Có", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                Text(text = "Không", modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.bodySmall, color = AppColors.TextHint)
                Text(text = "Không", modifier = Modifier.weight(1.3f), style = MaterialTheme.typography.bodySmall, color = AppColors.TextHint)
            }

            HorizontalDivider(color = AppColors.Divider.copy(alpha = 0.3f), thickness = 0.5.dp)

            // Thường Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Người dùng\nthường", modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = AppColors.TextSecondary)
                Text(text = "3 câu/ngày", modifier = Modifier.weight(1.3f), style = MaterialTheme.typography.bodySmall, color = AppColors.Blue)
                Text(text = "Có", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                Text(text = "Không", modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.bodySmall, color = AppColors.TextHint)
                Text(text = "Tặng Pre 🎁", modifier = Modifier.weight(1.3f), style = MaterialTheme.typography.bodySmall, color = AppColors.GreenPrimary, fontWeight = FontWeight.Medium)
            }

            HorizontalDivider(color = AppColors.Divider.copy(alpha = 0.3f), thickness = 0.5.dp)

            // Premium Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Premium", modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = AppColors.GreenPrimary)
                Text(text = "100 câu/ngày", modifier = Modifier.weight(1.3f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = AppColors.GreenPrimary)
                Text(text = "Không QC ✨", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, color = AppColors.GreenPrimary, fontWeight = FontWeight.Medium)
                Text(text = "Xuất JSON", modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                Text(text = "Kích hoạt", modifier = Modifier.weight(1.3f), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
            }
        }
    }
}
