# 🌾 Cân Lúa — Flutter App Blueprint

> **Mục đích:** Tài liệu tiền đề lộ trình phát triển ứng dụng Flutter mới, kế thừa toàn bộ giá trị nghiệp vụ từ dự án Android Kotlin hiện tại.
> **Phiên bản tham chiếu:** Android Kotlin Phase 2.10 (AppDatabase v17, 2026-06)
> **Đối tượng dùng app:** Nông dân ĐBSCL (không cần tài khoản, không role phức tạp)

---

## 0. Tổng quan sản phẩm

**Cân Lúa** là ứng dụng mobile phục vụ nông dân vùng ĐBSCL trong việc:

1. **Cân lúa** — số hóa phiếu cân tại ruộng, nhiều bao, tính toán thực tế (bì, tạp chất, độ ẩm)
2. **Bảng giá lúa / gạo** — xem giá thu mua realtime theo giống lúa, khu vực
3. **Đọc tin tức nông nghiệp** — RSS feed từ các báo uy tín
4. **AI khuyến nông** — hỏi đáp thông minh có context giá thị trường + knowledge base canh tác
5. **Thống kê vụ mùa** — dashboard AI insights, báo cáo tổng quan theo từng vụ

**Triết lý thiết kế:**
- **Offline-First** — hoạt động 100% khi mất sóng ở đồng ruộng
- **Không đăng nhập** — không yêu cầu tài khoản (bỏ auth, bỏ role thương lái)
- **Đơn giản, nhanh, dễ dùng** — nông dân lớn tuổi, ánh sáng mạnh ngoài trời
- **Chỉ 1 role: Nông dân** — không phân quyền phức tạp

---

## 1. Những vấn đề đặt lên hàng đầu (Priority P0)

### P0.1 — Không lag, không skeleton sai
- **Không dùng skeleton/shimmer** cho màn danh sách phiếu cân, chi tiết phiếu, nhập cân
- Dữ liệu local (SQLite/Drift) phải load tức thì — không có latency giả tạo
- Skeleton chỉ dùng khi fetch từ network (tin tức, giá lúa từ Firestore)

### P0.2 — Bàn phím không che nội dung
- Tất cả màn có input field PHẢI dùng `resizeToAvoidBottomInset: true` + `SingleChildScrollView`
- Form nhập phiếu cân: dùng **bottom sheet** hoặc **full-screen page** thay vì dialog
- Màn nhập cân (WeightInput): scroll xuống tự động khi bàn phím mở

### P0.3 — AI Chat không bị văng app
- Xử lý lỗi network graceful (retry, timeout, error state)
- Session chat lưu local (Drift) — quay lại app vẫn còn lịch sử
- Không crash khi API key rỗng / hết quota

### P0.4 — Bottom navigation nổi bật, dễ nhận biết
- Bottom bar phải tương phản rõ với nền — không đồng màu nền trắng/xám
- Active tab: filled icon + label + màu accent nổi bật
- Idle tab: outlined icon, màu mờ

### P0.5 — Công thức cân lúa chuẩn xác
- **Net Weight** = `TổngKg − (SốBao × KgBì) − TạpChất − TổngKg × (ĐộẨm / 100)`
- Có 2 cách tính bì: Cách A (kg/bao cố định) và Cách B (cân mẫu)
- Tạp chất có thể nhập theo kg hoặc theo %
- Giá nhân với Net Weight → Tổng tiền

---

## 2. Yêu cầu Style giao diện

### 2.1 Bảng màu (Color Palette)

| Token | Tên màu | Hex | Ghi chú |
|---|---|---|---|
| primary | Xanh lá lúa | `#2E7D32` | Màu chủ đạo — nút CTA, active tab |
| primaryLight | Xanh nhạt | `#4CAF50` | Hover, chip active |
| primaryDark | Xanh đậm | `#1B5E20` | Pressed, shadow |
| accent | Vàng lúa chín | `#F9A825` | Badge, số tiền, trend UP |
| accentDark | Vàng đậm | `#F57F17` | Trend tăng đậm |
| error | Đỏ | `#C62828` | Lỗi, trend DOWN |
| surface | Trắng sữa | `#FAFAFA` | Card background sáng |
| background | Xanh lá nhạt | `#F1F8E9` | Nền tổng thể |
| onSurface | Đen mềm | `#1A1C18` | Text chính |
| subtitle | Xám xanh | `#4A5241` | Text phụ |
| divider | Xanh nhạt | `#E8EDE3` | Đường phân cách |
| bottomBar | Xanh tối | `#1E4620` | Nền bottom nav tối để nổi bật |

> ❌ **CẤM dùng màu tím/violet** ở bất kỳ thành phần nào.

### 2.2 Typography

