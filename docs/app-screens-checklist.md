# Danh Sách Toàn Bộ Màn Hình & Master Checklist UI/UX (App Cân Lúa)

> **Mục tiêu:** Thống kê toàn bộ các màn hình trong ứng dụng `appCanLua`, phân loại trạng thái hiện tại (Đã hoàn thiện / Cần hoàn thiện), và thiết lập bảng kiểm (Checklist) chuẩn hóa theo tiêu chí: i18n 5 ngôn ngữ, công thái học touch target $\ge 48\text{dp}$, chuẩn 4 trạng thái giao diện (4 UI States), và hiệu ứng chuyển động có mục đích (`@hieu-ung-nangcao`).
> **Thời gian:** 2026-10-13
> **Quy chuẩn áp dụng:** `@ui-ux-playbook`, `@hieu-ung-nangcao`, `@mobile-design`, `@android-pro`, `DESIGN.md`.

---

## 1. Tổng Quan Tiến Độ Hệ Thống Màn Hình (Screen Inventory & Status: 19/19 HOÀN TẤT 100%)

| STT | Màn hình / Component | Đường dẫn tệp | Vai trò / Nhóm | Trạng thái | Ghi chú kiểm toán |
| :---: | :--- | :--- | :--- | :---: | :--- |
| **01** | **Chi tiết Phiếu Cân** | `ui/screen/CardDetailScreen.kt` | Core Flow | ✅ **Completed** | i18n 5 ngôn ngữ, nút chia sẻ/in PDF $\ge 48\text{dp}$, haptics, Purple ban |
| **02** | **Trang Chính (Cân Lúa / Home)** | `ui/screen/CardListScreen.kt` | Bottom Nav 1 | ✅ **Completed** | Filter chip $\ge 48\text{dp}$, empty state, haptics, i18n 5 thứ tiếng |
| **03** | **Thị Trường: Tin Tức** | `ui/component/market/NewsSection.kt` | Bottom Nav 2 (Tab 0) | ✅ **Completed** | Pull-to-refresh, topic chip $\ge 48\text{dp}$, haptics, thời gian tương đối |
| **04** | **Thị Trường: Bảng Giá** | `ui/screen/market/MarketScreen.kt` + `RicePriceCard.kt` | Bottom Nav 2 (Tab 1) | ✅ **Completed** | Bid Sheet công thái học, Price Trend Chart, haptics, i18n đầy đủ |
| **05** | **Nhập Cân Lúa (Bàn phím số)** | `ui/screen/WeightInputScreen.kt` | Core Flow | ✅ **Completed** | Bàn phím số lớn $\ge 56\text{dp}$, trừ bao bì tự động, haptics, i18n 5 thứ tiếng |
| **06** | **Hỏi Đáp AI (Trợ lý Gemini)** | `ui/screen/aichat/AiChatScreen.kt` | Bottom Nav 3 | ✅ **Completed** | Input Bar $\ge 48\text{dp}$, preset chip, haptics, i18n đầy đủ bảng so sánh gói |
| **07** | **Hồ Sơ Nông Dân / Cá Nhân** | `ui/screen/FarmerProfileScreen.kt` | Bottom Nav 4 (Farmer) | ✅ **Completed** | KPI Card mùa vụ, Empty Season State, Purple Ban xóa bỏ màu tím |
| **08** | **Hồ Sơ Thương Lái** | `ui/screen/trader/TraderProfileScreen.kt` | Bottom Nav 4 (Trader) | ✅ **Completed** | Sổ thu mua mùa vụ, tỷ lệ giống lúa, touch target $\ge 48\text{dp}$ |
| **09** | **Bản Đồ Lúa / Heatmap** | `ui/screen/map/RiceMapScreen.kt` | Bottom Nav (Trader) | ✅ **Completed** | MapFilters $\ge 48\text{dp}$, CardSummaryBottomSheet $\ge 54\text{dp}$, Empty states |
| **10** | **Nâng Cấp Gói Premium** | `ui/screen/profile/PremiumScreen.kt` | Monetization | ✅ **Completed** | Nút mua 56dp, 3 gói thời hạn, haptics, dialog thành công i18n |
| **11** | **Cài Đặt Ứng Dụng** | `ui/screen/SettingsScreen.kt` | Settings | ✅ **Completed** | Chuyển ngôn ngữ 5 thứ tiếng, backup/restore JSON, logout confirmation |
| **12** | **Sổ Giao Dịch Thương Lái** | `ui/screen/trader/TraderTransactionsScreen.kt` | Trader Feature | ✅ **Completed** | Ledger card, filter chip tuần/tháng/còn nợ, haptics, i18n 5 thứ tiếng |
| **13** | **Lịch Sử Giá Thương Lái** | `ui/screen/TraderHistoryScreen.kt` | Trader Feature | ✅ **Completed** | BottomSheet liên hệ Call/Zalo $\ge 52\text{dp}$, empty state, haptics |
| **14** | **Thùng Rác (Phiếu đã xóa)** | `ui/screen/DeletedCardsScreen.kt` | Data Management | ✅ **Completed** | Nút khôi phục & xóa vĩnh viễn $\ge 48\text{dp}$, dialog cảnh báo, haptics |
| **15** | **Gửi Phản Hồi / Góp Ý** | `ui/screen/profile/FeedbackScreen.kt` | Support | ✅ **Completed** | Input phản hồi, danh sách góp ý, nút gửi $\ge 52\text{dp}$, haptics, i18n |
| **16** | **Trạng Thái Đồng Bộ Đám Mây** | `ui/screen/SyncStatusScreen.kt` | Sync / Cloud | ✅ **Completed** | Nút đồng bộ 52dp, thẻ trạng thái realtime, namespace `sync_screen_*` |
| **17** | **Xác Thực / Đăng Nhập** | `ui/screen/AuthScreen.kt` | Onboarding | ✅ **Completed** | Google Sign-In $\ge 54\text{dp}$, Skip login $\ge 48\text{dp}$, OTP field, haptics |
| **18** | **Thiết Lập Hồ Sơ Lần Đầu** | `ui/screen/ProfileSetupScreen.kt` | Onboarding | ✅ **Completed** | Role cards 140dp, form fields 60dp, nút hoàn tất 56dp, haptics |
| **19** | **Màn Hình Khởi Động** | `ui/screen/AppSplashScreen.kt` | Splash | ✅ **Completed** | Gradient xanh nông nghiệp, slogan & loading i18n 5 thứ tiếng |

