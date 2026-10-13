# Kế Hoạch Triển Khai: Tính Năng Mở Rộng ViewModels & Hoàn Thiện UI (`feature-vm-extensions.md`)

> **Mục tiêu:** Dọn dẹp cảnh báo tồn đọng và kết nối toàn bộ các hàm chưa sử dụng trong `AuthViewModel`, `CardDetailViewModel`, `CardListViewModel` vào UI thực tế theo đúng chuẩn nghiệp vụ cân lúa.

---

## 📋 Danh Mục Công Việc

### Giai Đoạn 1: Dọn Dẹp Cảnh Báo & Tối Ưu Hóa (Code Cleanup)
- [ ] **1.1. `CardDetailViewModel.kt`**:
  - Bỏ safe call thừa (`oldCard?.impurityWeight` → `oldCard.impurityWeight`, `oldCard?.moisturePercent` → `oldCard.moisturePercent`).
  - Xóa lệnh gọi thừa `super.onCleared()`.
  - Dọn dẹp/giữ các hàm setter đơn lẻ cho nhất quán và an toàn (`isLocked` guard).
- [ ] **1.2. `CardListViewModel.kt`**:
  - Xóa dependency thừa `private val firestoreRepository: FirestoreRepository` khỏi constructor.
  - Kết nối `clearFilters()` vào `CardListFilterSheet.kt` và nút xoá bộ lọc trên `CardListFilterBar.kt`.

---

### Giai Đoạn 2: Triển Khai Tính Năng "Sửa Bao Cân" (Edit Weight Entry)
- [ ] **2.1. Cập nhật `BagEntriesCard.kt` (`BagEntryActionSheet`)**:
  - Thêm callback `onEdit: (WeightEntry) -> Unit`.
  - Hiển thị nút "Sửa số ký" bên cạnh nút "Xoá bao".
- [ ] **2.2. Dialog sửa số ký trong `CardDetailScreen.kt`**:
  - Thêm state `editingEntry: WeightEntry?`.
  - Dialog nhập số ký mới (kiểm tra `weight > 0` và `<= 200.0`, hỗ trợ 2 chữ số thập phân).
  - Xác nhận gọi `viewModel.updateWeightEntry(entry.copy(weight = newWeight))`.
  - Toast thông báo thành công và rung haptic feedback.

---

### Giai Đoạn 3: Triển Khai Tính Năng "Thanh Toán Thêm" (Add Payment)
- [ ] **3.1. Cập nhật `FinancialSummaryCard.kt`**:
  - Bổ sung nút bấm "+ Trả thêm" (Add Payment Action) hiển thị khi phiếu chưa khóa (`isLocked == false`) và chưa thanh toán đủ (`isPaid == false` hoặc `remainingAmount > 0`).
- [ ] **3.2. Dialog thanh toán trong `CardDetailScreen.kt`**:
  - Dialog nhập số tiền thanh toán đợt này (hỗ trợ nhập số có phân tách hàng nghìn).
  - Tuỳ chọn nhập phương thức / ghi chú (Tiền mặt, Chuyển khoản...).
  - Gọi `viewModel.addPayment(cardId, amount, note)`.
  - Tự động cập nhật `paidAmount`, `remainingAmount`, `overpaidAmount` và ghi nhận lịch sử vào bảng `transactions`.

---

### Giai Đoạn 4: Triển Khai Nút "Dùng Thử Ẩn Danh" (Sign In Anonymously)
- [ ] **4.1. Cập nhật `AuthScreen.kt`**:
  - Thêm nút "Dùng thử ngay (Không cần đăng nhập)" / "Tiếp tục với tư cách Khách".
  - Bấm nút gọi `viewModel.signInAnonymously()`.
  - Hiển thị loading indicator khi đang kết nối Firebase Auth Anonymous.

---

### Giai Đoạn 5: Kiểm Thử Toàn Diện & Đảm Bảo Chất Lượng
- [ ] **5.1. Biên dịch & Unit Tests**:
  - `.\gradlew.bat compileDebugKotlin`
  - `.\gradlew.bat testDebugUnitTest`
- [ ] **5.2. Xác nhận 0 warning & không có regression**.