| Style | Font | Size | Weight |
|---|---|---|---|
| Display | Be Vietnam Pro | 28sp | Bold |
| Heading | Be Vietnam Pro | 20sp | SemiBold |
| Title | Be Vietnam Pro | 16sp | Medium |
| Body | Be Vietnam Pro | 14sp | Regular |
| Caption | Be Vietnam Pro | 12sp | Regular |
| Numeric / KG | Noto Sans | 18sp | Bold |

- Font chính: **Be Vietnam Pro** (Google Fonts) — thân thiện, rõ ràng ngoài nắng
- Số liệu (kg, tiền): **Noto Sans** — mono-style tránh giật layout
- Min touch target: **48dp**
- Line height: 1.5 cho body text

### 2.3 Component Design System

```
Elevation layers:
  Level 0 — Background (#F1F8E9)
  Level 1 — Surface card (#FAFAFA), shadow nhẹ 2dp
  Level 2 — Bottom sheet, dialog (#FFFFFF), shadow 8dp
  Level 3 — FAB, snackbar (#FFFFFF), shadow 12dp

Border radius:
  xs: 8dp   — Input field, small chip
  sm: 12dp  — Card
  md: 16dp  — Bottom sheet top corners
  lg: 24dp  — FAB, pill button
  full: 99dp — Chip active, badge

Spacing grid: 4dp base unit
  xs: 4dp | sm: 8dp | md: 16dp | lg: 24dp | xl: 32dp
```

### 2.4 Motion & Animation
- Page transition: slide từ phải vào (go_router transition)
- Bottom sheet: slide up, 300ms, Curves.easeOut
- Card appear: fade + scale 0.96→1.0, 200ms
- Number change: animated counter (TweenAnimationBuilder)
- FAB: scale down khi scroll xuống, scale up khi scroll lên
- Active tab expand: width animation 150ms

### 2.5 Dark Mode
- Hỗ trợ Dark theme từ đầu (ThemeMode.system)
- Dark surface: `#1A1F16`, card: `#222820`
- Primary trong dark: `#81C784` (sáng hơn để contrast)
- Accent trong dark: `#FFD54F`

---

## 3. Các màn hình & luồng hoạt động

### 3.1 Tab Bar Navigation (5 tab)

```
Bottom Navigation:
├── 🌾 Cân Lúa     (scale icon)
├── 📊 Bảng Giá    (bar chart icon)
├── 📰 Tin Tức     (newspaper icon)
├── 🤖 AI Chat     (chat bot icon)
└── 📋 Tổng Quan   (dashboard icon)
```

**Bottom bar design:**
- Nền tối `#1E4620` để nổi bật trên mọi nền app
- Active: filled icon + label expand animation + màu `#F9A825` (vàng)
- Idle: outlined icon, màu `#A5D6A7` (xanh nhạt)

---

### 3.2 Tab 1: Cân Lúa (CardListScreen)

**Mục đích:** Danh sách phiếu cân, tạo phiếu mới, tra cứu nhanh.

```
┌─────────────────────────────┐
│ [🔍 Search] [Filter] [Sort] │  sticky header
├─────────────────────────────┤
│ 📋 Phiếu #001               │
│ Nguyễn Văn A · ST25         │
│ 2.450 kg · 12.250.000 đ     │
│ ⚡ Đông Xuân 2026  [Xem →]  │
├─────────────────────────────┤
│ ...                         │
└─────────────────────────────┘
                         [+ FAB]
```

**Chức năng:**
- Danh sách phiếu cân (sort: mới nhất trước)
- Search theo tên người bán, giống lúa
- Filter chip: Tất cả / Tuần này / Tháng này / Còn nợ
- Swipe left → Xóa (confirm), Swipe right → Sửa nhanh
- FAB "+" → Bottom Sheet tạo phiếu mới (không dùng dialog)
- **Không dùng skeleton** — dữ liệu Drift load tức thì

**Card item hiển thị:**

| Field | Ví dụ |
|---|---|
| Tên người bán / ruộng | Nguyễn Văn A |
| Giống lúa (chip tag) | ST25 |
| Vụ mùa | Đông Xuân 2026 |
| Tổng KG thực / bao | 2.450 kg · 48 bao |
| Tổng tiền | 12.350.000 đ |
| Trạng thái | 🔴 Còn nợ 5.360.000 đ |
| Ngày tạo | 15/06/2026 |

---

### 3.3 Tạo Phiếu Mới (CreateCardBottomSheet)

**Dùng Bottom Sheet thay vì Dialog** — scroll được, bàn phím không che.

