package com.GiaThinh.canlua.ui.screen

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.GiaThinh.canlua.R
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
            
            // Logo
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
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Social buttons
            GoogleSignInButton(
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Tính năng Đăng nhập với Google đang được phát triển.")
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            PhoneSignInButton(
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Tính năng Đăng nhập bằng Số điện thoại đang được phát triển.")
                    }
                }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Terms and policy
            Text(
                text = "Điều khoản sử dụng • Chính sách quyền riêng tư",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.padding(vertical = 24.dp).clickable {
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
    var password by remember { mutableStateOf("") }
    var isRegister by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        placeholder = { Text("Địa chỉ Email", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
    
    Spacer(modifier = Modifier.height(18.dp))
    
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        placeholder = { Text("Mật khẩu", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isRegister) "Đã có tài khoản? Đăng nhập" else "Chưa có tài khoản? Đăng ký",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable { isRegister = !isRegister }.padding(vertical = 4.dp)
        )
    }

    Spacer(modifier = Modifier.height(24.dp))
    
    Button(
        onClick = {
            if (email.isBlank() || password.isBlank()) {
                scope.launch { snackbarHostState.showSnackbar("Vui lòng nhập email và mật khẩu.") }
                return@Button
            }
            if (isRegister) {
                viewModel.registerEmail(
                    email.trim(),
                    password,
                    name = "",
                    phone = "",
                    region = "",
                    role = "FARMER",
                    cccd = "",
                    username = ""
                )
            } else {
                viewModel.signInEmail(email.trim(), password)
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
                text = "Tiếp tục",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GoogleSignInButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(50.dp),
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
        modifier = Modifier.fillMaxWidth().height(50.dp),
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
        Text("Tiếp tục với số điện thoại", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

