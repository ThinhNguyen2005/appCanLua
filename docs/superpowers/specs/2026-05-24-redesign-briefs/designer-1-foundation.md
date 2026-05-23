# Designer 1 — Foundation

**Phạm vi**: Design tokens, atom + molecule library, dark mode base, icon set, illustration style, design system documentation.

**Tại sao bạn quan trọng**: 5 designer còn lại đang đợi token draft của bạn để chuyển từ wireframe sang hi-fi. Hand-off chậm 1 ngày = 5 designer chậm 5 ngày. Tuần 1-2 là critical path.

**Spec gốc đọc trước**: section 11 (Design system hiện tại), section 13.2-13.7 (Direction đề xuất).

**Output gốc**:
- 47 pain points #1 (3 nguồn màu xung đột), #11 (không dark toggle), #36-40 (dead code, theme inconsistency)
- Style "Editorial nông nghiệp" — gather inspiration: Cropwise, John Deere Operations Center, Plantix, Pocket Casts, Linear, Apple Weather

---

## 1. Design tokens (deliverable chính)

### 1.1. Color tokens

**Yêu cầu**: 1 nguồn duy nhất, dark + light đầy đủ, semantic-driven (không màu rời rạc).

#### Brand
| Token | Light hex (proposed) | Dark hex (proposed) | Use |
|---|---|---|---|
| `brand.primary` | `#1F6B27` | `#7BC57F` | Primary action, hero accent, logo color |
| `brand.primary-container` | `#C8E6C9` | `#1E3A24` | Surface tint với primary content |
| `brand.on-primary` | `#FFFFFF` | `#0A1F0D` | Text trên primary |
| `brand.on-primary-container` | `#0E2D14` | `#C8E6C9` | Text trên primary container |
| `brand.accent` | `#F59E0B` | `#FBBF24` | Accent — lúa chín, gold actions |
| `brand.accent-container` | `#FEF3C7` | `#451A03` | Accent surface tint |
| `brand.on-accent` | `#1A1A1A` | `#1A1A1A` | Text trên accent |
| `brand.on-accent-container` | `#3F1F00` | `#FEF3C7` | Text trên accent container |

**Lý do số `#1F6B27`**: contrast 4.5:1 với `#FFFFFF` (AA+); contrast 7.0:1 với `#FAFAFA` (AAA cho số liệu); contrast 1.7:1 với `#C8E6C9` (chữ trắng đọc tốt). Bạn được phép điều chỉnh ±5% lightness nếu test thực tế cho contrast lệch.

#### Semantic
| Token | Light | Dark | Use |
|---|---|---|---|
| `sem.success` | `#22C55E` | `#4ADE80` | Sync success, paid, completed |
| `sem.success-container` | `#DCFCE7` | `#14532D` | Success banner bg |
| `sem.warning` | `#F59E0B` | `#FBBF24` | **TRÙNG với accent** — designer chọn 1 trong 2 hướng: (a) accent ≠ warning bằng cách đổi accent sang hồng đào `#FB923C`, (b) gộp accent vào warning. Đề xuất (a) để giữ "lúa chín" khác "cảnh báo" |
| `sem.warning-container` | `#FEF3C7` | `#451A03` | Warning banner bg |
| `sem.error` | `#DC2626` | `#F87171` | Error, delete, công nợ chưa trả |
| `sem.error-container` | `#FEE2E2` | `#7F1D1D` | Error banner bg |
| `sem.info` | `#2563EB` | `#60A5FA` | Info banner, syncing, neutral notification |
| `sem.info-container` | `#DBEAFE` | `#1E3A8A` | Info banner bg |

#### Surface scale (5 mức — bỏ Color.kt + AppColors mixed cũ)
| Token | Light | Dark | Use |
|---|---|---|---|
| `surface.0` | `#FFFFFF` | `#0F1411` | App bg lowest |
| `surface.1` | `#FAFAF7` | `#161B17` | Default screen bg |
| `surface.2` | `#F3F3EE` | `#1D241F` | Card resting |
| `surface.3` | `#EAEAE3` | `#252D27` | Card elevated, dialog |
| `surface.4` | `#DDDDD2` | `#2E382F` | Sheet, menu, overlay |
| `surface.tint` | `#1F6B27` α 6% | `#7BC57F` α 8% | Tonal elevation tint trên surface |