```
[THÔNG TIN PHIẾU]
  Tên người bán / ruộng *    TextField
  Số điện thoại              TextField (optional)
  CCCD                       TextField (optional)
  Giống lúa *                Dropdown
  Vụ mùa *                   Dropdown (auto-suggest)
  Địa chỉ ruộng             TextField (optional)
  Tên thương lái            TextField (optional)
  Giá thu mua (đ/kg) *      NumericField

[TÙY CHỌN CÂN]
  Cách tính bì:
    • Cách A: X kg/bao (default 0.3 kg)
    • Cách B: Cân mẫu (số bao mẫu + tổng kg mẫu)
  Tạp chất theo: kg | %
  Chế độ nhập KG: SMALL (3 số) | LARGE (4 số)
```

**Vụ mùa auto-suggest:**
- Tháng 11–12 → Đông Xuân năm sau
- Tháng 1–4 → Đông Xuân năm hiện tại
- Tháng 5–8 → Hè Thu
- Tháng 9–10 → Thu Đông

---

### 3.4 Chi tiết Phiếu (CardDetailScreen)

**Full scrollable page — không dialog, không skeleton.**

```
┌─────────────────────────────────┐
│ ← Phiếu #001         [✏️] [⋮] │
├─────────────────────────────────┤
│ [THÔNG TIN PHIẾU]               │
│ Nguyễn Văn A · 0901234567       │
│ ST25 · Đông Xuân 2026           │
│ 📍 Ấp 3, Xã Mỹ Thành Bắc       │
├─────────────────────────────────┤
│ [TỔNG HỢP KHỐI LƯỢNG]          │
│ Tổng cân:       2.450,0 kg      │
│ Tổng bì:         − 36,0 kg      │
│ Tạp chất:        − 12,5 kg      │
│ Khấu độ ẩm 1%:  − 24,5 kg      │
│ ─────────────────────────       │
│ KG THỰC:        2.377,0 kg      │
├─────────────────────────────────┤
│ [THANH TOÁN]                    │
│ Giá:              5.200 đ/kg    │
│ Tổng tiền:    12.360.400 đ      │
│ Đã cọc:       −2.000.000 đ      │
│ Đã trả:       −5.000.000 đ      │
│ ─────────────────────────       │
│ CÒN LẠI: 5.360.400 đ 🔴         │
├─────────────────────────────────┤
│ [DANH SÁCH BAO]                 │
│ #1: 50.3 kg  #2: 49.8 kg ...   │
├─────────────────────────────────┤
│ [Nhập Cân] [Thanh Toán] [PDF]  │
└─────────────────────────────────┘
```

**Hành động:**
- "Nhập Cân" → WeightInputScreen
- "Ghi nhận thanh toán" → bottom sheet: Cọc / Thanh toán / Hoàn tiền
- "Xuất PDF" → pdf package + share_plus
- "Sửa" → edit bottom sheet (thông tin + giá)
- "Xóa" → xác nhận → thùng rác (có khôi phục)

---

### 3.5 Nhập Cân (WeightInputScreen)

**Full Screen Page — tức thì, không skeleton, không lag.**

```
┌─────────────────────────────────┐
│ ← Nhập Cân · ST25 · #001       │
├─────────────────────────────────┤
│ [LIVE METRICS]                  │
│ Bao: 12     Tổng: 598.4 kg      │
│ KG Thực: 540.2 kg               │
│ Tạm tính: 2.809.040 đ           │
├─────────────────────────────────┤
│ [BẢNG CÂN — 3 cột, paging ngang│
│ ┌──────┬──────┬──────┐          │
│ │ 50.3 │ 49.8 │ 51.2 │ trang 1 │
│ │ 50.1 │ 50.5 │ 50.0 │         │
│ │ 49.9 │ 51.1 │ 50.8 │         │
│ └──────┴──────┴──────┘          │
│          ···  ●  ···            │
├─────────────────────────────────┤
│ [INPUT — luôn trên keyboard]   │
│ [ 5 0 3 ] → 50.3 kg            │
│ Đang nhập bao #13               │
│ [Lưu]  [Xóa cuối]  [⚙️ Options]│
└─────────────────────────────────┘
```

**Luồng nhập:**
1. Gõ số → auto-confirm sau N ký tự
2. Mode SMALL: `503` → `50.3 kg` (3 ký tự)
3. Mode LARGE: `1503` → `150.3 kg` (4 ký tự)
4. TTS tùy chọn: đọc số khi nhập
5. Bảng 3 cột, nhảy column-major (C1 đầy → C2 → C3)
6. Swipe bao để xóa

**Công thức RiceCalculator (Dart):**

```dart
double totalBag = bagMethodIsSampling
  ? (bagSampleTotalWeight / bagSampleCount) * bagCount
  : bagCount * bagWeightPerUnit;

double impurityKg = impurityIsPercent
  ? totalWeight * (impurityPercent / 100)
  : impurityFixed;

double moistureDeduction = totalWeight * (moisturePercent / 100);

double netWeight = totalWeight - totalBag - impurityKg - moistureDeduction;
double totalAmount = netWeight * pricePerKg;
double remainingAmount = totalAmount - depositAmount - paidAmount;
```

