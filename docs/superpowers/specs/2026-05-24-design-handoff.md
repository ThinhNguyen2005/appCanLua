# Cân Lúa — Design Handoff Document

**Phiên bản app**: master @ b28b4ae (2026-05-24)
**Tạo cho**: Design team redesign UI/UX
**Mục đích**: Trình bày toàn bộ chức năng, màn hình, design system hiện tại, pain points, và brief redesign

---

## Mục lục

1. [Brief & bối cảnh sản phẩm](#1-brief--bối-cảnh-sản-phẩm)
2. [Personas & 2 vai trò](#2-personas--2-vai-trò)
3. [Sitemap & navigation flow](#3-sitemap--navigation-flow)
4. [Auth flow](#4-auth-flow)
5. [Tab SCALE — Cân Lúa](#5-tab-scale--cân-lúa)
6. [Tab MARKET — Thị trường](#6-tab-market--thị-trường)
7. [Tab AI CHAT](#7-tab-ai-chat)
8. [Tab ACCOUNT — Hồ sơ](#8-tab-account--hồ-sơ)
9. [Trader-only screens](#9-trader-only-screens)
10. [Sub-screens (Settings, Premium, QR, Sync, Deleted)](#10-sub-screens)
11. [Design system hiện tại](#11-design-system-hiện-tại)
12. [Pain points & cơ hội redesign](#12-pain-points--cơ-hội-redesign)
13. [Design direction gợi ý](#13-design-direction-gợi-ý)

---

## 1. Brief & bối cảnh sản phẩm

### Sản phẩm là gì
**Cân Lúa** (`com.GiaThinh.canlua`) — app Android số hoá quy trình cân lúa giữa **nông dân** và **thương lái** tại đồng bằng sông Cửu Long. Thay thế sổ tay/giấy bằng phiếu điện tử, hỗ trợ chốt phiếu bằng QR, đồng bộ qua Firestore, tra cứu giá lúa & tin tức, tư vấn AI canh tác.

### Đặc tính cốt lõi
- **Offline-first**: Mọi thao tác viết vào Room SQLite trước, đồng bộ Firestore khi có mạng. Mất sóng vẫn cân — đặc thù ruộng vùng sâu.
- **Đa vai trò**: 1 app, 2 perspective (FARMER xem doanh thu, TRADER xem chi phí).
- **Đa ngôn ngữ**: vi, en, km (Khmer), lo (Lào), zh-CN (Trung) — phục vụ vùng biên giới Tây Nam.
- **Đa font scale**: 4 mức 0.9x→1.2x cho mắt người lớn tuổi.

### Tech stack ngắn gọn (để design team biết constraint)
- Android only, Jetpack Compose 2025.09, minSdk 24, targetSdk 36
- Material 3 + dynamic color (Android 12+ wallpaper-based)
- Room 2.8.1 (local DB) + Firestore (cloud sync) + Crashlytics + Performance Monitoring
- Google Maps SDK, Camera X + ML Kit (QR scan), Markwon (markdown render), Vico (chart)
- Auth: Firebase Email/Password, Phone OTP (+84), Google Sign-In qua Credential Manager
- AI: OpenRouter (Gemini Flash free), RAG-lite với top giá lúa + knowledge base nông học

### Target user
- **Nông dân (FARMER)**: 35–70 tuổi, tay chai/thô, có thể đeo găng, mắt yếu, dùng điện thoại 1 tay khi cân giữa nắng. Quen với cân đồng hồ và sổ giấy.
- **Thương lái (TRADER)**: 25–55 tuổi, lưu động bằng xe máy/ghe, cần xác nhận giao dịch nhanh, theo dõi công nợ nhiều nông dân, đăng giá thu mua công khai.
- **Cả hai**: gốc Việt-Khmer-Lào, học vấn THCS-THPT, kỹ năng số trung bình, không kiên nhẫn với form dài hay UI phức tạp.

### Brand vibe gợi ý cho redesign
- Nông nghiệp **hiện đại nhưng không "techy lạnh"** — không gradient neon, không glassmorphism mờ ảo, không dark mode mặc định.
- **Đáng tin cậy** — đây là sổ kế toán mini, lỗi 1 con số = mất tiền thật. Dùng typography chắc nịch, contrast cao, ít animation phù phiếm.
- **Quen thuộc** — ngôn ngữ ruộng đồng, màu lúa-vàng-đất, không né tránh hình ảnh nông sản thật.

### Constraints redesign
| Constraint | Lý do |
|---|---|
| Touch target ≥ 48dp, ưu tiên 56dp cho action chính | Tay đeo găng, ngón to |
| Contrast WCAG AA tối thiểu, AAA cho số liệu tài chính | Đọc dưới nắng ruộng |
| Không phụ thuộc internet | Mất sóng vẫn cân, mọi lỗi mạng phải đẹp |
| Layout không vỡ ở font 1.2x | Font scale settings |
| Hỗ trợ 5 locale, không hardcode text | Vùng biên giới |
| Tôn trọng dynamic color M3 nhưng giữ brand green | User Android 12+ có theme wallpaper |
| Hỗ trợ một-tay-trái và một-tay-phải | Tay kia đang bê bao lúa |

---

## 2. Personas & 2 vai trò

### FARMER — Anh Tám, 52 tuổi, 3 hecta ST25 ở Cần Thơ
- **Job-to-be-done**: "Khi thương lái tới cân lúa, tôi muốn ghi lại đúng số ký, đúng giá, đúng tiền cọc/đã trả, để không bị thiệt ở khâu thanh toán cuối vụ."
- **Trên app**: Tạo phiếu mới mỗi đợt thương lái tới → nhập từng bao → khoá phiếu bằng QR cho thương lái quét → theo dõi công nợ cho đến khi nhận đủ tiền.
- **Quan tâm**: tổng cân, còn nợ, doanh thu vụ này so vụ trước, thương lái nào mua nhiều nhất.
- **KHÔNG quan tâm**: đăng giá rao, đi tìm nông dân khác.

### TRADER — Anh Tư, 38 tuổi, thu mua lúa cho nhà máy gạo
- **Job-to-be-done**: "Khi đi thu mua ở nhiều ruộng, tôi muốn xác nhận giao dịch ngay tại chỗ, theo dõi đã trả ai chưa, và rao giá hôm nay cho các nông dân biết."
- **Trên app**: Quét QR của nông dân để xác nhận chốt phiếu → ghi lại đã trả bao nhiêu → xem bản đồ các ruộng chưa thu → đăng giá rao công khai trên Market.
- **Quan tâm**: tổng chi, công nợ với từng nông dân, cơ cấu giống lúa đã thu, bản đồ điểm thu mua.
- **KHÔNG quan tâm**: doanh thu vụ.

### Lưu ý cho design team
- **3 tab dùng chung** (Scale, Market, AI Chat) — mỗi screen tự đọc `profile.role` để render variant. Đừng thiết kế 2 app riêng, hãy thiết kế **1 hệ thống có 2 chế độ**.
- Đối xứng số liệu: FARMER thấy "Doanh thu" thì TRADER thấy "Đã chi". FARMER thấy "Top thương lái mua nhiều" thì TRADER thấy "Cơ cấu giống lúa đã thu".
- **TRADER có thêm tab thứ 5** — Bản đồ. Bottom nav phải scale từ 4 → 5 item gọn gàng.

---

## 3. Sitemap & navigation flow

### Cấu trúc 2-level NavHost
```
RootNavHost (MainActivity)
├── splash                  — splash 1.5s + animation
├── login                   — AuthScreen
├── profile_setup           — first-time user fill name/role
├── role_request            — FARMER xin nâng cấp TRADER
└── main?cardId={cardId}    — MainScreen mount AppNavHost bên trong
    │                         (cardId optional, dùng cho deep link)
    └── AppNavHost
        ├── (Bottom nav, 4 cho FARMER / 5 cho TRADER)
        │   ├── scale              — CardListScreen
        │   ├── market             — MarketScreen
        │   ├── ai_chat            — AiChatScreen (có drawer trái)
        │   ├── trader_map         — RiceMapScreen (TRADER only)
        │   └── account / trader_profile — Farmer/TraderProfileScreen
        │
        └── (Sub-routes, push lên bottom-nav)
            ├── card_detail/{cardId}
            ├── weight_input/{cardId}
            ├── qr_generate/{cardId}
            ├── qr_scan
            ├── settings
            ├── sync_status
            ├── premium
            ├── deleted_cards
            ├── trader_transactions
            └── trader_history
```

### Deep link
- App link `https://canluavn.web.app/share/{cardId}` → mở MainActivity → AppNavHost forward sang `card_detail/{cardId}`. Yêu cầu `assetlinks.json` ở `public/.well-known/`.

### Transition mặc định
- FadeScale 250ms cho mọi push/pop.
- Splash → next dùng fade.

### Auth state là single source of truth
- `AuthViewModel.uiState` quan sát `FirebaseAuth.AuthStateListener`.
- `MainActivity` `LaunchedEffect` đẩy nav theo `isSignedIn` + `needsProfileSetup`. **KHÔNG có** logic "đăng xuất → recreate Activity".

### Diagram luồng auth (ASCII)
```
                ┌──────────┐
                │  Splash  │
                └────┬─────┘
                     │ 1.5s
        ┌────────────┴────────────┐
   isSignedIn=false          isSignedIn=true
        │                          │
        ▼                          ▼
   ┌─────────┐            needsProfileSetup?
   │ AuthScrn│           ┌────┴────┐
   └────┬────┘          true     false
        │ login          │         │
        ▼                ▼         ▼
  isSignedIn? → ProfileSetup → main (4-5 tab)
                    │
                    │ "Thương lái"
                    ▼
              RoleRequest (admin duyệt)
```

---

## 4. Auth flow

### 4.1. AuthScreen (`login`)

**File**: `ui/screen/AuthScreen.kt`
**Mục đích**: Cổng vào duy nhất. Hỗ trợ 3 phương thức: Email/Password, OTP +84, Google Sign-In.

**Layout (ASCII)**
```
+─────────────────────────────────+
|  gradient: GreenSurface→Surface |
|                                 |
|         [Logo Eco 46dp]         |
|       Cân Lúa (headline)        |
|      Đăng nhập / đăng ký        |
|                                 |
| ┌── Card r28 ──────────────────┐|
| │ [📧/📱] Email hoặc SĐT       │|
| │ [🔒]    Mật khẩu (ẩn khi SĐT)│|
| │ [#]     Mã OTP (chỉ khi gửi) │|
| │ Quên MK?       Đăng ký/login │|
| │ ┌── Nút chính xanh 54dp ──┐  │|
| │ │ Đăng nhập / Gửi OTP /  │  │|
| │ │ Xác nhận OTP            │  │|
| │ └──────────────────────────┘  │|
| │ ───── HOẶC ─────              │|
| │ ┌─[G] Tiếp tục với Google ─┐  │|
| │ └─────────────────────────────┘│|
| └────────────────────────────────┘|
|   Điều khoản & Bảo mật            |
+───────────────────────────────────+
```

**Data hiển thị**
- title/subtitle từ `R.string.auth_title/subtitle`
- Placeholder field 1: auto-detect "email" vs "số điện thoại" theo regex `looksLikePhone(input)` → đổi keyboard, đổi icon, đổi autofill hint
- Label nút chính: 4 trạng thái tùy `isRegister + isPhoneMode + showOtpField`
- Error/info qua Snackbar

**Interactions**
- Gõ field 1: silent detect email/phone
- Toggle login/register: text link
- "Quên MK" → snackbar "Đang phát triển" *(dead-end)*
- Submit email: validate (regex email, password ≥ 6) → `signInEmail`/`registerEmail` (register hard-code role=FARMER)
- Submit phone: lần 1 gửi OTP qua Firebase PhoneAuth (timeout 60s, normalize `0xxx`→`+84xxx`); lần 2 verify 6 chữ số
- Google: bottom sheet Credential Manager → idToken → Firebase
- "Điều khoản & Bảo mật": snackbar "Đang phát triển" *(dead-end)*

**States**
- Loading: nút disable + CircularProgress trắng 24dp trong nút
- Error: snackbar tiếng Việt
- Success: AuthViewModel set `isSignedIn=true` → MainActivity điều hướng
- Offline: Firebase throw → raw message trong snackbar (xấu UX)

**Dialogs/Sheets**: Google Credential Manager bottom sheet (không control style được), Google "Save password" prompt

**UX issues**
1. "Quên MK" và "Điều khoản" đều là dead-end snackbar → tạo cảm giác app chưa hoàn chỉnh ngay lần đầu
2. Auto-detect phone vs email âm thầm → password field biến mất không báo trước
3. Toggle login/register chỉ là text link bodySmall, hierarchy yếu
4. Form không có label visible, chỉ placeholder — đã gõ thì mất ngữ cảnh
5. Error qua snackbar, không gắn vào field → khó biết field nào sai
6. Register hard-code role=FARMER → thương lái phải đi đường vòng qua RoleRequest
7. Không có confirm password khi đăng ký, không có password strength

---

### 4.2. ProfileSetupScreen (`profile_setup`)

**File**: `ui/screen/ProfileSetupScreen.kt`
**Mục đích**: Thu thập thông tin tối thiểu sau lần đăng nhập đầu.

**Layout**
```
+──────────────────────────────────+
|  Hoàn tất hồ sơ (headlineMedium) |
|  Cho biết bạn là ai (bodyMedium) |
|                                  |
| ┌─ 2 thẻ vai trò ngang 140dp ──┐ |
| │ [🌾 Agriculture]  [🏪 Storefront] │
| │   Nông dân         Thương lái  │ |
| │ (selected: border 2dp + tint)  │ |
| └────────────────────────────────┘ |
| hint "có thể đổi vai trò sau"     |
|                                   |
|  [ Họ và tên * ]                  |
|  [ Số điện thoại ]                |
|  [ Khu vực ]                      |
|  [ CCCD ]                         |
|                                   |
|  ┌── ✓ Hoàn tất thiết lập ──┐    |
|  └───────────────────────────┘    |
+───────────────────────────────────+
```

**Data**: 4 input rỗng, role mặc định FARMER (TRADER chọn được nhưng thực tế cần admin duyệt qua RoleRequest)

**Interactions**: Tap role card → đổi state · Submit enable khi `name` không blank → save → callback `onComplete` → root NavHost đẩy sang `main`

**States**
- Loading: không có indicator
- Error save: KHÔNG có UI hiển thị
- Success: navigate sang main

**UX issues**
1. Chọn TRADER trên ProfileSetup là **UX dối** — UI cho phép nhưng backend không grant quyền (cần admin duyệt riêng)
2. STAFF enum tồn tại nhưng không show
3. Không pre-fill `name`/`phone` từ Google account dù DB đã có
4. Không validate phone/CCCD/region
5. Không back button, không "skip for now"
6. 4 OutlinedTextField cùng kích thước, không group "Bắt buộc" vs "Tùy chọn"

---

### 4.3. RoleRequestScreen (`role_request`)

**File**: `ui/screen/RoleRequestScreen.kt`
**Mục đích**: Form xin nâng FARMER → TRADER. Admin duyệt thủ công qua Firestore.

**Layout**
```
+── TopAppBar "Đăng ký Thương lái" [<] ─+
|                                        |
| ┌── StatusCard (nếu đã submit) ─────┐ |
| │ [icon] Trạng thái: PENDING/        │ |
| │        APPROVED/REJECTED           │ |
| └────────────────────────────────────┘ |
|                                        |
|  Tại sao cần duyệt? (titleMedium)     |
|  Đoạn giải thích quyền TRADER...      |
|                                        |
|  [ Tên doanh nghiệp/cửa hàng * ]      |
|  [ Mã số thuế (tùy chọn) ]            |
|  [ Số điện thoại liên hệ * ]          |
|  [ Lý do (≥10 ký tự) * ] multi-line   |
|                                        |
|  (error text màu error)               |
|                                        |
|  ┌── 📨 Gửi yêu cầu ─────────────┐    |
|  └──────────────────────────────┘    |
|  "Đã gửi yêu cầu. 24-48h..."          |
+────────────────────────────────────────+
```

**States**: PENDING (nút lock), APPROVED ("hãy đăng nhập lại"), REJECTED (có note admin + cho phép submit lại)

**UX issues**
1. Toàn bộ text **hardcode tiếng Việt** trong code → vi phạm i18n (5 locale)
2. APPROVED bắt user logout/login thủ công thay vì refresh token tự động
3. Form layout không nhất quán với ProfileSetup (radius 12 vs 14, button radius 27 vs 28)
4. Không hiển thị counter real-time cho "Lý do ≥ 10 ký tự"

---

## 5. Tab SCALE — Cân Lúa

Đây là tab cốt lõi nhất, chiếm 60% thời gian dùng app. Gồm 4 màn chính.

### 5.1. CardListScreen (`scale`)

**File**: `ui/screen/CardListScreen.kt`
**Mục đích**: List phiếu cân theo ngày, entry tạo phiếu mới, vào detail.

**Layout (ASCII)**
```
+──────────────────────────────────────────+
| ┌── SummaryCard (sticky đầu list) ─────┐ |
| │ Hôm nay · N phiếu  ◯ sync             │ |
| │ 123.4 kg            1.234.000 đ        │ |
| └─────────────────────────────────────────┘|
| ┌── FilterBar ─────────────────────────┐  |
| │ [Bộ lọc (2)]  [ST25 ×]  [ĐX 2026 ×]  │  |
| └────────────────────────────────────────┘ |
|                                            |
|   24/05/2026                              |
|  ┌── CardItem (swipe → delete) ────────┐  |
|  │ 🔒 Nguyễn Văn A      24/05/2026  ›  │  |
|  │ 🌾 ST25    💧 18.2%                  │  |
|  │ 1.234 kg                  9.870.000đ│  |
|  │ 25 bao              Còn 4.870.000   │  |
|  │ [✓ QR đã xác thực]                  │  |
|  └────────────────────────────────────────┘|
|   23/05/2026                              |
|   ...                                      |
+────────────────────────────────────────────+
                                  [ + ] FAB
```

**Data hiển thị**
- SummaryCard: chỉ thống kê **HÔM NAY** (`card.date == today`), gồm count + totalKg + totalAmount + sync pulse
- CardItem: `name` (FARMER mode = farmer's friend; TRADER mode = farmer đối tác), badge khoá nếu `isLocked`, `riceVariety`, `moisturePercent`, `totalWeight`, `bagCount`, `totalAmount` (xanh), `remainingAmount` (vàng nếu >0), badge "QR đã xác thực"
- FilterBar: count active filter, chip filter có nút × clear

**Khác biệt FARMER vs TRADER**
- UI giống hệt nhau
- Chỉ khác **logic mapping field** khi tạo phiếu:
  - FARMER mode: `card.name = profile.name` (nông dân), input dialog "thương lái" → `card.traderName`
  - TRADER mode: `card.name = counterparty` (nông dân), `card.traderName = profile.name` (thương lái), `traderPhone = profile.phone`
- Label "Hôm nay", "Doanh thu" giữ nguyên (không swap theo role) — **có thể là vấn đề** (TRADER nên thấy "Đã chi")

**Interactions**
- Tap CardItem → `card_detail/{id}`
- Swipe end→start (chỉ khi unlocked) → confirm dialog xóa
- FAB Add → check premium quota → nếu vượt → `PremiumQuotaDialog`; nếu OK → `CreateCardDialog`
- Pull-to-refresh xanh → reload + sync
- FAB hide khi scroll xuống (`listState.isScrollingUp`)

**States**
- Loading: `SkeletonList(count=3)` fade
- Empty: `CardListEmptyState` (icon Scale 72dp + "Chưa có phiếu" + subtitle)
- Success: grouped theo ngày, header label nhỏ TextSecondary
- Error/Offline: chỉ qua `SyncStatusPulse` trong SummaryCard

**Dialogs/Sheets**
1. **CreateCardDialog** (full-width 97%, height 90%): Header GreenSurface + owner line → Section "Thông tin lô hàng" [RiceVarietyDropdown | SeasonDropdown] → counterparty name → counterparty phone → Section "Giá & Thanh toán" [Độ ẩm | Đơn giá] → Tiền cọc. Submit "Tạo phiếu" GreenPrimary, disabled cho đến khi name + variety không trống
2. **DeleteCardConfirmDialog**: icon Delete đỏ + name + Error/Cancel
3. **PremiumQuotaDialog**: icon Star + "Đã tạo {limit}/{n}" + Upgrade/Để sau
4. **CardListFilterSheet** (Modal bottom sheet): section "Vụ mùa" + section "Giống lúa" + "Xoá lọc"

**UX issues**
1. Summary card chỉ tính HÔM NAY nhưng list hiện **toàn bộ phiếu** → user dễ hiểu nhầm
2. Date header "24/05/2026" không sticky → khó orient khi list dài
3. **Không có search bar** — với 100+ phiếu khó tìm
4. Pull-to-refresh delay 600ms gây cảm giác lag
5. Swipe-to-delete 1 hướng + confirm ngay → người trung niên dễ vô tình kéo
6. FAB hide quá hung hăng khi list rỗng có thể đè summary

---

### 5.2. CardDetailScreen (`card_detail/{cardId}`)

**File**: `ui/screen/CardDetailScreen.kt`
**Mục đích**: Xem/chỉnh phiếu chi tiết, dẫn tới WeightInput, generate QR, khoá phiếu.

**Layout (4 large card + Extended FAB)**
```
+── CustomHeader (HeaderGreen #2E7D32) ──+
| ← Phiếu của Nguyễn Văn A  [QR] [⋮]    |
| Tổng 1.234 kg · Còn 4.870.000đ · 25b  |
+────────────────────────────────────────+

  ┌── Card 0: Thông tin phiếu ────────┐
  │ 📄 Thông tin phiếu                 │
  │ 👤 Thương lái      Trần Văn B      │
  │ 📞 SĐT             0912... [dial]  │
  │ ─────                              │
  │ 🌾 Giống lúa       ST25 · Đông Xuân│
  │ 📅 Ngày tạo        24/05/2026      │
  │ ─────                              │
  │ 📍 Địa chỉ ruộng   Ấp X, CT [↻ GPS]│
  └─────────────────────────────────────┘

  ┌── Card 1: Khối lượng ─────────────┐
  │ ⚖️ Khối lượng                       │
  │ ┌─ Tổng KG (WeightSurface) ──┐    │
  │ │   1.234 KG                  │    │
  │ │   Trước trừ bì              │    │
  │ └─────────────────────────────┘    │
  │ 🛍 Số bao              25 bao      │
  │ 📦 Khối lượng bao      12,5 KG     │
  │ ⚖️ Tạp chất            2 KG         │
  │ ─────                              │
  │ ┌─ KL thực (GreenSurface) ───┐    │
  │ │ ⚖️ KL thực  1.219,5 KG     │    │
  │ └─────────────────────────────┘    │
  └─────────────────────────────────────┘

  ┌── Card 2: Tài chính ──────────────┐
  │ 💰 Tài chính                       │
  │ $ Đơn giá              8.200 đ     │
  │ 🧮 Thành tiền          9.870.000 đ │
  │ 💳 Tiền cọc            500.000 đ   │
  │ ✓ Đã trả               4.500.000 đ │
  │ ─────                              │
  │ ┌─ Còn lại (GoldLight) ───────┐   │
  │ │ 👛 Còn lại 4.870.000 đ      │   │
  │ └─────────────────────────────┘    │
  └─────────────────────────────────────┘

  ┌── Card 3: Chi tiết bao cân ───────┐
  │ ⚖️ Chi tiết     [25 bao]          │
  │ [Bảng 1*] [Bảng 2]                 │
  │ 5×5 grid:                          │
  │  #1 50.5  #6 51.2 ... #21          │
  │  ...                               │
  └─────────────────────────────────────┘

                          [⚖ Cân lúa] Extended FAB
```

**Overflow menu** (icon ⋮): Edit / Delete / Create QR / Scan QR / Export PDF (premium gate) / Lock toggle

**Interactions**
- Phone row tap → dial intent, ripple + scale 0.97f
- Field address tap (nếu có GPS) → `geo:lat,lon?q=label`, có nút refresh GPS 40dp riêng
- Long-press bag cell trong grid (chỉ unlocked) → BagEntryActionSheet (modal) → Delete → AlertDialog confirm
- Extended FAB "Cân lúa" → `weight_input/{cardId}` (nếu locked → toast warning)
- Empty bag list → CTA "Thêm bao đầu tiên"
- Pull-to-refresh + 550ms delay perceivable
- HorizontalPager swipe ngang giữa các bảng

**Dialogs/Sheets**
1. EditCardDialog (97% width): farmer name, [variety | season], traderName, [moisture | price], [deposit | paid]
2. AlertDialog delete confirm (Error tint)
3. AlertDialog unlock confirm (LockOpen GoldDark, cảnh báo)
4. BagEntryActionSheet + nested delete confirm

**UX issues**
1. **4 card lớn cùng style trắng nền** → cuộn dài, khó scan. CardInfoCard 7 dòng + 2 divider nhìn dày
2. **"Card trong card" pattern** (highlight box WeightSurface + GreenSurface + GoldLight lồng trong Card trắng) — trùng visual hierarchy, không có focal point rõ
3. Sửa cọc/đã trả phải mở EditDialog — không inline được như WeightInput → 2 cách edit khác nhau cho cùng model
4. Lock/unlock có 3 entry-points (overflow, WeightInput toolbar, bag action) → confusing
5. **RemainingHeroCard tồn tại trong code nhưng KHÔNG render** → dead code, design team coi chừng nhầm tham chiếu
6. Bag cell 5×5 với 40+ ô < 48dp trên màn nhỏ — visual rất chật

---

### 5.3. WeightInputScreen (`weight_input/{cardId}`)

**File**: `ui/screen/WeightInputScreen.kt`
**Mục đích**: Nhập cân từng bao, real-time tính KL thực + thành tiền.

**Layout**
```
+── TopBar elev 3 ────────────────────────+
| ← Trần Văn B (faded khi scroll) [🔓ock] |
|   "1.234,5 kg • 25 bao" (khi scroll)    |
+──────────────────────────────────────────+
| WeightMetricsCard (fade 60% khi scroll) |
|  ⚖️ Chỉ số cân            [25 bao]      |
|  ┌─ Tổng KG (WeightSurface) ──┐         |
|  │  1234,5 kg displayMedium    │         |
|  │  Trước khấu trừ             │         |
|  └────────────────────────────┘          |
|  [Bì (kg)]  [Tạp chất (kg)]              |
|  [💧 Độ ẩm (%) ............ ⓘ]          |
|  ┌─ KL thực (GreenSurface) ──┐          |
|  │  1.219,5 kg                │          |
|  │  Sau khấu trừ              │          |
|  └────────────────────────────┘          |
|  ── divider                              |
|  Thành tiền        9.870.000 đ           |
+──────────────────────────────────────────+
| Chọn bảng nhập         [+ Thêm bảng]    |
+──────────────────────────────────────────+
| ┌── Bảng 1 ─── tổng 248.5 kg ──────┐    |
| │  Cột1  Cột2  Cột3  Cột4  Cột5    │    |
| │  [50.5][51.2][49.8][50.0][...]   │    |
| │  [50.1][...]                      │    |
| │  ... 5×5 grid 60dp min            │    |
| └────────────────────────────────────┘    |
| ┌── Cộng cột (GoldLight) ──────────┐    |
| │ [250.2][251.0][...]               │    |
| └────────────────────────────────────┘    |
| Bảng 2 ... (lazy mount 150ms anim)      |
+──────────────────────────────────────────+
```

**Tính toán real-time**
- `liveTotalWeight = Σ entries.weight`
- `liveNetWeight = RiceCalculator.calcNetWeight(rawWeight, bagWeight, impurityWeight=0.0, moisturePercent)` — **bỏ qua impurity** (design quirk, khác Card detail)
- `liveTotalAmount = liveNetWeight * pricePerKg`

**Quy ước nhập**: 3 chữ số chia 10. Nhập "505" → 50.5 kg. Auto-advance focus sau 3 ký tự. Layout column-major: ô `(col, row) → index t*25 + col*5 + row`

**Interactions**
- Nhập 3 digit → parse `/10` → emit + focus jump (row+1 hoặc col+1, hoặc new table)
- Lock icon: lock (red) → mở khoá ngay; unlock (gray) → confirm dialog đỏ "Chốt phiếu cân?"
- `manualTableCount++` → thêm bảng trống
- Locked: cells render `Text` (không TextField) + toast warning khi tap

**States**
- Loading (`currentCard == null`): CircularProgress center
- Animation gate 150ms: render placeholder 360dp box trước khi mount grid + 25 FocusRequester/bảng

**UX issues**
1. **"Chia 10" rule không có UI hint** — user mới nhập 1-2 digit rồi confused
2. **Column-major fill order ngược mental model** (đa số quen row-major) — trừ dân làm cân quen
3. Field "Tạp chất" trong metrics dùng `card.impurityWeight` global cho cả phiếu, nhưng tính `liveNetWeight` lại hardcode `impurityWeight=0.0` → cộng trừ không nhất quán
4. "Thêm bảng" cho phép tạo bảng trống vô hạn → dễ nhiễu khi nhầm
5. TextField cell không show validation lỗi (vd input > 999 → coerce hidden)
6. Lock/unlock đứng cạnh icon back — dễ nhấn nhầm

---

### 5.4. DeletedCardsScreen (`deleted_cards`)

**File**: `ui/screen/DeletedCardsScreen.kt`
**Mục đích**: Xem/khôi phục/xoá vĩnh viễn các tombstone (deleted_cards).

**Layout**
```
+── TopAppBar "Phiếu đã xoá" [<] ─+
| "{n} phiếu đã xoá. Khôi phục   |
|  hoặc xoá vĩnh viễn..."         |
|                                  |
| ┌── DeletedCardRow r14 ──────┐  |
| │ 🗑 (40dp Error 12%)         │  |
| │   Nguyễn Văn A             │  |
| │   Phiếu 24/05/2026 · ĐX'26 │  |
| │   1.234 kg     9.870.000 đ │  |
| │   Đã xoá lúc 24/05/2026    │  |
| │ [Xoá vĩnh viễn] [Khôi phục]│  |
| └─────────────────────────────┘  |
+──────────────────────────────────+
```

**Interactions**
- Restore → `card_detail/{newId}` popUp inclusive
- Purge → AlertDialog confirm với icon DeleteForever + "KHÔNG thể hoàn tác"

**UX issues**
1. **2 button cân nhau gây hesitation** — không có hierarchy primary/destructive rõ
2. "Đã xoá lúc" dùng cùng format dd/MM/yyyy như "Phiếu ngày" → không phân biệt timestamp xoá vs ngày phiếu

---

### 5.5. Dashboard components (dùng trong FarmerProfile / TraderProfile)

Đây là các component dashboard hiển thị KPI/biểu đồ, được render trong tab ACCOUNT.

**KpiGrid + KpiCard**: 2 cột grid, mỗi card 16dp radius, icon 34dp tinted accent 14% bg, value `ExtraBold` 18sp (24sp khi highlight), delta pill ("+12.3%" Up = Success green / Down = Error red / equal = hint gray)

**VarietyPieChart**: Canvas donut 140dp, stroke 24dp, gap 2° giữa slice, animation sweep 0→360 trong 700ms. Center: tổng tấn + "Tổng". Legend bên phải: dot 10dp + name + `${weight} · ${percent}`. **Palette 6 màu nông sản** (xanh lúa, vàng lúa chín, cam, tím, teal, nâu).

**SeasonComparisonBarChart**: Canvas bars 180dp, 6 vụ reversed, selected = GreenPrimary, else GreenLight 55%. Label rút gọn ("Đông Xuân 2026" → "ĐX'26"). 2 metric: WEIGHT hoặc REVENUE.

**AiInsightsCard**: avatar 32dp gradient purple→pink + AutoAwesome. 4 states:
- Idle: body text + button "Phân tích AI" (purple)
- Loading: 3 shimmer lines với infinite alpha animation
- Success: AiMarkdownText render `## heading`, `- bullet`, paragraph với GreenPrimary cho heading
- Error: Error text + Retry outlined

**TopTradersCard**: list 14dp gap, rank circle 28dp (Top1 Gold #F9A825, Top2 Silver #90A4AE, Top3 Bronze #8D6E63, ≥4 Green). Name + revenue (GreenPrimary) + progress bar 6dp relative to maxRevenue + "${deals} phiếu"

**SeasonSelectorChip**: LazyRow chips 20dp radius, selected = GreenPrimary fill + CheckCircle white, unselected = outline GreenPrimary 40%

**UX issues**
- KPI grid 2×N có thể lệch (Spacer chiếm cột thừa khi lẻ)
- Pie chart không có tooltip tap
- Bar chart không có legend
- AI card response có 2 paths render markdown (`AiMarkdownText` real + `MarkdownBlocks` dead code)

---

## 6. Tab MARKET — Thị trường

### 6.1. MarketScreen (`market`)

**File**: `ui/screen/market/MarketScreen.kt`
**Mục đích**: Hub thị trường — Weather + Native ad + News + Rice prices + Chart. TRADER có thêm bid editor.

**Layout (rất dày)**
```
+── LazyColumn (16dp h-pad, 12dp gap) ────────+
| [WeatherWidget — gradient r24, dynamic icon]|
|  📍 Cần Thơ              [↻ Vừa xong]      |
|  ☀ 32°C   Trời nắng                         |
|  💧65% | 💨3.2m/s | ☂20%                     |
|         Cảm giác 35°                        |
|  [Advisory: "Mưa lớn — hoãn phun thuốc"]   |
+──────────────────────────────────────────────+
| [NativeAd Placeholder]  (chỉ Free user)     |
|  🌾 Phân bón Lúa Vàng         [Tài trợ]     |
|  "Tăng năng suất 18% với gói NPK..."        |
+──────────────────────────────────────────────+
| [NewsSection — card r20]                    |
|  📄 Tin nông nghiệp · Google News  [↻]      |
|  ( Tất cả )( Lúa )( Gạo )( Thời tiết )      |
|  [thumb 86dp] Title 2 lines · desc 2 lines  |
|  ... (max 8 bài)                            |
+──────────────────────────────────────────────+
| TRADER only:                                 |
| "Giá rao của bạn — 3 tin · nhấn để sửa"     |
|  [MyBidCard GreenSurface]                   |
|    variety · trend badge · min-max          |
|    [Xoá]  [Sửa]                             |
+──────────────────────────────────────────────+
| "Bảng giá thu mua — N kết quả"              |
|  ( Tất cả )( Tăng )( Ổn định )( Giảm )      |
+──────────────────────────────────────────────+
| [RicePriceCard r20]                         |
|  ST25                       [↗ Tăng]        |
|  Cập nhật 12 phút trước                     |
|  ┌Thấp nhất┐ ┌Cao nhất ◆┐ ┌TB 7 ngày┐       |
|  │  7.800  │ │  8.500   │ │  8.150   │ đ/kg|
|  └─────────┘ └─────────┘  └─────────┘       |
|  Nguồn: Anh Tư · Cần Thơ      Xem chart →   |
+──────────────────────────────────────────────+
| Footer: "💡 Dữ liệu tham khảo từ ĐBSCL..."  |
+──────────────────────────────────────────────+
   [+] FAB "Đăng giá mới" (TRADER only)
```

**Chart sheet** (modal khi tap card): title variety + Card r20 chứa "Xu hướng giá" + toggle `(7n)(30n)` + Vico LineChart 200dp + Min/TB/Max chips

**Data sources**
- WeatherInfo: OpenWeather qua `WeatherViewModel` (`OPENWEATHER_API_KEY`)
- NewsArticle: RSS Google News + báo VN qua `NewsRepository.refresh()`, max 8 bài/render, limit 30 in flow, Room cache
- RicePrice: Room (mock seed) + Firestore (bid từ TRADER), merge bởi `MarketRepository`
- PricePoint: chỉ vẽ `priceAvg`, 7d hoặc 30d

**Interactions chính**
- Pull-to-refresh → đồng thời refresh market/weather/news
- Tap RicePriceCard → mở chart sheet
- Tap chips trend → filter list reactive
- Tap chips news topic → filter Room
- Tap news card → mở Chrome Custom Tab màu GreenPrimary
- Tap weather → request location permission + reload
- TRADER FAB → mở BidEditorSheet (create); tap MyBidCard → edit; Xoá → `deleteBid`
- BidEditorSheet submit → validate `variety + min/max>0 + max≥min` → snackbar + auto close

**States**
- Loading first-time: `MarketSkeletonList(4)` + `NewsSkeletonList(3)` + Weather LoadingRow
- Refreshing với cache: spinner trên cùng + giữ list cũ
- Empty: news EmptyState; chart "Chưa có dữ liệu lịch sử"
- Error/Offline: news ErrorBanner đỏ "Không cập nhật được — đang dùng dữ liệu cũ"; weather ErrorRow

**BidEditorSheet (TRADER)**
- RiceVarietyDropdown
- 2 numeric field (ThousandSeparator) cho min/max
- region text field
- 3 trend chips (UP/STABLE/DOWN)
- note 200 char
- Huỷ / Đăng giá (disabled khi invalid hoặc isSaving)

**UX issues**
1. **Information overload**: 1 màn ghép 5 section nặng (Weather 280dp + Ad + News 8 bài dọc + MyBids + price list + chart sheet). User phải scroll dài trước khi tới value chính (Bảng giá)
2. **Ad placement** ưu tiên monetize hơn UX — code comment thừa nhận "đặt ở viewport đầu tiên không cần scroll"
3. **News 8 bài dọc** đẩy bảng giá xuống fold sâu — nên LazyRow snippet hoặc tab riêng
4. **RicePriceCard density**: 3 cột price cùng size font, chỉ "Cao nhất" highlight → không rõ focal
5. **Chart simplistic**: chỉ vẽ 1 line `priceAvg`, không axis labels, gridlines, tooltip, hay date markers. Range chỉ 7/30 ngày
6. **FilterChipsRow** chỉ filter theo trend, không filter variety/region dù data class hỗ trợ
7. **Trader edit affordance**: tap card → edit; có Xoá/Sửa button dưới → trùng action
8. **Weather decorative glow** hardcoded → vỡ trên tablet/landscape
9. **Permission flow**: tap weather → spam dialog mỗi lần (không cache denied state)
10. **ErrorBanner dismiss icon là `OpenInNew`** (sai semantic, nên là `Close`)

---

## 7. Tab AI CHAT

### 7.1. AiChatScreen (`ai_chat`)

**File**: `ui/screen/aichat/AiChatScreen.kt`
**Mục đích**: RAG-lite chat (OpenRouter, Gemini Flash free). System prompt inject 4 nguồn: profile, weather, top 8 giá lúa, top 2 knowledge entries (nông học).

**Welcome message + system prompt khác nhau theo role** (FARMER: tư vấn canh tác; TRADER: kiểm định/logistics/biên lợi nhuận).

**Layout**
```
┌─────────────────────────────────────────┐
│ [Drawer trái — ModalNavigationDrawer]   │
│  "Lịch sử trò chuyện"                   │
│  [+ Phiên chat mới] (capsule, GreenSurface)│
│  ─ 🤖 Session 1   (current = Green bg)  │
│  ─ 🤖 Session 2                          │
│  ...                                     │
│  "Lịch sử chỉ giữ đến khi tắt ứng dụng"│
└──────────────────────────────────────────┘
[Main area — Column, imePadding]
  Preset chips (chỉ khi messages.size ≤ 1):
  [Lúa bị đạo ôn…] [Bón đợt 2 ST25…] [Rầy…]
  ┌─ LazyColumn (weight=1) ────────────┐
  │ ┌── ai bubble (left, 🤖 32dp) ────┐│
  │ │ markdown render (Markwon)        ││
  │ │ - Bullet                         ││
  │ │ **Bold** *italic* `code`         ││
  │ └─────────────────────────────────┘ │
  │                ┌── user (GreenPrimary, white text) ┐
  │                │ Câu hỏi của tôi...                │
  │                └────────────────────────────────────┘
  │ [TypingIndicator: spinner + "Đang soạn..."]
  └─────────────────────────────────────┘
  [VoiceErrorBanner — AnimatedVisibility]
  Input bar (padding bottom 88dp khi IME tắt):
  [TextField "Hỏi điều bạn muốn biết..."]
  [48dp Circle: Mic | Send | Stop]
```

**Interactions**
- Send: append user msg, stream call (giả lập — single response), append reply hoặc error bubble
- Switch session: tap row trong drawer → switch + đóng drawer
- New session: nút "+ Phiên chat mới"
- 4 preset chips: tap → set input + send ngay
- Voice input: long-press mic → SpeechRecognizerHelper (RECORD_AUDIO), partial text show placeholder, pulse 1.0→1.15 scale 800ms khi listening, haptic
- Markdown: Markwon (bold/italic/code/link/table)

**Tính năng KHÔNG có** (đáng chú ý)
- Copy message, regenerate, edit, share message
- Delete session, rename session
- Persist session sau kill app
- Timestamp trên session row
- Hiển thị model name / token usage / cost

**States**
- Streaming: TypingIndicator
- Empty/welcome: 1 assistant welcome message (text khác theo role) + 4 preset chips
- Error API: bubble đỏ prefix "Lỗi:", background Error α0.1, avatar đỏ
- Error đặc biệt: thiếu OPENROUTER_API_KEY → "Thiếu OPENROUTER_API_KEY trong local.properties"
- Voice error: banner đỏ trượt từ trên có nút Close
- Offline: KHÔNG có offline state riêng — rơi vào error bubble qua exception

**Keyboard handling**
- `imePadding()` ở Column root
- Auto-scroll xuống cuối khi messages.size / sessionId / imeVisible đổi
- `imeAction = Send`, `capitalization = Sentences`, `maxLines = 4`
- IME đóng: padding bottom 88dp né bottom nav; IME mở: 0dp

**UX issues**
1. **Session bị mất sau kill app** (in-memory store) — chỉ có footer cảnh báo, không export/persist
2. **Không delete/rename session** — drawer chỉ tích lũy
3. **Không copy/regenerate message** — user mất message dài
4. **Streaming là fake** — UI "Đang soạn..." nhưng repository trả single response
5. **Error message thô** lẫn vào history thật, gọi tiếp sẽ resend cả error
6. **InputBar magic number 88dp** ăn theo BottomBar
7. **Preset chips disappear sau message 1** — không có cách bật lại quick actions
8. **AiMarkdownText dùng AndroidView (TextView)** — không inherit theme color Compose
9. **TRADER welcome khác FARMER nhưng preset chips dùng chung 4 câu hỏi nông dân**

---

## 8. Tab ACCOUNT — Hồ sơ

### 8.1. FarmerProfileScreen (`account`)

**File**: `ui/screen/FarmerProfileScreen.kt`
**Mục đích**: "Tôi là ai + thống kê mùa vụ của tôi". KHÔNG mix action settings (đã tách qua `settings` route).

**Layout — LazyColumn 8 tier, gap 16dp**
```
+── Tier 0: GradientProfileHeader (flat, no bg) ─+
| [Tên nông dân 26sp ExtraBold]   [🌾 Nông dân]  |
| email@... (TextHint 13sp)                       |
+─────────────────────────────────────────────────+
| Tier 1: QuickStatsGlassGrid (3 cột, 104dp)      |
| [🌿 Vụ] [⚖ Tấn] [💰 Doanh thu]                  |
+─────────────────────────────────────────────────+
| Tier 2: ProfileSectionTitle "Thống kê mùa vụ"   |
|         SeasonSelectorChip [ĐX][HT][TĐ]...      |
+─────────────────────────────────────────────────+
| Tier 3: KpiGrid 2x2 (highlight 2 ô đầu)         |
| [Sản lượng ↑x%]  [Doanh thu ↑x%]                |
| [KG/bao TB]      [Số bao]                       |
+─────────────────────────────────────────────────+
| Tier 4: SecondaryStatsRow                       |
| [💧 Độ ẩm TB]   [🧹 Tạp chất]                    |
| [🌾 Khô N phiếu | 💧 Ướt N phiếu]                |
+─────────────────────────────────────────────────+
| Tier 5: SeasonComparisonBarChart                |
+─────────────────────────────────────────────────+
| Tier 6: AiInsightsCard (analyze/reset)          |
+─────────────────────────────────────────────────+
| Tier 7: TopTradersCard                          |
|         ProfileNavigationRow "Lịch sử thương lái"→
+─────────────────────────────────────────────────+
| Tier 8: PersonalInfoCard (edit toggle)          |
+─────────────────────────────────────────────────+
| Tier 9: Premium card (Status hoặc Upsell)       |
+─────────────────────────────────────────────────+
```

**Data**
- Header: `profile.name`, role icon, email
- Quick Stats lifetime: `seasonCount`, `totalNetWeight`, `totalRevenue`
- KPI per-season: weight, revenue, avgKgPerBag, totalBags, delta% vs previous
- Secondary: avgMoisture, totalImpurity, dryCardCount, wetCardCount
- TraderHistory: row tóm tắt "${size} thương lái" hoặc "Chưa có giao dịch"
- PersonalInfo: name, phone (KeyboardType.Phone), region, CCCD (KeyboardType.Number)

**Interactions chính**
- Edit PersonalInfo: pencil icon toggle → save khi nhấn lần 2 → `updateProfile`
- Tap "Lịch sử thương lái" → `trader_history`
- Tap Premium card → `premium`
- Season chip → `selectSeason`
- AI: `analyzeWithAi` / `resetAiAnalysis`

**KHÔNG có ở đây** (đã dời sang Settings)
- Sign-out
- Change avatar (KHÔNG hỗ trợ avatar)
- Change role / language / font scale / theme

---

### 8.2. TraderProfileScreen (`trader_profile`)

**File**: `ui/screen/trader/TraderProfileScreen.kt`
**Khác Farmer**:

| Tier | Farmer | Trader |
|---|---|---|
| Header default name | "Nông dân" | "Thương lái" |
| Quick Stats #3 | "Doanh thu" | **"Đã chi"** |
| Quick Stats #1 | `seasonCount` (số vụ) | **`cardCount` (số phiếu)** |
| Primary KPI | 4 ô (Sản lượng/Doanh thu/KG-bao/Số bao) | **6 ô** (Đã mua / Đã chi / KG-bao / **Công nợ NCC** / Số phiếu / Số bao) |
| Tier 7 | TopTradersCard + "Lịch sử thương lái" | **VarietyPieChart** (cơ cấu giống lúa) + "Sổ giao dịch" → `trader_transactions` |

KPI accent colors trader: Đã chi = `#E65100` (orange); Công nợ = `Error`.

---

### 8.3. Profile data model

`Profile` Room entity, PK = `uid` (Firebase UID):
- `name`, `phone`, `region`, `note`
- `role` (FARMER | TRADER)
- `cccd`, `username`, `email`
- `roleGrantedBy` (`self` | `admin` | `system`), `roleGrantedAt`
- **KHÔNG có** field avatar URL

---

### UX issues mục Profile

1. **Không có avatar/photo upload** — header dùng icon role; thiếu personalization
2. **Hai màn Profile gần như sao chép cấu trúc** — 8 tier giống hệt, chỉ khác 2 ô KPI + Tier 7 → khó maintain
3. **Trader 6 KPI trong KpiGrid 2 cột** → tràn màn nhỏ
4. **Edit toggle không có Cancel** — đang edit mà nhấn Save ở góc luôn ghi đè, không validate
5. **Section title "Thống kê mùa vụ"** dùng Box padding lặp lại 8+ lần thay vì global content padding
6. **QuickStatsGlassGrid** đặt tên "Glass" nhưng đã refactor thành flat — tên misleading
7. **Trader navigation row** subtitle chỉ show `totalCards` — thiếu KPI quan trọng (công nợ chưa thanh toán)
8. **Header email overflow**: không có `maxLines` cho email → tràn dài
9. **CCCD KeyboardType.Number** không format 12 số

---

## 9. Trader-only screens

### 9.1. TraderTransactionsScreen (`trader_transactions`)

**Mục đích**: TRADER xem sổ giao dịch — tất cả thẻ đã verify QR.

**Layout**
```
+── TopAppBar "Sổ Giao Dịch" [<] ────────────+
| "Sổ giao dịch" (headlineMedium bold)        |
| "X thẻ đã thu mua · cập nhật realtime"      |
| ┌─ StatsHeroCard (gradient Green→GreenDark) ─┐
| │ "Tổng giá trị thu mua"                    │
| │ <Total> đ (headlineLarge bold white)      │
| │ ▰▰▰▰▰▱▱▱  6dp progress (paid/total)      │
| │ Đã thanh toán: X đ          75%           │
| │ ┌Scale 1,234kg┐ ┌Warn 3 nợ┐               │
| │ │ Khối lượng │ │ Còn nợ  │                │
| └────────────────────────────────────────────┘
| [Tất cả] [Đã trả] [Còn nợ] ← Filter pill    |
| ┌── TransactionRow r18 white card ─────────┐
| │ ⓟ Tên ND       12/05/2026  [✓ Đã trả]   │
| │ ┌Scale 500kg┐ ┌Wallet 4.5tr┐ ┌Receipt 0đ┐│
| │ [OM5451 chip Info] [50 bao chip Gold]    │
| └──────────────────────────────────────────┘
+─────────────────────────────────────────────+
```

**Data**: totalCards, totalAmount, totalPaid, totalNetWeight, unpaidCount, items. Currency vi-VN với short format (k/tr/tỷ).

**Filter**: ALL / PAID / UNPAID

**States**
- Loading: CircularProgress Green
- Empty (`totalCards==0`): icon ReceiptLong vàng
- Filter empty: FilteredEmptyState inline

**UX issues**
1. Row KHÔNG clickable → không drill detail card
2. Không pull-to-refresh
3. Chỉ 3 filter cứng, không sort/search
4. FilterRow dùng SurfaceContainer không phân biệt rõ với card khi list dài
5. Progress overflow nếu paid > total chưa được làm rõ visually
6. `formatCurrency` ép `.toLong()` mất phần thập phân
7. AnimatedVisibility(visible=true) trong items không có ý nghĩa

---

### 9.2. TraderHistoryScreen (`trader_history`)

**Mục đích**: FARMER xem danh sách thương lái đã từng mua lúa của mình.

**Layout**
```
+── TopAppBar "Lịch sử thương lái" [<] ──+
| ┌── SummaryHeader (GreenSurface r16) ┐|
| │ ⓘ History  "N thương lái đã từng mua"│
| │           "Sắp xếp theo lần gần nhất"│
| └──────────────────────────────────────┘|
| ┌── TraderHistoryRowFull (white r14) ─┐|
| │ (A) avatar initial green            │|
| │  Tên Thương Lái (bodyLarge SemiBold)│|
| │  📞 0901... (bodySmall hint)         │|
| │  💰 12,500,000 đ · 8 phiên           │|
| │  📅 Lần cuối 12/05/2026              │|
| └──────────────────────────────────────┘|
+─────────────────────────────────────────+
```

**UX issues**
1. Thiếu search/filter (không scale với 50+ trader)
2. Row không tap (no detail nav)
3. Phone icon nhưng KHÔNG clickable để call
4. Key `name-phone` có thể conflict
5. Không sort tùy chỉnh

---

### 9.3. RiceMapScreen (`trader_map`)

**Mục đích**: TRADER xem bản đồ các phiếu có GPS để đi thu mua.

**Layout**
```
+── Scaffold ───────────────────────────────+
| ┌── SearchAndFilterBar (CardBg α0.96) ──┐ |
| │ 🔍 [Tìm tên / giống / địa...] ✕       │ |
| │                          [≡ filter]   │ |
| └────────────────────────────────────────┘ |
| "Hiện X/Y điểm" (pill đen overlay)        |
| ── AnimatedVisibility FilterChipsRow ──   |
| Giá lúa: [Tất cả][Có giá][≥7k][≥8k][≥9k] |
| Giống lúa: [Tất cả][OM5451][Đài Thơm 8]   |
| ┌── Map (Hybrid satellite, r20 clip) ──┐ |
| │     [● cluster bubble 48dp green=N]   │|
| │           ● ●  markers                 │|
| │  [EmptyMapHint card overlay bottom]   │|
| │                              [⊙ FAB]  │|
| └────────────────────────────────────────┘|
+────────────────────────────────────────────+

Tap marker → ModalBottomSheet:
+── CardSummaryBottomSheet ─────────────────+
| [🗺 48dp GreenSurface] Tên + ngày         |
| ┌Khối lượng┐ ┌Số bao┐                     |
| [Chip giống Info] [Chip giá Green α0.14]  |
| 📍 fieldAddress  📞 traderPhone           |
| [⚖ Vào Nhập Cân] (54dp r14)               |
+────────────────────────────────────────────+
```

**Markers**: Google Maps Compose + clustering. ClusterBubble = circle 48dp GreenPrimary, count bold trắng. Map type Hybrid, myLocation enabled. Fallback ĐBSCL (10.0452, 105.7469) zoom 10.

**Filters**: SearchAndFilterBar + FilterChipsRow (PriceFilter enum + LazyRow giống lúa distinct). Search normalize lowercase + strip diacritics + "đ"→"d".

**Cluster tap**: size=1 → mở sheet, size>1 → zoom +2 (max 18).

**States**: API key blank → MapComingSoonPlaceholder. EmptyMapHint 3 trạng thái (permission denied / filter empty / no GPS card).

**UX issues**
1. Search bar không submit/IME action
2. Chip giá thang cứng (≥7/8/9k) — không slider
3. PriceFilter hiển thị "đ" không kèm "/kg"
4. Bottom sheet không có nút **gọi điện** hay **mở Google Maps route**
5. Phone không clickable
6. FAB padding 88dp magic-number coupling bottom bar

---

## 10. Sub-screens

### 10.1. SettingsScreen (`settings`)

**File**: `ui/screen/SettingsScreen.kt`

**Groups (mỗi group = 1 Card r20)**
1. **Premium** — IconTile gold, title + badge ACTIVE/EARLY ADOPTER, subtitle = plan + ngày kích hoạt. CTA Quản lý / Nâng cấp
2. **TTS (đọc kết quả cân)** — IconTile green + Switch
3. **Sync & Backup** — IconTile blue + SyncStatusText + Switch "Tự động đồng bộ ngầm" + 2 StatusRow (lastSyncTime, lastBackupTime) + OutlinedButton "Sao lưu" + Button "Đồng bộ" + TextButton "Chi tiết" → `sync_status`
4. **Language** — 5 options radio: vi, en, km (Khmer), lo (Lào), zh-CN
5. **Font scale** — 4 options radio: SMALL 0.9 / NORMAL 1.0 / LARGE 1.1 / XLARGE 1.2
6. **Phiếu đã xoá** — full row clickable → `deleted_cards`. Error/red icon
7. **Role Switcher** (chỉ show khi `profile != null`) — 2 chip Nông dân / Thương lái + confirm dialog
8. **Account** — title + `authState.userLabel` + Button "Đăng xuất" (Error α0.1 bg, 52dp)

**UX issues**
1. **Không có Dark mode toggle** dù tokens hỗ trợ — language/font có UI nhưng theme không
2. **Sign-out** KHÔNG có AlertDialog xác nhận → một tap mất phiên
3. **Role Switcher tự ý đổi role TRADER không qua admin** (mâu thuẫn với roleGrantedBy="self")

---

### 10.2. PremiumScreen (`premium`)

**Layout**
```
[← Cân Lúa Premium]
+─ HeaderBanner (gradient Green→GreenDark r24) ─+
| ⭐ icon trong vòng tròn gold                   |
| "Nâng Cấp Premium" (display)                   |
| "Giải phóng toàn bộ sức mạnh..."               |
+─ Feature Card (r20) ───────────────────────────+
| ✓ Không giới hạn phiếu cân lúa                |
| ✓ Chat AI không giới hạn                       |
| ✓ Bản đồ vệ tinh nhiệt (Heatmap)              |
| ✓ Tự động đồng bộ tức thời                    |
| ✓ Hoàn toàn không có quảng cáo                |
+─ Package cards (3 options) ────────────────────+
| Gói Mùa Gặt   49.000đ           / 1 tháng     |
| Gói Cả Năm   299.000đ ~~588.000~~ /12 tháng   |
|                            [HỜI NHẤT]          |
| Gói Vĩnh Viễn 599.000đ ~~999.000~~ /Trọn đời |
+─ [Đăng ký với {price}] (56dp Green) ───────────+
| disclaimer Google Play / hủy bất kỳ lúc nào    |
```

**Tiers**
- MONTHLY "Gói Mùa Gặt" 49.000đ
- YEARLY "Gói Cả Năm" 299.000đ (gốc 588.000đ) — `isBestValue=true`, default selected
- LIFETIME "Gói Vĩnh Viễn" 599.000đ (gốc 999.000đ)

**Flow**: tap "Đăng ký" → `isPurchasing=true` → haptic + `delay(1800ms)` giả lập → flip flag local + analytics → success dialog → pop back

**UX issues**
1. **Premium thanh toán FAKE** — `delay(1800)` rồi flip flag → KHÔNG tích hợp Google Play Billing thật, disclaimer ghi "qua cổng thanh toán Google Play" gây hiểu lầm
2. **String hardcode tiếng Việt** — vi phạm i18n (5 locale)
3. Không có nút "Quản lý gói" / "Hủy đăng ký" / xem hóa đơn

---

### 10.3. QrGenerateScreen (`qr_generate/{cardId}`) — FARMER

**Layout**
```
+── TopAppBar "Tạo mã QR" [<] ────────+
| ┌─ Card summary (GreenSurface chip) ┐|
| │ Tên · weight · total · variety    │|
| └────────────────────────────────────┘|
| ┌── Card r20 elev 4 ─────────────────┐|
| │   [QR 240dp]                        │|
| │   Hint: "Thương lái quét mã này..."│|
| └────────────────────────────────────┘|
| [↻ Làm mới mã]                       |
| ✓ Đã xác nhận (khi isLocked)         |
| Token preview "...12 chars"          |
+──────────────────────────────────────+
```

**Payload format**: `CANLUA|id|name|totalWeight|totalAmount|sha256Token`

**UX issues**
1. **Payload chứa dữ liệu nhạy plaintext** (name + totalAmount) trong QR
2. Không share/save QR
3. Không max brightness khi hiển thị QR (UX ngoài nắng)
4. Không countdown token sẽ hết hạn
5. "Refresh" silent — không confirm sẽ invalidate token cũ

---

### 10.4. QrScanScreen (`qr_scan`) — TRADER

**Layout**
```
+── Permission denied: ───────────────+
| [📷 CameraAlt 64dp]                  |
| Cấp quyền camera để quét QR          |
| [Cấp quyền]                          |
+──────────────────────────────────────+

+── Active scan: ─────────────────────+
| CameraPreview (full, weight=1)      |
|   [center 250dp white α0.1 r20]     |
|   (frame visual, không clip)         |
| ─────                                |
| Bottom Surface: hướng dẫn quét       |
+──────────────────────────────────────+

+── Success: ──────────────────────────+
| ✓ CheckCircle 72dp Success          |
| GreenSurface card với info:          |
|  Farmer name / Weight / Amount       |
| [Tiếp tục]  [Xem sổ giao dịch →]    |
+──────────────────────────────────────+
```

**States** (sealed `qrVerificationState`): Idle, Loading, Success, AlreadyConfirmed, NotFound, LockedByOtherTrader, Error. Toast + Haptic theo state.

**UX issues**
1. Overlay 250dp chỉ trang trí — không clip camera analysis region
2. Không flashlight toggle
3. Không nhập tay token
4. Success không show full trader-card details
5. `LockedByOtherTrader` không nói rõ trader nào lock
6. Payload pipe-delimited dễ collision nếu name chứa "|"

---

### 10.5. SyncStatusScreen (`sync_status`)

**Layout**
```
+── TopAppBar "Trạng thái đồng bộ" [<] ─+
| ┌── State Card (M3 colorScheme) ────┐ |
| │   [icon 64dp CloudDone/Error/    ] │ |
| │    CloudSync/CloudOff             ] │ |
| │   Status headline                  │ |
| │   "Lần cuối: dd/MM HH:mm:ss"      │ |
| └────────────────────────────────────┘ |
| [Đồng bộ ngay] (full-width)           |
| ┌── Info Card bullets ────────────┐  |
| │ • Đồng bộ tất cả thẻ            │  |
| │ • Yêu cầu internet               │  |
| │ • Backend: Firebase              │  |
| └──────────────────────────────────┘  |
+────────────────────────────────────────+
```

**UX issues**
1. **Dùng MaterialTheme.colorScheme M3 thuần** thay vì AppColors → không nhất quán branding với rest of app
2. Không show số item pending/queued
3. Không có "huỷ" sync
4. Thiếu progress percentage

---

### 10.6. TraderComingSoonScreen

Placeholder dùng cho các feature chưa hoàn thiện.

**Layout**: Box centered, icon 120dp circle (tint α0.12), title headlineSmall, description bodyLarge, badge "Đang phát triển — Sprint 6" (GreenSurface bg, GreenPrimary text).

**UX issues**
1. Badge **hardcode "Sprint 6"**
2. Không có CTA "Nhận thông báo khi xong"

---

## 11. Design system hiện tại

### 11.1. Color palette

**3 nguồn màu cùng tồn tại — đây là vấn đề lớn cần consolidate**:
1. `AppColors.kt` — custom semantic tokens
2. `Color.kt` — Material 3 seed colors
3. M3 dynamic color (Android 12+ wallpaper-based, mặc định ON)

#### Brand "lúa" (chủ đạo)
| Token | Light | Dark |
|---|---|---|
| GreenPrimary | `#2E7D32` | `#A5D6A7` (soft) |
| GreenLight | `#4CAF50` | — |
| GreenDark | `#1B5E20` | — |
| GreenSurface | `#E8F5E9` | `#1E3A24` |

#### Accent "lúa chín"
| Token | Hex |
|---|---|
| GoldAccent | `#F9A825` |
| GoldLight | `#FFF8E1` |
| GoldDark | `#E65100` |

#### Surface neutrals (animated transitions)
| Token | Light | Dark |
|---|---|---|
| Surface | `#FAFAFA` | `#121212` |
| CardBg | `#FFFFFF` | `#1E1E1E` |
| SurfaceContainer | `#F5F5F5` | `#252525` |

#### Text
| Token | Light | Dark |
|---|---|---|
| TextPrimary | `#1A1A1A` | `#E0E0E0` |
| TextSecondary | `#424242` | `#E0E0E0` |
| TextHint | `#757575` | `#AAAAAA` |
| Divider | `#EEEEEE` | `#333333` |

#### Status
| Token | Hex |
|---|---|
| Success | `#43A047` |
| Warning | `#FF8F00` |
| Error | `#E53935` |
| Info | `#1976D2` |

#### Specialty surfaces
- **OfflineBg** `#FFF3E0` / **OfflineText** `#E65100`
- **SyncingBg** `#E3F2FD` / **SyncingText** `#1565C0`
- **LockedBg** `#FCE4EC` / **LockedText** `#C62828`
- **ComingSoonBg** `#F3E5F5` / **ComingSoonText** `#6A1B9A`

#### Detail tonal layers
- **WeightSurface** `#FFF8E1` / `#332B14` (nhấn khối lượng)
- **MoneySurface** `#E8F5E9` / `#1A2E1F` (nhấn tài chính)
- **RemainingHighlight** `#B71C1C` / `#EF5350` (cảnh báo công nợ)

#### Acrylic/depth (chưa thực sự dùng)
- AcrylicLight `0x14FFFFFF`, AcrylicMedium `0x33FFFFFF`
- ShadowAmbient `0x1A000000`, ShadowDirect `0x33000000`

#### Material seeds (Color.kt)
- Green40 `#2E7D32` / Green80 `#A5D6A7`
- GreenGrey40/80, Amber40 `#F57C00` / Amber80 `#FFE082`
- SurfaceGreen `#F1F8E9`

#### Gradients dùng trong app
- **TraderTransactions hero**: linear GreenPrimary → GreenDark
- **Premium banner**: linear GreenPrimary → GreenDark
- **MapComingSoon**: vertical GreenPrimary α0.08 → Surface
- **Weather widget**: dynamic theo iconKey (01/02–04/09–10/11/13)

---

### 11.2. Typography scale

Font Default = **Roboto** (chưa custom font). 15 levels M3:

| Level | Size | Weight | Line height |
|---|---|---|---|
| displayLarge | 36 sp | Bold | 44 |
| displayMedium | 32 sp | Bold | 40 |
| headlineLarge | 28 sp | Bold | — |
| headlineMedium | 22 sp | SemiBold | — |
| headlineSmall | 20 sp | SemiBold | — |
| titleLarge | 18 sp | SemiBold | — |
| titleMedium | 16 sp | Medium | — |
| titleSmall | 14 sp | Medium | — |
| bodyLarge | 16 sp | Normal | (letterSpacing 0.5) |
| bodyMedium | 14 sp | Normal | (letterSpacing 0.25) |
| bodySmall | 12 sp | Normal | (letterSpacing 0.4) |
| labelLarge | 16 sp | Medium | (letterSpacing 0.1) |
| labelMedium | 12 sp | Medium | (letterSpacing 0.5) |
| labelSmall | 11 sp | Medium | (letterSpacing 0.5) |

Scale qua `FontScale` enum: SMALL 0.9x, NORMAL 1.0x, LARGE 1.1x, XLARGE 1.2x. **Chỉ scale font + lineHeight, không scale letterSpacing.**

---

### 11.3. Theme & dynamic color

`CanLuaTheme(darkTheme=system, dynamicColor=true, fontScale=NORMAL)`.

- **Android 12+ mặc định dynamic** (wallpaper-derived) → có thể che mất brand green.
- Fallback: LightColorScheme (Green40/GreenGrey40/Amber40 + SurfaceGreen bg) hoặc DarkColorScheme (Green80/GreenGrey80/Amber80 + `#121212`).
- Mỗi token trong ColorScheme bọc `animateColorAsState` → transition mượt sáng/tối **nhưng cost recomposition cao toàn UI**.

---

### 11.4. Component library hiện tại

#### Bottom bar (ModernBottomBar)
- Floating capsule margin 16dp ngang, padding vertical 10dp, navigationBarsPadding
- Surface r32 CardBg + custom shadow 16dp (ambient + spot)
- Row height 64dp, SpaceEvenly
- **PillNavItem** height 48dp:
  - Selected: bg GreenPrimary r24 + icon filled 22dp + label SemiBold (CardBg fg cho contrast cả light/dark)
  - Idle: transparent + icon outlined + TextSecondary, no label
- Animations: icon crossfade 220/160ms, label expandHorizontally spring lowBouncy + fadeIn delay 60ms
- Haptic tick on tap
- **KHÔNG có badge/notification dot** (mặc dù có thể cần cho sync error, premium expiring...)

#### TopBar (CustomHeader)
- Dùng riêng cho CardDetail
- Bg `#2E7D32` (HeaderGreen), height 56dp fixed
- Back arrow 48dp + title (totalWeight KG · bagCount bao, 17sp SemiBold white) + actions (Edit / Lock / MoreVert dropdown)
- Lock icon: GoldAccent khi locked, LockOpen white α0.8 khi unlock
- Dropdown menu items: 15sp với leadingIcon green

#### Banners
- **OfflineStatusBanner**: full-width, OfflineBg `#FFF3E0` + icon CloudOff + OfflineText. AnimatedVisibility slide top + fade. Hiển thị ở MainScreen ngay dưới TopBar.
- **ErrorBanner** (News): đỏ + nút dismiss (sai icon: dùng OpenInNew thay vì Close)

#### Indicators
- **SyncStatusPulse**: Dot 10dp với infinite transition khi Syncing: scale 0.85↔1.25 + alpha 0.45↔1.0 tween 1200ms. Color map Syncing/Success = GreenPrimary, Error = `#E53935`, Idle = GreenLight
- **TypingIndicator** (AI chat): CircularProgress + "Đang soạn..."

#### Dialogs
- **AlertDialog** (M3): dùng cho delete, lock, premium quota
- **Custom Dialog** (CreateCard, EditCard, BagEntryAction): full-width 97%, các section header
- **ModalBottomSheet**: filter (cardlist), chart sheet (market), bid editor (market), bag entry actions

#### Cards
- Hero card r24 + gradient
- Standard card r20 + elevation 2-4
- Inline highlight box r12 (WeightSurface / GreenSurface / GoldLight) — "card trong card"

#### Chips/Pills
- Filter chip r10 GreenPrimary bg khi selected
- Status pill r6-8 + bg α0.12 màu status
- Trend chip r10 GreenPrimary / Error / Info

#### Empty states
- Icon 56-72dp circle (color α0.1) + headline + subtitle
- Một số có CTA

#### Skeleton
- ShimmerEffect 45° gradient
- SkeletonCard, NewsSkeleton, MarketSkeleton

---

### 11.5. Spacing / radius / elevation tokens

#### Corner radius scale hiện tại — quá nhiều bậc
8 (small chip), 10 (filter chip), 12 (button/metric box), 14 (row card / dropdown), 16 (banner/card), 18 (transaction row), 20 (QR card / map clip), 24 (hero card / pill nav item), 32 (bottom bar capsule).

**→ Design team nên chuẩn hoá thành 4-5 token: xs/sm/md/lg/xl.**

#### Padding
- Phổ biến: 8, 10, 12, 14, 16, 20, 24, 32 dp
- Content padding ngang chủ yếu 16dp
- Bottom bar magic offset 88dp xuất hiện ở FAB → cần token `bottomBarOffset`

#### Elevation
- Card phổ biến 2-4dp
- Bottom bar custom shadow 16dp (ambient/spot tokens)
- Dropdown 8dp shadow + 4dp tonal
- Theme dùng dynamic color M3 chia tonal qua surfaceContainer levels nhưng codebase phần lớn vẫn hard-code AppColors.SurfaceContainer/CardBg → mismatch giữa M3 token vs custom token

#### Hero pattern
- Hero card 24dp radius + gradient linear theo trục Green → GreenDark
- Status badge 6-8dp vertical / 10-14dp horizontal / r8-12

#### Animation
- AnimatedVisibility fadeIn+slide phổ biến (offline banner, bottom bar, transaction rows, filter chips)
- Pulse 1200ms cho sync
- Spring lowBouncy cho pill expand
- Theme color transitions trên mọi token — cost recomposition cao

#### Icon sizing
- Action: 22dp
- Inline meta: 16-18dp
- Empty state: 32-56dp
- Illustration: 64-96-120dp

---

### 11.6. Bottom nav items + icons

| Tab | Route | Outlined | Filled | Label |
|---|---|---|---|---|
| SCALE | `scale` | Outlined.Scale | Filled.Scale | "Cân Lúa" |
| MARKET | `market` | AutoMirrored.TrendingUp Outlined | Filled | "Thị Trường" |
| AI_CHAT | `ai_chat` | Outlined.SmartToy | Filled.SmartToy | "Hỏi đáp AI" |
| ACCOUNT (FARMER) | `account` | Outlined.Person | Filled.Person | "Cá nhân" |
| TRADER_MAP | `trader_map` | Outlined.Map | Filled.Map | "Bản đồ" |
| TRADER_PROFILE | `trader_profile` | Outlined.Person | Filled.Person | "Cá nhân" |

**Icon đã import nhưng KHÔNG dùng**: Sell, BarChart, Storefront, QrCodeScanner.

---

## 12. Pain points & cơ hội redesign

### 12.1. Pain points tổng hợp (theo mức độ)

#### CRITICAL — phải xử trong redesign
1. **3 nguồn màu xung đột** (AppColors / Color.kt / M3 dynamic) — risk inconsistency cao. SyncStatusScreen dùng M3 colorScheme thuần trong khi mọi screen khác dùng AppColors → màu khác hẳn.
2. **Premium thanh toán FAKE** — `delay(1800)` rồi flip flag, không tích hợp Google Play Billing. Disclaimer ghi "qua Google Play" gây hiểu lầm pháp lý.
3. **Role Switcher tự ý đổi TRADER** — bypass admin grant, mâu thuẫn với `roleGrantedBy` design.
4. **QR payload chứa dữ liệu nhạy plaintext** (name + totalAmount) — risk lộ thông tin tài chính khi share QR.
5. **Information overload tại Market** — 5 section nặng trên 1 màn; native ad chen giữa weather và news ưu tiên monetize hơn UX.

#### HIGH — ảnh hưởng usability nhiều
6. **CardDetail "4 card trắng lớn cùng cấp"** — không có visual hierarchy, focal point. Pattern "card trong card" (highlight box r12 lồng trong card r20) lặp 3 lần trên 1 màn.
7. **Dialog vs inline edit không nhất quán** — WeightInput cho phép inline; CardDetail bắt mở EditDialog cho mọi field. Cùng 1 model nhưng 2 cách edit.
8. **WeightInput column-major fill** ngược mental model row-major; 3-digit-chia-10 rule không có UI hint → user mới confused.
9. **CardListScreen không có search** — không scale với 100+ phiếu.
10. **AI Chat không persist session sau kill app**, không delete/rename, không copy/regenerate message, "streaming" là fake.
11. **Không có Dark mode toggle** dù tokens hỗ trợ. Dynamic color M3 default ON che mất brand.
12. **Không avatar/photo upload** — header dùng icon role, thiếu personalization.
13. **Không sign-out confirm dialog** — 1 tap mất phiên.
14. **Lock/unlock có 3 entry points** (overflow menu, WeightInput toolbar, bag action) — confusing.
15. **Filter chip "Tất cả" green ambiguous** với trend "ổn định" trên Market.

#### MEDIUM — đáng làm
16. Auth: "Quên MK" và "Điều khoản" là dead-end snackbar → first impression tệ.
17. Auth: auto-detect phone vs email không có affordance → password field biến mất bí ẩn.
18. Auth: error qua snackbar không gắn field → khó biết field nào sai.
19. ProfileSetup: chọn TRADER là UX dối (UI cho phép nhưng backend không grant).
20. RoleRequest: hardcode tiếng Việt trong code → vi phạm i18n (5 locale).
21. RoleRequest: APPROVED bắt logout/login thủ công thay vì refresh token.
22. Premium: tất cả string hardcode tiếng Việt.
23. ComingSoon badge hardcode "Sprint 6".
24. Bag cell 5×5 grid touch target < 48dp trên màn nhỏ.
25. Trader transactions row không clickable; không pull-to-refresh; không sort/search.
26. RiceMap bottom sheet không có nút gọi/route Google Maps; phone không clickable; chip giá thang cứng không slider.
27. Trader History phone icon không clickable để call.
28. QR Scan không flashlight, không nhập tay token, success không show full info.
29. Sync Status dùng M3 colorScheme khác mọi nơi.
30. Footer note Market padding bất thường (placeholder).
31. Date header CardList không sticky.
32. FAB hide quá hung hăng khi list rỗng.
33. Edit toggle profile không có Cancel.
34. CCCD field không format 12 số.
35. Header email không có maxLines.

#### LOW — code/visual cleanup
36. **Dead code**: RemainingHeroCard tồn tại nhưng không render. MarkdownBlocks không dùng. MetaItem/MetricChip legacy.
37. **AnimatedVisibility(visible=true)** vô nghĩa ở TraderTransactions.
38. **Magic number 88dp** padding-bottom xuất hiện ở nhiều FAB.
39. **Tên class misleading**: QuickStatsGlassGrid không còn "Glass".
40. **AiMarkdownText dùng AndroidView (TextView)** — không inherit theme color Compose.
41. **Two Markdown render paths** trong AI insights — dead path.
42. **Chart Market simplistic** — chỉ vẽ priceAvg, không axis labels, gridlines, tooltip, range 90d/1y.
43. **TopBar SyncStatusScreen** dùng MaterialTheme.colorScheme M3 → không nhất quán.
44. **ErrorBanner dismiss icon là OpenInNew** (sai semantic).
45. **Pull-to-refresh delay 600ms** giả cảm giác lag.
46. **PullEffect color cứng** không adapt dark.
47. **OpenWeather/Maps API key blank → silent degrade**, không cảnh báo developer/user.

---

### 12.2. Cơ hội redesign nổi bật

#### 12.2.1. Information architecture
- **Tab MARKET**: tách Weather + News thành tab phụ (Bảng giá / Tin & thời tiết), hoặc gập Weather thành chip cuộn ngang ở top.
- **Tab ACCOUNT**: 2 màn Farmer/Trader đang sao chép 8 tier → consolidate thành 1 ProfileScreen với role-aware sections.
- **Sub-navigation**: Premium / Settings / DeletedCards đang lẫn trong Account → có thể chia "Hồ sơ" vs "Cài đặt" rõ ràng hơn.

#### 12.2.2. Visual hierarchy
- Mỗi screen 1 focal point. CardDetail có 4 card cùng cấp → chọn 1 "hero" (Còn lại nợ, hoặc Tổng KG) làm điểm dừng mắt đầu tiên.
- Bỏ pattern "card trong card" — dùng surface tone contrast (Surface vs SurfaceVariant) + divider thay vì box r12 lồng trong card r20.

#### 12.2.3. Form ergonomics
- Đồng nhất 1 cách edit (inline) cho mọi field — bỏ EditDialog của CardDetail.
- Sticky save bar khi sửa form dài (RoleRequest, Premium, EditCardDialog).
- Inline validation error gắn field, bỏ snackbar.

#### 12.2.4. Bottom nav scale
- 4 → 5 tab khi role TRADER — capsule hiện tại có thể chật. Cân nhắc grouping hoặc rail navigation.
- Thêm badge cho sync error / premium expiring / unverified QR.

#### 12.2.5. Bag input redesign
- Chuyển 5×5 column-major sang row-major hoặc 5-column scroll dọc, touch target ≥56dp.
- Thay rule "nhập 3 digit chia 10" bằng hint "ví dụ: 50.5" hoặc decimal keyboard.

#### 12.2.6. Dark mode + accessibility
- Bật toggle Dark/Light/System trong Settings.
- Tách brand green khỏi dynamic color cho status bar / bottom bar / hero card — giữ identity ngay cả khi user có wallpaper màu khác.
- AA contrast cho mọi text/background; AAA cho số tiền.

#### 12.2.7. Empty/error/offline polish
- Mọi empty state có CTA dẫn dắt (ví dụ DeletedCards empty đang chỉ "Chưa có phiếu nào bị xoá").
- Offline state riêng cho mỗi tab thay vì rơi vào generic error.
- ErrorBanner dùng đúng icon Close.

---

## 13. Design direction gợi ý

### 13.1. Style direction đề xuất: "Editorial nông nghiệp"

**Không** dùng generic Material defaults, không glassmorphism, không gradient neon, không dark-mode-by-default.

**Có** typography mạnh kiểu báo, lưới editorial breaking, palette xanh-vàng-đất với accent rõ, ảnh thật từ ruộng/lúa làm texture, ít animation phù phiếm.

#### Inspiration references
- **Cropwise** (Syngenta) — agritech production-grade
- **John Deere Operations Center** — farm management dashboard editorial
- **Plantix** — plant disease diagnosis, illustrative
- **Pocket Casts** — editorial typography + clean dark mode
- **Linear** — tokens & motion language
- **Apple Weather** — micro-interaction trên data viz

### 13.2. Color tokens đề xuất

#### Brand
- **Primary "Lúa xanh"**: `#1F6B27` (đậm hơn hiện tại để contrast AAA trên surface trắng)
- **Primary container** `#C8E6C9` light / `#1E3A24` dark
- **Accent "Lúa chín"**: `#F59E0B` (warmer than GoldAccent hiện tại)
- **Accent container** `#FEF3C7` light / `#451A03` dark

#### Semantic
- **Success** `#22C55E`, **Warning** `#F59E0B`, **Error** `#DC2626`, **Info** `#3B82F6`
- Tách hẳn `Warning` ≠ `Accent` (hiện tại bị trùng concept gold).

#### Surface scale (1 nguồn duy nhất, bỏ Color.kt + AppColors mixed)
- 5 mức: surface-1 → surface-5 với tonal elevation rõ
- Dark mode tự derive, không hardcode

#### Status surfaces semantic
- Locked / Offline / Syncing / ComingSoon: dùng tint của primary/warning/info thay vì 4 màu pastel riêng biệt.

### 13.3. Typography đề xuất

- **Display/Heading**: font sans-serif đặc trưng, weight 700-800 cho display, 600 cho headline. Có thể custom font (Manrope, Plus Jakarta Sans, Sora, Inter Tight). Roboto hiện tại quá generic.
- **Body**: font khác nếu muốn editorial vibe (kết hợp 1 serif slab cho headline + sans cho body), hoặc 1 sans super-readable cho người lớn tuổi.
- **Vietnamese diacritic test**: ưu tiên font hỗ trợ đầy đủ dấu thanh không vỡ ascender (Be Vietnam Pro, Lexend, Inter).
- Scale rút gọn về 8 cấp thay vì 15 M3 levels.

### 13.4. Radius tokens đề xuất

5 cấp duy nhất:
- xs: 4dp (chip, badge)
- sm: 8dp (button, input)
- md: 12dp (card, sheet handle)
- lg: 20dp (hero card)
- xl: 28dp (bottom bar capsule)

### 13.5. Hierarchy nguyên tắc

1. **1 hero data point per screen** — Card Detail = "Còn lại", WeightInput = "KL thực", Trader Transactions = "Tổng giá trị", Profile = "Doanh thu vụ".
2. **3 cấp visual weight**: Hero (huge + accent) → Primary (medium + neutral) → Secondary (small + muted).
3. **Bỏ "card trong card"** — dùng surface tone + divider thay vì box r12 lồng trong card r20.
4. **Mỗi card đảm bảo 1 mục đích duy nhất** — CardDetail Card 1 đang trộn "Thông tin lô hàng" + "Địa chỉ ruộng" → tách riêng.

### 13.6. Pattern thay thế

| Cũ | Mới |
|---|---|
| Bag grid 5×5 column-major, touch < 48dp | Row-major, touch ≥ 56dp, hoặc list dọc 1-column với index lớn |
| Dialog edit (CardDetail) | Inline edit nhất quán với WeightInput |
| 4 card trắng cùng cấp | 1 hero + 2-3 secondary có tonal contrast |
| Native ad chen Weather/News | Banner ở footer, hoặc tách hẳn lên Premium upsell flow |
| 8 bài news dọc trong Market | LazyRow snippet hoặc tab riêng |
| Filter chip "Tất cả" green | Outline chip + label "Tất cả", chỉ filled khi có filter |
| Snackbar error generic | Inline error gắn field + banner top nếu network-level |
| Avatar = icon role | Avatar thật (upload), fallback initials có gradient |
| Bottom bar 5 tab capsule chật | Cân nhắc rail navigation cho tablet, hoặc grouping (gộp Map vào sub-route của Trader Profile) |

### 13.7. Motion ngôn ngữ

- Spring lowBouncy cho UI feedback (như hiện tại — giữ).
- Tween 200-300ms cho enter/exit page (hiện 250ms FadeScale — giữ).
- **Bỏ** infinite animateColorAsState trên mọi colorScheme token → tốn recomposition vô ích. Chỉ animate khi theme thay đổi (1 lần).
- Pulse 1200ms cho sync (giữ).
- Skeleton shimmer 45° (giữ).
- Haptic tick trên mọi primary action (giữ).

### 13.8. Accessibility checklist

- [ ] Touch target ≥ 48dp (≥ 56dp cho action chính như "Cân lúa", "Tạo phiếu", "Đồng bộ")
- [ ] Contrast AA mọi text trên background; AAA cho số liệu tài chính
- [ ] Font scale 0.9x → 1.2x không vỡ layout (test trên tất cả screen)
- [ ] 5 locale rendering (vi/en/km/lo/zh) không cắt text — bỏ width fixed
- [ ] Voice input cho mọi form text dài (chat, RoleRequest reason)
- [ ] Talkback labels cho icon-only button (lock, refresh, mic, filter)
- [ ] Reduced motion mode (System setting) → tắt pulse + animateColor
- [ ] Color-blind-safe palette: Success/Error/Warning phân biệt được mà không chỉ dựa màu — kèm icon

### 13.9. Brand keywords

| Có | Không |
|---|---|
| "Ruộng đồng" — ấm, gần gũi | "Smart", "AI-powered" buzzword |
| "Tin cậy" — sổ kế toán mini | "Cute", playful |
| "Rõ ràng" — số to, contrast cao | "Sleek", "futuristic" |
| "Không cần dạy" — affordance hiển nhiên | "Power user features" ẩn |
| "Mộc" — màu đất, vàng lúa, xanh lá thật | Gradient kim loại, neon |
| "Bền chắc" — corner radius vừa phải | Bo tròn quá mức (>32dp) trừ pill nav |

### 13.10. Deliverable đề xuất từ design team

1. **Design tokens file** (JSON / Figma Tokens / Style Dictionary)
   - color, typography, spacing, radius, elevation, motion
2. **Component library** trên Figma — đủ variants cho:
   - Button (primary/secondary/tertiary/destructive × default/loading/disabled)
   - Card (default/elevated/outlined × với/không gradient)
   - Input (text/number/dropdown/multi-line × default/focused/error/disabled)
   - Chip (filter/status/trend × selected/unselected)
   - Badge (status/notification/quantity)
   - List item (card/transaction/news/trader)
   - Empty state, Error state, Skeleton
   - Bottom sheet (handle + content)
   - Dialog (info/confirm/destructive)
3. **Screen mockups** cho 27 màn đã liệt kê
4. **Flow diagrams** cho 3 flow chính: Auth, Tạo phiếu → Cân → Chốt QR, Trader quét QR → Xác nhận
5. **Dark mode** đầy đủ
6. **2 ngôn ngữ test** vi + en (tối thiểu)
7. **Prototype Figma** cho 3 flow chính
8. **Animation spec** (Lottie / motion guide)

### 13.11. Phasing đề xuất

| Phase | Mục tiêu | Thời gian gợi ý |
|---|---|---|
| 1 | Design tokens + component library | 2 tuần |
| 2 | Auth + Scale tab (core 60% usage) | 2 tuần |
| 3 | Market + AI Chat | 1.5 tuần |
| 4 | Account + Trader screens | 1.5 tuần |
| 5 | Sub-screens (Settings, Premium, QR, Sync, Deleted) | 1 tuần |
| 6 | Dark mode + Locale test + Prototype | 1 tuần |

---

## Phụ lục: Danh sách file màn hình (cho dev mapping)

| Màn hình | File |
|---|---|
| AuthScreen | `app/src/main/java/com/GiaThinh/canlua/ui/screen/AuthScreen.kt` |
| ProfileSetupScreen | `.../ui/screen/ProfileSetupScreen.kt` |
| RoleRequestScreen | `.../ui/screen/RoleRequestScreen.kt` |
| CardListScreen | `.../ui/screen/CardListScreen.kt` |
| CardDetailScreen | `.../ui/screen/CardDetailScreen.kt` |
| WeightInputScreen | `.../ui/screen/WeightInputScreen.kt` |
| DeletedCardsScreen | `.../ui/screen/DeletedCardsScreen.kt` |
| MarketScreen | `.../ui/screen/market/MarketScreen.kt` |
| AiChatScreen | `.../ui/screen/aichat/AiChatScreen.kt` |
| FarmerProfileScreen | `.../ui/screen/FarmerProfileScreen.kt` |
| TraderProfileScreen | `.../ui/screen/trader/TraderProfileScreen.kt` |
| TraderTransactionsScreen | `.../ui/screen/trader/TraderTransactionsScreen.kt` |
| TraderHistoryScreen | `.../ui/screen/TraderHistoryScreen.kt` |
| RiceMapScreen | `.../ui/screen/map/RiceMapScreen.kt` |
| TraderComingSoonScreen | `.../ui/screen/trader/TraderComingSoonScreen.kt` |
| QrGenerateScreen | `.../ui/screen/qr/QrGenerateScreen.kt` |
| QrScanScreen | `.../ui/screen/qr/QrScanScreen.kt` |
| SettingsScreen | `.../ui/screen/SettingsScreen.kt` |
| PremiumScreen | `.../ui/screen/profile/PremiumScreen.kt` |
| SyncStatusScreen | `.../ui/screen/SyncStatusScreen.kt` |
| Design system | `.../ui/theme/AppColors.kt`, `Color.kt`, `Theme.kt`, `Type.kt` |
| Navigation | `.../ui/navigation/AppNavHost.kt`, `BottomNavItem.kt`, `CanLuaNavigation.kt`, `NavTransitions.kt` |
| Shared components | `.../ui/component/*` (ModernBottomBar, CustomHeader, OfflineStatusBanner, SyncStatusPulse...) |

---

**Tài liệu này được tạo bởi audit tự động + tổng hợp 6 lượt quét song song trên codebase tại commit b28b4ae (2026-05-24).**