**Quy tắc**: M3 tonal elevation phải derive từ scale này, không hardcode `surfaceContainer` riêng nữa.

#### Text scale
| Token | Light | Dark | Use |
|---|---|---|---|
| `text.primary` | `#0F1411` | `#F3F3EE` | Main text |
| `text.secondary` | `#3D4640` | `#C5CBC6` | Secondary text, label |
| `text.tertiary` | `#6B7269` | `#9CA29F` | Hint, helper, meta |
| `text.disabled` | `#A8ADAA` | `#5F6562` | Disabled state |
| `text.inverse` | `#FFFFFF` | `#0F1411` | Text trên primary/error |
| `text.link` | `#1F6B27` | `#7BC57F` | Link inline |

#### Border / divider
| Token | Light | Dark |
|---|---|---|
| `border.subtle` | `#EBEBE5` | `#262C27` |
| `border.default` | `#D6D6CB` | `#363D37` |
| `border.strong` | `#A8ADAA` | `#5F6562` |
| `border.focus` | `#1F6B27` α 40% | `#7BC57F` α 40% |

#### Status-specialty (tinted, không rời rạc)
**Đề xuất bỏ 4 cặp pastel (OfflineBg/SyncingBg/LockedBg/ComingSoonBg) thay bằng tint của semantic**:
- `status.offline` = `sem.warning` α 12% bg + `sem.warning` text
- `status.syncing` = `sem.info` α 12% bg + `sem.info` text
- `status.locked` = `sem.error` α 12% bg + `sem.error` text
- `status.coming-soon` = `brand.accent` α 12% bg + `brand.accent` text
- `status.success` = `sem.success` α 12% bg + `sem.success` text

#### Money tints (chỉ dùng trong CardDetail / Profile dashboard)
- `money.weight-surface` = `brand.accent-container` (vàng nhẹ, nhấn KG)
- `money.amount-surface` = `brand.primary-container` (xanh nhẹ, nhấn tiền)
- `money.remaining-surface` = `sem.error-container` (cảnh báo công nợ)

### 1.2. Typography tokens

**Yêu cầu**: rút từ 15 M3 levels xuống 8 cấp. Test bắt buộc với tiếng Việt có dấu (huyền, sắc, hỏi, ngã, nặng).

#### Font selection
**Đề xuất**:
- **Display + Heading**: `Be Vietnam Pro` (hỗ trợ đầy đủ dấu, weight 800 chắc nịch) HOẶC `Plus Jakarta Sans` (modern, geometric, weight 800 mạnh)
- **Body**: `Inter Tight` HOẶC tiếp tục `Be Vietnam Pro` cho monoset

**Yêu cầu test**: in 1 trang preview với câu mẫu chứa `Cần Thơ, lúa Đài Thơm 8, đường ĐT.954, cẩn thận khoá phiếu, không gửi mã OTP cho người khác.` ở 4 weight × 3 size, kiểm dấu không vỡ ascender.

#### Scale 8 cấp
| Token | Size | Weight | Line height | Use |
|---|---|---|---|---|
| `text.display` | 32 sp | 800 | 38 | Splash, hero number (totalAmount, doanh thu) |
| `text.h1` | 24 sp | 700 | 32 | Screen title |
| `text.h2` | 20 sp | 700 | 28 | Section title, card hero |
| `text.h3` | 17 sp | 600 | 24 | Card title, list group header |
| `text.body` | 15 sp | 400 | 22 | Default body |
| `text.body-strong` | 15 sp | 600 | 22 | Body emphasis (label trong row) |
| `text.caption` | 13 sp | 400 | 18 | Meta, timestamp |
| `text.label` | 11 sp | 600 | 14 | Pill, chip, badge — UPPERCASE optional |