---

### 3.6 Tab 2: Bảng Giá Lúa & Gạo (MarketScreen)

```
┌─────────────────────────────────┐
│ Bảng Giá    [Lúa] [Gạo] [Nếp] │
│             [Khu vực ĐBSCL ▼]  │
├─────────────────────────────────┤
│ Cập nhật: hôm nay 08:30        │
├─────────────────────────────────┤
│ ST25                   ↑ +200  │
│ 8.500 – 9.200 đ/kg             │
│ [────────────────] 7-day chart  │
├─────────────────────────────────┤
│ OM5451                 → =     │
│ 7.200 – 7.800 đ/kg             │
├─────────────────────────────────┤
│ Đài Thơm 8             ↓ −100  │
│ 7.500 – 8.000 đ/kg             │
└─────────────────────────────────┘
```

**Data flow:** Google Sheets → Apps Script → Firestore → Drift cache → UI

**Trend badge:**
- ↑ vàng `#F9A825` — tăng
- ↓ đỏ `#C62828` — giảm
- → xám — ổn định

**Offline:** Cache Drift + badge "Đã lưu · Xh trước"

---

### 3.7 Tab 3: Tin Tức (NewsScreen)

```
┌─────────────────────────────────┐
│ Tin Nông Nghiệp                 │
│ [Lúa] [Gạo] [Thị trường]       │
├─────────────────────────────────┤
│ 🖼 [thumbnail]                  │
│ "Giá lúa ST25 tăng mạnh..."    │
│ Báo Nông Nghiệp · 2h trước     │
├─────────────────────────────────┤
│ ...                             │
└─────────────────────────────────┘
```

**RSS Sources:**
- Báo Nông Nghiệp VN: `https://nongnghiep.vn/rss.rss`
- VnExpress Kinh doanh: `https://vnexpress.net/rss/kinh-doanh.rss`

**Auto-classify** keyword → Lúa / Gạo / Thị trường / Khác

**Mở bài:** `url_launcher` Chrome Custom Tabs (không WebView)

**Cache:** Drift `news_articles`, TTL 1 giờ

---

### 3.8 Tab 4: AI Chat (AiChatScreen)

```
┌─────────────────────────────────┐
│ 🤖 AI Khuyến Nông        [🗑️] │
├─────────────────────────────────┤
│   [User: Giá ST25 hôm nay?]    │
│                                 │
│ [AI: Giá ST25 khu vực ĐBSCL    │
│  hôm nay 8.500–9.200 đ/kg.     │
│                                 │
│  | Giống  | Giá  | Trend |     │
│  | ST25   | 8.9k | ↑     |  ]  │  Markdown table
│                                 │
├─────────────────────────────────┤
│ [🎤] [Nhập câu hỏi...   ] [▶] │  luôn trên keyboard
└─────────────────────────────────┘
```

**System Prompt context inject:**
```
Bạn là AI khuyến nông Việt Nam, chuyên tư vấn canh tác lúa vùng ĐBSCL.

Ngữ cảnh hôm nay:
- Giá lúa top-5: {rice_prices}
- Knowledge base canh tác: {agronomy_knowledge}

Trả lời tiếng Việt, ngắn gọn, dẫn số liệu thực tế.
```

**Features:**
- Voice input: `speech_to_text`
- Markdown render: `flutter_markdown` (heading, bold, table, code)
- Session Drift (không mất khi tắt app)
- Typing dots animation
- Retry khi lỗi
- Graceful error khi thiếu API key

**API:** OpenRouter (`google/gemini-flash-1.5` free)

---

### 3.9 Tab 5: Tổng Quan (DashboardScreen)

```
┌─────────────────────────────────┐
│ Tổng Quan  [Đông Xuân 2026 ▼]  │
├─────────────────────────────────┤
│ [HERO STATS — 2×2 grid]        │
│ ┌──────────┬──────────┐        │
│ │ 45 phiếu│ 98,5 tấn │        │
│ ├──────────┼──────────┤        │
│ │ 512 tr đ │ 62 tr nợ │        │
│ └──────────┴──────────┘        │
├─────────────────────────────────┤
│ [PHÂN BỔ GIỐNG LÚA]            │
│ ST25:   ████████ 45%            │
│ OM18:   ██████   30%            │
│ Khác:   ████     25%            │
├─────────────────────────────────┤
│ [THANH TOÁN]                   │
│ Đã thu: 450tr   Còn nợ: 62tr   │
│ [████████████████░░░] 87%       │
├─────────────────────────────────┤
│ [KPI CHI TIẾT]                 │
│ Giá TB/kg:    5.200 đ          │
│ KG TB/bao:    49.8 kg          │
│ Độ ẩm TB:     13.2%            │
│ Tạp chất TB:  0.8%             │
│ Lúa khô: 38   Lúa ướt: 7      │
├─────────────────────────────────┤
│ [🤖 AI INSIGHTS]               │
│ "Vụ này KG/bao cao hơn TB 5%  │
│  ST25 chiếm 45%..."            │  Markdown
│                    [Làm mới]   │
└─────────────────────────────────┘
```

