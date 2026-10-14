package com.giathinh.canlua.ui.screen

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giathinh.canlua.R
import com.giathinh.canlua.auth.GoogleSignInHelper
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.theme.AppDimensions
import com.giathinh.canlua.ui.viewmodel.AuthUiState
import com.giathinh.canlua.ui.viewmodel.AuthViewModel
import com.giathinh.canlua.util.HapticUtil
import com.giathinh.canlua.util.TrackScreenRender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val SCREEN_NAME_AUTH = "auth"
private const val DEFAULT_ROLE_FARMER = "FARMER"
private const val OTP_LENGTH = 6
private const val MIN_PASSWORD_LENGTH = 6

private val EMAIL_REGEX = Regex("""^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$""")

@Composable
fun AuthScreen(
    onSuccess: () -> Unit,
    onSkipLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    TrackScreenRender(SCREEN_NAME_AUTH)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? Activity
    val googleHelper = remember(context) { GoogleSignInHelper(context) }
    val featureInProgressMsg = stringResource(R.string.auth_feature_in_progress)

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
        containerColor = AppColors.Surface,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Surface)
        ) {
            ModernRiceFieldBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = AppDimensions.SpacingLg,
                        vertical = AppDimensions.SpacingXl
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Column(
                    modifier = Modifier.widthIn(max = AppDimensions.FormMaxWidth),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(AppDimensions.CornerRadiusLg),
                        color = AppColors.CardBg,
                        tonalElevation = 2.dp,
                        shadowElevation = 4.dp,
                        border = BorderStroke(1.dp, AppColors.GreenPrimary.copy(alpha = 0.16f))
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            AppColors.GreenSurface,
                                            AppColors.CardBg
                                        )
                                    )
                                )
                                .padding(AppDimensions.SpacingMd),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Eco,
                                contentDescription = stringResource(R.string.auth_logo_content),
                                modifier = Modifier.size(AppDimensions.IconXl),
                                tint = AppColors.GreenPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(AppDimensions.SpacingMd))

                    Text(
                        text = stringResource(R.string.auth_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(AppDimensions.SpacingXs))

                    Text(
                        text = stringResource(R.string.auth_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = AppDimensions.SpacingSm)
                    )

                    Spacer(modifier = Modifier.height(AppDimensions.SpacingXl))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppDimensions.CornerRadiusXl),
                        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
                        border = BorderStroke(1.dp, AppColors.Divider.copy(alpha = 0.6f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(AppDimensions.SpacingLg),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AuthEmailForm(
                                uiState = uiState,
                                viewModel = viewModel,
                                snackbarHostState = snackbarHostState,
                                scope = scope,
                                activity = activity
                            )

                            Spacer(modifier = Modifier.height(AppDimensions.SpacingLg))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = AppColors.Divider.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = stringResource(R.string.auth_or),
                                    modifier = Modifier.padding(horizontal = AppDimensions.SpacingMd),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AppColors.TextSecondary
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = AppColors.Divider.copy(alpha = 0.6f)
                                )
                            }

                            Spacer(modifier = Modifier.height(AppDimensions.SpacingMd))

                            GoogleSignInButton(
                                onClick = {
                                    HapticUtil.tick(context)
                                    scope.launch {
                                        googleHelper.signIn(
                                            onSuccess = { account -> viewModel.signInWithGoogle(account) },
                                            onCancel = { /* User huỷ */ },
                                            onError = { msg ->
                                                scope.launch { snackbarHostState.showSnackbar(msg) }
                                            }
                                        )
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(AppDimensions.SpacingXs))

                            TextButton(
                                onClick = {
                                    HapticUtil.tick(context)
                                    viewModel.signInAnonymously()
                                    onSkipLogin()
                                },
                                enabled = !uiState.loading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = AppDimensions.TouchTargetMin),
                                shape = RoundedCornerShape(AppDimensions.CornerRadiusMd),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = AppColors.TextSecondary
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.auth_skip_login),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.GreenPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(AppDimensions.SpacingMd))

                    Box(
                        modifier = Modifier
                            .defaultMinSize(minHeight = AppDimensions.TouchTargetMin)
                            .clip(RoundedCornerShape(AppDimensions.CornerRadiusSm))
                            .clickable {
                                HapticUtil.tick(context)
                                scope.launch {
                                    snackbarHostState.showSnackbar(featureInProgressMsg)
                                }
                            }
                            .padding(
                                horizontal = AppDimensions.SpacingSm,
                                vertical = AppDimensions.SpacingXs
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.auth_terms_privacy),
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextSecondary,
                            textDecoration = TextDecoration.Underline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernRiceFieldBackground() {
    val primaryGlow = AppColors.GreenPrimary.copy(alpha = 0.12f)
    val secondaryGlow = AppColors.GoldAccent.copy(alpha = 0.08f)
    val surfaceTint = AppColors.GreenSurface.copy(alpha = 0.65f)
    val lineStrokeColor = AppColors.GreenPrimary.copy(alpha = 0.05f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(surfaceTint, primaryGlow, Color.Transparent),
                center = Offset(w * 0.15f, h * 0.1f),
                radius = w * 0.9f
            ),
            radius = w * 0.9f,
            center = Offset(w * 0.15f, h * 0.1f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondaryGlow, primaryGlow.copy(alpha = 0.05f), Color.Transparent),
                center = Offset(w * 0.85f, h * 0.88f),
                radius = w * 0.85f
            ),
            radius = w * 0.85f,
            center = Offset(w * 0.85f, h * 0.88f)
        )

        for (i in 0..3) {
            val offsetFactor = i * AppDimensions.TouchTargetMin.toPx()
            val wavePath = Path().apply {
                moveTo(0f, h * 0.22f + offsetFactor)
                cubicTo(
                    w * 0.35f, h * 0.14f + offsetFactor,
                    w * 0.65f, h * 0.30f + offsetFactor,
                    w, h * 0.18f + offsetFactor
                )
            }
            drawPath(
                path = wavePath,
                color = lineStrokeColor,
                style = Stroke(width = 1.dp.toPx())
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
    var identifier by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var otp by rememberSaveable { mutableStateOf("") }
    var isRegister by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Đọc chuỗi qua stringResource ở tầng Composable thay vì gọi context.getString()
    val errorOtpLengthMsg = stringResource(R.string.auth_error_otp_length)
    val errorContextMsg = stringResource(R.string.auth_error_context)
    val errorIdentifierRequiredMsg = stringResource(R.string.auth_error_identifier_required)
    val errorEmailInvalidMsg = stringResource(R.string.auth_error_email_invalid)
    val errorPasswordLengthMsg = stringResource(R.string.auth_error_password_length)

    val isPhoneMode = remember(identifier) { viewModel.looksLikePhone(identifier) }
    val showOtpField = uiState.verificationId != null

    val submitAction: () -> Unit = {
        focusManager.clearFocus()
        HapticUtil.tick(context)
        val idTrim = identifier.trim()
        val passwordTrim = password

        if (isPhoneMode) {
            if (showOtpField) {
                if (otp.length < OTP_LENGTH) {
                    scope.launch { snackbarHostState.showSnackbar(errorOtpLengthMsg) }
                } else {
                    viewModel.verifyOtp(otp)
                }
            } else {
                if (activity == null) {
                    scope.launch { snackbarHostState.showSnackbar(errorContextMsg) }
                } else {
                    viewModel.sendOtp(activity, idTrim)
                }
            }
        } else {
            val validationErrorMsg = validateAuthForm(
                email = idTrim,
                password = passwordTrim,
                errorIdentifierRequired = errorIdentifierRequiredMsg,
                errorEmailInvalid = errorEmailInvalidMsg,
                errorPasswordLength = errorPasswordLengthMsg
            )
            if (validationErrorMsg != null) {
                scope.launch { snackbarHostState.showSnackbar(validationErrorMsg) }
            } else if (isRegister) {
                viewModel.registerEmail(
                    email = idTrim,
                    password = passwordTrim,
                    name = "",
                    phone = "",
                    region = "",
                    role = DEFAULT_ROLE_FARMER,
                    cccd = "",
                    username = ""
                )
            } else {
                viewModel.signInEmail(idTrim, passwordTrim)
            }
        }
    }

    OutlinedTextField(
        value = identifier,
        onValueChange = { identifier = it },
        placeholder = {
            Text(
                text = if (isPhoneMode) stringResource(R.string.auth_phone_placeholder)
                else stringResource(R.string.auth_identifier_placeholder),
                color = AppColors.TextSecondary
            )
        },
        leadingIcon = {
            Icon(
                imageVector = if (isPhoneMode) Icons.Outlined.Phone else Icons.Outlined.AlternateEmail,
                contentDescription = null
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AppDimensions.InputFieldMinHeight)
            .semantics {
                contentType = if (isPhoneMode) ContentType.PhoneNumber else ContentType.EmailAddress
            },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPhoneMode) KeyboardType.Phone else KeyboardType.Email,
            imeAction = if (isPhoneMode && !showOtpField) ImeAction.Done else ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) },
            onDone = { submitAction() }
        ),
        shape = RoundedCornerShape(AppDimensions.CornerRadiusMd),
        colors = authTextFieldColors()
    )

    AnimatedVisibility(
        visible = !isPhoneMode,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column {
            Spacer(modifier = Modifier.height(AppDimensions.SpacingSm))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = {
                    Text(
                        text = stringResource(R.string.auth_password_placeholder),
                        color = AppColors.TextSecondary
                    )
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Lock, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            HapticUtil.tick(context)
                            passwordVisible = !passwordVisible
                        }
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Outlined.VisibilityOff
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = null,
                            tint = AppColors.TextSecondary
                        )
                    }
                },
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AppDimensions.InputFieldMinHeight)
                    .semantics {
                        contentType = if (isRegister) ContentType.NewPassword else ContentType.Password
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { submitAction() }
                ),
                shape = RoundedCornerShape(AppDimensions.CornerRadiusMd),
                colors = authTextFieldColors()
            )
        }
    }

    AnimatedVisibility(
        visible = isPhoneMode && showOtpField,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(AppDimensions.SpacingMd))
            Text(
                text = stringResource(R.string.auth_otp_placeholder),
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.TextSecondary,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = AppDimensions.SpacingXs)
            )
            OtpSixDigitField(
                otp = otp,
                onOtpChange = { newOtp ->
                    otp = newOtp
                    if (newOtp.length == OTP_LENGTH) {
                        focusManager.clearFocus()
                        viewModel.verifyOtp(newOtp)
                    }
                }
            )
        }
    }

    Spacer(modifier = Modifier.height(AppDimensions.SpacingXs))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isRegister && !isPhoneMode) {
            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = AppDimensions.TouchTargetMin)
                    .clip(RoundedCornerShape(AppDimensions.CornerRadiusSm))
                    .clickable {
                        HapticUtil.tick(context)
                        viewModel.requestPasswordReset(identifier.trim())
                    }
                    .padding(horizontal = AppDimensions.SpacingXxs),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = stringResource(R.string.auth_forgot_password),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Spacer(modifier = Modifier.width(AppDimensions.SpacingXxs))
        }

        Box(
            modifier = Modifier
                .defaultMinSize(minHeight = AppDimensions.TouchTargetMin)
                .clip(RoundedCornerShape(AppDimensions.CornerRadiusSm))
                .clickable {
                    HapticUtil.tick(context)
                    isRegister = !isRegister
                }
                .padding(horizontal = AppDimensions.SpacingXxs),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = if (isRegister) {
                    stringResource(R.string.auth_toggle_login)
                } else {
                    stringResource(R.string.auth_toggle_register)
                },
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.GreenPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    Spacer(modifier = Modifier.height(AppDimensions.SpacingMd))

    Button(
        onClick = submitAction,
        enabled = !uiState.loading,
        modifier = Modifier
            .fillMaxWidth()
            .height(AppDimensions.ButtonHeight),
        shape = RoundedCornerShape(AppDimensions.CornerRadiusMd),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.GreenPrimary,
            contentColor = Color.White,
            disabledContainerColor = AppColors.GreenPrimary.copy(alpha = 0.45f),
            disabledContentColor = Color.White.copy(alpha = 0.75f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 0.dp
        )
    ) {
        if (uiState.loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(AppDimensions.IconLg),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = when {
                    isPhoneMode && showOtpField -> stringResource(R.string.auth_confirm_otp)
                    isPhoneMode -> stringResource(R.string.auth_send_otp)
                    isRegister -> stringResource(R.string.auth_register)
                    else -> stringResource(R.string.auth_login)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun OtpSixDigitField(
    otp: String,
    onOtpChange: (String) -> Unit
) {
    BasicTextField(
        value = otp,
        onValueChange = { input ->
            val filtered = input.filter { it.isDigit() }.take(OTP_LENGTH)
            onOtpChange(filtered)
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done
        ),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentType = ContentType.SmsOtpCode },
        decorationBox = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingXs)
            ) {
                repeat(OTP_LENGTH) { index ->
                    val char = otp.getOrNull(index)?.toString().orEmpty()
                    val isFocused = otp.length == index || (otp.length == OTP_LENGTH && index == OTP_LENGTH - 1)
                    val borderColor = when {
                        isFocused -> AppColors.GreenPrimary
                        char.isNotEmpty() -> AppColors.GreenPrimary.copy(alpha = 0.5f)
                        else -> AppColors.Divider
                    }
                    val borderWidth = if (isFocused) 2.dp else 1.dp

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(AppDimensions.ButtonHeight)
                            .clip(RoundedCornerShape(AppDimensions.CornerRadiusMd))
                            .background(AppColors.SurfaceContainer)
                            .border(
                                width = borderWidth,
                                color = borderColor,
                                shape = RoundedCornerShape(AppDimensions.CornerRadiusMd)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    )
}

private fun validateAuthForm(
    email: String,
    password: String,
    errorIdentifierRequired: String,
    errorEmailInvalid: String,
    errorPasswordLength: String
): String? {
    if (email.isBlank()) return errorIdentifierRequired
    if (!email.matches(EMAIL_REGEX)) return errorEmailInvalid
    if (password.length < MIN_PASSWORD_LENGTH) return errorPasswordLength
    return null
}

@Composable
private fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AppColors.GreenPrimary,
    unfocusedBorderColor = AppColors.Divider,
    focusedLeadingIconColor = AppColors.GreenPrimary,
    unfocusedLeadingIconColor = AppColors.TextSecondary,
    focusedTextColor = AppColors.TextPrimary,
    unfocusedTextColor = AppColors.TextPrimary,
    cursorColor = AppColors.GreenPrimary,
    focusedContainerColor = AppColors.SurfaceContainer,
    unfocusedContainerColor = AppColors.SurfaceContainer
)

@Composable
private fun GoogleSignInButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(AppDimensions.ButtonHeight),
        shape = RoundedCornerShape(AppDimensions.CornerRadiusMd),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = AppColors.SurfaceContainer,
            contentColor = AppColors.TextPrimary
        ),
        border = BorderStroke(1.dp, AppColors.Divider)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_google),
            contentDescription = null,
            modifier = Modifier.size(AppDimensions.IconMd),
            tint = Color.Unspecified
        )
        Spacer(modifier = Modifier.width(AppDimensions.SpacingSm))
        Text(
            text = stringResource(R.string.auth_continue_google),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}