---

## 2. Tiêu Chí Kiểm Toán Toàn Diện (Universal Screen Checklist)

Mọi màn hình khi thực hiện nâng cấp đã vượt qua 5 nhóm tiêu chí sau:

### Tiêu chí A: Quốc Tế Hóa & Bản Địa Hóa (i18n)
- [x] **Không chuỗi hardcode:** 100% văn bản giao diện (tiêu đề, nhãn phụ, placeholder, lỗi, dialog, nút bấm) lấy từ `stringResource(R.string.*)`.
- [x] **Đồng bộ 5 ngôn ngữ:** Mọi chuỗi được khai báo đồng bộ tại 5 tệp XML:
  - `values/strings.xml` (Tiếng Việt)
  - `values-en/strings.xml` (Tiếng Anh)
  - `values-km/strings.xml` (Tiếng Khmer)
  - `values-lo/strings.xml` (Tiếng Lào)
  - `values-zh-rCN/strings.xml` (Tiếng Trung Giản thể)
- [x] **Định dạng số & tiền tệ:** Dùng `NumberFormat` theo locale, hiển thị đúng ký hiệu tiền tệ và đơn vị khối lượng (`kg`, `đ`).
- [x] **Thời gian tương đối:** Dùng chung bộ chuỗi `news_time_*` (`vừa xong`, `X phút trước`, `X giờ trước`, v.v...).

### Tiêu chí B: Công Thái Học Ngoài Đồng Ruộng (Ergonomics & Touch Targets)
- [x] **Touch Target $\ge 48\text{dp}$:** Mọi thành phần tương tác (nút bấm, icon button, checkbox, chip bộ lọc, tab) đạt kích thước tối thiểu $48 \times 48\text{dp}$ hoặc `heightIn(min = 48.dp)`.
- [x] **Vùng an toàn Bottom Bar:** Content padding đáy tính thêm chiều cao của Navigation Bar để không bị che khuất nội dung hoặc FAB.
- [x] **Keyboard / IME Handling:** Tự động điều chỉnh padding khi mở bàn phím ảo, không làm đẩy thanh công cụ sai lệch.