---

## 4. Data Models (Dart / Drift)

### 4.1 Card (Phiếu cân)

```dart
// Table: cards
class Card {
  int id;                       // local auto-increment PK
  String? firestoreId;          // Firestore sync key
  DateTime date;
  DateTime lastModified;

  // Người bán
  String name;                  // Tên người bán / ruộng *
  String? phone;
  String? cccd;
  String? fieldAddress;         // Địa chỉ ruộng

  // Thương lái
  String traderName;

  // Lúa
  String riceVariety;           // ST25, OM18...
  String seasonLabel;           // Đông Xuân 2026

  // Cân lúa (aggregate tự tính từ WeightEntry)
  double totalWeight;           // Tổng cân (kg)
  double bagWeight;             // Tổng bì (kg)
  double impurityWeight;        // Tổng tạp chất (kg)
  double netWeight;             // KG thực
  double moisturePercent;       // Độ ẩm (%)
  int bagCount;                 // Số bao

  // Cách tính bì
  bool bagMethodIsSampling;     // false=Cách A, true=Cách B
  int bagSampleCount;
  double bagSampleTotalWeight;

  // Cách tính tạp chất
  bool impurityIsPercent;       // false=kg, true=%

  // Mode nhập số
  String weightInputMode;       // "SMALL" | "LARGE"

  // Tài chính
  double pricePerKg;
  double totalAmount;
  double depositAmount;
  double paidAmount;
  double remainingAmount;
  bool isPaid;

  // GPS (optional)
  double? latitude;
  double? longitude;
}
```

### 4.2 WeightEntry (Từng lần cân)

```dart
// Table: weight_entries
class WeightEntry {
  int id;
  int cardId;                   // FK → Card (CASCADE DELETE)
  double weight;                // kg nhập
  double bagWeight;             // bì lần này
  double impurityWeight;        // tạp chất lần này
  double netWeight;             // thực lần này
  DateTime timestamp;
  String? firestoreId;
}
```

### 4.3 Transaction (Giao dịch tiền)

```dart
// Table: transactions
enum TransactionType { DEPOSIT, PAYMENT, REFUND }

class Transaction {
  int id;
  int cardId;                   // FK → Card (CASCADE DELETE)
  double amount;
  TransactionType type;
  String? description;
  DateTime date;
  String? firestoreId;
}
```

### 4.4 RicePrice (Giá thị trường — cache)

```dart
// Table: rice_prices
class RicePrice {
  String id;
  String variety;               // ST25, OM18...
  double priceMin;              // đ/kg
  double priceMax;
  double priceAvg7d;
  String region;                // ĐBSCL, Cần Thơ...
  DateTime updatedAt;
  String trend;                 // UP | DOWN | STABLE
  String riceType;              // lúa khô | lúa ướt | gạo tẻ | nếp
  String? source;
}
```

### 4.5 NewsArticle (Tin tức cache)

```dart
// Table: news_articles
class NewsArticle {
  String id;                    // URL hash
  String title;
  String? description;
  String url;
  String? imageUrl;
  String sourceName;
  DateTime publishedAt;
  DateTime cachedAt;
  String category;              // Lúa | Gạo | Thị trường | Khác
}
```

### 4.6 ChatMessage (Lịch sử AI)

```dart
// Table: chat_messages
enum MessageRole { user, assistant }

class ChatMessage {
  int id;
  String content;
  MessageRole role;
  DateTime timestamp;
  bool isError;
}
```

### 4.7 SeasonStats (Aggregate — không lưu DB)

```dart
class SeasonStats {
  final String season;
  final int cardCount;
  final double totalNetWeight;
  final double totalRevenue;
  final double totalPaid;
  final double totalRemaining;
  final double avgPricePerKg;
  final double avgMoisture;
  final int totalBags;
  final double totalImpurity;
  final int wetCardCount;        // độ ẩm > 14%
  final int dryCardCount;        // độ ẩm <= 14%

  double get avgKgPerBag => totalBags > 0 ? totalNetWeight / totalBags : 0.0;
  bool get isEmpty => cardCount == 0;
}
```

---

## 5. Cấu trúc thư mục Flutter (đề xuất)

