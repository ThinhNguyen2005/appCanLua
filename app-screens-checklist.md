# Danh Sách Toàn Bộ Màn Hình & Master Checklist UI/UX (App Cân Lúa)

> **Mục tiêu:** Thống kê toàn bộ các màn hình trong ứng dụng `appCanLua`, phân loại trạng thái hiện tại (Đã hoàn thiện / Cần hoàn thiện), và thiết lập bảng kiểm (Checklist) chuẩn hóa theo tiêu chí: i18n 5 ngôn ngữ, công thái học touch target $\ge 48\text{dp}$, chuẩn 4 trạng thái giao diện (4 UI States), và hiệu ứng chuyển động có mục đích (`@hieu-ung-nangcao`).
> **Thời gian:** 2026-10-13
> **Quy chuẩn áp dụng:** `@ui-ux-playbook`, `@hieu-ung-nangcao`, `@mobile-design`, `@android-pro`, `DESIGN.md`.

---

## 1. Tổng Quan Tiến Độ Hệ Thống Màn Hình (Screen Inventory & Status)

| STT | Màn hình / Component | Đường dẫn tệp | Vai trò / Nhóm | Trạng thái | Kế hoạch tương ứng |
| :---: | :--- | :--- | :--- | :---: | :--- |
| **01** | **Chi tiết Phiếu Cân** | `ui/screen/CardDetailScreen.kt` | Core Flow | ✅ **Completed** | `card-detail-polish.md` |
| **02** | **Trang Chính (Cân Lúa / Home)** | `ui/screen/CardListScreen.kt` | Bottom Nav 1 | ✅ **Completed** | `home-screen-polish.md` |
| **03** | **Thị Trường: Tin Tức** | `ui/component/market/NewsSection.kt` | Bottom Nav 2 (Tab 0) | ✅ **Completed** | `news-screen-polish.md` |
| **04** | **Thị Trường: Bảng Giá** | `ui/screen/market/MarketScreen.kt` + `RicePriceCard.kt` | Bottom Nav 2 (Tab 1) | ✅ **Completed** | `market-prices-polish.md` |
| **05** | **Nhập Cân Lúa (Bàn phím số)** | `ui/screen/WeightInputScreen.kt` | Core Flow | ✅ **Completed** | `weight-input-polish.md` |
| **06** | **Hỏi Đáp AI (Trợ lý Gemini)** | `ui/screen/aichat/AiChatScreen.kt` | Bottom Nav 3 | ⏳ **Ưu tiên 2** | `aichat-screen-polish.md` |
| **07** | **Hồ Sơ Nông Dân / Cá Nhân** | `ui/screen/FarmerProfileScreen.kt` | Bottom Nav 4 (Farmer) | ⏳ **Ưu tiên 3** | `farmer-profile-polish.md` |
| **08** | **Hồ Sơ Thương Lái** | `ui/screen/trader/TraderProfileScreen.kt` | Bottom Nav 4 (Trader) | ⏳ **Ưu tiên 4** | `trader-profile-polish.md` |
| **09** | **Bản Đồ Lúa / Heatmap** | `ui/screen/map/RiceMapScreen.kt` | Bottom Nav (Trader) | ⏳ Chờ kiểm toán | `rice-map-polish.md` |
| **10** | **Nâng Cấp Gói Premium** | `ui/screen/profile/PremiumScreen.kt` | Monetization | ⏳ Chờ kiểm toán | `premium-screen-polish.md` |
| **11** | **Cài Đặt Ứng Dụng** | `ui/screen/SettingsScreen.kt` | Settings | ⏳ Chờ kiểm toán | `settings-screen-polish.md` |
| **12** | **Sổ Giao Dịch Thương Lái** | `ui/screen/trader/TraderTransactionsScreen.kt` | Trader Feature | ⏳ Chờ kiểm toán | `trader-tx-polish.md` |
| **13** | **Lịch Sử Giá Thương Lái** | `ui/screen/TraderHistoryScreen.kt` | Trader Feature | ⏳ Chờ kiểm toán | `trader-history-polish.md` |
| **14** | **Thùng Rác (Phiếu đã xóa)** | `ui/screen/DeletedCardsScreen.kt` | Data Management | ⏳ Chờ kiểm toán | `deleted-cards-polish.md` |
| **15** | **Gửi Phản Hồi / Góp Ý** | `ui/screen/profile/FeedbackScreen.kt` | Support | ⏳ Chờ kiểm toán | `feedback-screen-polish.md` |
| **16** | **Trạng Thái Đồng Bộ Đám Mây** | `ui/screen/SyncStatusScreen.kt` | Sync / Cloud | ⏳ Chờ kiểm toán | `sync-status-polish.md` |
| **17** | **Xác Thực / Đăng Nhập** | `ui/screen/AuthScreen.kt` | Onboarding | ⏳ Chờ kiểm toán | `auth-screen-polish.md` |
| **18** | **Thiết Lập Hồ Sơ Lần Đầu** | `ui/screen/ProfileSetupScreen.kt` | Onboarding | ⏳ Chờ kiểm toán | `profile-setup-polish.md` |
| **19** | **Màn Hình Khởi Động** | `ui/screen/AppSplashScreen.kt` | Splash | ⏳ Chờ kiểm toán | `splash-screen-polish.md` |

