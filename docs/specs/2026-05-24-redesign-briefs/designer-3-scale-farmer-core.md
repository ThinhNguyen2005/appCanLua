# Designer 3 — Scale tab (FARMER core)

**Phạm vi**: 4 màn hình core nghiệp vụ FARMER.
- CardListScreen (`scale`) — danh sách phiếu cân
- CardDetailScreen (`card_detail/{cardId}`) — chi tiết phiếu
- WeightInputScreen (`weight_input/{cardId}`) — nhập bao
- DeletedCardsScreen (`deleted_cards`) — thùng rác phiếu

**Đây là core 60% usage của app**. Một nông dân mở app chủ yếu để dùng tab này. Mọi friction đều thiệt hại doanh thu cảm nhận.

**Spec gốc đọc trước**: section 5 (Tab SCALE).

**Phụ thuộc**: D1 ship token + card + chip + FAB + sheet + dialog + list row (cuối tuần 2).

**Critical pain points fix**:
- #4 (QR payload chứa name + totalAmount plaintext) — vẫn cần D6 spec QR generate, nhưng D3 spec CardDetail show CTA tới QR
- #6 (CardDetail "4 card trắng cùng cấp")
- #7 (Dialog vs inline edit không nhất quán)
- #8 (WeightInput column-major + rule chia 10)
- #9 (CardList không có search)
- #14 (Lock/unlock 3 entry points)
- #24 (Bag cell < 48dp)
- #31 (Date header CardList không sticky)
- #32 (FAB hide hung hăng)

---

## 0. Mục tiêu trải nghiệm

Nông dân đứng giữa nắng, tay đeo găng dầu, bê bao lúa nặng. Mỗi phiếu = vài chục bao. Mỗi bao nhập sai = mất tiền. Redesign phải:

1. **Tốc độ nhập bao** — 1-tap mở phiếu, 1-tap mở input bao, gõ số → submit ngay
2. **Đọc số to** — kg và tiền là 2 con số quan trọng nhất, không bao giờ < 24sp
3. **Không nhầm bao** — index bao phải lớn, dễ tìm bao nào đã nhập / chưa
4. **Khoá phiếu phải chắc** — vô tình tap không thể edit khi đã khoá; có khoá thì phải có entry point QR rõ ràng

---

## 1. CardListScreen

### 1.1. Yêu cầu chức năng (giữ nguyên backend)

- Danh sách phiếu cân của user hiện tại, sort theo date desc
- Group theo ngày tạo
- Filter: tất cả / chưa khoá / đã khoá / chưa trả đủ
- Empty state khi chưa có phiếu
- FAB: tạo phiếu mới
- Pull-to-refresh
- Tap card → CardDetailScreen
- Long-press card → menu (Khoá, Xoá, Chia sẻ)
- Offline indicator nếu mất mạng
- Trader cũng dùng screen này (variant ở section 9.x của spec gốc, D5 owns)

### 1.2. Layout đề xuất

```
┌──────────────────────────────────┐
│  Phiếu cân           [⋮] [🔍]    │  ← TopBar h56, search icon
│                                  │
│  [Search inline expanded khi 🔍] │  ← Search field h56, animate slide
│                                  │
│  ┌─ Filter chips row ───────────┐│
│  │ [Tất cả] [Chưa khoá] [Đã ...] │ ← LazyRow horizontal scroll
│  └──────────────────────────────┘│
│                                  │
│  ─── Hôm nay ──────────────────  │  ← Sticky group header
│                                  │
│  ┌─ Card phiếu ────────────────┐ │
│  │ [Avatar Trader 40dp]        │ │
│  │ Anh Tư · Đài Thơm 8         │ │  ← text.h3
│  │ 12 bao · 580.5kg            │ │  ← text.body text.secondary
│  │ 5.220.000đ          [🔒]    │ │  ← text.h2 amount + lock badge
│  │ [chip status]               │ │  ← "Đã khoá" / "Còn nợ" pill
│  └─────────────────────────────┘ │
│                                  │
│  ┌─ Card phiếu ────────────────┐ │
│  │ [Avatar 40dp]               │ │
│  │ Anh Năm · OM 5451           │ │
│  │ 8 bao · 412.0kg             │ │
│  │ 3.708.000đ                  │ │
│  │ [chip "Chưa khoá"]          │ │
│  └─────────────────────────────┘ │
│                                  │
│  ─── Hôm qua ─────────────────   │  ← Sticky khi scroll
│                                  │
│  ...                             │
│                                  │
│                                  │
│                  ┌─ FAB ───┐    │
│                  │  + bao  │    │  ← Extended FAB
│                  └────────┘     │
└──────────────────────────────────┘
   [ ⚖ Cân  🛒 Chợ  💬 AI  👤 ]
```

