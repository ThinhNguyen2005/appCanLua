package com.GiaThinh.canlua.ui.screen

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.GiaThinh.canlua.R
import com.GiaThinh.canlua.auth.GoogleSignInHelper
import com.GiaThinh.canlua.auth.saveLoginCredential
import com.GiaThinh.canlua.ui.viewmodel.AuthUiState
import com.GiaThinh.canlua.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? Activity
    val googleHelper = remember(context) { GoogleSignInHelper(context) }

    LaunchedEffect(uiState.isSignedIn) {
        if (uiState.isSignedIn) onSuccess()
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { scope.launch { snackbarHostState.showSnackbar(it) } }
    }
    LaunchedEffect(uiState.info) {
        uiState.info?.let { scope.launch { snackbarHostState.showSnackbar(it) } }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            Icon(
                imageVector = Icons.Default.Eco,
                contentDescription = "Logo Cân Lúa",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Đăng nhập hoặc đăng ký",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Quản lý dữ liệu thông minh, lưu trữ an toàn và đồng bộ mọi lúc.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            AuthEmailForm(
                uiState = uiState,
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                scope = scope,
                activity = activity
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = "HOẶC",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            }

            Spacer(modifier = Modifier.height(24.dp))

            GoogleSignInButton(
                onClick = {
                    scope.launch {
                        googleHelper.signIn(
                            onSuccess = { account -> viewModel.signInWithGoogle(account) },
                            onCancel = { /* user huỷ — không show error để tránh nhiễu */ },
                            onError = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Điều khoản sử dụng • Chính sách quyền riêng tư",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .padding(vertical = 24.dp)
                    .clickable {
                        scope.launch { snackbarHostState.showSnackbar("Tính năng đang hoàn thiện.") }
                    }
            )
        }
    }
}

@Composable
private fun AuthEmailForm(
    uiState: AuthUiState,
    viewModel: AuthViewModel,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    activity: Activity?
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var isRegister by remember { mutableStateOf(false) }

    // Detect phone vs email theo input thật-time → đổi keyboard + autofill hint phù hợp.
    val isPhoneMode = remember(identifier) { viewModel.looksLikePhone(identifier) }
    val showOtpField = uiState.verificationId != null

    // Lưu thành công → prompt Google Smart Lock save password cho login lần sau.
    LaunchedEffect(uiState.isSignedIn) {
        if (uiState.isSignedIn && !isPhoneMode && password.isNotBlank() && activity != null) {
            saveLoginCredential(activity, identifier.trim(), password)
        }
    }

    // Email / Phone identifier
    OutlinedTextField(
        value = identifier,
        onValueChange = { identifier = it },
        placeholder = {
            Text(
                if (isPhoneMode) "Số điện thoại (VD: 0901234567)"
                else "Email hoặc số điện thoại",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                if (isPhoneMode) Icons.Default.Phone else Icons.Default.AlternateEmail,
                contentDescription = null
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .semantics {
                contentType = if (isPhoneMode) ContentType.PhoneNumber else ContentType.EmailAddress
            },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPhoneMode) KeyboardType.Phone else KeyboardType.Email
        ),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary
        )
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Mật khẩu — chỉ show khi nhập email; ẩn khi mode phone (sẽ dùng OTP).
    if (!isPhoneMode) {
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("Mật khẩu (≥ 6 ký tự)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .semantics {
                    // NewPassword khi register (Google đề xuất pwd mạnh) — Password khi sign-in (autofill).
                    contentType = if (isRegister) ContentType.NewPassword else ContentType.Password
                },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )
    }

    // OTP input — chỉ hiện sau khi đã gửi mã.
    if (isPhoneMode && showOtpField) {
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
            value = otp,
            onValueChange = { input -> otp = input.filter { it.isDigit() }.take(6) },
            placeholder = { Text("Nhập mã OTP (6 số)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .semantics { contentType = ContentType.SmsOtpCode },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Forgot password (chỉ ở login email) + Toggle register/login
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isRegister && !isPhoneMode) {
            Text(
                text = "Quên mật khẩu?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable {
                        viewModel.requestPasswordReset(identifier.trim())
                    }
                    .padding(vertical = 4.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }
        Text(
            text = if (isRegister) "Đã có tài khoản? Đăng nhập" else "Chưa có tài khoản? Đăng ký",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clickable { isRegister = !isRegister }
                .padding(vertical = 4.dp)
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = {
            val idTrim = identifier.trim()
            val passwordTrim = password
            // ---- Phone OTP flow ----
            if (isPhoneMode) {
                if (showOtpField) {
                    if (otp.length < 6) {
                        scope.launch { snackbarHostState.showSnackbar("Nhập đủ 6 chữ số OTP.") }
                        return@Button
                    }
                    viewModel.verifyOtp(otp)
                } else {
                    if (activity == null) {
                        scope.launch { snackbarHostState.showSnackbar("Lỗi context. Khởi động lại app.") }
                        return@Button
                    }
                    viewModel.sendOtp(activity, idTrim)
                }
                return@Button
            }

            // ---- Email/Password flow ----
            val validation = validateAuthForm(
                email = idTrim,
                password = passwordTrim,
                isRegister = isRegister
            )
            if (validation != null) {
                scope.launch { snackbarHostState.showSnackbar(validation) }
                return@Button
            }
            if (isRegister) {
                viewModel.registerEmail(
                    email = idTrim,
                    password = passwordTrim,
                    name = "",
                    phone = "",
                    region = "",
                    role = "FARMER",
                    cccd = "",
                    username = ""
                )
            } else {
                viewModel.signInEmail(idTrim, passwordTrim)
            }
        },
        enabled = !uiState.loading,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(25.dp)
    ) {
        if (uiState.loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = when {
                    isPhoneMode && showOtpField -> "Xác nhận OTP"
                    isPhoneMode -> "Gửi mã OTP"
                    isRegister -> "Đăng ký"
                    else -> "Đăng nhập"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Validate email/password form. Trả null nếu hợp lệ; ngược lại trả lỗi để hiển thị.
 */
private fun validateAuthForm(
    email: String,
    password: String,
    isRegister: Boolean
): String? {
    if (email.isBlank()) return "Vui lòng nhập email hoặc số điện thoại."
    if (!email.matches(EMAIL_REGEX)) return "Email không đúng định dạng."
    if (password.length < 6) return "Mật khẩu phải có ít nhất 6 ký tự."
    @Suppress("UNUSED_PARAMETER")
    val _r = isRegister
    return null
}

private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")

@Composable
private fun GoogleSignInButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(25.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_google),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color.Unspecified
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text("Tiếp tục với Google", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}
