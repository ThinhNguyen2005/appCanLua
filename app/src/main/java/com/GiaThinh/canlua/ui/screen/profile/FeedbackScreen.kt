package com.GiaThinh.canlua.ui.screen.profile

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.GiaThinh.canlua.R
import com.GiaThinh.canlua.data.firestore.FirestoreFeedback
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.viewmodel.FeedbackViewModel
import com.GiaThinh.canlua.util.TrackScreenRender
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    navController: NavController,
    viewModel: FeedbackViewModel = hiltViewModel()
) {
    TrackScreenRender("feedback")
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    val feedbacks by viewModel.feedbacks.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var inputText by remember { mutableStateOf("") }

    // Đánh dấu các tin nhắn đã đọc ngay khi vào màn hình
    LaunchedEffect(Unit) {
        viewModel.markAsRead()
    }

    // Auto-scroll xuống cuối danh sách khi có tin nhắn mới hoặc tải xong
    LaunchedEffect(feedbacks.size) {
        if (feedbacks.isNotEmpty()) {
            listState.animateScrollToItem(feedbacks.size * 2) // Nhân 2 vì mỗi doc có thể tách thành 2 bubble (gửi + trả lời)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.feedback_screen_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_back),
                            tint = AppColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Surface,
                    titleContentColor = AppColors.TextPrimary
                ),
                // Scaffold của MainScreen đã thêm status bar inset cho content rồi —
                // tắt windowInsets của TopAppBar này để tránh double-padding (gap với status bar).
                windowInsets = WindowInsets(0.dp)
            )
        },
        containerColor = AppColors.Surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppColors.Surface)
        ) {
            // Danh sách tin nhắn dạng bong bóng chat
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (feedbacks.isEmpty()) {
                    // Trạng thái trống
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Chat,
                            contentDescription = null,
                            tint = AppColors.TextHint.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.feedback_empty_messages),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextHint,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        feedbacks.forEach { feedback ->
                            // 1. Bong bóng chat của User (gửi đi)
                            item(key = "${feedback.id}_user") {
                                UserFeedbackBubble(feedback = feedback)
                            }
                            
                            // 2. Bong bóng chat của Admin (trả lời - nếu có)
                            if (feedback.replyText != null) {
                                item(key = "${feedback.id}_admin") {
                                    AdminReplyBubble(feedback = feedback)
                                }
                            }
                        }
                    }
                }
            }

            // Thanh nhập tin nhắn ở dưới cùng
            Surface(
                tonalElevation = 2.dp,
                color = AppColors.CardBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                stringResource(R.string.feedback_placeholder),
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.TextHint
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank() && !uiState.isSending) {
                                    val textToSend = inputText
                                    viewModel.sendFeedback(textToSend) {
                                        inputText = ""
                                        focusManager.clearFocus()
                                        Toast.makeText(context, context.getString(R.string.feedback_toast_success), Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.GreenPrimary,
                            unfocusedBorderColor = AppColors.Divider
                        )
                    )

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !uiState.isSending) {
                                val textToSend = inputText
                                viewModel.sendFeedback(textToSend) {
                                    inputText = ""
                                    focusManager.clearFocus()
                                    Toast.makeText(context, context.getString(R.string.feedback_toast_success), Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = inputText.isNotBlank() && !uiState.isSending,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (inputText.isNotBlank() && !uiState.isSending) AppColors.GreenPrimary 
                                else AppColors.Divider.copy(alpha = 0.5f)
                            )
                    ) {
                        if (uiState.isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(R.string.feedback_btn_send),
                                tint = if (inputText.isNotBlank()) Color.White else AppColors.TextHint
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserFeedbackBubble(feedback: FirestoreFeedback) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 40.dp),
        horizontalAlignment = Alignment.End
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp))
                .background(AppColors.GreenPrimary)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = feedback.message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = formatTime(feedback.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextHint,
            fontSize = 10.sp
        )
    }
}

@Composable
fun AdminReplyBubble(feedback: FirestoreFeedback) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 40.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Text(
                text = stringResource(R.string.feedback_admin_name),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.GoldDark
            )
            Spacer(modifier = Modifier.width(4.dp))
            Surface(
                color = AppColors.GoldLight,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = "ADMIN",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.GoldDark,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 8.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
        
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp))
                .background(AppColors.SurfaceContainer)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = feedback.replyText ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = formatTime(feedback.replyTimestamp ?: 0L),
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextHint,
            fontSize = 10.sp
        )
    }
}

// Cache formatter top-level — SimpleDateFormat không thread-safe, chỉ dùng từ Main thread (composable).
private val TIME_FORMAT = SimpleDateFormat("HH:mm, dd/MM/yyyy", Locale.getDefault())

private fun formatTime(timestamp: Long): String {
    if (timestamp == 0L) return ""
    return TIME_FORMAT.format(Date(timestamp))
}