**Letter-spacing**: chỉ `text.label` nếu UPPERCASE thì +0.05em. Còn lại letter-spacing 0.

**Font scale rule**: scale 0.9× / 1.0× / 1.1× / 1.2× chỉ áp font-size + line-height; **không scale letter-spacing**.

#### Number-specific
Số liệu tài chính (tiền, KG, công nợ): dùng **tabular figures** (`font-feature-settings: "tnum"`). Designer chú thích rõ trong token spec.

### 1.3. Spacing tokens (4-base scale)

| Token | dp | Use |
|---|---|---|
| `space.0` | 0 | — |
| `space.1` | 4 | Inline gap |
| `space.2` | 8 | Tight gap, chip padding |
| `space.3` | 12 | Default gap |
| `space.4` | 16 | Standard padding, content edge |
| `space.5` | 20 | Card padding, section gap |
| `space.6` | 24 | Section break |
| `space.7` | 32 | Major section break |
| `space.8` | 40 | Hero block |
| `space.9` | 56 | Empty state, splash |
| `space.10` | 80 | — |

**Quy tắc**: content padding ngang = `space.4` (16dp). Card padding = `space.5` (20dp). Section gap = `space.6` (24dp).

### 1.4. Radius tokens (5 cấp, bỏ 9 cấp cũ)

| Token | dp | Use |
|---|---|---|
| `radius.xs` | 4 | Chip nhỏ, badge, tag inline |
| `radius.sm` | 8 | Button, input, small card |
| `radius.md` | 12 | Card, dialog, sheet handle |
| `radius.lg` | 20 | Hero card, premium banner |
| `radius.xl` | 28 | Bottom bar capsule, FAB extended |
| `radius.full` | 999 | Circular avatar, FAB |

### 1.5. Elevation / shadow tokens

| Token | Shadow spec | Use |
|---|---|---|
| `elev.0` | none | Flat |
| `elev.1` | 0 1 2 rgba(0,0,0,0.06) | Resting card |
| `elev.2` | 0 2 6 rgba(0,0,0,0.08) | Hover/raised card |
| `elev.3` | 0 4 12 rgba(0,0,0,0.10) | Dropdown, menu |
| `elev.4` | 0 8 24 rgba(0,0,0,0.12) | Dialog, sheet |
| `elev.5` | 0 12 32 rgba(0,0,0,0.16) | Bottom bar capsule (matched với codebase hiện tại) |

**Dark mode**: thay shadow bằng border `border.subtle` + surface tint, không dùng shadow đậm trên dark.

### 1.6. Motion tokens

| Token | Spec | Use |
|---|---|---|
| `motion.duration.instant` | 100ms | Haptic feedback, ripple |
| `motion.duration.quick` | 150ms | Toggle, checkbox |
| `motion.duration.standard` | 250ms | Page transition (FadeScale) |
| `motion.duration.slow` | 400ms | Sheet expand, dialog |
| `motion.duration.pulse` | 1200ms | Sync pulse |
| `motion.easing.standard` | cubic-bezier(0.4, 0, 0.2, 1) | Default |
| `motion.easing.emphasized` | cubic-bezier(0.2, 0, 0, 1) | Entrance |
| `motion.easing.exit` | cubic-bezier(0.4, 0, 1, 1) | Exit |
| `motion.spring.lowBouncy` | stiffness 350, damping 30 | UI feedback (pill nav) |
| `motion.spring.responsive` | stiffness 700, damping 35 | Reorder, drag |

**Quy tắc reduced-motion**: nếu System reducedMotion = true, mọi `motion.duration > 200ms` → 0ms (instant), `motion.spring` → tween, pulse → static.

---

## 2. Atom library (D1 must ship)

### 2.1. Button
**Variants** × **States**: (primary / secondary / tertiary / destructive / ghost) × (default / hover / pressed / loading / disabled).

