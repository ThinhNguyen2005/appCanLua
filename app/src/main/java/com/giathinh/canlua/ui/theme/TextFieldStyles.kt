package com.giathinh.canlua.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable

/**
 * Màu cho OutlinedTextField giữ nguyên độ tương phản khi phiếu khoá.
 *
 * Mặc định Material3 nhuộm disabled state alpha ~38% — số liệu trên phiếu khoá
 * trở nên mờ tới mức người trẻ còn khó đọc. Ngoài đồng ruộng (nắng gắt, mắt
 * người cao tuổi) còn tệ hơn. Override các disabled token về cùng màu với
 * enabled (chỉ khác border/label nhẹ để báo trạng thái), giữ text + giá trị
 * sắc nét.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun lockedAwareTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    disabledTextColor = AppColors.TextPrimary,
    disabledBorderColor = AppColors.DividerStrong,
    disabledLabelColor = AppColors.TextSecondary,
    disabledLeadingIconColor = AppColors.TextSecondary,
    disabledTrailingIconColor = AppColors.TextSecondary,
    disabledPlaceholderColor = AppColors.TextHint,
    disabledSuffixColor = AppColors.TextSecondary,
    disabledPrefixColor = AppColors.TextSecondary,
    disabledSupportingTextColor = AppColors.TextHint
)
