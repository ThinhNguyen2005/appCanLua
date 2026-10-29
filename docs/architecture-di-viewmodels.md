# Kế hoạch Thực thi: Chuẩn hóa Architecture Repository & DI (Nhóm 3)

> **Mã kế hoạch:** `architecture-di-viewmodels.md`  
> **Loại dự án:** MOBILE (Android / Kotlin / Jetpack Compose / Clean Architecture / MVVM)  
> **Áp dụng nguyên tắc:** Clean Code (KISS, DRY, Reactive UDF) + Android Modern Architecture (Flow, Hilt DI, Lifecycle-aware)

---

## 1. Tổng quan (Overview)

Tiếp nối Nhóm 1 (Toán & Bảo mật) và Nhóm 2 (Clean Code `WeightInputViewModel` & `DeltaCalculator`), **Nhóm 3** tập trung chuẩn hóa kiến trúc Repository, loại bỏ các anti-pattern quản trị State và đồng bộ hóa bất đồng bộ trong các ViewModel còn lại:

1. **Chuẩn hóa Phản ứng (Reactive UDF) trong `CardDetailViewModel`**:
   - Hiện tại `CardDetailViewModel` có tới 15+ phương thức gọi lại `loadCardById(cardId)` sau mỗi thao tác ghi, gây ra tình trạng huỷ rồi sub lại Flow của `weightEntries`, làm nháy UI (`isLoading = true`) và có rủi ro race condition.
   - Chuyển `currentCard` và `weightEntries` sang Reactive StateFlow gắn trực tiếp với Room Flow (`flatMapLatest` từ `_cardId`).
   - Gom các mutation đơn lẻ thành higher-order function `updateCardField`.
2. **Nguyên tử hóa (Atomic Synchronization) trong `MarketViewModel`**:
   - Thay thế 2 coroutine độc lập quan sát `PremiumState` và `AdConsentManager` (vốn đọc chéo `.value` của nhau) bằng một luồng `combine` duy nhất, loại bỏ hoàn toàn race condition khi khởi tạo quảng cáo Native Ad.
3. **Bổ sung Kiểm thử Tự động cho `CardDetailViewModel`**:
   - Viết trọn bộ `CardDetailViewModelTest.kt` kiểm chứng các luồng cập nhật reactive, phân bổ tiền cọc/đã trả vào bảng `transactions`, khóa phiếu, và xử lý null.

---

## 2. Tiêu chí Thành công (Success Criteria)

- [x] Cắt giảm hơn 120+ dòng code dư thừa và lặp lại trong [`CardDetailViewModel.kt`](file:///d:/APP/appCanLua/app/src/main/java/com/giathinh/canlua/ui/viewmodel/CardDetailViewModel.kt).
- [x] Không còn bất kỳ lời gọi `loadCardById` thủ công nào bên trong các phương thức mutation của `CardDetailViewModel`.
- [x] `MarketViewModel` quan sát quảng cáo thông qua luồng `combine` nguyên tử.
- [x] Viết mới unit test [`CardDetailViewModelTest.kt`](file:///d:/APP/appCanLua/app/src/test/java/com/giathinh/canlua/ui/viewmodel/CardDetailViewModelTest.kt) với 100% test pass (19/19 tests).
- [x] Toàn bộ test suite dự án (`testDebugUnitTest`) vượt qua 100% (350/350 tests) không có lỗi hồi quy.

---

## 3. Phân rã Nhiệm vụ (Task Breakdown)

### Task 1: Refactor `CardDetailViewModel.kt` sang Reactive StateFlow
- **Mục tiêu:** Chuyển `currentCard` và `weightEntries` sang `_cardId.flatMapLatest`, tạo `updateCardField`, rút gọn 15 phương thức.
- **Tập tin:** [`app/src/main/java/com/giathinh/canlua/ui/viewmodel/CardDetailViewModel.kt`](file:///d:/APP/appCanLua/app/src/main/java/com/giathinh/canlua/ui/viewmodel/CardDetailViewModel.kt)

### Task 2: Nguyên tử hóa luồng quảng cáo trong `MarketViewModel.kt`
- **Mục tiêu:** Sử dụng `combine(PremiumState.isPremium, AdConsentManager.canRequestAds)` trong `observeAdRequirements()`.
- **Tập tin:** [`app/src/main/java/com/giathinh/canlua/ui/viewmodel/MarketViewModel.kt`](file:///d:/APP/appCanLua/app/src/main/java/com/giathinh/canlua/ui/viewmodel/MarketViewModel.kt)

### Task 3: Viết Unit Test `CardDetailViewModelTest.kt`
- **Mục tiêu:** Kiểm thử cập nhật thông tin thẻ, tạo transaction cọc/thanh toán, khóa thẻ, cập nhật tọa độ.
- **Tập tin:** [`app/src/test/java/com/giathinh/canlua/ui/viewmodel/CardDetailViewModelTest.kt`](file:///d:/APP/appCanLua/app/src/test/java/com/giathinh/canlua/ui/viewmodel/CardDetailViewModelTest.kt)

### Task 4: Kiểm thử Tích hợp & Kiểm tra Toàn diện Dự án
- **Mục tiêu:** Chạy `./gradlew testDebugUnitTest` đảm bảo 100% bài test thành công.
