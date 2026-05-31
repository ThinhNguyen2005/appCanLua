# Designer 6 — Sub-screens & Motion

**Phạm vi**: 5 màn hình phụ + 1 spec motion toàn app.
- SettingsScreen
- PremiumScreen
- QrGenerateScreen
- QrScanScreen
- SyncStatusScreen
- **Motion spec global**

**Spec gốc đọc trước**: section 10 (Sub-screens), section 11.8 (Motion).

**Phụ thuộc**: D1 ship token + sheet + dialog + motion tokens (cuối tuần 2).

**Critical pain points fix**: #2 (premium "Early Adopter" fake check), #4 (QR scan sensitive data plaintext), #11 (dark mode toggle UI thiếu), #14 (lock action 2 chỗ inconsistent), #22 (premium screen hardcode VI), #28 (QR scan flashlight thiếu, camera permission denied dead-end), #29 (sync M3 inconsistent), #41 (markdown render edge case), #45-46 (pull-to-refresh delay 800ms + color không đổi theme).

---

## 0. Mục tiêu trải nghiệm

5 màn hình này là "phần lưng" của app — user ít thấy nhưng khi cần phải hoạt động ngon. Motion thì hiện tại loạn: chỗ thì M3 default, chỗ thì hardcode 300ms, chỗ thì không có animation. Redesign:

1. **Settings** — IA rõ ràng, group theo function, không list 15 row flat
2. **Premium** — minh bạch về Early Adopter, có roadmap, fix verify logic
3. **QR** — bảo mật thông tin nhạy cảm (CCCD, SĐT) + camera UX đầy đủ
4. **Sync** — visual feedback rõ về state, retry control
5. **Motion** — 1 bộ tokens duration + easing, áp dụng nhất quán

---

## 1. SettingsScreen

### 1.1. Yêu cầu chức năng

Tổ hợp các cài đặt:
- Giao diện (theme light/dark/system) — FIX pain #11
- Ngôn ngữ (5 locale)
- Font scale (0.9× - 1.2×)
- Thông báo (push permissions per channel)
- Đồng bộ (manual sync + auto sync toggle)
- Tài khoản (đổi email/password/SĐT, delete account)
- Bảo mật (PIN lock app, biometric)
- Storage (clear cache, export data)
- Về app (version, terms, privacy, licenses, feedback)
- **Developer options** (debug build only): role switcher, log viewer, force crash test

### 1.2. Layout đề xuất

