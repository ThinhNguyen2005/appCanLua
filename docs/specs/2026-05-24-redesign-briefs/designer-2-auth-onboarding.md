# Designer 2 — Auth & Onboarding

**Phạm vi**: 3 màn hình + splash transition.
- AuthScreen (login)
- ProfileSetupScreen
- RoleRequestScreen

**Spec gốc đọc trước**: section 4 (Auth flow).

**Phụ thuộc**: D1 ship token + button + input + social-button + OTP field + dialog (cuối tuần 2).

**Critical pain points fix**: #16 (Auth dead-end snackbar), #17 (auto-detect phone/email không có affordance), #18 (error snackbar không gắn field), #19 (UI cho TRADER nhưng backend không grant), #20 (RoleRequest hardcode tiếng Việt), #21 (APPROVED bắt logout/login thủ công).

---

## 0. Mục tiêu trải nghiệm

**First impression chiếm 30 giây** của user mới. Hiện tại Auth có 3 dead-end ("Quên MK" snackbar, "Điều khoản" snackbar, password field biến mất bí ẩn khi gõ số). Redesign phải:
1. Không có dead-end — mọi tap dẫn đến state hợp lý
2. Affordance rõ ràng — user biết đang ở chế độ Email hay SĐT
3. Error gắn field — không snackbar generic
4. Onboarding tránh "ghosting" — không hứa Trader rồi bắt admin duyệt mà không nói trước

---

## 1. AuthScreen (login)

### 1.1. Yêu cầu chức năng (giữ nguyên backend)

Backend đã có:
- Email/Password (Firebase Auth)
- Phone OTP (+84, format `0xxx` → `+84xxx`)
- Google Sign-In (Credential Manager)
- Toggle login ↔ register
- Validate: email regex / phone regex `^[3-9]\d{8}$` / mật khẩu ≥ 6
- OTP timeout 60s, resend được

### 1.2. Layout đề xuất

**Wireframe ASCII** (đề xuất, designer có thể đổi):
```
┌─────────────────────────────────┐
│ (status bar)                    │
│                                 │
│         [Logo 64dp]             │
│         Cân Lúa                 │  ← text.h1
│  Số tay điện tử cho ruộng       │  ← text.body text.secondary
│                                 │
│  ┌─[ Email | SĐT | Google ]──┐  │  ← Segmented control 3 options
│  └─────────────────────────────┘  │
│                                 │
│  ┌─ Input field ─────────────┐  │
│  │ [icon] Email              │  │  ← text 16sp, h56
│  └───────────────────────────┘  │
│                                 │
│  ┌─ Input field ─────────────┐  │
│  │ [icon] Mật khẩu     [👁]  │  │
│  └───────────────────────────┘  │
│   helper: 6 ký tự trở lên       │  ← caption text.tertiary
│                                 │
│              Quên mật khẩu? →   │  ← tertiary button right
│                                 │
│  ┌─ Primary CTA h56 ──────────┐  │
│  │      Đăng nhập             │  │  ← brand.primary
│  └───────────────────────────┘  │
│                                 │
│  ─────── hoặc ───────           │
│                                 │
│  ┌─ Secondary CTA h56 ────────┐  │
│  │  [G] Tiếp tục với Google   │  │
│  └───────────────────────────┘  │
│                                 │
│  Chưa có tài khoản? Đăng ký →   │
│                                 │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│  Điều khoản · Chính sách        │  ← footer links text.tertiary
└─────────────────────────────────┘
```

### 1.3. 3 mode rõ ràng (FIX pain #17)

Thay vì auto-detect ngầm, dùng **Segmented control 3 tab** ở top:
- **Email** (default)
- **Số điện thoại**
- **Google** (nếu user chọn tab này, ẩn 2 input và show 1 button Google full-width thay)

Lý do: user không phải đoán app đang hiểu họ là email hay SĐT.

### 1.4. Email mode

