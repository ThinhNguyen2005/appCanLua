# QUY CHUẨN KIỂM TOÁN UI/UX (MATERIAL 3 EXPRESSIVE, A11Y, MOTION) & UNIT TEST (`appCanLua`)

> **Cách sử dụng:** Mỗi khi gọi lệnh:
> *"Chạy audit theo `docs/AUDIT_UIUX_AND_TESTS.md` cho [màn hình / module]"*
> AI đóng vai trò **Principal Android Engineer** kiêm **QA Architect**, thực hiện rà soát toàn diện mã nguồn thực tế theo đúng **Phần A** và **Phần B** dưới đây, phân loại mức độ ưu tiên (`P0 - Nghiêm trọng`, `P1 - Nên sửa`, `P2 - Tối ưu`), chỉ rõ `TênFile.kt:Dòng` và đề xuất giải pháp kèm kế hoạch theo từng Phase. **Không tự ý sửa code khi chưa được duyệt.**

---

## PHẦN A: KIỂM TOÁN UI/UX, LUỒNG TƯƠNG TÁC, MATERIAL 3 EXPRESSIVE, TRỢ NĂNG & HIỆU ỨNG

### A1. Phát hiện trùng lặp chức năng & Lỗi luồng tương tác (Interaction Flow)
- [ ] **Trùng lặp hành động (Action Duplication):**
  - Kiểm tra các nút/biểu tượng bị trùng chức năng hoặc trùng icon gây nhầm lẫn trên cùng một màn hình (giữa `TopAppBar`, `ModernBottomBar`, `FAB`, `FilterBar` và các `Card` bên trong).
  - Kiểm tra cài đặt bị phân mảnh hoặc đặt sai chỗ (ví dụ: cài đặt toàn cục như Cỡ chữ, Giọng đọc TTS nằm trong bottom sheet cục bộ nhưng lại thiếu trong `SettingsScreen`).
- [ ] **Luồng điều hướng (Navigation Flow):**
  - Đếm số bước thao tác cho tác vụ chính; phát hiện bước thừa hoặc ngõ cụt (dead-end).
  - Kiểm tra cách gọi `navController.navigate(...)` khi chuyển giữa các tab chính: bắt buộc có `popUpTo(findStartDestination().id) { saveState = true }`, `launchSingleTop = true`, `restoreState = true` để không đẩy trùng tab lên backstack.
  - Kiểm tra tính nhất quán giữa 2 vai trò **Nông dân (`FARMER`)** và **Thương lái (`TRADER`)**: không để tính năng hoặc dữ liệu hiển thị bị thiếu hụt vô lý giữa 2 role.
- [ ] **Tính toàn vẹn của 4 trạng thái màn hình (Empty / Loading / Content / Error):**
  - Mọi trạng thái lỗi (`errorMessage`, `refreshError`, `rateLimitSeconds`, lỗi validation form) trong `UiState`/`ViewModel` phải được bind và hiển thị rõ ràng trên UI kèm hướng khắc phục (Retry / Nâng cấp / Sửa lỗi). Tuyệt đối không nuốt lỗi trong im lặng.
  - Thao tác phá hủy dữ liệu (xóa phiếu, xóa giá thu mua, đăng xuất, ghi đè bản sao lưu) phải có hộp thoại xác nhận hoặc `Snackbar` hỗ trợ `Hoàn tác (Undo)`.

### A2. Rà soát Component tự dựng vs. Chuẩn Google Material 3 / M3 Expressive
- [ ] **Thay thế `Dialog` nhập liệu dài / chồng lớp bằng `ModalBottomSheet` hoặc `TooltipBox`:**
  - Mọi form nhập liệu hoặc bộ lọc nhiều tùy chọn phải dùng `ModalBottomSheet` có `BottomSheetDefaults.DragHandle()` chuẩn (không set `dragHandle = null` làm mất điểm tựa kéo đóng 1 tay).
  - Không mở `Dialog` chồng lên trên `ModalBottomSheet` (modal-over-modal); ưu tiên `TooltipBox` (`RichTooltip`) hoặc inline expandable section.