### 1.3. Search inline (FIX pain #9)

**Trigger**: tap icon 🔍 ở TopBar → field expand inline (slide-down 200ms easing emphasized), thay thế title.

**State**:
- Default: closed, chỉ icon
- Open empty: field placeholder "Tìm phiếu theo tên thương lái, giống lúa, ngày..."
- Open typing: field filled + clear icon trailing + danh sách filter realtime (debounce 150ms)
- Open no result: empty state inline "Không tìm thấy phiếu khớp '...'"

**Algorithm**: full-text search trên fields {traderName, riceVariety, note, date string formatted vi}.

**Close**: tap 🔍 lần nữa hoặc back icon ở left thay vị trí 🔍.

### 1.4. Filter chips

3 chip:
- Tất cả (default, outline khi không chọn)
- Chưa khoá (số đếm trailing)
- Còn nợ (số đếm trailing)

**FIX pain #15**: "Tất cả" idle = outline + text.secondary; selected = `brand.primary` bg + white. Không green nhầm với trend "ổn định".

### 1.5. Group header sticky (FIX pain #31)

Headers "Hôm nay", "Hôm qua", "Tuần này", "Tháng N", "Năm NNNN" sticky khi scroll. Bg `surface.1` + divider top + text `text.h3` `text.secondary` UPPERCASE optional.

### 1.6. Card phiếu

**Anatomy**:
```
┌────────────────────────────────┐
│ [Avatar 40]  Anh Tư            │  ← Title row
│              Đài Thơm 8 · 10/05│  ← Subtitle
│                                 │
│  5.220.000đ              [🔒]  │  ← Hero amount + status icon
│  12 bao · 580.5kg              │  ← Meta row
│                                 │
│  [Còn nợ 1.000.000đ]           │  ← Status chip (optional)
└────────────────────────────────┘
   tap → CardDetailScreen
   long-press → context menu (Khoá / Xoá / Chia sẻ)
```

**Variants**:
- **Default** (chưa khoá): `surface.2` bg, không icon lock
- **Locked**: icon 🔒 `brand.accent` trailing
- **Còn nợ**: chip `sem.error` α 12% bg + amount
- **Đã trả đủ**: chip `sem.success` α 12% bg + "Đã trả đủ"
- **Offline modified**: icon CloudOff `status.offline` trailing → "Chưa đồng bộ"

### 1.7. FAB (FIX pain #32)

- **Extended FAB**: "+ bao" hoặc "Tạo phiếu" tùy ngữ cảnh? **Đề xuất**: chỉ "Tạo phiếu" — luôn tạo phiếu mới. Tránh confusing.
- **Hide rule**: scroll down → fade-out 200ms; scroll up 16dp → fade-in 250ms. **Không hide hung hăng** — chỉ hide khi user actively scroll down ≥ 48dp.
- **List rỗng**: vẫn show FAB, không hide.

### 1.8. Empty state