Mỗi button có 3 size: **sm** (h32, padding 12, text body 13sp), **md** (h48, padding 16, text body 15sp), **lg** (h56, padding 20, text body-strong 16sp). Action chính dùng `lg`.

Pattern:
- **primary**: `brand.primary` bg + `brand.on-primary` text + `radius.sm`. Pressed: `brand.primary` α 92%.
- **secondary**: `surface.2` bg + `text.primary` + border `border.default`.
- **tertiary**: transparent bg + `brand.primary` text.
- **destructive**: `sem.error` bg + white text.
- **ghost**: transparent bg + `text.primary` (cho menu item, dropdown).
- **loading**: hiển thị spinner thay text, giữ kích thước.
- **disabled**: opacity 50% + cursor not-allowed (pointer-events: none).

Icon button (icon-only): `radius.full`, 44dp × 44dp, hit area 48dp.

### 2.2. Input field
**Variants**: text / number / password / OTP / phone / email / multi-line.

**States**: default / focus / filled / error / disabled.

Spec:
- Height 56dp (default) hoặc 48dp (compact)
- `surface.2` bg, `border.default` border 1dp, `radius.sm`
- Focus: border 2dp `border.focus` + `brand.primary` α 5% bg tint
- Error: border 2dp `sem.error` + helper text dưới
- Helper text dưới ô (caption 13sp), max 2 dòng
- Leading icon 24dp + trailing icon 24dp (eye toggle, clear)
- Floating label: animate scale 0.75 + move up 8dp khi focus/filled
- OTP field: 6 ô riêng biệt, h56 w48 mỗi ô, monospace font, auto-advance khi gõ

**Phone field**: prefix `+84` lock, format `xxx xxx xxx` khi user gõ, validate regex `^[3-9]\d{8}$`.

### 2.3. Chip
**Variants**: filter / status / trend / input (removable).

- Height 32dp, padding ngang 12dp, `radius.xs` hoặc `radius.full` (designer chọn 1 hướng), text caption 13sp Medium
- **Filter selected**: `brand.primary` bg + white text + ✓ icon 16dp leading
- **Filter unselected**: `surface.2` bg + `text.secondary` + border `border.default`
- **Status**: bg `status.X` α 12% + text `status.X`, icon 14dp leading (lock / cloud-off / refresh / sparkle)
- **Trend**: ↑ / ↓ / − icon + giá trị, color `sem.success` / `sem.error` / `text.secondary`
- **Input/removable**: ✕ icon trailing 16dp, hit area 32dp

### 2.4. Badge
- **Dot**: 8dp circle, color theo semantic
- **Numeric**: pill h16, padding ngang 6dp, text label 11sp, max "99+"
- **Status**: pill h20, padding ngang 8dp, icon 12dp + text label

### 2.5. Avatar
- **Sizes**: 24 / 32 / 40 / 48 / 64 / 96 dp
- **Fallback**: initials max 2 ký tự + gradient bg (deterministic theo userId hash → 1 trong 8 gradient cố định)
- **Photo**: `radius.full`, border 2dp `surface.0` cho ring khi cần
- **Role badge** overlay góc dưới phải: nhỏ 14dp (ở 40dp avatar) hoặc 18dp (ở 64dp+), icon `Storefront` / `Eco`

### 2.6. Icon set
Inventory tối thiểu (designer ship 80+ icon):

**Navigation**: back, close, menu, more-vert, more-horiz, drawer, refresh
**Action**: add, edit, delete, save, share, copy, scan, qr-code, download, upload, search, filter, sort
**Status**: lock, unlock, sync, sync-error, cloud-off, cloud, check, alert-circle, info, sparkle, premium
**Domain**: rice-grain, scale, weight, farmer, trader, field, weather, sun, rain, cloud, wind
**Bottom nav**: scale, market, ai-chat, map (trader-only), account
**Money**: vnd-symbol, trend-up, trend-down, calculator

**Style**: 2dp stroke, 24dp viewport, rounded join, outlined default + filled variant cho selected nav. Avoid Material default — phải có character riêng.

