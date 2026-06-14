package com.GiaThinh.canlua.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Dialog hiển thị giải thích lý do tại sao app cần một quyền cụ thể.
 * Hiển thị TRƯỚC KHI yêu cầu quyền, giúp user hiểu và quyết định có grant không.
 *
 * Sử dụng khi user lần đầu cần quyền hoặc đã từ chối và cần được giải thích lại.
 */
@Composable
fun PermissionRationaleDialog(
    title: String,
    message: String,
    confirmText: String = "Cho phép",
    dismissText: String = "Để sau",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Text(
                text = "\uD83D\uDCCD",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}