```
┌────────────────────────────────┐
│  [Illustration 160dp]          │
│                                 │
│  Chưa có phiếu cân nào          │  ← text.h2
│                                 │
│  Khi thương lái đến cân lúa,    │  ← text.body text.secondary
│  bấm "Tạo phiếu" để ghi lại     │
│  từng bao và tính tiền tự động. │
│                                 │
│  ┌─ Primary CTA ────────────┐  │
│  │ + Tạo phiếu đầu tiên     │  │
│  └─────────────────────────┘   │
└────────────────────────────────┘
```

### 1.9. Loading state

Skeleton: 3 card skeleton 80dp height + group header skeleton.

### 1.10. Offline banner

Banner top h36 `status.offline` "Đang offline. Phiếu mới sẽ được lưu và đồng bộ khi có mạng."

### 1.11. States

- Default empty
- Default with cards (3, 10, 100 cards — test scroll performance)
- Filter active
- Search typing
- Search no result
- Loading
- Offline
- Pull-to-refresh
- FAB extended hover/pressed
- Long-press menu
- Dark mode

### 1.12. CreateCardDialog (FIX pain #7 — sẽ thống nhất với CardDetail edit)

**Đề xuất bỏ Dialog, dùng full-screen sheet** để consistency với WeightInput.

```
┌──────────────────────────────────┐
│  [✕]  Tạo phiếu mới       [Lưu]│  ← Sticky topbar
│                                  │
│  Thương lái                      │  ← text.h3
│  ┌─ Input ──────────────────┐   │
│  │ Tên thương lái           │   │  ← autocomplete từ danh sách traders đã giao dịch
│  └─────────────────────────┘    │
│                                  │
│  Giống lúa                       │
│  ┌─ Dropdown ───────────────┐   │
│  │ Đài Thơm 8           [▾] │   │  ← 10 variety phổ biến + custom
│  └─────────────────────────┘    │
│                                  │
│  Ngày                            │
│  ┌─ Date picker inline ─────┐   │
│  │ 24/05/2026               │   │  ← default today
│  └─────────────────────────┘    │
│                                  │
│  Đơn giá (đ/kg)                  │
│  ┌─ Number input ────────────┐  │
│  │ 9.000                     │  │  ← numeric keyboard, format thousand
│  └──────────────────────────┘   │
│                                  │
│  Ghi chú (optional)              │
│  ┌─ Multi-line ──────────────┐  │
│  │                           │  │
│  └──────────────────────────┘   │
│                                  │
│  ┌─ Primary CTA h56 ─────────┐  │
│  │ Lưu & bắt đầu cân          │  │  ← navigate to WeightInput
│  └──────────────────────────┘   │
└──────────────────────────────────┘
```

---

## 2. CardDetailScreen

### 2.1. Yêu cầu chức năng (giữ nguyên backend)

- Hero header với traderName + variety + date
- 4 khối thông tin: Tổng KG · Tổng tiền · Đã trả · Còn lại
- List bao cân (mỗi bao 1 row): index + weight + paid status
- Action toolbar: Edit / Lock / Unlock / Add bag / QR / Delete / Share
- Khi locked: read-only, chỉ cho phép edit nếu unlock
- Sync status indicator
- Tap bag row → BagEntryActionDialog
- Add bag → WeightInputScreen
- QR → QrGenerateScreen (D6)

### 2.2. Layout đề xuất — FIX pain #6 (4 card cùng cấp)

**Hierarchy mới**: 1 hero amount + 3 secondary metrics + bag list.