### 2.7. Divider
- **Subtle**: 1dp `border.subtle`, dùng giữa row
- **Default**: 1dp `border.default`, dùng giữa section
- **Section break**: spacing `space.6` + optional label center

### 2.8. Loading spinner / progress
- **Spinner**: 16/24/32/48 dp, `brand.primary`, 2dp stroke, indeterminate rotation 800ms linear
- **Linear progress**: h4 `surface.2` track + `brand.primary` fill, `radius.xs`
- **Skeleton**: shimmer 45° gradient từ `surface.2` → `surface.3` → `surface.2`, 1200ms loop

---

## 3. Molecule library

### 3.1. Card
**Variants**:
- **Default**: `surface.2` bg + `radius.md` + `elev.1` + padding `space.5` (20dp)
- **Elevated**: `surface.3` bg + `radius.md` + `elev.2`
- **Outlined**: `surface.1` bg + `radius.md` + border `border.default`
- **Hero**: `radius.lg` + gradient `brand.primary` → `brand.primary` darken 8% + `elev.2`
- **Interactive**: thêm pressed state (scale 0.98 + `surface.3`)

**Anti-pattern (cấm)**: card-in-card. Inline highlight dùng surface tone contrast (`money.weight-surface` etc.) + divider thay vì card r12 lồng trong card r20.

### 3.2. List row
**Variants**:
- **Card list row** (CardListScreen): avatar + 2 dòng + trailing badge + chevron
- **Transaction row** (TraderTransactions): icon + amount + meta + chevron, h72
- **Setting row**: leading icon + label + trailing control (switch / chevron / value)
- **News row**: thumb 80×80 + title 2 line + source + time

Quy tắc: min h56, padding `space.4`, divider `border.subtle` giữa các row.

### 3.3. Empty state
- Icon 56dp trong circle `surface.3` `radius.full`
- Headline `text.h3`
- Subtitle `text.body` `text.secondary` max 2 line
- Optional CTA button md

### 3.4. Error state
- Icon `sem.error` α 40%, 56dp
- Headline `text.h3`
- Subtitle `text.body` `text.secondary`
- 2 CTA: primary "Thử lại", tertiary "Báo lỗi"

### 3.5. Offline banner
- Bg `status.offline`, padding `space.3` × `space.4`
- Icon CloudOff 18dp + text "Đang offline. Sẽ tự đồng bộ khi có mạng." body 14sp
- Position: top, ngay dưới TopBar, full-width, không có dismiss

### 3.6. Snackbar
- `surface.4` bg, white text, padding `space.4`, `radius.md`, max 2 line
- Optional action button tertiary trailing
- Auto-dismiss 4s, swipe to dismiss

### 3.7. Dialog
- `surface.3` bg, `radius.lg`, padding `space.5`
- Icon header optional 32dp `brand.primary`
- Headline `text.h2` center
- Body `text.body` `text.secondary`, max 4 line
- Actions row: primary right + tertiary/destructive left

### 3.8. Bottom sheet
- `surface.3` bg, top corner `radius.lg`, drag handle 36×4dp `border.strong` center top
- Header optional với title + close icon
- Content scrollable
- Action footer optional sticky bottom với `elev.2`

### 3.9. FAB
- 56dp circular, `brand.primary`, white icon 24dp, `elev.3`
- Extended: pill h56, icon 24dp + label `text.body-strong`, padding `space.4`
- Position: bottom-right margin 16dp + 88dp bottom (khoảng cách bottom nav). **Token `fab.bottom-offset = 88dp`.**

### 3.10. Bottom bar (ModernBottomBar)
**Giữ pill capsule pattern hiện tại** nhưng update với token:
- Float capsule margin 16dp ngang, bottom navigationBars + 8dp
- Bg `surface.4` + `elev.5` (matched cust shadow hiện tại)
- `radius.xl` (28dp)
- Height 64dp, 4 item (FARMER) hoặc 5 item (TRADER) SpaceEvenly
- **Item selected**: `brand.primary` `radius.lg` bg + icon filled white 22dp + label `text.label` white SemiBold
- **Item idle**: transparent + icon outlined `text.secondary` 22dp, no label
- Animation: icon crossfade 150ms, label expand + fade 220ms spring lowBouncy
- Haptic instant on tap
- **Badge slot**: dot 8dp `sem.error` góc trên phải icon — designer phải spec position chính xác