```
┌──────────────────────────────────┐
│ [← back]  Cài đặt                │
├──────────────────────────────────┤
│  ┌─ Search bar (optional) ─────┐ │
│  │ [🔍] Tìm cài đặt...          │ │  ← search settings keyword
│  └─────────────────────────────┘ │
│                                  │
│  GIAO DIỆN                        │  ← section label uppercase caption
│  ┌─ List row 56dp ──────────────┐│
│  │ [🌙] Chế độ                   ││
│  │      Sáng / Tối / Hệ thống   ││  ← inline value text.secondary
│  │                       [chevron]│
│  └──────────────────────────────┘│
│  ┌──────────────────────────────┐│
│  │ [🌐] Ngôn ngữ                 ││
│  │      Tiếng Việt              ││
│  └──────────────────────────────┘│
│  ┌──────────────────────────────┐│
│  │ [Aa] Cỡ chữ                   ││
│  │      Bình thường (1.0×)      ││  
│  │      [—————●————] slider      ││  ← inline slider preview
│  └──────────────────────────────┘│
│                                  │
│  THÔNG BÁO                        │
│  ┌──────────────────────────────┐│
│  │ [🔔] Tất cả thông báo  [●━━]  ││  ← master toggle
│  ├──────────────────────────────┤│
│  │ [💵] Giá lúa biến động  [━━●]││
│  │ [🌧] Thời tiết           [●━━]││
│  │ [📰] Tin nông nghiệp     [━━●]││
│  │ [💬] Tin nhắn từ Trader  [●━━]││
│  └──────────────────────────────┘│
│                                  │
│  ĐỒNG BỘ                          │
│  ┌──────────────────────────────┐│
│  │ [☁] Tự động đồng bộ    [●━━] ││
│  │      Khi có wifi / mọi mạng  ││  ← sub-row chip toggle
│  ├──────────────────────────────┤│
│  │ [🔄] Đồng bộ ngay            ││  
│  │      Lần cuối: 09:32         ││
│  └──────────────────────────────┘│
│                                  │
│  TÀI KHOẢN                        │
│  ┌──────────────────────────────┐│
│  │ [🔑] Đổi mật khẩu             ││
│  ├──────────────────────────────┤│
│  │ [✉] Đổi email                ││
│  ├──────────────────────────────┤│
│  │ [📱] Số điện thoại            ││
│  └──────────────────────────────┘│
│                                  │
│  BẢO MẬT                          │
│  ┌──────────────────────────────┐│
│  │ [🔒] Khoá ứng dụng    [●━━]  ││
│  │      Mã PIN / Vân tay        ││
│  ├──────────────────────────────┤│
│  │ [👤] Yêu cầu xác thực khi    ││
│  │      mở thông tin nhạy cảm    ││
│  └──────────────────────────────┘│
│                                  │
│  BỘ NHỚ                           │
│  ┌──────────────────────────────┐│
│  │ [💾] Dung lượng đã dùng       ││
│  │      245 MB                   ││
│  │      [▓▓▓░░░░░] 30% trong 1GB ││
│  ├──────────────────────────────┤│
│  │ [🗑] Xoá cache                ││  
│  │      125 MB                   ││
│  ├──────────────────────────────┤│
│  │ [📥] Xuất dữ liệu             ││
│  └──────────────────────────────┘│
│                                  │
│  VỀ ỨNG DỤNG                      │
│  ┌──────────────────────────────┐│
│  │ [ⓘ] Phiên bản 1.2.3 (142)    ││
│  ├──────────────────────────────┤│
│  │ [📖] Điều khoản                ││
│  ├──────────────────────────────┤│
│  │ [🔒] Chính sách                ││
│  ├──────────────────────────────┤│
│  │ [📜] Giấy phép mã nguồn        ││
│  ├──────────────────────────────┤│
│  │ [💬] Phản hồi & báo lỗi        ││
│  └──────────────────────────────┘│
│                                  │
│  ─── DEBUG OPTIONS (dev only) ───│  ← chỉ visible BuildConfig.DEBUG
│  ┌──────────────────────────────┐│
│  │ [⚠] Chuyển vai trò            ││  ← FIX pain #3 — chỉ dev build
│  │     [FARMER ▾]                ││
│  ├──────────────────────────────┤│
│  │ [🐛] Log viewer                ││
│  ├──────────────────────────────┤│
│  │ [💥] Force crash test          ││
│  └──────────────────────────────┘│
└──────────────────────────────────┘
```

### 1.3. Theme picker (FIX pain #11)

Tap row "Chế độ" → bottom sheet:
```
┌─ Sheet ───────────────────────────┐
│ Chế độ giao diện                  │
├───────────────────────────────────┤
│  ┌─ Option card ───────────────┐  │
│  │ [☀ illustration 80dp]        │  │
│  │ Sáng                         │  │  ← radio selected
│  └──────────────────────────────┘  │
│  ┌─ Option card ───────────────┐  │
│  │ [🌙 illustration]            │  │
│  │ Tối                          │  │
│  └──────────────────────────────┘  │
│  ┌─ Option card ───────────────┐  │
│  │ [📱 illustration]            │  │
│  │ Theo hệ thống                │  │  ← default
│  └──────────────────────────────┘  │
│                                    │
│  [Áp dụng]                         │
└────────────────────────────────────┘
```

Apply: theme thay đổi instant với crossfade 300ms (xem Motion spec).

### 1.4. Pain point fix

| ID | Fix |
|---|---|
| #3 | Role switcher chỉ visible khi `BuildConfig.DEBUG = true` |
| #11 | Theme picker sheet với 3 option illustrated + system option |

---

## 2. PremiumScreen

### 2.1. Yêu cầu chức năng

Hiển thị status Premium hiện tại + benefits + upgrade path.

Logic Premium:
- **Early Adopter**: install date < 2026-12-31 → free Premium hết hạn 22/05/2026
- **Free**: hết hạn Early Adopter, downgrade về free tier (limited features)
- **Paid**: subscription monthly/yearly (Sprint 6+, hiện tại UI placeholder)