```
┌──────────────────────────────────┐
│  [← back]  Phiếu cân     [⋮]    │  ← TopBar default
│                                  │
│  ┌─ Hero card ─────────────────┐│
│  │ gradient brand.primary       ││
│  │                              ││
│  │ Anh Tư · Đài Thơm 8          ││  ← text.body white α 80%
│  │ 24/05/2026                   ││
│  │                              ││
│  │ 5.220.000 đ                  ││  ← text.display 32sp Bold white
│  │ Tổng tiền                    ││  ← text.caption white α 70%
│  │                              ││
│  │ ┌─ Lock pill ──┐             ││
│  │ │ 🔒 Đã khoá   │             ││  ← status chip white α 20% bg
│  │ └──────────────┘             ││
│  └─────────────────────────────┘│
│                                  │
│  ┌─ Action bar (sticky) ───────┐│
│  │ [✏Edit] [🔒Unlock] [📷QR]   ││  ← 3 button quick action, h48
│  └─────────────────────────────┘│
│                                  │
│  ┌─ 3 metric row (compact) ────┐│
│  │ 580.5kg  │ 4.220.000đ │ 1tr ││
│  │ 12 bao   │ đã trả     │ nợ  ││
│  └─────────────────────────────┘│
│                                  │
│  ─── Bao cân (12) ──────────    │  ← section header sticky
│                                  │
│  ┌─ Bag row ───────────────────┐│
│  │ [01]  50.5 kg          [💰]││  ← Index pill + weight + paid icon
│  └─────────────────────────────┘│
│  ┌─ Bag row ───────────────────┐│
│  │ [02]  48.0 kg          [💰]││
│  └─────────────────────────────┘│
│  ...                             │
│                                  │
│              ┌─ FAB add ─┐       │
│              │  + bao    │       │
│              └──────────┘        │
└──────────────────────────────────┘
```

### 2.3. Hero card

**Decision**: con số nào lên hero?

3 options (designer chọn 1 sau testing):
- **A. Tổng tiền** (đề xuất) — quan trọng nhất với farmer, đơn vị đ
- **B. Tổng KG** — neutral, đơn vị kg
- **C. Còn lại nợ** — urgency cao nhất, đỏ nếu > 0

→ Đề xuất **A** mặc định, **C** khi `remaining > 0` (dynamic). Không option B.

Khi hero là "Còn lại nợ" và > 0:
- Bg gradient `sem.error` → `sem.error` darken 10%
- Text "Còn lại chưa trả" + amount đỏ to + CTA "Ghi nhận đã trả"

### 2.4. Action bar sticky