### 3.11. TopBar
**3 variants**:
- **Default**: surface.1 bg, h56, back 48dp + title `text.h2` + actions
- **Hero**: gradient `brand.primary` → darken 8%, h56-80, white text, dùng CardDetail
- **Large**: collapsing, expanded h120 với title `text.h1` 28sp, collapsed h56

### 3.12. Status pulse
- Dot 10dp + ring 16dp animated alpha 0.45↔1.0 + scale 0.85↔1.25, `motion.duration.pulse`
- Color theo `status.X`
- Reduced-motion: static dot không pulse

---

## 4. Theme variants (Light + Dark)

### Light
- App bg = `surface.1`
- Card = `surface.2`
- Status bar: translucent + theme color overlay
- Navigation bar: `surface.0` với divider top

### Dark
- App bg = `surface.1` (dark scale)
- Card = `surface.2` (dark scale)
- **Không** dùng shadow đậm — thay bằng border `border.subtle` + surface tint
- Brand primary dùng variant `#7BC57F` (lighten) để contrast với dark bg
- Hero gradient nhẹ hơn: `brand.primary` α 60% thay vì full
- Photography overlay: dark gradient overlay alpha 40% để text trắng đọc tốt

### Dark mode trigger
- **3 mode**: System (default) / Light force / Dark force
- Toggle ở SettingsScreen
- **Dynamic color M3 OFF mặc định** — để brand identity giữ vững. User power-user có thể bật trong Settings nếu muốn.

---

## 5. Iconography deliverable

- **80+ icon** SVG export, 2dp stroke, 24×24 viewport
- Categorized 5 nhóm: Navigation / Action / Status / Domain (nông nghiệp) / Money
- 2 variant cho icon nav: outlined + filled
- File format: SVG + Compose ImageVector code-ready (designer hand-off cho dev pipeline)
- Reference: Phosphor Icons, Lucide, Tabler — chọn 1 base library và customize 20% icon domain riêng (rice-grain, scale, farmer, trader, weather)

---

## 6. Illustration style

### Phong cách đề xuất
- **Editorial flat with grain texture** — flat color + subtle film grain overlay (3-5% noise)
- Palette giới hạn: `brand.primary`, `brand.accent`, `text.primary`, 2 neutral
- Không vector overly polished, không cute mascot, không 3D render
- Có thể có hand-drawn line accents (rice stalk, field furrow)

### Inventory phải ship
1. **Splash** illustration — logo + 1 mảng ruộng/lúa cách điệu
2. **Empty: Card list** — sổ trắng + biểu tượng cân
3. **Empty: Transaction** — ví rỗng có hạt lúa
4. **Empty: AI chat** — bóng nói + lúa
5. **Empty: Map** — bản đồ trắng + ghim
6. **Empty: News** — báo cuộn
7. **Empty: Deleted cards** — thùng rác có lúa
8. **Error: Network** — wifi off + ruộng
9. **Error: Generic** — biểu tượng "?" + chăn lúa
10. **Premium upsell** — vương miện lúa
11. **Role onboarding farmer** — nông dân với cân
12. **Role onboarding trader** — thương lái với xe máy/ghe

### Photography direction
Moodboard với 30+ ảnh được chấp nhận:
- Ruộng lúa chín, xanh, đang gặt
- Hạt thóc cận cảnh
- Tay đếm tiền / cầm sổ ghi
- Người Việt-Khmer-Lào (tránh stock photo white-collar)
- Đồng bằng sông Cửu Long landmark (kênh rạch, ghe, máy gặt)