**FIX pain #2**: hiện tại `firstInstallTime` từ `PackageManager` — reinstall app reset → giả Early Adopter. Cần backend verify (Firestore `users/{uid}/premium` với serverTimestamp lần đăng ký đầu).

### 2.2. Layout đề xuất

**State: Early Adopter active**
```
┌──────────────────────────────────┐
│ [← back]                         │
│                                  │
│  ┌─ Hero card gradient ───────┐  │
│  │   [Premium crown 64dp]      │  │  ← brand.accent gradient bg
│  │   Bạn là Early Adopter      │  │  ← display 24sp 800w
│  │   ✨ Miễn phí 100% tính       │  │
│  │   năng Premium               │  │
│  │                              │  │
│  │   Hết hạn: 22/05/2026        │  │  ← caption white
│  │   [▓▓▓▓▓░░] 65% còn lại      │  │  ← progress bar visual
│  └──────────────────────────────┘ │
│                                  │
│  Tính năng đang dùng              │
│  ┌─ Feature list ──────────────┐ │
│  │ ✓ AI tư vấn không giới hạn   │ │
│  │ ✓ Xuất Excel chi tiết         │ │
│  │ ✓ Đồng bộ Cloud không giới hạn│
│  │ ✓ Sao lưu tự động hằng ngày  │ │
│  │ ✓ Bản đồ Trader chi tiết      │ │
│  │ ✓ Không quảng cáo            │ │
│  │ ✓ Hỗ trợ ưu tiên             │ │
│  └─────────────────────────────┘ │
│                                  │
│  Kế hoạch sau Early Adopter       │
│  ┌─ Plan card ─────────────────┐ │
│  │ Sau 22/05/2026               │ │
│  │ Bạn có thể:                  │ │
│  │  · Tiếp tục free (giới hạn)  │ │
│  │  · Nâng cấp Premium 49k/tháng│ │
│  │  · Premium yearly 399k/năm   │ │
│  │                              │ │
│  │  [Xem chi tiết →]            │ │
│  └─────────────────────────────┘ │
│                                  │
│  [💬 Phản hồi về Premium]         │
└──────────────────────────────────┘
```

**State: Free (sau Early Adopter)**
```
┌─ Hero card ────────────────────┐
│   [Crown grey 64dp]             │
│   Bạn đang dùng bản Free        │
│   Một số tính năng giới hạn     │
└─────────────────────────────────┘

So sánh Free / Premium
┌─ Feature comparison table ─────┐
│ Tính năng    Free    Premium    │
│ Tạo phiếu    50/tháng  ∞         │
│ AI chat      10/ngày   ∞         │
│ Sync         Manual    Auto      │
│ Xuất Excel   ✗         ✓         │
│ Quảng cáo    Có        Không     │
└────────────────────────────────┘

[Nâng cấp Premium - 49k/tháng]  ← primary CTA
[Yearly - 399k (tiết kiệm 17%)] ← secondary highlighted
```

### 2.3. States