3 button quick action ngay dưới hero, sticky khi scroll:
- **Edit** — mở edit inline mode hoặc edit sheet (FIX pain #7, **dùng inline** — consistent với WeightInput)
- **Lock/Unlock toggle** (FIX pain #14 — chỉ 1 entry point chính, dropdown ⋮ ở topbar có shortcut nhưng không phải primary)
- **QR** — navigate QrGenerateScreen (FIX pain #4 — QR sẽ có warning sensitive data, D6 spec)

Khi locked:
- Edit button disabled với tooltip "Mở khoá để chỉnh sửa"
- Lock → Unlock label đổi
- QR vẫn enable

### 2.5. 3 metric row (FIX pain #6)

Compact horizontal row dưới action bar, **không phải 3 card cùng cấp** với hero:
- Bg `surface.2`, padding `space.4`, divider giữa các metric
- Mỗi metric: number text.h3 + label caption
- Nếu remaining > 0: cell remaining có dấu chấm `sem.error` cảnh báo

### 2.6. Bag list

Mỗi row h56:
- Index pill `radius.full` 32dp `surface.3` bg + number `text.body-strong`
- Weight text.h3 monospace tabular
- Trailing: paid icon `sem.success` nếu đã trả; empty nếu chưa
- Tap row → BagEntryActionDialog (sheet thay vì dialog)

**Row variant locked**: opacity giữ nhưng tap không action, snackbar "Đã khoá. Mở khoá để chỉnh sửa."

### 2.7. BagEntryActionSheet (FIX pain #7 — sheet thay dialog)

```
┌──────────────────────────────────┐
│  ━━━ (handle)                    │
│                                  │
│  Bao #03                          │  ← text.h2
│  50.5 kg · 454.500đ              │  ← text.body
│                                  │
│  Trạng thái thanh toán            │
│  ┌─ Switch row ─────────────────┐│
│  │ Đã trả tiền cho bao này       ││  ← toggle
│  └─────────────────────────────┘│
│                                  │
│  ┌─ Tertiary button ───────────┐│
│  │ ✏ Sửa cân                    ││  ← inline edit weight
│  └─────────────────────────────┘│
│                                  │
│  ┌─ Destructive button ────────┐│
│  │ 🗑 Xoá bao                   ││
│  └─────────────────────────────┘│
└──────────────────────────────────┘
```

### 2.8. Lock confirm dialog

```
┌──────────────────────────────────┐
│  [Icon 🔒 32dp brand.accent]      │
│                                   │
│  Khoá phiếu cân?                  │  ← text.h2
│                                   │
│  Sau khi khoá, phiếu sẽ chỉ đọc,  │
│  bạn không thể thêm/sửa/xoá bao   │
│  hay sửa thông tin. Có thể mở     │
│  khoá lại bất cứ lúc nào.         │
│                                   │
│              [Huỷ]  [Khoá phiếu] │
└──────────────────────────────────┘
```

### 2.9. States

- Default unlocked với bag list
- Locked
- Locked + còn nợ → hero variant C
- Empty bag (vừa tạo phiếu, chưa nhập bao nào) → CTA lớn "Bắt đầu cân"
- Editing (inline edit field active)
- Offline (banner top)
- Loading skeleton
- Long card với 50+ bag (test scroll, sticky behaviour)
- Dark mode

### 2.10. Pain points fix

| ID | Fix |
|---|---|
| #6 | 1 hero + 3 compact metric thay 4 card cùng cấp |
| #7 | Inline edit thay dialog cho mọi field |
| #14 | Lock có 1 entry chính ở action bar, dropdown ⋮ chỉ là shortcut |

---

## 3. WeightInputScreen

### 3.1. Yêu cầu chức năng (giữ nguyên backend)

- Nhập cân theo bao
- Hiển thị đang nhập bao thứ mấy (auto-increment)
- Hiển thị tổng KG runtime, tổng tiền runtime
- Numeric keypad: 0-9, ., backspace, "Lưu bao", "Tiếp tục bao mới"
- Quick-pick weight: 50.0, 49.5, 50.5 (common bag weight) — nhưng cần redesign affordance
- Quick-pick paid status
- Lock/unlock toggle ở toolbar (đồng nhất với CardDetail)

### 3.2. Layout đề xuất — FIX pain #8 (column-major + chia 10)

```
┌──────────────────────────────────┐
│  [← back] Cân bao            [🔒]│  ← TopBar
│                                  │
│  ┌─ Summary strip ─────────────┐│
│  │ Phiếu: Anh Tư · Đài Thơm 8   ││  ← caption
│  │ 11 bao · 525.5kg · 4.7tr     ││  ← runtime totals body-strong
│  └─────────────────────────────┘│
│                                  │
│  ┌─ Current bag hero ──────────┐│
│  │ Bao #12                      ││  ← text.h2
│  │                              ││
│  │   50.5                       ││  ← text.display 48sp Bold tabular
│  │   kg                         ││  ← text.body label
│  │                              ││
│  │ [chip "✓ Đã trả tiền"]       ││  ← toggle quick paid status
│  └─────────────────────────────┘│
│                                  │
│  ┌─ Quick presets row ─────────┐│
│  │ [49.0] [49.5] [50.0] [50.5] ││  ← 4 button common bag weight
│  └─────────────────────────────┘│
│                                  │
│  ┌─ Numeric keypad ────────────┐│
│  │  1    2    3                ││
│  │  4    5    6                ││
│  │  7    8    9                ││
│  │  .    0    ⌫                ││  ← decimal, zero, backspace
│  └─────────────────────────────┘│
│                                  │
│  ┌─ Primary CTA h56 ───────────┐│
│  │  Lưu bao & cân tiếp          ││
│  └─────────────────────────────┘│
└──────────────────────────────────┘
```

### 3.3. Numeric keypad design

Decision lớn: **bỏ rule "nhập 3 digit chia 10"** (FIX pain #8).

**Đề xuất A** (đề xuất): Keypad có dấu `.`, user gõ `50.5` trực tiếp.

**Đề xuất B**: Keep auto-divide, nhưng có hint persistent "Gõ 505 → 50.5 kg" với animation visual ngay khi gõ.

→ Pick **A**. Tab numeric phải có decimal point. Validation: 0 < weight < 200 kg.

**Spec keypad**:
- 4 hàng × 3 cột, mỗi cell 80dp × 80dp (touch target ≥ 56dp dễ tap)
- Cell bg `surface.2`, pressed `surface.3`, `radius.md`
- Number text.h2 28sp Bold tabular
- Backspace icon 28dp
- Decimal "." nổi bật hơn bằng `brand.primary` α 8% bg

### 3.4. Quick presets

Row 4 button common bag weight:
- 49.0, 49.5, 50.0, 50.5 (Vietnam common)
- Customizable từ Settings (D6 spec): user có thể đổi 4 preset

**Tap preset**: set weight ngay, không cần gõ. Highlight 200ms pulse.

### 3.5. Paid status toggle

Chip `radius.full` h36 cạnh "Bao #12":
- Off: outline + text.secondary "Chưa trả"
- On: `sem.success` α 12% bg + `sem.success` text + ✓ icon "Đã trả"

Tap toggle ngay không cần submit.

### 3.6. Bag history strip (optional, scrollable horizontal)

Phía trên current bag hero, có thể thêm strip nhỏ show 5 bao trước đó:
```
┌─ History strip ─────────────────┐
│ [#08 49.5] [#09 50.0] [#10 ...] │  ← chip cuộn ngang, tap xem detail
└─────────────────────────────────┘
```

Để designer quyết định thêm hay không (test với user).

### 3.7. Submit & next bag

CTA "Lưu bao & cân tiếp" — tap → save + reset weight = 0 + increment bag index + haptic tick.

Alternative CTA secondary "Lưu & xong" — save + back to CardDetail.

### 3.8. Lock state

Toolbar 🔒 icon — tap mở dialog confirm. Khi locked, screen này không accessible (back về CardDetail). Hoặc enter screen khi locked → show banner "Phiếu đã khoá" + nút "Mở khoá để cân tiếp".

### 3.9. States

- Empty (bao đầu tiên, summary strip 0 bao)
- Filled weight không paid
- Filled weight có paid
- Quick preset selected
- Validation error (weight ≤ 0 hoặc > 200): inline message dưới hero
- Loading submit
- Offline
- Lock conflict (lock được toggle từ device khác trong lúc đang nhập)
- Dark mode

### 3.10. Pain points fix

| ID | Fix |
|---|---|
| #8 | Bỏ column-major grid 5×5. Numeric keypad standard với decimal. Bỏ rule chia 10. |
| #24 | Cell keypad 80×80dp (touch ≥ 56dp). |
| #14 | Lock icon ở toolbar — 1 entry, hoà với CardDetail action bar. |

---

## 4. DeletedCardsScreen

### 4.1. Yêu cầu chức năng

- List phiếu đã xoá (tombstone trong `deleted_cards` table)
- Mỗi row: thông tin phiếu + deletedAt + nút khôi phục
- Empty state
- Pull-to-refresh
- Optional: empty trash bulk

### 4.2. Layout

```
┌──────────────────────────────────┐
│  [← back]  Phiếu đã xoá     [⋮] │  ← ⋮ = "Dọn sạch" destructive option
│                                  │
│  ┌─ Banner info ────────────────┐│
│  │ ⓘ Phiếu xoá quá 30 ngày sẽ   ││
│  │   tự động dọn vĩnh viễn.     ││  ← status.info banner
│  └─────────────────────────────┘│
│                                  │
│  ─── Đã xoá hôm nay ──────────  │
│                                  │
│  ┌─ Deleted card row ──────────┐│
│  │ Anh Tư · Đài Thơm 8          ││  ← muted opacity 70%
│  │ 12 bao · 580.5kg · 5.22tr    ││
│  │ Đã xoá 14:32 hôm nay         ││  ← caption text.tertiary
│  │             [↻ Khôi phục]    ││  ← tertiary button
│  └─────────────────────────────┘│
│                                  │
│  ...                             │
└──────────────────────────────────┘
```

### 4.3. Empty state

```
[Illustration thùng rác 160dp]

Thùng rác trống

Phiếu bạn xoá sẽ giữ ở đây 30 ngày
trước khi xoá vĩnh viễn. Bạn có thể
khôi phục lại bất cứ lúc nào.
```

### 4.4. Restore confirm

```
Khôi phục phiếu của Anh Tư?

Phiếu sẽ trở về danh sách Phiếu cân
với toàn bộ bao và thông tin gốc.

           [Huỷ]  [Khôi phục]
```

### 4.5. States

- Empty (default)
- 1-5 deleted cards
- Many deleted cards (50+, test scroll)
- Loading
- Offline
- Dark mode

---

## 5. Acceptance criteria

- [ ] 4 screen × multi states = ≥30 layout total
- [ ] Light + Dark cả 4 screen
- [ ] Showcase 1 screen ở font scale 1.2× (đề xuất WeightInput — screen có nhiều con số to)
- [ ] Showcase 2 locale vi + en cho CardListScreen
- [ ] IME up state cho CreateCardSheet và WeightInput
- [ ] Animation spec: FAB hide/show, search expand, sticky header, sheet open
- [ ] Prototype flow: tạo phiếu → cân 3 bao → khoá → mở QR
- [ ] Prototype flow: tap card cũ → xem detail → unlock → edit → relock
- [ ] Prototype flow: search "Anh Tư" → filter result → tap
- [ ] Tag fix pain points #6, #7, #8, #9, #14, #15, #24, #31, #32
- [ ] Hand-off Markdown spec: components used from D1, state transitions, validation rules

---

## 6. Timeline

| Tuần | Output |
|---|---|
| **1** | UX flow + low-fi wireframe 4 màn. Decision: hero là gì (tổng tiền vs còn lại), quick preset, keypad layout. |
| **2** | Hi-fi visual CardList + WeightInput. |
| **3** | Hi-fi visual CardDetail + Deleted. Iterate khi D1 ship final token. |
| **4** | Prototype 3 flow chính. QA + hand-off. |
| **5** | Polish edge cases (long card 100 bag, offline modified, lock conflict). |
| **6** | Hand-off final + dev review. |

---

## 7. References

- **Spec gốc**: section 5 (Tab SCALE — 5 screens chi tiết)
- **Files implementation**:
  - `app/src/main/java/com/GiaThinh/canlua/ui/screen/CardListScreen.kt`
  - `app/src/main/java/com/GiaThinh/canlua/ui/screen/CardDetailScreen.kt`
  - `app/src/main/java/com/GiaThinh/canlua/ui/screen/WeightInputScreen.kt`
  - `app/src/main/java/com/GiaThinh/canlua/ui/screen/DeletedCardsScreen.kt`
  - `app/src/main/java/com/GiaThinh/canlua/ui/component/cardlist/*`
  - `app/src/main/java/com/GiaThinh/canlua/ui/component/detail/*`
  - `app/src/main/java/com/GiaThinh/canlua/ui/component/weight/*`
- **Inspiration**:
  - **Splitwise** — list group sticky + amount hero
  - **Notion database** — search inline + filter chip
  - **Mint / Monarch Money** — financial number hierarchy
  - **iA Writer keypad** — clean numeric input
