package com.GiaThinh.canlua.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.GiaThinh.canlua.R
import com.GiaThinh.canlua.auth.GoogleSignInHelper
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
                scope = scope
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
                        googleHelper.requestIdToken(
                            onSuccess = { idToken -> viewModel.signInWithGoogle(idToken) },
                            onError = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            PhoneSignInButton(
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Tính năng OTP qua SĐT đang được phát triển.")
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
    scope: CoroutineScope
) {
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegister by remember { mutableStateOf(false) }

    // Email
    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        placeholder = { Text("Địa chỉ Email", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary
        )
    )

    // Phone — chỉ hiển thị ở mode register
    if (isRegister) {
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { input -> phone = input.filter { it.isDigit() || it == '+' } },
            placeholder = { Text("Số điện thoại (VD: 0901234567)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Password
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        placeholder = { Text("Mật khẩu (≥ 6 ký tự)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary
        )
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Forgot password (chỉ ở login) + Toggle register/login
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isRegister) {
            Text(
                text = "Quên mật khẩu?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable {
                        viewModel.requestPasswordReset(email.trim())
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
            // Validation chung
            val emailTrim = email.trim()
            val passwordTrim = password
            val phoneTrim = phone.trim()

            val validation = validateAuthForm(
                email = emailTrim,
                password = passwordTrim,
                phone = if (isRegister) phoneTrim else null,
                isRegister = isRegister
            )
            if (validation != null) {
                scope.launch { snackbarHostState.showSnackbar(validation) }
                return@Button
            }

            if (isRegister) {
                viewModel.registerEmail(
                    email = emailTrim,
                    password = passwordTrim,
                    name = "",
                    phone = phoneTrim,
                    region = "",
                    role = "FARMER",
                    cccd = "",
                    username = ""
                )
            } else {
                viewModel.signInEmail(emailTrim, passwordTrim)
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
                text = if (isRegister) "Đăng ký" else "Đăng nhập",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Trả về null nếu valid, ngược lại trả về message hiển thị.
 *
 * Validation: email regex, password ≥ 6 chars, phone 10–11 chữ số bắt đầu 0 hoặc +84xxx (12-13 chars).
 * Phone chỉ kiểm tra khi [isRegister] = true.
 */
private fun validateAuthForm(
    email: String,
    password: String,
    phone: String?,
    isRegister: Boolean
): String? {
    if (email.isBlank()) return "Vui lòng nhập email."
    if (!email.matches(EMAIL_REGEX)) return "Email không đúng định dạng."
    if (password.length < 6) return "Mật khẩu phải có ít nhất 6 ký tự."
    if (isRegister) {
        val p = phone?.trim().orEmpty()
        if (p.isBlank()) return "Vui lòng nhập số điện thoại."
        val isLocal = p.startsWith("0") && p.length in 10..11 && p.all { it.isDigit() }
        val isIntl = p.startsWith("+84") && p.length in 12..13 && p.drop(1).all { it.isDigit() }
        if (!isLocal && !isIntl) return "Số điện thoại không hợp lệ (10–11 số hoặc +84...)."
    }
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

@Composable
private fun PhoneSignInButton(onClick: () -> Unit) {
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
            imageVector = Icons.Default.Phone,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text("OTP qua số điện thoại", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}
