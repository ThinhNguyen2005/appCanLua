package com.GiaThinh.canlua.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.GiaThinh.canlua.repository.RoleRequestRepository
import com.GiaThinh.canlua.ui.viewmodel.RoleRequestViewModel
import com.GiaThinh.canlua.util.TrackScreenRender

/**
 * Form xin nâng cấp role TRADER. Submit ghi vào Firestore `roleRequests/{uid}`.
 * Admin duyệt thủ công ở phase này.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleRequestScreen(
    navController: NavController,
    viewModel: RoleRequestViewModel = hiltViewModel()
) {
    TrackScreenRender("role_request")
    val ui by viewModel.ui.collectAsState()
    val existing by viewModel.myRequest.collectAsState()

    var businessName by remember { mutableStateOf("") }
    var taxId by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đăng ký Thương lái") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            // Trạng thái request hiện có
            existing?.let { req -> StatusCard(req) }

            Spacer(Modifier.height(8.dp))

            Text(
                "Tại sao cần duyệt?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Vai trò Thương lái có quyền truy cập bảng giá thị trường, đăng giá lúa, " +
                    "và truy cập bản đồ nông dân đang cân lúa. Để bảo vệ cộng đồng, " +
                    "chúng tôi cần xác minh thông tin doanh nghiệp trước khi cấp quyền.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            // Form
            val canSubmit = businessName.isNotBlank() &&
                phone.isNotBlank() &&
                reason.length >= 10 &&
                !ui.submitting &&
                existing?.status != RoleRequestRepository.Status.PENDING.name &&
                existing?.status != RoleRequestRepository.Status.APPROVED.name

            OutlinedTextField(
                value = businessName,
                onValueChange = { businessName = it },
                label = { Text("Tên doanh nghiệp / cửa hàng *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                enabled = canSubmit || existing == null
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = taxId,
                onValueChange = { taxId = it },
                label = { Text("Mã số thuế (tuỳ chọn)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Số điện thoại liên hệ *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Lý do (tối thiểu 10 ký tự) *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                shape = RoundedCornerShape(12.dp),
                maxLines = 5,
                placeholder = { Text("Vd: Tôi đang thu mua lúa cho HTX X tại Cần Thơ...") }
            )

            ui.error?.let { err ->
                Spacer(Modifier.height(8.dp))
                Text(err, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    viewModel.submit(businessName, taxId, phone, reason)
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(27.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Spacer(Modifier.height(0.dp))
                Text(
                    "  Gửi yêu cầu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (ui.justSubmitted) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Đã gửi yêu cầu. Chúng tôi sẽ phản hồi trong 24-48 giờ.",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun StatusCard(req: RoleRequestRepository.RoleRequest) {
    val (icon, label, container) = when (req.status) {
        RoleRequestRepository.Status.APPROVED.name -> Triple(
            Icons.Default.CheckCircle,
            "Yêu cầu đã được duyệt — vui lòng đăng nhập lại để áp dụng vai trò mới.",
            MaterialTheme.colorScheme.tertiaryContainer
        )
        RoleRequestRepository.Status.REJECTED.name -> Triple(
            Icons.Default.Storefront,
            "Yêu cầu bị từ chối${req.reviewerNote?.let { ": $it" } ?: "."} Bạn có thể gửi lại.",
            MaterialTheme.colorScheme.errorContainer
        )
        else -> Triple(
            Icons.Default.HourglassBottom,
            "Yêu cầu đang chờ admin duyệt. Thông thường mất 24-48 giờ.",
            MaterialTheme.colorScheme.secondaryContainer
        )
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(icon, contentDescription = null)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
