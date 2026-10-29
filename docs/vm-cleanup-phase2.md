# Kế Hoạch Triển Khai: Dọn Dẹp Cảnh Báo ViewModel & Tích Hợp UI (Phase 2) (`vm-cleanup-phase2.md`)

> **Mục tiêu:** Xử lý triệt để toàn bộ danh sách cảnh báo tồn đọng trong `WeightInputUiState.kt`, `TraderBidsViewModel.kt`, `SyncViewModel.kt`, `SettingsViewModel.kt`, `MarketViewModel.kt` và kết nối các chức năng nghiệp vụ vào giao diện thực tế.

---

## 📋 Danh Mục Công Việc Chi Tiết

### Giai Đoạn 1: Sửa Lỗi Tham Chiếu KDoc (`WeightInputUiState.kt`)
- [ ] **1.1.** Sửa lỗi `Cannot resolve symbol 'WeightInputScreen'`:
  - Thêm import hoặc dùng fully-qualified name `[com.giathinh.canlua.ui.screen.WeightInputScreen]` trong KDoc.

---

### Giai Đoạn 2: Dọn Dẹp Stub ViewModel (`TraderBidsViewModel.kt`)
- [ ] **2.1.** Loại bỏ thuộc tính không sử dụng `marketPrices` và constructor parameter `marketRepository`.
- [ ] **2.2.** Áp dụng `@Suppress("UNUSED_PARAMETER")` cho các tham số stub trong `submitBid` và `deleteBid` (do chức năng báo giá thương lái đã được chuyển sang Supabase / admin).

---

### Giai Đoạn 3: Tích Hợp Cloud Backup (`SyncViewModel.kt` & `SyncStatusScreen.kt`)
- [ ] **3.1.** Tích hợp `backupStatus`, `lastBackupTime` và nút bấm sao lưu `backupNow()` vào `SyncStatusScreen.kt`.
- [ ] **3.2.** Hiển thị trạng thái sao lưu đám mây (Cloud Storage) trực quan với icon và thời gian sao lưu gần nhất.

---

### Giai Đoạn 4: Hoàn Thiện Cài Đặt Ngôn Ngữ & Tự Động Đồng Bộ (`SettingsViewModel.kt` & `SettingsScreen.kt`)
- [ ] **4.1.** Bổ sung hàng cài đặt **Ngôn ngữ ứng dụng (App Language)** trong `SettingsScreen.kt`:
  - Hiển thị ngôn ngữ hiện tại (`AppLanguage`).
  - Dialog chọn ngôn ngữ (`VIETNAMESE`, `ENGLISH`, `KHMER`, `LAO`, `CHINESE`) gọi `viewModel.setLanguage(lang)`.
- [ ] **4.2.** Bổ sung toggle switch **Tự động đồng bộ khi có mạng (Auto-sync)**:
  - Quan sát `isAutoSyncEnabled` và gọi `viewModel.setAutoSyncEnabled(enabled)`.

---

### Giai Đoạn 5: Hoàn Thiện Bộ Lọc & Quảng Cáo Thị Trường (`MarketViewModel.kt` & `MarketScreen.kt`)
- [ ] **5.1.** Xóa lệnh gọi thừa `super.onCleared()` trong `MarketViewModel.kt`.
- [ ] **5.2.** Kết nối `clearFilters()` và `setVarietyFilter()` vào giao diện lọc giá lúa trong `MarketScreen.kt`.
- [ ] **5.3.** Kết nối `loadNativeAd(forceReload = true)` vào sự kiện kéo làm mới (pull-to-refresh) của `MarketScreen.kt`.

---

### Giai Đoạn 6: Kiểm Thử & Xác Nhận (Verification)
- [ ] **6.1.** Chạy biên dịch mã nguồn Android Kotlin: `.\gradlew.bat compileDebugKotlin`.
- [ ] **6.2.** Chạy bộ kiểm thử đơn vị: `.\gradlew.bat testDebugUnitTest`.
- [ ] **6.3.** Đảm bảo 0 warning và hoàn tất mục tiêu.