```
lib/
├── main.dart
├── app.dart                        # MaterialApp, theme, router setup
│
├── core/
│   ├── theme/
│   │   ├── app_colors.dart         # Tất cả color tokens
│   │   ├── app_typography.dart     # TextStyle system
│   │   └── app_theme.dart          # ThemeData light + dark
│   ├── constants/
│   │   ├── app_constants.dart      # API URLs, cache TTL...
│   │   └── rice_varieties.dart     # Danh sách giống lúa hardcoded
│   ├── utils/
│   │   ├── rice_calculator.dart    # Công thức cân lúa (single source of truth)
│   │   ├── formatters.dart         # Format tiền, kg, ngày, phần trăm
│   │   └── season_helper.dart      # Suggest + list vụ mùa
│   └── widgets/
│       ├── app_card.dart           # Card base widget
│       ├── app_chip.dart           # Chip variants
│       ├── shimmer_widget.dart     # Chỉ dùng cho network data
│       ├── empty_state.dart        # Empty state widget
│       └── offline_banner.dart     # Banner khi mất mạng
│
├── features/
│   ├── weighing/                   # Tab 1: Cân Lúa
│   │   ├── data/
│   │   │   ├── models/             # Card, WeightEntry, Transaction
│   │   │   ├── dao/                # Drift DAOs
│   │   │   └── repository/
│   │   ├── presentation/
│   │   │   ├── screens/
│   │   │   │   ├── card_list_screen.dart
│   │   │   │   ├── card_detail_screen.dart
│   │   │   │   └── weight_input_screen.dart
│   │   │   ├── widgets/
│   │   │   │   ├── card_item.dart
│   │   │   │   ├── weight_grid.dart          # Bảng 3 cột
│   │   │   │   ├── live_metrics_card.dart
│   │   │   │   ├── financial_summary_card.dart
│   │   │   │   ├── create_card_bottom_sheet.dart
│   │   │   │   └── weigh_options_sheet.dart
│   │   │   └── providers/          # Riverpod providers / notifiers
│   │
│   ├── market/                     # Tab 2: Bảng Giá
│   │   ├── data/
│   │   └── presentation/
│   │       ├── screens/market_screen.dart
│   │       └── widgets/
│   │           ├── price_card.dart
│   │           └── price_chart.dart    # fl_chart line chart
│   │
│   ├── news/                       # Tab 3: Tin Tức
│   │   ├── data/
│   │   └── presentation/
│   │       ├── screens/news_screen.dart
│   │       └── widgets/news_item.dart
│   │
│   ├── ai_chat/                    # Tab 4: AI Chat
│   │   ├── data/
│   │   └── presentation/
│   │       ├── screens/ai_chat_screen.dart
│   │       └── widgets/
│   │           ├── message_bubble.dart
│   │           └── chat_input_bar.dart
│   │
│   └── dashboard/                  # Tab 5: Tổng Quan
│       ├── data/
│       └── presentation/
│           ├── screens/dashboard_screen.dart
│           └── widgets/
│               ├── season_stats_hero.dart
│               ├── variety_chart.dart
│               ├── payment_progress.dart
│               └── ai_insights_card.dart
│
├── database/
│   ├── app_database.dart           # Drift DB setup
│   ├── app_database.g.dart         # Generated (don't edit)
│   └── migrations/
│
└── services/
    ├── firestore_service.dart      # Read rice_prices từ Firestore
    ├── ai_service.dart             # OpenRouter API calls
    ├── rss_service.dart            # Parse RSS XML feed
    └── tts_service.dart            # flutter_tts wrapper
```

---

## 6. Tech Stack Flutter (đề xuất)

| Lớp | Package | Lý do |
|---|---|---|
| State Management | `riverpod` + `hooks_riverpod` | Scalable, testable, null-safe |
| Local DB | `drift` | Type-safe SQLite, stream realtime |
| Navigation | `go_router` | ShellRoute 5 tab, deep link |
| Network | `dio` | Interceptors, retry, timeout |
| Firebase | `firebase_core`, `cloud_firestore` | Sync giá lúa read-only |
| Charts | `fl_chart` | Line, pie, bar chart |
| Markdown | `flutter_markdown` | Render AI response có bảng |
| Voice STT | `speech_to_text` | Voice input AI chat |
| TTS | `flutter_tts` | Đọc số khi nhập cân |
| RSS | `webfeed` hoặc `dart:xml` | Parse RSS feed |
| PDF | `pdf` + `printing` | Xuất phiếu cân |
| Share | `share_plus` | Chia sẻ PDF file |
| Image | `cached_network_image` | Thumbnail tin tức |
| Browser | `url_launcher` | Mở bài báo Chrome |
| DI | `get_it` + `injectable` | Service locator |
| Fonts | `google_fonts` | Be Vietnam Pro + Noto Sans |
| Animations | `flutter_animate` | Micro-animation dễ dùng |
| Connectivity | `connectivity_plus` | Offline banner |
| Permission | `permission_handler` | Mic, location |

---

## 7. Firebase / Backend

### 7.1 Firestore Schema (Read-only)