- Field 1: Email (keyboard email, autofill `email`)
- Field 2: Mật khẩu (eye toggle, autofill `password`)
- Validation:
  - Onblur regex check
  - Inline error dưới field, không snackbar (FIX pain #18)
- Primary CTA: "Đăng nhập" / "Đăng ký" tùy mode
- Toggle ở dưới: "Chưa có tài khoản? Đăng ký →" (text link)
- "Quên mật khẩu?" → **mở dialog reset password** với email field + nút "Gửi link reset" — không snackbar dead-end (FIX pain #16)

### 1.5. Phone mode

**State 1: Nhập số**
```
[Segmented: Email | (Số điện thoại) | Google]

┌─ Input ──────────────────┐
│ +84 │ 9xx xxx xxx        │  ← prefix +84 lock, format nhóm 3-3-3
└──────────────────────────┘
  helper: 9 chữ số sau +84

┌─ Primary CTA h56 ────────┐
│     Gửi mã OTP           │
└──────────────────────────┘
```

**State 2: Nhập OTP**
```
[← back to edit phone]

Mã đã gửi tới +84 9xx xxx 123
                                  ← editable: bấm "← back" sửa số

┌─[ ][ ][ ][ ][ ][ ]──────┐  ← 6 ô riêng, h56 w48, auto-advance
└─────────────────────────┘

Gửi lại mã sau 0:48        ← countdown timer text caption text.tertiary
                                Khi 0:00 → button tertiary "Gửi lại mã"

┌─ Primary CTA h56 ────────┐
│     Xác nhận             │  ← disabled khi chưa đủ 6 chữ số
└──────────────────────────┘
```

### 1.6. Google mode

```
[Segmented: Email | SĐT | (Google)]

         [G logo 80dp]
        Đăng nhập với Google

  Tài khoản Google của bạn chỉ
  dùng để xác thực, không lưu
  email hay danh bạ.            ← text.body text.secondary

┌─ Primary CTA h56 ────────┐
│  [G] Chọn tài khoản      │
└──────────────────────────┘
```

### 1.7. States cần spec

- **Default** (empty form)
- **Filled** (user đã nhập)
- **Focused** (1 field active, border 2dp `border.focus`)
- **Error** (helper text dưới field màu `sem.error`, icon alert leading)
- **Loading** (CTA spinner, form disabled, opacity 60%)
- **OTP countdown active** (state 2 phone mode)
- **OTP resend available** (countdown 0)
- **IME open** (keyboard up, content scroll lên, CTA vẫn visible)
- **Network error banner top** (offline, "Không có mạng. Đăng nhập sẽ thử lại khi có mạng.")
- **Dark mode** đầy đủ

### 1.8. Affordance fix

- Mode tab có icon kèm label
- Field 1 icon đổi theo mode (Email: envelope, Phone: phone với +84 chip)
- Password eye toggle hover state rõ
- CTA disabled state dùng `text.disabled` + `surface.3` bg, không phải opacity hardcode
- "Quên mật khẩu" mở dialog, không snackbar

### 1.9. Pain points đã fix

| ID | Fix |
|---|---|
| #16 | "Quên MK" → dialog reset, "Điều khoản" → mở screen web view (D6 spec) |
| #17 | Segmented 3 mode rõ ràng thay auto-detect |
| #18 | Inline error gắn field, kèm icon, không snackbar |

---

## 2. ProfileSetupScreen

### 2.1. Yêu cầu chức năng

User mới sau khi sign-in lần đầu — chưa có profile. Yêu cầu:
- Họ tên (bắt buộc, min 2 ký tự)
- Vai trò (FARMER / TRADER)
- Số điện thoại (optional nếu đã đăng nhập bằng Email/Google)
- CCCD (optional, 12 số)
- Địa chỉ (optional, free text)
- Quê quán (optional)

Đặc biệt: chọn TRADER → KHÔNG được set role TRADER ngay, phải redirect qua RoleRequestScreen (FIX pain #19).

### 2.2. Layout đề xuất — chia 2-step thay vì 1 form dài

**Step 1: Vai trò (full screen, không scroll)**

```
┌──────────────────────────────────┐
│  [← skip]            [progress]  │  ← 1/2 dots
│                                  │
│  Chào mừng đến Cân Lúa!          │  ← text.h1
│  Bạn là...?                      │  ← text.body text.secondary
│                                  │
│  ┌─ Card option FARMER ────────┐ │
│  │  [Illustration 96dp]         │ │
│  │  Nông dân                    │ │  ← text.h2
│  │  Tôi trồng và bán lúa        │ │  ← text.body text.secondary
│  │  ✓ Ghi sổ phiếu cân           │ │
│  │  ✓ Theo dõi công nợ           │ │
│  │  ✓ Tư vấn AI canh tác         │ │
│  └─────────────────────────────┘ │
│                                  │
│  ┌─ Card option TRADER ────────┐ │
│  │  [Illustration 96dp]         │ │
│  │  Thương lái                  │ │
│  │  Tôi thu mua lúa             │ │
│  │  ✓ Quét QR chốt phiếu         │ │
│  │  ✓ Quản lý công nợ thương lái │ │
│  │  ✓ Bản đồ điểm thu mua        │ │
│  │  ⓘ Cần admin duyệt           │ │  ← warning chip nhỏ
│  └─────────────────────────────┘ │
└──────────────────────────────────┘
```

Card được selected có border 2dp `brand.primary` + checkmark badge góc trên phải.

**Step 2: Thông tin cá nhân (scrollable form)**

```
[← back]                  [progress 2/2]

Cho mình biết về bạn

┌─ Avatar upload (optional) ─┐
│      [📷 64dp circle]       │  ← tap to upload, optional
│      Thêm ảnh đại diện      │
└────────────────────────────┘

┌─ Input ─────────────────┐
│ Họ tên *                │  ← floating label
└─────────────────────────┘

┌─ Input ─────────────────┐
│ Số điện thoại           │  ← prefilled nếu đăng nhập bằng phone
└─────────────────────────┘

┌─ Input ─────────────────┐
│ Quê quán                │  ← dropdown 63 tỉnh thành VN
└─────────────────────────┘

┌─ Input ─────────────────┐
│ Địa chỉ chi tiết        │  ← optional, multi-line
└─────────────────────────┘

┌─ Input ─────────────────┐
│ CCCD (12 số)            │  ← optional, format 4-4-4
└─────────────────────────┘

┌─ Primary CTA h56 ───────┐
│      Hoàn tất            │
└─────────────────────────┘
```

Nếu user chọn TRADER ở step 1, sau khi submit step 2 → redirect sang RoleRequestScreen tự động (không hoàn tất profile vào main app).

### 2.3. States

- Step 1 default / Step 1 selected (1 trong 2)
- Step 2 default / focused / filled / error
- Loading (submit)
- Network error banner
- Dark mode

### 2.4. Pain point fix

| ID | Fix |
|---|---|
| #19 | TRADER ở step 1 hiển thị warning chip "Cần admin duyệt" rõ; submit step 2 → RoleRequest, không silently bypass |
| #34 | CCCD field format `xxxx-xxxx-xxxx` (12 số) tự động |
| #33 | Edit toggle (về sau ở Profile screen) sẽ có Cancel — D5 spec |

---

## 3. RoleRequestScreen

### 3.1. Yêu cầu chức năng

FARMER đang dùng app muốn nâng cấp lên TRADER. Cần submit request với:
- Tên doanh nghiệp/cá nhân (bắt buộc)
- SĐT liên hệ (bắt buộc)
- Mã số thuế (optional)
- Địa chỉ làm việc (bắt buộc)
- Lý do nâng cấp (bắt buộc, min 50 ký tự)
- Đính kèm ảnh CCCD / giấy phép kinh doanh (optional, 1-3 ảnh)

Backend status: PENDING / APPROVED / REJECTED.

### 3.2. Layout đề xuất

**State: chưa submit**
```
[← back]      Yêu cầu nâng cấp Thương lái

┌─ Banner info ─────────────────────┐
│ ⓘ Vì sao cần duyệt?               │
│ Để bảo vệ nông dân khỏi thương lái│
│ không uy tín, chúng tôi xác minh  │
│ thông tin trước khi cho phép.     │
│ Thường mất 1-2 ngày làm việc.     │  ← status.info banner
└───────────────────────────────────┘

Thông tin doanh nghiệp                 ← text.h3

┌─ Input ─────────────────┐
│ Tên doanh nghiệp *      │
└─────────────────────────┘

┌─ Input ─────────────────┐
│ SĐT liên hệ *           │
└─────────────────────────┘

┌─ Input ─────────────────┐
│ Mã số thuế              │
└─────────────────────────┘

┌─ Input multi-line ──────┐
│ Địa chỉ làm việc *      │
│                         │
└─────────────────────────┘

Lý do                                   ← text.h3

┌─ Input multi-line ──────┐
│ Tại sao bạn muốn trở    │
│ thành thương lái?       │
│ (tối thiểu 50 ký tự)    │  ← counter 0/50
│                         │
└─────────────────────────┘

Tài liệu                                ← text.h3

┌─ Upload box ────────────┐
│  [📎] Thêm ảnh (0/3)    │
│  CCCD, giấy phép kinh   │
│  doanh, ảnh hộ kinh doanh│
└─────────────────────────┘

┌─ Primary CTA h56 ───────┐
│      Gửi yêu cầu        │  ← disabled khi form invalid
└─────────────────────────┘
```

**State: PENDING (đã submit, chưa duyệt)**
```
[← back]   Yêu cầu của bạn

┌─ Status card ─────────────────────┐
│   [Illustration: clock 96dp]      │
│   Đang chờ duyệt                  │  ← text.h2
│   Yêu cầu gửi lúc 14:32, 22/05    │  ← caption text.tertiary
│                                   │
│   Admin sẽ xem xét trong 1-2 ngày │
│   làm việc. Mình sẽ thông báo qua │
│   notification khi có kết quả.    │
└───────────────────────────────────┘

[Nút "Xem chi tiết đã gửi" tertiary]
[Nút "Huỷ yêu cầu" destructive ghost]
```

**State: APPROVED**
```
[Auto-redirect home + show success dialog]

┌─ Dialog success ─────────────────┐
│       [Confetti illustration]     │
│       Bạn đã là Thương lái! 🎉    │
│                                   │
│       Vai trò Thương lái đã được  │
│       kích hoạt. Khám phá tab     │
│       mới: Bản đồ & Giao dịch.    │
│                                   │
│  ┌─ Primary CTA ────────────┐    │
│  │   Bắt đầu khám phá        │    │
│  └──────────────────────────┘    │
└───────────────────────────────────┘
```

**FIX pain #21**: APPROVED phải refresh token tự động + redirect, **không** bắt user logout/login. Dev đã fix backend, designer chỉ cần spec hành vi.

**State: REJECTED**
```
[← back]   Yêu cầu của bạn

┌─ Status card ─────────────────────┐
│   [Illustration: warning 96dp]     │
│   Chưa duyệt                       │  ← text.h2 sem.error
│   Lý do: <admin note>              │
│                                    │
│   Bạn có thể chỉnh sửa và gửi lại  │
│   yêu cầu.                          │
└───────────────────────────────────┘

[Nút "Chỉnh sửa và gửi lại" primary]
[Nút "Bỏ qua, quay về làm Nông dân" tertiary]
```

### 3.3. Pain point fix

| ID | Fix |
|---|---|
| #20 | Mọi text dùng `R.string.*`, support 5 locale. Designer thiết kế UI giả định string độ dài variable. |
| #21 | APPROVED state có dialog success + auto-redirect; không có nút "Đăng nhập lại" |

---

## 4. Splash transition

**Yêu cầu**: 1.5s, fade-in logo + tagline → fade-out → AuthScreen.

**Đề xuất**:
- Bg `surface.1` (không phải full primary để tránh chói)
- Logo center, scale từ 0.9 → 1.0 trong 600ms easing emphasized
- Tagline "Số tay điện tử cho ruộng" fade-in sau 200ms delay
- Auto-transition sau 1.5s với crossfade 300ms sang AuthScreen
- **Tránh**: animation phù phiếm, lottie phức tạp, audio

**Reduced motion**: static logo + tagline trong 1.5s, không scale.

---

## 5. Acceptance criteria

- [ ] 3 màn hình × 3 mode (Email/Phone/Google trong Auth) + 2 step (ProfileSetup) + 3 state (RoleRequest) = ≥10 layout
- [ ] Mỗi layout có 4 state: Default, Loading, Error, Disabled
- [ ] Light + Dark cả 3 màn
- [ ] Showcase 2 locale vi + en cho RoleRequestScreen (text dài nhất)
- [ ] Showcase font scale 1.2× cho AuthScreen
- [ ] IME up state cho 3 form
- [ ] Tag fix pain points #16, #17, #18, #19, #20, #21
- [ ] Prototype flow: sign-in Email → ProfileSetup step1 → step2 → main app
- [ ] Prototype flow: sign-in Email → ProfileSetup step1 chọn TRADER → step2 → RoleRequest → PENDING
- [ ] Prototype flow: AuthScreen → "Quên mật khẩu" dialog → submit → snackbar success
- [ ] Hand-off Markdown spec: mỗi screen liệt kê components dùng từ D1, state transitions, validation rules

---

## 6. Timeline

| Tuần | Output |
|---|---|
| **1** | UX flow + low-fi wireframe 3 màn. Validation rules document. |
| **2** | Hi-fi visual 3 màn dùng D1 token draft. Iterate khi D1 ship final. |
| **3** | Prototype 3 flow chính. 2 locale showcase. |
| **4** | QA + hand-off + dev review. |
| **5-6** | (Idle / hỗ trợ designer khác / polish edge case) |

---

## 7. References

- **Spec gốc**: section 4 (Auth flow)
- **Files implementation**:
  - `app/src/main/java/com/GiaThinh/canlua/ui/screen/AuthScreen.kt`
  - `app/src/main/java/com/GiaThinh/canlua/ui/screen/ProfileSetupScreen.kt`
  - `app/src/main/java/com/GiaThinh/canlua/ui/screen/RoleRequestScreen.kt`
- **Inspiration cho onboarding**: Wise (segmented control auth), Cash App (clean OTP), Notion (role onboarding cards)