- [ ] **Thay thế các control tự chế (`Box`/`Surface` + `clickable`) bằng Component Material 3 chính chủ:**
  - **Bottom Navigation:** Đánh giá `ModernBottomBar` so với `NavigationBar` / `ShortNavigationBar` / `HorizontalFloatingToolbar` chuẩn M3 (đảm bảo `selectableGroup()`, `Role.Tab`, không khóa cứng font size).
  - **Segmented Controls / Mode Toggle:** Thay các cụm `Row` + `Box.clickable` tự chế bằng `SingleChoiceSegmentedButtonRow` / `MultiChoiceSegmentedButtonRow` hoặc `ButtonGroup` (M3 Expressive).
  - **Chips:** Thay các `Box` bo góc tự chế bằng `FilterChip`, `InputChip` (có nút xóa đạt chuẩn vùng chạm), `AssistChip`, `SuggestionChip`.
  - **Dropdown & Search:** Dùng `ExposedDropdownMenuBox` (`MenuAnchorType.PrimaryEditable` / `PrimaryNotEditable`) và `SearchBar` / `DockedSearchBar`.
  - **Pull-to-Refresh & Feedback:** Dùng `PullToRefreshBox` chuẩn M3 và `SnackbarHost` / inline state đồng bộ.
- [ ] **Edge-to-Edge (`WindowInsets`) & Predictive Back Gesture:**
  - Kiểm tra `statusBarsPadding()`, `navigationBarsPadding()`, `imePadding()` — không để trùng lặp (double padding tạo khoảng trống thừa) hoặc thiếu hụt (bị floating bottom bar hay bàn phím che khuất).
  - Đảm bảo chiều cao `TopAppBar` không bị nhảy đột ngột khi chuyển qua lại giữa các tab trong cùng `Scaffold`.

### A3. Chuẩn Visual Hierarchy & UX Playbook
- [ ] **Quy tắc 1 Primary CTA:** Mỗi màn hình/sheet chỉ có duy nhất **1 Primary CTA** nổi bật nhất (màu `GreenPrimary` / `ExtendedFloatingActionButton`); các hành động phụ phải hạ cấp xuống Secondary (`FilledTonalButton` / `OutlinedButton`) hoặc Tertiary (`TextButton` / `IconButton`).
- [ ] **Hệ thống khoảng cách trên thang lưới `4.dp` (`AppDimensions`):**
  - Mọi `padding`, `Arrangement.spacedBy`, `Spacer`, `CornerRadius` phải tuân thủ bội số của `4.dp` (`4, 8, 12, 16, 20, 24, 32, 48.dp`).
  - Loại bỏ các số lẻ tẻ (`6.dp`, `10.dp`, `14.dp`, `18.dp`, `22.dp`, `30.dp`, `38.dp`, `44.dp`, `54.dp`, `60.dp`, `68.dp`).
- [ ] **Luật gần gũi (Gestalt Proximity) & Căn lề dữ liệu:**
  - Chữ/nhãn căn trái (`TextAlign.Start`); con số, khối lượng (`kg`, `tấn`) và tiền tệ (`đ`) trong bảng/danh sách đối chiếu bắt buộc căn phải (`TextAlign.End` / `Alignment.End`) để thẳng hàng đơn vị.
  - Bo góc và bộ icon đồng nhất (`Outlined` cho trạng thái nghỉ/meta, `Filled` cho trạng thái active).
- [ ] **Cấm Hardcode (i18n & Type Safety):**
  - Không hardcode chuỗi tiếng Việt trực tiếp trong `@Composable` hay `ViewModel` (kể cả `Toast`, `contentDescription`, `placeholder`, thông báo lỗi) -> bắt buộc đưa vào `res/values/strings.xml` (và `values-en/strings.xml`) qua `stringResource` / `context.getString`.
  - Không dùng string literal rời rạc cho `role` (`"FARMER"`, `"TRADER"`), `weightInputMode` (`"SMALL"`, `"LARGE"`), `trend` (`"UP"`, `"DOWN"`, `"STABLE"`), hoặc navigation routes -> bắt buộc dùng `enum class` / `sealed interface` / hằng số tập trung.

### A4. Trợ năng (Accessibility - A11y)
- [ ] **Vùng chạm tối thiểu `48.dp x 48.dp` & Vùng ngón cái (Thumb Zone):**
  - Mọi phần tử tương tác (`IconButton`, `Chip`, `SegmentedButton`, nút đóng `Close`, nút xóa bộ lọc) phải có hit target `>= 48.dp x 48.dp`.
  - Không tồn tại cử chỉ độc quyền (Gesture-only): mọi thao tác vuốt (`SwipeToDismissBox` để xóa phiếu) bắt buộc phải có nút bấm thay thế hoặc `CustomAccessibilityAction` cho người dùng TalkBack / người cao tuổi.
- [ ] **Độ tương phản màu sắc WCAG AA (>= 4.5:1 cho chữ thường, >= 3:1 cho chữ lớn/UI control):**
  - Kiểm tra kỹ cả **Light Mode**, **Dark Mode (`HIGH_CONTRAST`)** và **OLED Mode**.
  - Tuyệt đối tránh lỗi chữ tối trên nền tối ở Dark Mode (ví dụ `GreenDark #1B5E20` trên `GreenSurface #1E3A24`) hoặc chữ trắng (`#FFFFFF`) trên nền vàng sáng (`GoldAccent #F9A825`).
  - Không dùng duy nhất màu sắc để biểu thị trạng thái (tăng/giảm, lỗi, đã thanh toán) -> luôn đi kèm Icon (`↑`/`↓`, `Lock`, `Check`) + Nhãn chữ.
