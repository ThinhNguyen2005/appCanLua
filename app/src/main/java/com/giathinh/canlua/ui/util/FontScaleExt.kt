package com.giathinh.canlua.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Factor cỡ chữ user chọn trong Cài đặt (FontScale.scale: 0.9 / 1.0 / 1.1 / 1.2).
 * Provided ở [com.giathinh.canlua.ui.theme.CanLuaTheme] để các Composable con đọc
 * khi cần scale các fontSize hero không nằm trong MaterialTheme.typography
 * (vd 56.sp số nhiệt độ, 26.sp tên ở ProfileHeader).
 *
 * Với 90%+ text khác, dùng `style = MaterialTheme.typography.XXX` — Typography đã được
 * scale sẵn trong CanLuaTheme nên không cần đụng tới CompositionLocal này.
 */
val LocalFontScaleFactor = compositionLocalOf { 1f }

@Composable
@ReadOnlyComposable
fun Int.scaledSp(): TextUnit = (this * LocalFontScaleFactor.current).sp

@Composable
@ReadOnlyComposable
fun Float.scaledSp(): TextUnit = (this * LocalFontScaleFactor.current).sp
