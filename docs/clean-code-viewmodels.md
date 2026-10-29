# Kế hoạch Thực thi: Tối ưu Clean Code & ViewModels (Nhóm 2)

> **Mã kế hoạch:** `clean-code-viewmodels.md`  
> **Loại dự án:** MOBILE (Android / Kotlin / Jetpack Compose / Architecture Components)  
> **Áp dụng nguyên tắc:** Clean Code (KISS, DRY, Single Responsibility, No Over-engineering) + MVVM Architecture  

---

## 1. Tổng quan (Overview)

Sau khi hoàn thành Nhóm 1 (Chuẩn hóa tính toán, vá bảo mật CCCD, tối ưu Room Relation), giai đoạn Nhóm 2 tập trung vào **Clean Code** và tái cấu trúc tầng **Presentation / ViewModel**:
1. **Triệt tiêu Boilerplate & Race Conditions trong `WeightInputViewModel`**:
   - Hiện tại có 6 phương thức update lặp lại nguyên vẹn 10 dòng code (fetch $\rightarrow$ copy $\rightarrow$ update $\rightarrow$ recalc $\rightarrow$ refetch).
   - Biến `currentCard` đang được cập nhật thủ công bằng one-shot query thay vì Flow phản ứng.
2. **Loại bỏ File phân mảnh (Anti-pattern: Single-function util)**:
   - File `DeltaCalculator.kt` chỉ có 6 dòng code chứa 1 hàm toán đơn vị `(current - previous) / previous * 100.0`. Cần colocate về nơi sử dụng (`DashboardViewModel` / `DashboardFormatter`).
3. **Chuẩn hóa Reactive Streams giữa Room và ViewModels**:
   - Thêm `getCardByIdFlow` trong `CardDao` và Repository để `currentCard` tự động phát ra StateFlow mới khi DB thay đổi, loại bỏ việc truy vấn lặp thủ công.
4. **Giảm thiểu phụ thuộc Singleton tĩnh trong `MarketViewModel`**:
   - Chuẩn hóa việc thu thập trạng thái `PremiumState` và `AdConsentManager`.

---

## 2. Tiêu chí Thành công (Success Criteria)