```
rice_prices/{priceId}:
  variety:   "ST25"
  priceMin:  8500
  priceMax:  9200
  priceAvg7d: 8850
  region:    "ĐBSCL"
  updatedAt: Timestamp
  trend:     "UP"      // UP | DOWN | STABLE
  riceType:  "lúa khô" // lúa khô | lúa ướt | gạo tẻ | nếp
  source:    "Google Sheet Cân Lúa"
```

### 7.2 Google Apps Script Flow

```
Google Sheet (admin nhập giá)
  └── On Edit Trigger
      └── Apps Script
          └── Firestore Admin SDK
              └── rice_prices collection
                  └── Flutter app reads (cache → Drift)
```

Cấu trúc Sheet:
| A: Ngày | B: Giống | C: Giá Thấp | D: Giá Cao | E: Xu Hướng | F: Nguồn | G: Khu vực | H: Loại |

### 7.3 Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /rice_prices/{doc} {
      allow read: if true;          // Public read
      allow write: if false;        // Apps Script only via Admin SDK
    }
  }
}
```

---

## 8. Offline Strategy

| Feature | Offline | Behavior |
|---|---|---|
| Danh sách phiếu cân | ✅ Full | Drift — load tức thì, không delay |
| Nhập cân | ✅ Full | Drift — lưu local ngay |
| Chi tiết phiếu | ✅ Full | Drift — tức thì |
| Dashboard stats | ✅ Full | Tính aggregate từ Drift local |
| Bảng giá lúa | ⚡ Cache | Drift cache + badge "Đã lưu · Xh trước" |
| Tin tức | ⚡ Cache | Drift cache TTL 1h + badge |
| AI Chat | ❌ Cần mạng | Toast "Cần internet để dùng AI" |

---

## 9. Danh sách Giống Lúa mặc định

```dart
const List<String> kRiceVarieties = [
  'ST25',
  'OM5451',
  'OM18',
  'Đài Thơm 8',
  'Jasmine 85',
  'IR50404',
  'Nàng Hoa 9',
  'Lúa Nếp',
  'RVT',
  'OM4900',
  'OM9582',
  'OM6976',
  'Khác',
];
```

---

## 10. Logic Vụ Mùa ĐBSCL

| Vụ | Gieo sạ | Thu hoạch | Ghi chú |
|---|---|---|---|
| Đông Xuân | Tháng 11–12 | Tháng 2–4 | Vụ chính, năng suất cao nhất |
| Hè Thu | Tháng 4–5 | Tháng 7–8 | Vụ phụ |
| Thu Đông | Tháng 8–9 | Tháng 11–12 | Không phải nơi nào cũng làm |

```dart
String suggestSeason(DateTime date) {
  final month = date.month;
  final year = date.year;
  if (month == 11 || month == 12) return 'Đông Xuân ${year + 1}';
  if (month >= 1 && month <= 4) return 'Đông Xuân $year';
  if (month >= 5 && month <= 8) return 'Hè Thu $year';
  return 'Thu Đông $year';
}

List<String> getSeasonOptions(DateTime date) {
  final dxYear = date.month >= 11 ? date.year + 1 : date.year;
  return [
    'Đông Xuân $dxYear',
    'Hè Thu ${date.year}',
    'Thu Đông ${date.year}',
  ];
}
```

---

## 11. UX Guidelines chi tiết

### Nhập số kg
- **Mode SMALL:** 3 ký tự → `503` = `50.3 kg`
- **Mode LARGE:** 4 ký tự → `1503` = `150.3 kg`
- Bàn phím số (TextInputType.number), không cần dấu chấm
- Highlight ô đang nhập với border accent

### Format số hiển thị

```dart
// Tiền — thông minh
512_360_400 → "512 tr đ"
12_360_400  → "12,3 tr đ"
5_360_400   → "5.360.400 đ"
360_400     → "360.400 đ"

// Khối lượng
2450.5   → "2.450,5 kg"
98500.0  → "98,5 tấn"