---

## 2. Tiêu Chí Kiểm Toán Toàn Diện (Universal Screen Checklist)

Mỗi màn hình khi thực hiện nâng cấp bắt buộc phải vượt qua 5 nhóm tiêu chí sau:

### Tiêu chí A: Quốc Tế Hóa & Bản Địa Hóa (i18n)
- [ ] **Không chuỗi hardcode:** 100% văn bản giao diện (tiêu đề, nhãn phụ, placeholder, lỗi, dialog, nút bấm) lấy từ `stringResource(R.string.*)`.
- [ ] **Đồng bộ 5 ngôn ngữ:** Mọi chuỗi mới phải có bản dịch tương ứng tại:
  - `values/strings.xml` (Tiếng Việt)
  - `values-en/strings.xml` (Tiếng Anh)
  - `values-km/strings.xml` (Tiếng Khmer)
  - `values-lo/strings.xml` (Tiếng Lào)
  - `values-zh-rCN/strings.xml` (Tiếng Trung Giản thể)
- [ ] **Định dạng số & tiền tệ:** Dùng `NumberFormat` theo locale, hiển thị đúng ký hiệu tiền tệ và đơn vị khối lượng (`kg`, `đ`).
- [ ] **Thời gian tương đối:** Dùng chung bộ chuỗi `news_time_*` (`vừa xong`, `X phút trước`, `X giờ trước`, v.v...).

### Tiêu chí B: Công Thái Học Ngoài Đồng Ruộng (Ergonomics & Touch Targets)
- [ ] **Touch Target $\ge 48\text{dp}$:** Mọi thành phần tương tác (nút bấm, icon button, checkbox, chip bộ lọc, tab) phải có kích thước tối thiểu $48 \times 48\text{dp}$ hoặc `heightIn(min = 48.dp)`.
- [ ] **Vùng an toàn Bottom Bar:** Content padding đáy tính thêm chiều cao của Floating Navigation Bar (`96.dp + navBarPadding`) để không bị che khuất nội dung hoặc FAB.
- [ ] **Keyboard / IME Handling:** Tự động điều chỉnh padding khi mở bàn phím ảo, không làm đẩy thanh công cụ sai lệch.

### Tiêu chí C: Chuẩn 4 Trạng Thái Giao Diện (4 UI States)
- [ ] **1. Loading State:** Hiển thị Skeleton shimmer mượt mà tương ứng với cấu trúc layout thật, không dùng loading spinner trơ trọi giữa màn hình.
- [ ] **2. Empty State:** Khi không có dữ liệu, phải có hình minh họa/icon trực quan, thông điệp rõ ràng và **Primary CTA Button** ($\ge 48\text{dp}$) hướng dẫn hành động tiếp theo (ví dụ: "Tạo phiếu mới", "Làm mới").
- [ ] **3. Error / Offline State:** Có banner hoặc thông báo nhẹ nhàng khi mất mạng, giữ dữ liệu đệm cache ngoại tuyến và cung cấp nút thử lại.
- [ ] **4. Content / Success State:** Bố cục phân cấp thị giác rõ nét (Visual Hierarchy), tương phản cao ($\ge 4.5:1$).