### Tiêu chí C: Chuẩn 4 Trạng Thái Giao Diện (4 UI States)
- [x] **1. Loading State:** Hiển thị Skeleton shimmer / ProgressIndicator mượt mà tương ứng với cấu trúc layout.
- [x] **2. Empty State:** Khi không có dữ liệu, có hình minh họa/icon trực quan, thông điệp rõ ràng và **Primary CTA Button** ($\ge 48\text{dp}$) hướng dẫn hành động tiếp theo.
- [x] **3. Error / Offline State:** Có banner hoặc thông báo nhẹ nhàng khi mất mạng, giữ dữ liệu đệm cache ngoại tuyến và cung cấp nút thử lại.
- [x] **4. Content / Success State:** Bố cục phân cấp thị giác rõ nét (Visual Hierarchy), tương phản cao ($\ge 4.5:1$).

### Tiêu chí D: Hiệu Ứng Chuyển Động Nâng Cao (`@hieu-ung-nangcao`)
- [x] **Spring Physics Animation:** Sử dụng `spring(stiffness = Spring.StiffnessMediumLow)` cho các chuyển đổi ẩn/hiện, mở rộng Bottom Sheet hoặc thay đổi tab.
- [x] **Color Transition:** Dùng `animateColorAsState` mượt mà khi đổi màu chip lọc, nút toggle.
- [x] **Haptic Feedback:** Tích hợp `HapticUtil.tick(context)` cho mọi thao tác bấm nút, chọn mục, vuốt làm mới, hoặc thao tác bàn phím số.

### Tiêu chí E: Thẩm Mỹ & Nguyên Tắc Thiết Kế (`DESIGN.md`)
- [x] **Tuân thủ Purple Ban:** Tuyệt đối không dùng màu tím (Purple/Violet) theo quy định AG Kit. Sử dụng hệ màu nông nghiệp chủ đạo: Xanh lúa chín/lá mạ (`GreenPrimary`), vàng hạt gạo (`GoldAccent`), nền bề mặt kem/sáng (`CardBg`, `SurfaceContainer`).
- [x] **Typography Hierarchy:** Phân cấp cỡ chữ rõ ràng (`titleLarge` cho số liệu quan trọng, `labelMedium` cho nhãn).

---

## 3. Kế Hoạch Thực Thi Từng Bước (Execution Roadmap)

```
Bước 1: [HOÀN TẤT] CardDetailScreen, CardListScreen, MarketScreen (Tin tức + Bảng giá)
       ↓
Bước 2: [HOÀN TẤT] WeightInputScreen (Bàn phím số cân lúa + Trừ bao bì + Haptics)
       ↓
Bước 3: [HOÀN TẤT] AiChatScreen (Chat Gemini + Presets + Bảng gói thành viên)
       ↓
Bước 4: [HOÀN TẤT] FarmerProfileScreen, SettingsScreen (Hồ sơ + Cài đặt 5 ngôn ngữ)
       ↓
Bước 5: [HOÀN TẤT] Nhóm Màn hình Thương Lái & Bản Đồ (TraderProfile, TraderTransactions, TraderHistory, RiceMap)
       ↓
Bước 6: [HOÀN TẤT] Nhóm Màn hình Phụ Trợ & Onboarding (PremiumScreen, DeletedCards, Feedback, SyncStatus, AuthScreen, ProfileSetupScreen, AppSplashScreen)
```

---

## 4. Báo Cáo Nghiệm Thu & Kiểm Chứng

- **Biên dịch Kotlin:** Toàn bộ dự án biên dịch thành công 100% với lệnh:
  `.\gradlew.bat compileDebugKotlin` (Build Success).
- **Kiểm tra tài nguyên chuỗi:** 100% chuỗi mới được đối chiếu và dịch đầy đủ qua cả 5 ngôn ngữ:
  - `app/src/main/res/values/strings.xml` (Tiếng Việt)
  - `app/src/main/res/values-en/strings.xml` (English)
  - `app/src/main/res/values-km/strings.xml` (ភាសាខ្មែរ)
  - `app/src/main/res/values-lo/strings.xml` (ພາສາລາວ)
  - `app/src/main/res/values-zh-rCN/strings.xml` (中文)
- **Công thái học & Haptics:** Tất cả nút bấm, danh sách lựa chọn, ô nhập liệu và tương tác ngoài thực địa đều được trang bị phản hồi xúc giác `HapticUtil.tick(context)` và touch target đạt chuẩn $\ge 48\text{dp}$.

<!-- GOAL_COMPLETE -->
