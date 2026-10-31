# Kế hoạch Hoàn thiện UI/UX & Hiệu Ứng Nâng Cao — Home Screen (MainScreen & CardListScreen)

> **Mục tiêu:** Nâng cấp toàn diện màn hình Home (`MainScreen.kt`, `CardListScreen.kt`, `ModernBottomBar.kt`, `OfflineStatusBanner.kt`, `CardListSummaryCard.kt`) theo chuẩn UI/UX Playbook, tối ưu công thái học nông dân/thương lái ($\ge 48\text{dp}$ touch target), khắc phục hardcoded strings sang 5 locales, và bổ sung hiệu ứng chuyển động có mục đích (Purposeful Motion) theo chuẩn `@hieu-ung-nangcao`.
> **Thời gian:** 2026-10-12
> **Trạng thái:** Completed (BUILD SUCCESSFUL — Xác minh bởi gradlew compileDebugKotlin)

---

## 4. Kết Quả Nghiệm Thu (Verification Results)

- ✅ **Biên dịch Gradle:** `.\gradlew.bat compileDebugKotlin` đạt `BUILD SUCCESSFUL in 11s` (0 error, 0 warning).
- ✅ **Chuỗi đa ngôn ngữ (i18n):** 10 chuỗi mới được đồng bộ hoàn toàn trên cả 5 locales (`values`, `values-en`, `values-km`, `values-lo`, `values-zh-rCN`).
- ✅ **Motion Spec:**
  - Extended FAB dùng physics `spring(dampingRatio = MediumBouncy, stiffness = MediumLow)` + tactile haptic `HapticUtil.tick(context)`.
  - Offline Banner dùng physics `spring(dampingRatio = 0.85f, stiffness = MediumLow)`.
  - Modern Bottom Bar dùng physics `spring(dampingRatio = 0.88f, stiffness = MediumLow)` cho cảm giác floating capsule nhẹ nhàng.
- ✅ **Công thái học (Ergonomics):** Toàn bộ navigation / action `IconButton` và tab items đạt diện tích chạm $\ge 48\text{dp} \times 48\text{dp}$.


---

## 1. Bối cảnh & Phân tích Hiện trạng (Current State Audit)

### 1.1 Vấn đề Phát hiện (Checklist Audit)
- ❌ **Hardcoded Strings:**
  - Nút FAB trong `CardListScreen.kt`: `text = "Tạo phiếu cân"` thay vì dùng `R.string.card_list_create_card_action`.
  - Các nhãn trong `CardListSummaryCard.kt`: "Tổng quan hôm nay", "Theo dõi nhanh số phiếu...", "Sản lượng", "Đơn giá TB", "Số bao", "Năng suất ước tính" bị hardcode tiếng Việt.
  - Các `contentDescription` trong `CardItem.kt`: "Xem chi tiết", "Độ ẩm", "Trọng lượng bao bì", "Tỷ lệ tạp chất" bị hardcode.
- 🟡 **Hiệu ứng chuyển động (Motion & Micro-interactions):**
  - Thanh `ModernBottomBar` ẩn/hiện khi cuộn trong `MainScreen.kt` dùng tween cố định, thiếu cảm giác nổi tự nhiên (floating spring physics).
  - Banner `OfflineStatusBanner.kt` trượt vào/ra cần độ nảy nhẹ mềm mại (damping spring) khi trạng thái mạng thay đổi.
  - Extended FAB "Tạo phiếu cân" trong `CardListScreen.kt` cần spring physics tương tự màn hình Chi tiết (`CardDetailScreen.kt`) kèm phản hồi rung haptic.
- 🟡 **Công thái học & Touch Target:**
  - Đảm bảo các `IconButton` trên TopAppBar (`Help`, `Tune`, `Settings`, `Back`) và các tab điều hướng đạt diện tích chạm chuẩn $\ge 48\text{dp} \times 48\text{dp}$.

---

## 2. Motion Spec (`@hieu-ung-nangcao`)

### Motion Spec 1: Modern Bottom Bar Glide & Spring
```text
Intent: Ẩn thanh điều hướng dưới đáy khi cuộn xuống đọc danh sách và hiện lại êm ái khi cuộn lên
Trigger: Trạng thái cuộn listState.isScrollingUp() thay đổi
Start state: Bottom bar dịch chuyển theo trục Y
End state: Bottom bar trượt vào vị trí với độ nảy nhẹ, không gây giật khung hình
User-controlled or automatic: Tự động phản ứng theo thao tác cuộn của người dùng
Interruptible: Có
Physics/easing: spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessMediumLow)
Performance risk: Rất thấp (dùng graphicsLayer/slideInVertically, không remeasure toàn bộ cây giao diện)
Fallback: Giữ nguyên vị trí tĩnh nếu giảm chuyển động
```

### Motion Spec 2: Offline Status Banner Alert
```text
Intent: Báo hiệu trạng thái mất/có kết nối mạng một cách tự nhiên mà không gây gián đoạn đột ngột
Trigger: isOffline thay đổi từ ConnectivityManager callback
Start state: Banner ẩn hoàn toàn ngoài màn hình (-offsetY)
End state: Banner trượt xuống êm dịu, thông điệp rõ ràng, độ tương phản cao
User-controlled or automatic: Tự động
Interruptible: Có
Physics/easing: spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
Performance risk: 0 (AnimatedVisibility đơn lẻ)
Fallback: Fade in / Fade out tĩnh
```

### Motion Spec 3: Extended FAB Spring & Haptic
```text
Intent: Nút hành động chính (Primary Action) thu hút nhưng không che khuất nội dung, phản hồi xúc giác rõ nét
Trigger: Cuộn danh sách hoặc chạm vào nút "Tạo phiếu cân"
Physics/easing: spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)
Feedback: HapticUtil.tick(context) khi bấm
```

---

## 3. Các Bước Thực Thi (Action Breakdown)

### Bước 1: Đồng bộ Chuỗi Đa ngôn ngữ (5 Locales)
- Bổ sung các chuỗi mới cho Card Summary và Content Descriptions vào 5 file:
  - `values/strings.xml` (Tiếng Việt)
  - `values-en/strings.xml` (Tiếng Anh)
  - `values-km/strings.xml` (Tiếng Khmer)
  - `values-lo/strings.xml` (Tiếng Lào)
  - `values-zh-rCN/strings.xml` (Tiếng Trung Giản thể)

### Bước 2: Cập nhật `CardListSummaryCard.kt` & `CardItem.kt`
- Sử dụng `stringResource` cho toàn bộ nhãn thống kê tóm tắt.
- Sử dụng `stringResource` cho các `contentDescription` của icon.

### Bước 3: Nâng cấp `CardListScreen.kt`
- Sửa hardcoded string của FAB sang `R.string.card_list_create_card_action`.
- Nâng cấp `AnimatedVisibility` của FAB với Spring physics + Haptic feedback khi click.

### Bước 4: Tinh chỉnh `MainScreen.kt` & `OfflineStatusBanner.kt`
- Nâng cấp animation của `OfflineStatusBanner` với spring physics.
- Nâng cấp animation ẩn/hiện của `ModernBottomBar` trong `MainScreen.kt`.
- Rà soát touch target của TopAppBar navigation và action icons.

### Bước 5: Kiểm tra Biên dịch & Nghiệm thu
- Biên dịch bằng `.\gradlew.bat compileDebugKotlin`.
- Đóng dấu hoàn tất `<!-- GOAL_COMPLETE -->`.