- **Early Adopter active** (90+ ngày còn)
- **Early Adopter expiring** (<14 ngày → banner cảnh báo orange)
- **Early Adopter expired → free downgrade**
- **Free** (full comparison + upgrade CTA)
- **Paid monthly** (status + invoice history + cancel)
- **Paid yearly**
- **Loading** (skeleton hero + list)
- **Offline** (cached status + banner)
- **Error verify** (FIX pain #2): "Không thể xác minh trạng thái. Thử lại?" + retry
- **Dark mode** (hero card adjust luminance)

### 2.4. Locale (FIX pain #22)

Hiện tại screen hardcode VI. Cần:
- Mọi text dùng `R.string.premium_*`
- Số tiền dùng `NumberFormat.getCurrencyInstance(Locale.current)` — không hardcode "đ"
- Date format theo locale (DateUtils.formatDateTime)
- Designer thiết kế UI giả định 1.5× text length cho EN/KM/LO

### 2.5. Pain point fix

| ID | Fix |
|---|---|
| #2 | Verify status từ Firestore `users/{uid}/premium.installSeen` serverTimestamp; UI có error state khi verify fail |
| #22 | Mọi text dùng R.string, currency + date format theo locale |

---

## 3. QrGenerateScreen

### 3.1. Yêu cầu chức năng

FARMER share thông tin phiếu cân cho TRADER quét. Payload chứa:
- Card ID (UUID)
- Owner phone (display)
- Owner name (display)
- Bag entries summary
- Total amount

**FIX pain #4**: hiện tại payload là JSON plaintext có CCCD + email + full address — quá nhiều thông tin nhạy cảm. Redesign:
- Payload chỉ chứa `cardId` + ephemeral token (TTL 5 phút) signed bằng Firebase Function
- TRADER scan → call Function verify token → fetch card public data
- Sensitive fields (CCCD/email/address chi tiết) **không bao giờ** trong QR

### 3.2. Layout đề xuất

```
┌──────────────────────────────────┐
│ [← back]  Chia sẻ phiếu cân      │
├──────────────────────────────────┤
│                                  │
│  ┌─ Card info chip ──────────┐   │
│  │ 🌾 Phiếu #ABC123           │   │
│  │ Lúa OM5451 · 5 tấn         │   │
│  └────────────────────────────┘   │
│                                  │
│         ┌─────────────────┐       │
│         │                 │       │
│         │   [QR code]     │       │  ← 240×240dp center
│         │                 │       │     border 8dp white pad
│         │                 │       │
│         └─────────────────┘       │
│                                  │
│   ⓘ Mã có hiệu lực 5 phút         │  ← caption + countdown timer
│      [Hết sau 4:42]              │     dùng status.warning chip khi <1min
│                                  │
│  Hoặc gửi link                    │
│  ┌─ URL display ─────────────┐    │
│  │ canluavn.web.app/share/   │    │  ← truncated middle
│  │ ABC1...XYZ                 │    │
│  │  [📋 Copy]   [↗ Share]     │    │
│  └────────────────────────────┘   │
│                                  │
│  ┌─ Primary CTA h56 ─────────┐   │
│  │  📲 Chia sẻ ngay            │   │  ← system share sheet
│  └────────────────────────────┘   │
│                                  │
│  ┌─ Tertiary CTA ────────────┐   │
│  │  🔄 Tạo mã mới             │   │  ← regenerate (extend TTL)
│  └────────────────────────────┘   │
└──────────────────────────────────┘
```

### 3.3. States

- **Default** (QR active, countdown 5:00)
- **Countdown warning** (<1 phút, chip warning đỏ)
- **Expired** (QR grey overlay + button "Tạo mã mới")
- **Generating** (skeleton QR + spinner)
- **Error** (generate fail, retry)
- **Offline** (cached QR nếu vừa generate; banner "Cần mạng để tạo mới")
- **Dark mode** (QR đảo màu đen-trắng để contrast)

### 3.4. Pain point fix

| ID | Fix |
|---|---|
| #4 | QR payload chỉ cardId + ephemeral token; sensitive data fetch sau verify; countdown TTL 5 phút visible |

---

## 4. QrScanScreen

### 4.1. Yêu cầu chức năng

Camera live preview với QR detection. TRADER scan QR FARMER tạo → verify → mở CardDetail.

### 4.2. Layout đề xuất

```
┌──────────────────────────────────┐
│ [Camera preview full screen]     │
│                                  │
│  ┌─ Top bar overlay ──────────┐  │
│  │ [← back]  Quét mã          │  │  ← glass background
│  │            [🔦] [🖼 Gallery] │  │
│  └────────────────────────────┘  │
│                                  │
│         ┌──┐         ┌──┐         │  ← scan frame 240×240
│         │  │         │  │         │     corner brackets only
│         └──┘         └──┘         │     animated laser line
│                                  │
│            [scan laser]           │
│              ━━━━━━━━              │  ← animated pulse top→bottom
│                                  │
│         ┌──┐         ┌──┐         │
│         │  │         │  │         │
│         └──┘         └──┘         │
│                                  │
│  ┌─ Bottom hint ─────────────┐   │
│  │ Đặt mã QR vào khung         │   │
│  │ Tự động quét khi phát hiện  │   │
│  └────────────────────────────┘   │
│                                  │
│  [Bottom controls floating]      │
│  [🔦 flashlight toggle]            │  ← FIX pain #28
│  [📷 switch camera]               │
└──────────────────────────────────┘
```

### 4.3. States cần spec

- **Default scanning** (laser animation, idle)
- **Detected** (frame turn brand.primary, haptic vibration, "Đã tìm thấy mã" caption)
- **Verifying** (frame keep + spinner overlay "Đang xác minh...")
- **Success** (checkmark green animation 600ms → navigate CardDetail)
- **Invalid QR** (frame red + caption "Mã không hợp lệ" + auto-resume sau 1.5s)
- **Expired QR** (sheet bottom "Mã đã hết hạn. Hỏi nông dân tạo mã mới?")
- **Camera permission denied** (FIX pain #28): full-screen empty state
  ```
  [Illustration camera-off 200dp]
  Cần quyền camera để quét mã

  Đi tới Cài đặt → Quyền → Bật Camera
  
  [Mở cài đặt →]   ← deep link Settings.intent
  [Quét từ ảnh →]  ← fallback dùng gallery
  ```
- **Camera permission permanently denied** (don't ask again):
  same + emphasize "Mở cài đặt"
- **Flashlight on** (icon filled brand.accent)
- **Flashlight unavailable** (icon disabled grey + tooltip)
- **Low light auto-suggest flashlight** ("Ánh sáng yếu — bật đèn?")
- **Gallery pick mode** (image picker → scan QR từ ảnh, fallback nếu camera fail)
- **Dark mode** (camera preview vẫn là raw, control overlay adapt)

### 4.4. Pain point fix

| ID | Fix |
|---|---|
| #28 | Flashlight toggle + camera permission empty state có hướng dẫn + gallery fallback |

---

## 5. SyncStatusScreen

### 5.1. Yêu cầu chức năng

Hiển thị real-time status của SyncManager:
- Items pending (count + breakdown by type: cards / weights / transactions)
- Last sync timestamp
- Current state: idle / syncing / error / offline
- Manual retry button
- Conflict list (nếu có)

### 5.2. Layout đề xuất

```
┌──────────────────────────────────┐
│ [← back]  Đồng bộ                │
├──────────────────────────────────┤
│                                  │
│  ┌─ Status hero card ─────────┐  │
│  │ [Animated sync icon 48dp]   │  │  ← icon spin khi syncing
│  │                             │  │     check khi idle ok
│  │ Đang đồng bộ...              │  │     warning khi error
│  │ 3/12 phiếu                   │  │
│  │ [▓▓▓░░░░░░] 25%             │  │  ← progress bar
│  │                             │  │
│  │ Lần cuối: 09:32              │  │
│  └─────────────────────────────┘ │
│                                  │
│  Chi tiết                         │
│  ┌─ Stat row ─────────────────┐  │
│  │ ☁ Đã đồng bộ        2.341  │  │
│  │ ⏳ Đang chờ          9      │  │  ← chip warning if > 0
│  │ ⚠ Lỗi               0      │  │
│  │ 🔒 Đã khoá           45     │  │
│  └────────────────────────────┘  │
│                                  │
│  Hoạt động gần đây                │
│  ┌─ Activity log ─────────────┐  │
│  │ 09:32 ✓ Sync 12 phiếu       │  │
│  │ 09:30 ✓ Sync 3 bag entries  │  │
│  │ 08:15 ⚠ Network timeout      │  │  ← retry chip inline
│  │       [Thử lại]              │  │
│  │ ...                          │  │
│  └─────────────────────────────┘ │
│                                  │
│  ┌─ Primary CTA h56 ─────────┐   │
│  │  🔄 Đồng bộ ngay            │   │  ← disabled khi syncing
│  └────────────────────────────┘   │
│                                  │
│  Cài đặt                          │
│  ┌─ List ─────────────────────┐  │
│  │ Tự động đồng bộ      [●━━]│  │
│  │ Chỉ wifi             [━━●]│  │
│  │ Thông báo khi xong   [●━━]│  │
│  └────────────────────────────┘  │
└──────────────────────────────────┘
```

### 5.3. Hero card states

- **Idle ok** (icon check brand.primary, "Đã đồng bộ", "Lần cuối: 09:32")
- **Syncing** (icon spin brand.primary, "Đang đồng bộ X/Y", progress bar)
- **Pending offline** (icon cloud-off, "9 mục chờ đồng bộ", "Sẽ tự sync khi có mạng")
- **Error** (icon warning sem.error, "Đồng bộ lỗi: <message>", retry button visible)
- **Conflict** (icon merge, "Phát hiện xung đột", "Xem chi tiết →" mở screen riêng)

### 5.4. Conflict resolver flow (advanced state)

Nếu cùng card sửa ở 2 thiết bị (offline trên cả 2, sau khi sync xung đột), show conflict screen:
```
[← back]  Xung đột (1)

┌─ Conflict card ────────────────┐
│ ⚠ Phiếu OM5451 #ABC123          │
│                                 │
│ Bạn: 5.2 tấn · 41M               │  ← left side
│ ──────  Thiết bị khác  ──────    │
│ Cloud: 5.0 tấn · 40M             │  ← right side
│                                 │
│ Sửa lần cuối:                   │
│ Bạn: 09:30 (offline)             │
│ Cloud: 09:35                     │
│                                 │
│  [Giữ bản của bạn]               │  ← keep local
│  [Dùng bản cloud]                │  ← use remote
│  [Xem chi tiết để chọn]          │  ← merge manual
└────────────────────────────────┘
```

### 5.5. Pain point fix

| ID | Fix |
|---|---|
| #29 | Sync screen có hero card hierarchy + stat row + activity log, không list flat M3 default |
| #45 | Pull-to-refresh dùng motion token, không delay 800ms hardcode |
| #46 | Indicator dùng `brand.primary` token, đổi theo theme |
| #44 | Error state có icon affordance + retry CTA |

---

## 6. MOTION SPEC GLOBAL

### 6.1. Duration tokens (dùng D1)

```
motion.duration.instant   = 0ms       (toggle, hover)
motion.duration.fast      = 150ms     (chip select, button press)
motion.duration.normal    = 250ms     (modal open, sheet)
motion.duration.slow      = 400ms     (screen transition, drawer)
motion.duration.deliberate= 600ms     (success celebration, onboarding)
```

### 6.2. Easing tokens

```
motion.ease.standard    = cubic-bezier(0.4, 0.0, 0.2, 1)   // M3 standard
motion.ease.emphasized  = cubic-bezier(0.2, 0.0, 0.0, 1.0) // emphasis transitions
motion.ease.decelerate  = cubic-bezier(0, 0, 0.2, 1)       // entering
motion.ease.accelerate  = cubic-bezier(0.4, 0, 1, 1)       // exiting
motion.ease.spring      = SpringSpec(dampingRatio=0.7, stiffness=400)
```

### 6.3. Mapping per UI pattern

| Pattern | Property | Duration | Easing |
|---|---|---|---|
| Screen entrance | translateY 20→0 + alpha 0→1 | normal | emphasized |
| Screen exit | translateY 0→-20 + alpha 1→0 | fast | accelerate |
| Drawer slide | translateX (-100% → 0) | slow | emphasized |
| Bottom sheet | translateY + scrim opacity | normal | emphasized |
| Dialog | scale 0.9→1 + alpha | normal | spring |
| Chip select | scale 0.95→1 + bg color | fast | standard |
| Button press | scale 1→0.97 | instant | standard |
| Snackbar | translateY + alpha | normal | decelerate |
| FAB scroll hide | translateY + scale | fast | standard |
| Skeleton shimmer | gradient offset | 1200ms loop | linear |
| Pulse | scale 1→1.05→1 | 1500ms loop | standard |
| Cross-fade theme | alpha | normal | standard |
| Cross-fade locale | alpha | normal | standard |
| Tab switch | alpha | fast | standard |
| Pull-to-refresh | translateY indicator | normal | spring |
| QR scan laser | translateY loop | 2000ms | linear |
| Streaming cursor | alpha 0→1 | 600ms loop | standard |
| Success checkmark | path draw | deliberate | standard |
| Confetti | particle physics | deliberate | spring |

### 6.4. Reduced motion fallback

Khi `Settings.Global.TRANSITION_ANIMATION_SCALE = 0` hoặc System "Reduce motion" ON:
- Tắt: skeleton shimmer, pulse, scan laser, streaming cursor blink, confetti, FAB scale-hide animation, pull-to-refresh spring
- Chỉ giữ: alpha cross-fade (đỡ jarring) ở duration `fast`
- Hero number animated count-up → static
- Drawer slide → fade
- Sheet slide → fade

### 6.5. Reduced motion test

Designer phải showcase 1 màn (đề xuất AiChat + Market) với:
- Normal motion
- Reduced motion ON
So sánh side-by-side trong prototype.

### 6.6. Pain point fix

| ID | Fix |
|---|---|
| #45 | Pull-to-refresh dùng spring + normal duration thay 800ms hardcode delay |
| #46 | Pull indicator dùng `brand.primary` token, đổi theo theme automatically |

---

## 7. Acceptance criteria

- [ ] 5 màn × multi-state = ≥25 layout
- [ ] Motion spec table với 20+ pattern + duration + easing + reduced-motion variant
- [ ] Showcase reduced motion comparison 2 màn (Market + AiChat) prototype
- [ ] Settings có search bar (optional) + group section + debug section conditional
- [ ] Premium có 5 state lifecycle (active / expiring / expired / free / paid)
- [ ] QrGenerate có TTL countdown + expired state + payload security note in spec
- [ ] QrScan có 8 state (scan / detect / verify / success / invalid / expired / permission denied / gallery fallback)
- [ ] SyncStatus có 5 hero state + conflict resolver
- [ ] Light + Dark cả 5 màn
- [ ] 2 locale showcase (vi + en) Premium screen (FIX #22)
- [ ] Font scale 1.2× Settings + Premium hero
- [ ] Tag fix pain points #2, #3, #4, #11, #14, #22, #28, #29, #44, #45, #46
- [ ] Prototype flow: Settings → tap "Chế độ" → sheet 3 option → Tối → instant crossfade
- [ ] Prototype flow: Premium Early Adopter expiring → upgrade tap → comparison table
- [ ] Prototype flow: QrGenerate → countdown → expired → regenerate
- [ ] Prototype flow: QrScan permission denied → Open Settings → grant → back → scan
- [ ] Prototype flow: SyncStatus syncing → done check + snackbar
- [ ] Hand-off: motion spec table riêng + Compose code snippet đề xuất (anim spec) cho dev

---

## 8. Timeline

| Tuần | Output |
|---|---|
| **1** | Motion principles document. Wireframe 5 sub-screen. |
| **2** | Hi-fi Settings + Premium dùng D1 token. Motion token table draft. |
| **3** | Hi-fi QrGenerate + QrScan + SyncStatus. Conflict resolver flow. |
| **4** | Motion spec final + reduced-motion comparison. QA + 2 locale. |
| **5** | Polish edge cases (empty sync, expired QR, low-light scan). |
| **6** | Final motion library prototype + Compose anim snippet hand-off. |

---

## 9. References

- **Spec gốc**: section 10 (Sub-screens), section 11.8 (Motion)
- **Files implementation**:
  - `app/src/main/java/com/GiaThinh/canlua/ui/screen/SettingsScreen.kt`
  - `app/src/main/java/com/GiaThinh/canlua/ui/screen/PremiumScreen.kt`
  - `app/src/main/java/com/GiaThinh/canlua/ui/screen/QrGenerateScreen.kt`
  - `app/src/main/java/com/GiaThinh/canlua/ui/screen/QrScanScreen.kt`
  - `app/src/main/java/com/GiaThinh/canlua/ui/screen/SyncStatusScreen.kt`
  - `app/src/main/java/com/GiaThinh/canlua/ui/navigation/NavTransitions.kt`
  - `app/src/main/java/com/GiaThinh/canlua/data/sync/SyncManager.kt`
- **Inspiration**:
  - Settings: iOS Settings (grouping clarity), Linear settings (search), Notion (deep IA)
  - Premium: Spotify Premium screen (clear comparison), Apple One (lifecycle states)
  - QR: WeChat QR (overlay polish), Snapchat scan (animation)
  - Sync: Dropbox sync icon states, iCloud status hierarchy
  - Motion: Material 3 Motion guidelines, Apple HIG Reduce Motion section
- **Lib có sẵn**: ML Kit Barcode (QR scan), CameraX, WorkManager (sync), Markwon