### Tiêu chí D: Hiệu Ứng Chuyển Động Nâng Cao (`@hieu-ung-nangcao`)
- [ ] **Spring Physics Animation:** Sử dụng `spring(stiffness = Spring.StiffnessMediumLow)` cho các chuyển đổi ẩn/hiện (`AnimatedVisibility`), mở rộng Bottom Sheet hoặc thay đổi tab.
- [ ] **Color Transition:** Dùng `animateColorAsState` mượt mà khi đổi màu chip lọc, nút toggle.
- [ ] **Haptic Feedback:** Tích hợp `HapticUtil.tick(context)` cho mọi thao tác bấm nút, chọn mục, vuốt làm mới, hoặc thao tác bàn phím số.

### Tiêu chí E: Thẩm Mỹ & Nguyên Tắc Thiết Kế (`DESIGN.md`)
- [ ] **Tuân thủ Purple Ban:** Tuyệt đối không dùng màu tím (Purple/Violet) theo quy định AG Kit. Sử dụng hệ màu nông nghiệp chủ đạo: Xanh lúa chín/lá mạ (`GreenPrimary`), vàng hạt gạo (`GoldAccent`), nền bề mặt kem/sáng (`CardBg`, `SurfaceContainer`).
- [ ] **Typography Hierarchy:** Phân cấp cỡ chữ rõ ràng (`titleLarge` cho số liệu quan trọng, `labelMedium` cho nhãn).

---

## 3. Checklist Chi Tiết Cho Từng Màn Hình Tiếp Theo

### 🎯 Ưu tiên 1: Màn hình Nhập Cân Lúa (`WeightInputScreen.kt`)
*Màn hình thao tác cốt lõi khi nông dân và thương lái trực tiếp cân từng bao lúa ngoài ruộng.*
- [ ] **Bàn phím số cân lúa:** Kích thước từng phím số $\ge 56\text{dp}$, phản hồi rung `HapticUtil.tick(context)` mỗi lần nhấn số.
- [ ] **Hiển thị tổng bao & tổng ký:** Số nhảy mượt mà bằng hiệu ứng chuyển số hoặc animation nhẹ.
- [ ] **Nút Lưu / Hoàn thành:** Nút Primary kích thước lớn $\ge 52\text{dp}$ đặt ở vị trí thuận tay cái.
- [ ] **Bản địa hóa:** Các nút "Xóa", "Lưu", "Bao kế tiếp", "Trừ bao bì", "Độ ẩm" liên kết 100% với `strings.xml`.
- [ ] **Chế độ ngoại tuyến:** Lưu tức thời vào Room SQLite cục bộ kèm phản hồi thị giác rõ ràng.

### 🎯 Ưu tiên 2: Màn hình Hỏi Đáp AI (`AiChatScreen.kt`)
*Trợ lý nông nghiệp & thị trường Gemini dành cho Nông dân và Thương lái.*
- [ ] **Input Bar & Bàn phím:** Tự động ẩn BottomBar khi mở IME, thanh nhập liệu có nút Gửi $\ge 48\text{dp}$.
- [ ] **Empty State / Gợi ý câu hỏi:** Các thẻ câu hỏi mẫu ("Giá lúa hôm nay", "Cách phòng ngừa đạo ôn") có kích thước chạm lớn, chuyển màu êm dịu khi bấm.
- [ ] **Bong bóng chat & Markdown:** Phân biệt rõ lời thoại người dùng và AI, thời gian gửi tin nhắn bản địa hóa.
- [ ] **Bản địa hóa 5 ngôn ngữ:** Toàn bộ tiêu đề TopBar, trạng thái đang gõ, lỗi kết nối mạng.