**Counter-example** (10 ảnh bị reject):
- AI-generated farmers
- Cliché "happy farmer flexing rice"
- Western farm imagery
- Over-saturated golden hour
- Drone shot quá điện ảnh (không khớp app)

---

## 7. Acceptance criteria

Trước khi ship, đảm bảo:

- [ ] **Token coverage**: mọi giá trị design trong 5 designer khác đều có token tương ứng — không cần hardcode
- [ ] **Token file** export được 3 format: Figma Tokens JSON, Style Dictionary JSON, CSS variables (sample)
- [ ] **Dark mode**: 100% token có dark variant, không có hardcode hex riêng cho dark
- [ ] **Contrast check**: mỗi cặp `text.X` trên `surface.Y` pass WCAG AA tối thiểu (tools: Stark, axe)
- [ ] **AAA** cho mọi text trên surface dùng cho hiển thị số tiền/KG (showcase 1 mẫu)
- [ ] **Atom library**: 8 atom × ≥4 state mỗi cái → ≥32 variant
- [ ] **Molecule library**: 12 molecule × ≥3 state → ≥36 variant
- [ ] **Icon set**: 80+ icon, 2 variant cho nav (outlined/filled)
- [ ] **Illustration set**: 12 illustration đầu tiên + photography moodboard 30 + 10 reject
- [ ] **Spec document**: 1 file PDF/Markdown mô tả token semantics, naming convention, usage rule, anti-pattern
- [ ] **Hand-off check** với D2-D6 ngày cuối tuần 2: chạy qua 1 screen tiêu biểu từng designer, đảm bảo token đủ dùng — list gap nếu thiếu

---

## 8. Pain points spec gốc cần fix (D1 owns)

Tag rõ trong deliverable:

| ID | Pain point | Fix bởi |
|---|---|---|
| #1 | 3 nguồn màu xung đột (AppColors / Color.kt / M3 dynamic) | Token nhất 1 nguồn, dynamic color off mặc định |
| #11 | Không có dark mode toggle | 3-mode toggle (system/light/dark) trong Settings spec, D6 implement UI |
| #15 | Filter chip "Tất cả" green ambiguous với trend "ổn định" | Chip token rõ ràng: filter idle = outlined, trend = colored với icon |
| #29 | Sync Status M3 colorScheme khác mọi nơi | Force surface scale token, không dùng M3 colorScheme thuần |
| #36 | Dead code RemainingHeroCard, MarkdownBlocks, MetaItem | Spec rõ component nào còn dùng, dev refactor theo |
| #43 | TopBar SyncStatusScreen M3 inconsistent | TopBar default variant token, áp dụng cho mọi screen |
| #44 | ErrorBanner dismiss icon sai (OpenInNew) | Icon set có đúng `close` icon, banner spec dùng đúng |

---

## 9. Tuần-by-tuần milestone

| Tuần | Output |
|---|---|
| **1** | Color + typography + spacing + radius token draft. Showcase 3 atom cơ bản (button, input, chip). Photography moodboard. |
| **2** | Token final. Atom library complete. Molecule library WIP. **Hand-off draft cho D2-D6 cuối tuần 2.** |
| **3** | Dark mode complete. Icon set 80+. Illustration 6-8 cái đầu tiên. |
| **4** | Illustration set complete. QA hỗ trợ 5 designer khác (token gap, contrast check). |
| **5** | Polish. Design system documentation (PDF/Markdown). Hand-off package cho dev. |
| **6** | Final review + dev hand-off + token freeze |

---

## 10. References

- **Spec gốc**: section 11 (Design system hiện tại), section 13.2-13.7 (Direction đề xuất)
- **Inspiration**: Cropwise, John Deere Operations Center, Plantix, Pocket Casts, Linear, Apple Weather
- **Token tools**: Figma Tokens, Style Dictionary, Tokens Studio
- **Contrast check**: Stark, axe DevTools
- **Font hosting**: Google Fonts Vietnamese, Adobe Fonts
- **Icon library base**: Phosphor Icons / Lucide / Tabler (chọn 1)