- [ ] **TalkBack Semantics, FontScale & Bàn phím (`KeyboardOptions` / `KeyboardActions`):**
  - Đầy đủ `contentDescription` cho icon có chức năng; gán `contentDescription = null` cho icon trang trí đi kèm text.
  - Dùng `semantics(mergeDescendants = true)` cho các thẻ thống kê/hàng cài đặt, `Modifier.selectableGroup()` cho thanh tab/nhóm radio, và `semantics { heading() }` cho tiêu đề nhóm.
  - Đối với biểu đồ `Canvas` (`VarietyPieChart`), phải cung cấp `semantics { contentDescription = ... }` đọc tóm tắt số liệu.
  - **Không khóa cứng chiều cao container (`height(...)`) hoặc `fontSize` tĩnh** làm mất tác dụng của `FontScale` (`LARGE 1.15x`, `XLARGE 1.30x`). Test giao diện ở `200% font scale`.
  - Mọi `OutlinedTextField` / `TextField` bắt buộc khai báo đủ `KeyboardType` (`Number`, `Decimal`, `Phone`, `Text`) VÀ `ImeAction` (`Next` cho các ô giữa form, `Done` cho ô cuối form, `Send` cho ô chat, `Search` cho ô tìm kiếm) kèm `KeyboardActions` thực thi hành động tương ứng.

### A5. Motion, Micro-interactions & Haptics (Trải nghiệm App Hiện đại)
- [ ] **Hiệu ứng mở app & Chuyển cảnh (Splash -> Main & Skeleton -> Content):**
  - `NavHost` gốc (`MainActivity`) và `AppNavHost` phải có hiệu ứng chuyển cảnh mượt mà.
  - Quá trình chuyển từ `Skeleton` sang nội dung thật trên mọi màn hình phải bọc trong `Crossfade` hoặc `AnimatedContent` (không dùng `if (showSkeleton)` cắt cảnh đột ngột).
  - Các con số tổng hợp tài chính/sản lượng lớn nên dùng `AnimatedNumber` hoặc `AnimatedContent` trượt số khi giá trị thay đổi.
- [ ] **Vật lý chuyển động (Motion Physics - M3 Expressive):**
  - Ưu tiên chuyển động lò xo (`spring(dampingRatio = ..., stiffness = ...)`) hoặc `MaterialTheme.motionScheme` thay vì `LinearEasing` hoặc nhảy trạng thái tức thời.
  - Các danh sách `LazyColumn` / `LazyRow` có lọc, thêm, xóa phần tử bắt buộc gắn `Modifier.animateItem()` để các thẻ trượt lấp chỗ tự nhiên.
- [ ] **Phản hồi lỗi & Xúc giác (Error Feedback, Shake Animation & Haptics):**
  - Không khóa mờ (`enabled = false`) nút Submit form một cách câm lặng khi dữ liệu phức tạp chưa hợp lệ; hoặc nếu người dùng bấm vào nút/ô đang lỗi, phải kích hoạt đồng bộ **3 lớp phản hồi**:
    1. **Hiệu ứng rung lắc ngang (Horizontal Shake Animation)** bằng `Animatable` / `spring` trên ô nhập bị lỗi.
    2. **Rung xúc giác cảnh báo lỗi** qua `HapticUtil.error(context)` (hoặc `HapticFeedbackConstants.REJECT`).
    3. **Highlight viền đỏ (`isError = true`) kèm dòng giải thích lỗi inline (`supportingText`)** ngay dưới ô nhập và tự động cuộn/focus tới ô lỗi đầu tiên.
  - Chuẩn hóa điểm chạm Haptics (không gọi trùng 2 hàm rung cùng lúc):
    - Chạm nhẹ / chuyển tab / chọn chip: `HapticUtil.tick(context)` (chỉ gọi 1 nguồn duy nhất).
    - Lưu phiếu / đăng giá / thanh toán thành công: `HapticUtil.confirm(context)` hoặc `HapticUtil.success(context)`.
    - Nhập sai validation / vuốt xóa / chạm giới hạn quota: `HapticUtil.error(context)`.

---

## PHẦN B: KIỂM TOÁN UNIT TEST & DẤU HIỆU "VIBE CODE"