- [ ] Cắt giảm ít nhất 50+ dòng code boilerplate trùng lặp trong [`WeightInputViewModel.kt`](file:///d:/APP/appCanLua/app/src/main/java/com/giathinh/canlua/ui/viewmodel/WeightInputViewModel.kt).
- [ ] `currentCard` trong `WeightInputViewModel` phản ứng tức thì qua `StateFlow` bắt nguồn từ Room Flow, không còn rủi ro race condition do re-fetch thủ công.
- [ ] Xóa bỏ hoàn toàn [`DeltaCalculator.kt`](file:///d:/APP/appCanLua/app/src/main/java/com/giathinh/canlua/util/DeltaCalculator.kt), đưa hàm `calcDeltaPercent` vào đúng ngữ cảnh dashboard.
- [ ] 100% các bài kiểm thử unit tests hiện có (`OrganizeIntoTablesTest`, `WeightInputUiStateTest`, `DashboardFormatterTest`, v.v.) và bài kiểm thử toàn hệ thống đều pass (`BUILD SUCCESSFUL`).
- [ ] Ứng dụng biên dịch không có cảnh báo hoặc lỗi type inference mới.

---

## 3. Ngăn xếp Công nghệ & Nguyên tắc Thiết kế (Tech Stack & Rationale)

- **Kotlin Coroutines & Flow (`StateFlow`, `flatMapLatest`, `SharingStarted.WhileSubscribed(5000)`)**: Quản lý vòng đời state chuẩn Android Modern Architecture.
- **Room SQLite Flow**: Source of Truth phản ứng tự động kích hoạt phát thải khi bảng `cards` thay đổi.
- **Clean Code Principles**:
  - *DRY (Don't Repeat Yourself)*: Gom các mutation có cùng pattern vào higher-order function `updateCardField`.
  - *Colocation*: Đặt các hàm phụ trợ sát nơi nó được sử dụng, không tạo file util chỉ cho 1 dòng tính toán.
  - *Single Responsibility*: ViewModel quản lý trạng thái hiển thị, Room/Repository quản lý lưu trữ và phát thải dữ liệu.

---

## 4. Các Tập tin Trọng tâm (File Structure in Scope)

| Tập tin | Trách nhiệm hiện tại | Mục tiêu tối ưu |
|---|---|---|
| [`WeightInputViewModel.kt`](file:///d:/APP/appCanLua/app/src/main/java/com/giathinh/canlua/ui/viewmodel/WeightInputViewModel.kt) | Điều phối nhập cân, TTS, card state | Thay thế 6 hàm lặp bằng `updateCardField`, chuyển `currentCard` sang `Flow` |
| [`CardDao.kt`](file:///d:/APP/appCanLua/app/src/main/java/com/giathinh/canlua/data/dao/CardDao.kt) | Truy vấn Room DB | Bổ sung `getCardByIdFlow(id, uid): Flow<Card?>` |
| [`CardRepository.kt`](file:///d:/APP/appCanLua/app/src/main/java/com/giathinh/canlua/repository/CardRepository.kt) & [`SyncableCardRepository.kt`](file:///d:/APP/appCanLua/app/src/main/java/com/giathinh/canlua/repository/SyncableCardRepository.kt) | Cung cấp dữ liệu thẻ | Expose `getCardByIdFlow(id)` kèm tự động giải mã CCCD |
| [`DeltaCalculator.kt`](file:///d:/APP/appCanLua/app/src/main/java/com/giathinh/canlua/util/DeltaCalculator.kt) | File util 6 dòng | Di chuyển hàm về `DashboardViewModel` / `DashboardFormatter` và xóa file |
| [`DashboardViewModel.kt`](file:///d:/APP/appCanLua/app/src/main/java/com/giathinh/canlua/ui/viewmodel/DashboardViewModel.kt) | Thống kê vụ mùa | Cập nhật import `calcDeltaPercent` sau khi colocate |

---

## 5. Phân rã Nhiệm vụ (Task Breakdown)

### Task 1: Colocate và Dọn dẹp `DeltaCalculator.kt`
- **Agent**: `mobile-developer`
- **Skill**: `clean-code`
- **Độ ưu tiên**: P1
- **Phụ thuộc**: Không
- **INPUT**: [`DeltaCalculator.kt`](file:///d:/APP/appCanLua/app/src/main/java/com/giathinh/canlua/util/DeltaCalculator.kt), [`DashboardViewModel.kt`](file:///d:/APP/appCanLua/app/src/main/java/com/giathinh/canlua/ui/viewmodel/DashboardViewModel.kt)
- **OUTPUT**: 
  - Đưa `calcDeltaPercent` vào [`DashboardFormatter.kt`](file:///d:/APP/appCanLua/app/src/main/java/com/giathinh/canlua/ui/util/DashboardFormatter.kt) hoặc nội bộ `DashboardViewModel.kt`.
  - Xóa file `DeltaCalculator.kt`.
  - Cập nhật các import trong `DashboardViewModel.kt`.
- **VERIFY**: Chạy `./gradlew testDebugUnitTest --tests "*.DashboardFormatterTest"` và kiểm tra compilation.

---

### Task 2: Bổ sung Reactive Flow cho Card theo ID trong Data Layer
- **Agent**: `mobile-developer`
- **Skill**: `clean-code`
- **Độ ưu tiên**: P0
- **Phụ thuộc**: Task 1
- **INPUT**: [`CardDao.kt`](file:///d:/APP/appCanLua/app/src/main/java/com/giathinh/canlua/data/dao/CardDao.kt), [`CardRepository.kt`](file:///d:/APP/appCanLua/app/src/main/java/com/giathinh/canlua/repository/CardRepository.kt), [`SyncableCardRepository.kt`](file:///d:/APP/appCanLua/app/src/main/java/com/giathinh/canlua/repository/SyncableCardRepository.kt)
- **OUTPUT**:
  - `CardDao`: Thêm `@Query("SELECT * FROM cards WHERE id = :id AND ownerUid = :uid") fun getCardByIdFlow(id: Long, uid: String): Flow<Card?>`.
  - `CardRepository`: Thêm `fun getCardByIdFlow(id: Long): Flow<Card?>` (tự động mapping `decrypt` CCCD).
  - `SyncableCardRepository`: Thêm delegate `getCardByIdFlow(id: Long): Flow<Card?>`.
- **VERIFY**: Kiểm tra compilation và chạy `./gradlew testDebugUnitTest --tests "*.CardRepositoryTest"`.

---

### Task 3: Tối ưu Hóa Toàn diện `WeightInputViewModel` (Khử Boilerplate & Reactive State)
- **Agent**: `mobile-developer`
- **Skill**: `clean-code`
- **Độ ưu tiên**: P0
- **Phụ thuộc**: Task 2
- **INPUT**: [`WeightInputViewModel.kt`](file:///d:/APP/appCanLua/app/src/main/java/com/giathinh/canlua/ui/viewmodel/WeightInputViewModel.kt)
- **OUTPUT**:
  - Tạo `_currentCardId = MutableStateFlow<Long?>(null)`.
  - Biến `currentCard: StateFlow<Card?>` chuyển thành Flow reactive:
    ```kotlin
    val currentCard: StateFlow<Card?> = _currentCardId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(null)
            else repository.getCardByIdFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    ```
  - Viết hàm trợ năng dùng chung `updateCardField(cardId: Long, block: (Card) -> Card)`.
  - Tinh gọn 6 phương thức (`updateCardBagWeight`, `updateCardBagMethod`, `updateCardImpurityWeight`, `updateCardPricePerKg`, `updateCardMoisture`, `toggleCardLock`) thành các lời gọi 1 dòng biểu cảm.
- **VERIFY**: 
  - Chạy `./gradlew testDebugUnitTest --tests "*.OrganizeIntoTablesTest" --tests "*.WeightInputUiStateTest"`.
  - Xác nhận không còn mã lặp `_currentCard.value = repository.getCardById(cardId)` trong ViewModel.

---

### Task 4: Kiểm thử Tích hợp & Xác minh Hồi quy (Regression Verification)
- **Agent**: `mobile-developer`
- **Skill**: `clean-code`
- **Độ ưu tiên**: P0
- **Phụ thuộc**: Task 1, 2, 3
- **INPUT**: Toàn bộ codebase
- **OUTPUT**: Báo cáo kết quả kiểm thử toàn diện
- **VERIFY**: Chạy `./gradlew testDebugUnitTest` không có bất kỳ lỗi hoặc thất bại nào.

---

## 6. Giai đoạn X: Danh mục Kiểm định Cuối cùng (Phase X: Final Checklist)

- [x] `DeltaCalculator.kt` đã được xóa sạch sẽ, không còn dangling import.
- [x] `WeightInputViewModel.kt` đã triệt tiêu toàn bộ 6 khối boilerplate lặp lại, chuyển sang reactive Flow `currentCard` và `weightEntries`.
- [x] Không có memory leak hoặc unclosed Flow subscriptions trong ViewModel.
- [x] Toàn bộ unit tests của ứng dụng vượt qua 100% (331/331 tests passed).
- [x] Không xuất hiện lỗi Crash hoặc Unresolved Reference khi build Debug APK.