### 🎯 Ưu tiên 3: Màn hình Hồ Sơ Nông Dân (`FarmerProfileScreen.kt`)
*Nơi quản lý thông tin nông trại, tổng sản lượng mùa vụ, thống kê doanh thu và cài đặt tài khoản.*
- [ ] **Thẻ tổng quan mùa vụ (KPI Card):** Thống kê sản lượng, số phiếu, doanh thu với độ tương phản cao.
- [ ] **Danh sách chức năng / Menu items:** Mỗi dòng menu đạt chiều cao $\ge 52\text{dp}$, icon trực quan, có chevron điều hướng.
- [ ] **Haptic feedback:** Rung nhẹ khi chạm chọn menu chức năng.
- [ ] **Bản địa hóa:** Tên các chức năng, thông tin cá nhân, vai trò "Nông dân".

### 🎯 Ưu tiên 4: Màn hình Hồ Sơ Thương Lái (`TraderProfileScreen.kt`)
*Dành riêng cho thương lái thu mua lúa gạo.*
- [ ] **Quản lý danh sách thu mua & Bảng giá cá nhân.**
- [ ] **Nút chuyển đổi trạng thái thu mua / địa bàn hoạt động.**
- [ ] **Đảm bảo touch target $\ge 48\text{dp}$ và bản địa hóa 5 ngôn ngữ.**

### 🎯 Ưu tiên 5: Màn hình Bản Đồ Lúa (`RiceMapScreen.kt`)
- [ ] **Bộ lọc bản đồ (`MapFilters.kt`):** Chiều cao chip lọc $\ge 44-48\text{dp}$, animate color khi chọn.
- [ ] **Card tóm tắt khi bấm vào ruộng (`CardSummaryBottomSheet.kt`):** Bo góc $24\text{dp}$, kéo vuốt mượt mà.
- [ ] **Empty State (`MapEmptyStates.kt`):** Nút CTA tải lại bản đồ $\ge 48\text{dp}$.

### 🎯 Ưu tiên 6: Màn hình Cài Đặt (`SettingsScreen.kt`)
- [ ] **Chọn ngôn ngữ (Tiếng Việt, English, ភាសាខ្មែរ, ພາສາລາວ, 中文):** Dialog hoặc BottomSheet chọn ngôn ngữ trực quan, phản hồi tức thời.
- [ ] **Cấu hình trừ bao bì mặc định, đơn vị đo lường.**
- [ ] **Các toggle switch:** Kích thước công thái học chuẩn Material 3.

---

## 4. Kế Hoạch Thực Thi Từng Bước Đề Xuất (Execution Roadmap)

```
Bước 1: [HOÀN TẤT] CardDetailScreen, CardListScreen, MarketScreen (Tin tức + Bảng giá)
       ↓
Bước 2: [TIẾP THEO] Nâng cấp Màn hình Nhập Cân Lúa (WeightInputScreen.kt)
       ↓
Bước 3: Nâng cấp Màn hình Hỏi Đáp AI (AiChatScreen.kt)
       ↓
Bước 4: Nâng cấp Màn hình Hồ Sơ & Cài Đặt (FarmerProfileScreen.kt, SettingsScreen.kt)
       ↓
Bước 5: Nâng cấp Nhóm Màn hình Thương Lái & Bản Đồ (TraderProfile, TraderTransactions, RiceMap)
       ↓
Bước 6: Nâng cấp Nhóm Màn hình Phụ Trợ (PremiumScreen, DeletedCards, Feedback, SyncStatus, Auth)
```

---

## 5. Báo Cáo Nghiệm Thu & Kiểm Chứng

- Mọi màn hình sau khi chỉnh sửa đều được biên dịch kiểm tra bằng `.\gradlew.bat compileDebugKotlin`.
- Toàn bộ chuỗi được đối chiếu qua 5 tệp XML: `values`, `values-en`, `values-km`, `values-lo`, `values-zh-rCN`.
- Đánh dấu hoàn tất với `<!-- GOAL_COMPLETE -->`.