### B1. Đối chiếu Quy mô Thực tế Dự án vs. Độ phủ theo Rủi ro Nghiệp vụ
- [ ] **Thống kê định lượng:**
  - Đếm tổng số `Screen`, `ViewModel`, `Repository`, `DAO`, `Domain/Util Engine` trong `app/src/main` so với số lượng file test thực chất trong `app/src/test` và `app/src/androidTest`.
- [ ] **Đánh giá độ phủ theo tầng rủi ro (Risk-Based Coverage):**
  - **Tầng P0 (Tiền bạc, Cân lúa, Đồng bộ Cloud, Bảo mật):** `RiceSettlement`, `RiceCalculator`, `CardSettlement`, `SyncableCardRepository` (atomic ledger & idempotency), `CardRepository`, `SyncManager` (`syncAll`, `pullAllForCurrentUser`, conflict resolution, tombstone), `CccdCrypto`.
  - **Tầng P1 (Luồng điều phối nghiệp vụ & State):** `AuthViewModel`, `DashboardViewModel`, `CardDetailViewModel`, `WeightInputViewModel`, `CardListViewModel`, `MarketViewModel`, `TraderBidsViewModel`, `TraderTransactionsViewModel`, `SettingsViewModel`, `SyncViewModel`.
  - **Tầng P2 (Formatters & Helpers):** `MoneyFormatter`, `DashboardFormatter`, `MapFormatters`, `PremiumQuotaLogic`, `HtmlUtils`, `SeasonUtil`.

### B2. Phát hiện Dấu hiệu "Vibe Code" trong Cây thư mục Test
- [ ] **Tách file song song vô tội vạ:** Phát hiện các cặp `*Test.kt` và `*EdgeCaseTest.kt` (hoặc tách từng hàm nhỏ thành 1 file test riêng) gây phân mảnh và trùng lặp setup.
- [ ] **Tỷ lệ dung lượng Test / Source phình to bất thường:** Chỉ ra các file test có số dòng gấp **4–10 lần** file code gốc do copy-paste hàng chục hàm `@Test` giống hệt nhau thay vì gom nhóm hoặc dùng Parameterized Test / Table-Driven Test.
- [ ] **Lệch trọng tâm (Inverted Risk Pyramid):** Test quá dày ở tầng P2 (`util`/formatter thuần chuỗi dễ viết) nhưng bỏ trống hoàn toàn hoặc chỉ viết test bề mặt cho tầng P0/P1 (`SyncManager`, `ViewModel`, `Repository`).
- [ ] **File rác & Lỗi cấu trúc thư mục:** Phát hiện `ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt`, hoặc đường dẫn package sai chữ hoa/thường (`com/GiaThinh/...` vs `com/giathinh/...`).

### B3. Soi trực tiếp Code Test để bắt 5 Lỗi Kinh điển của Newbie / Vibe Code
- [ ] **1. Test gương (Tautological / Mirror Test):**
  - Copy nguyên xi công thức hoặc hàm `private` từ code production dán vào trong file test để tự test bản sao đó (khiến test luôn xanh dù code thật trong app bị sửa sai hoặc bị xóa).
- [ ] **2. Test chi tiết cài đặt / Over-mocking (Implementation Coupling):**
  - Dùng `spyk` để mock chính hàm nội bộ của đối tượng đang test (SUT), hoặc lạm dụng `verify(exactly = 1)` / capture lambda nội bộ thay vì kiểm tra trạng thái/kết quả đầu ra (state & behavior verification).
- [ ] **3. Sửa test cho xanh thay vì sửa bug (Assertion Tampering):**
  - Tên hàm `@Test` và comment khẳng định kỳ vọng kết quả `A`, nhưng giá trị `expected` trong `assertEquals(expected, actual)` lại bị sửa thành `B` để khớp với hành vi sai của code gốc.
- [ ] **4. Cày coverage ảo (Redundant / Boilerplate Padding):**
  - Viết hàng chục test case đi qua cùng 1 nhánh `if/else`, chỉ đổi con số đầu vào, hoặc copy-paste cùng một test case xuất hiện ở cả `*Test.kt` lẫn `*EdgeCaseTest.kt`.
- [ ] **5. Flaky Test / Bẫy môi trường CI (Environment & Global State Leaks):**
  - Phụ thuộc `Locale.getDefault()`, `TimeZone.getDefault()`, `Calendar.getInstance()`, `System.currentTimeMillis()` mà không truyền `Locale` / `TimeZone` / `Clock` tường minh.
  - Gọi `Locale.setDefault(...)` hoặc dùng Reflection sửa `object` Singleton toàn cục (`PremiumState`) trong `@Before` mà không dọn dẹp/khôi phục chuẩn trong `@After`, gây ô nhiễm trạng thái khi chạy test song song trên CI.