// Giá
8500 → "8.500 đ/kg"
```

### Màu trạng thái thanh toán
- `isPaid = true` → chip xanh "Đã thanh toán ✓"
- `remainingAmount > 0` → chip đỏ "Còn nợ X đ"
- `remainingAmount == 0 && !isPaid` → chip xanh nhạt "Đủ tiền"

### Swipe actions
- Swipe left → Xóa (nền đỏ, icon trash)
- Swipe right → Sửa nhanh (nền xanh, icon edit)

### Empty states

| Màn | Nội dung |
|---|---|
| Danh sách phiếu | Minh họa nông dân + "Tạo phiếu cân đầu tiên" |
| Bảng giá (offline) | "Đang tải dữ liệu thị trường..." |
| Tin tức (offline) | "Không có kết nối internet" |
| AI Chat (lần đầu) | "Xin chào! Tôi là AI khuyến nông. Hỏi tôi bất kỳ điều gì về lúa gạo 🌾" |
| Dashboard (chưa có phiếu) | "Chưa có dữ liệu vụ này. Tạo phiếu cân để bắt đầu!" |

---

## 12. Những tính năng KHÔNG làm trong Flutter version mới

| Loại bỏ | Lý do |
|---|---|
| Firebase Auth / Đăng nhập | Đơn giản hóa, nông dân không cần account |
| Role TRADER | Chỉ phục vụ nông dân |
| QR Handshake | Không còn multi-role |
| Google Maps / Bản đồ thu mua | Phức tạp, không phải core feature |
| OpenWeatherMap / Thời tiết | Đã bỏ theo yêu cầu |
| Firestore sync 2 chiều phiếu cân | Chi phí + phức tạp, chỉ sync giá lúa |
| Multi-language Khmer / Lào / Trung | Phase sau |
| Premium gate / PayWall | Phase sau |
| Role request flow | Không còn TRADER |
| Sổ giao dịch TRADER | Không còn TRADER |

---

## 13. Kỹ thuật quan trọng (Flutter-specific)

1. **Drift migrations:** Mỗi thay đổi schema PHẢI có `MigrationStrategy` — không được xóa column tùy tiện, user cũ có dữ liệu
2. **Riverpod StreamProvider:** Subscribe Drift Stream realtime cho danh sách phiếu cân
3. **IME Padding:** `Scaffold(resizeToAvoidBottomInset: true)` + `SingleChildScrollView` ở tất cả form có input
4. **Background sync:** `workmanager` package — pull giá lúa từ Firestore khi app background
5. **PDF export:** `pdf` + `path_provider` lưu temp + `share_plus` chia sẻ qua system share sheet
6. **TTS tiếng Việt:** `flutter_tts` set `language: "vi-VN"` — bắt buộc test trên device thật (emulator thiếu voice pack)
7. **Firestore cost:** Cache local Drift TTL 30 phút, tránh over-read khi online

---

## 14. Roadmap Flutter — 6 Sprint

### Sprint 1 — Foundation (2 tuần)
- [ ] Setup project, theme system (colors, typography, dark mode)
- [ ] go_router ShellRoute với 5 tab
- [ ] Drift database: Card, WeightEntry, Transaction + migrations
- [ ] CardListScreen + CardItem widget
- [ ] CreateCardBottomSheet (tất cả fields)
- [ ] RiceCalculator logic + unit tests

### Sprint 2 — Nhập cân (1 tuần)
- [ ] CardDetailScreen (full scroll, tất cả sections)
- [ ] WeightInputScreen (bảng 3 cột paging, auto-confirm)
- [ ] WeighOptionsBottomSheet (TTS, font size, bì mode)
- [ ] Financial summary + payment recording bottom sheet
- [ ] flutter_tts integration (vi-VN)
- [ ] DeletedCards (thùng rác + restore)

### Sprint 3 — Thị trường (1 tuần)
- [ ] Firebase + Firestore setup (read-only rice_prices)
- [ ] MarketScreen + PriceCard + fl_chart (7-day line)
- [ ] Filter chip (Lúa/Gạo/Nếp) + region dropdown
- [ ] Drift cache + badge "Đã lưu · Xh trước"
- [ ] Pull-to-refresh + shimmer khi fetch network

### Sprint 4 — Tin tức & AI (1 tuần)
- [ ] RSS service + NewsScreen + cached_network_image
- [ ] AiChatScreen + OpenRouter API (dio)
- [ ] ChatMessage Drift storage (persistent session)
- [ ] flutter_markdown render (bảng, heading, bold, code)
- [ ] speech_to_text voice input + pulse animation
- [ ] agronomy_knowledge.json inject vào system prompt

### Sprint 5 — Dashboard (1 tuần)
- [ ] SeasonStats aggregate từ Drift (SQL GROUP BY)
- [ ] DashboardScreen: hero 2x2 grid, variety bar chart, payment progress
- [ ] AI Insights generation + local cache (không gọi lại khi data không đổi)
- [ ] PDF export phiếu cân (pdf + printing)
- [ ] Season picker dropdown

### Sprint 6 — Polish & Release (1 tuần)
- [ ] Dark mode hoàn thiện 100% màn hình
- [ ] flutter_animate micro-animations (card appear, number change, tab expand)
- [ ] connectivity_plus offline banner
- [ ] Empty states có minh họa tất cả màn
- [ ] Performance audit (60fps target, không jank)
- [ ] WorkManager background sync giá lúa

---

*Tài liệu được tổng hợp từ dự án Android Kotlin Cân Lúa Mobile Phase 2.10 — Tháng 6/2026*
*Tác giả: Gia Thịnh*
