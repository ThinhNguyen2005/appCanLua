# Designer 5 — Account & Trader-only

**Phạm vi**: 6 màn hình.
- FarmerProfileScreen (account tab cho FARMER)
- TraderProfileScreen (account tab cho TRADER)
- TraderTransactionsScreen (Trader-only tab)
- TraderHistoryScreen
- RiceMapScreen (Trader-only tab)
- TraderComingSoonScreen (placeholder Sprint 6)

**Spec gốc đọc trước**: section 6 (Account), section 8 (Trader screens), section 9 (Trader map).

**Phụ thuộc**: D1 ship token + avatar + list row + map tokens (cuối tuần 2).

**Critical pain points fix**: #3 (role switcher dev-only nhưng public visible), #12 (no avatar upload), #13 (sign-out không confirm), #25-27 (trader screens incomplete), #33 (edit toggle không có Cancel revert), #34 (CCCD plain text không format), #35 (header email overflow maxLines).

---

## 0. Mục tiêu trải nghiệm

Account screen hiện là 1 danh sách phẳng "Hồ sơ / Đổi vai trò (dev) / Cài đặt / Đăng xuất" — không có brand identity, không hiển thị role rõ, sign-out tap nhầm là logout luôn. Trader screens (Transactions, History, Map) hiện là placeholder. Redesign:

1. **Profile hero**: avatar + tên + role badge + 1 stat hữu ích (FARMER: tổng KG cân; TRADER: tổng giao dịch tháng)
2. **Role switcher**: ẨN khỏi production build (FIX pain #3). Chỉ debug build mới thấy + có warning chip.
3. **Confirm cho destructive action**: sign-out, delete account
4. **Trader screens** = đầy đủ hoặc dùng "Coming Soon" có ETA, không placeholder bí ẩn

---

## 1. FarmerProfileScreen

### 1.1. Yêu cầu chức năng

Read: profile (name, phone, email, address, role, hometown, CCCD, avatar), stats (totalKg, totalCards, totalRevenue), settings shortcuts.
Write: edit profile, upload avatar, request role upgrade (→ RoleRequest), sign out, delete account.

### 1.2. Layout đề xuất

```
┌──────────────────────────────────┐
│ Tài khoản            [⚙ settings]│  ← top bar minimal
├──────────────────────────────────┤
│                                  │
│  ┌─ Hero profile card ────────┐  │
│  │      [Avatar 96dp circle]   │  │  ← tap → upload sheet
│  │      [edit pencil badge]    │  │
│  │                             │  │
│  │      Nguyễn Văn A           │  │  ← h1
│  │      🌾 Nông dân             │  │  ← role chip filled brand.primary
│  │                             │  │
│  │      📍 Long An             │  │  ← caption text.tertiary
│  └─────────────────────────────┘  │
│                                  │
│  ┌─ Stat strip 3 cards ───────┐  │
│  │ [12.5T]  [85]   [102M]      │  │
│  │ Tổng KG  Phiếu  Doanh thu   │  │
│  └─────────────────────────────┘  │
│   tap → mở Statistics screen     │
│                                  │
│  Thông tin cá nhân            ▾  │  ← section header
│  ┌─ List row ─────────────────┐  │
│  │ [📱] SĐT                    │  │
│  │     +84 903 xxx xxx         │  │  ← masked: 3 số cuối ẩn
│  │     (cách 1 dòng)           │  │     toggle "Hiện"
│  ├────────────────────────────┤  │
│  │ [✉] Email                   │  │
│  │     nguyenvana@gmail.com    │  │  ← maxLines 1, ellipsis nếu dài
│  ├────────────────────────────┤  │
│  │ [📋] CCCD                    │  │  
│  │     1234-5678-9012          │  │  ← format 4-4-4 FIX pain #34
│  │     [👁 toggle]              │  │     ẩn 4 số giữa khi off
│  ├────────────────────────────┤  │
│  │ [🏠] Địa chỉ                 │  │
│  │     123 Lê Lợi, Long An      │  │
│  ├────────────────────────────┤  │
│  │ [🌾] Quê quán                │  │
│  │     Tiền Giang               │  │
│  └────────────────────────────┘  │
│                                  │
│  [Chỉnh sửa hồ sơ →]              │  ← tertiary button center
│                                  │
│  Vai trò                       ▾ │
│  ┌─ Card ────────────────────┐   │
│  │ 🌾 Bạn đang là Nông dân    │   │
│  │ Muốn nâng cấp thành        │   │
│  │ thương lái?                │   │
│  │  [Tìm hiểu →]               │   │  ← tertiary button
│  └────────────────────────────┘  │
│                                  │
│  Tiện ích                        │
│  ┌─ List row ───────────────────┐│
│  │ [💎] Premium                  ││  
│  │      Bạn đang là Early Adopter│ ← stat chip success
│  │      Đến 22/05/2026           │
│  └──────────────────────────────┘│
│  ┌─ List row ───────────────────┐│
│  │ [📊] Thống kê chi tiết       ││
│  ├──────────────────────────────┤│
│  │ [📥] Xuất dữ liệu            ││
│  ├──────────────────────────────┤│
│  │ [🌙] Giao diện               ││
│  │     Sáng / Tối / Hệ thống    ││  ← dropdown trailing
│  ├──────────────────────────────┤│
│  │ [🌐] Ngôn ngữ                ││
│  │     Tiếng Việt               ││
│  └──────────────────────────────┘│
│                                  │
│  Hỗ trợ                          │
│  ┌─ List row ───────────────────┐│
│  │ [💬] Phản hồi & Báo lỗi      ││
│  ├──────────────────────────────┤│
│  │ [📖] Điều khoản              ││
│  ├──────────────────────────────┤│
│  │ [🔒] Chính sách              ││
│  └──────────────────────────────┘│
│                                  │
│  [Đăng xuất]                     │  ← destructive ghost button
│  v1.2.3 (build 142)              │  ← caption text.tertiary
│                                  │
└──────────────────────────────────┘
```

### 1.3. Avatar upload (FIX pain #12)

Tap avatar circle → bottom sheet:
```
┌─ Sheet ──────────────────────┐
│ Ảnh đại diện                 │
├──────────────────────────────┤
│  [📷] Chụp ảnh mới            │
│  [🖼] Chọn từ thư viện         │
│  [✏] Vẽ chữ cái đầu (default) │  ← fallback 1 chữ trên màu random
│  [🗑] Xoá ảnh                  │  ← destructive red, chỉ show nếu có ảnh
└──────────────────────────────┘
```

Backend: Firebase Storage upload + URL lưu trong `profile.avatarUrl`. Crop circle preview trước upload.

Empty avatar (chưa upload): show initials trên solid color generated từ uid hash. Color palette: 8 màu pastel (xem D1).

### 1.4. Edit profile flow (FIX pain #33)

Tap "Chỉnh sửa hồ sơ" → mở full-screen edit form:
```
[← Huỷ]   Chỉnh sửa            [Lưu]
                                  ↑
                          disabled khi không có thay đổi

┌─ Avatar mini + edit ────┐
│ [Avatar 64dp] [Thay ảnh]│
└─────────────────────────┘

┌─ Input field ──────┐
│ Họ tên             │
│ Nguyễn Văn A       │
└────────────────────┘

... (all fields editable)
```

- **Cancel logic** (FIX pain #33): tap [← Huỷ] mà có thay đổi → dialog "Huỷ thay đổi? Thông tin chưa lưu sẽ mất. [Tiếp tục sửa] [Huỷ thay đổi]"
- **Save logic**: tap [Lưu] → spinner, success snackbar "Đã cập nhật". Failure → inline error per field.
- **No-change** state: [Lưu] disabled grey

### 1.5. Sign-out confirm (FIX pain #13)

Tap "Đăng xuất" → dialog:
```
┌─ Dialog ──────────────────────┐
│        [Illustration 80dp]     │
│        Đăng xuất khỏi          │
│        Cân Lúa?                │
│                                │
│  Dữ liệu offline vẫn được giữ. │
│  Bạn có thể đăng nhập lại bất  │
│  cứ lúc nào.                   │
│                                │
│  [Huỷ]    [Đăng xuất]          │
└────────────────────────────────┘
```

[Đăng xuất] là destructive button đỏ.

### 1.6. Delete account (chỉ trong Settings → Tài khoản, không primary)

Multi-step confirm:
1. Sheet "Xoá tài khoản? Hành động này không thể hoàn tác."
2. Input "Nhập 'XOÁ' để xác nhận"
3. Loading 3s với cancel option
4. Sign-out + redirect Auth

### 1.7. States

- **Default fully filled**
- **Default minimum** (chưa upload avatar, chưa fill optional fields → show "+ Thêm ảnh", "+ Thêm CCCD" rows clickable)
- **Loading** (skeleton 3 row hero + stat strip)
- **Offline** (banner top "Đang offline · thay đổi sẽ sync khi có mạng")
- **Edit mode** (full screen separate)
- **Dialog states** (sign-out, delete, role-upgrade explainer)

### 1.8. Pain point fix table

| ID | Fix |
|---|---|
| #3 | Role switcher đã rời section "Vai trò" — xem Settings spec D6 |
| #12 | Avatar upload sheet với 4 option |
| #13 | Sign-out dialog confirm + destructive button |
| #33 | Edit mode có Cancel button + dirty check dialog |
| #34 | CCCD format 4-4-4 auto + toggle "Hiện/Ẩn" |
| #35 | Email row maxLines 1 + ellipsis (không vỡ) |

---

## 2. TraderProfileScreen

### 2.1. Khác FARMER ở đâu

- Role badge: `🚚 Thương lái` filled `brand.accent` (lúa chín ấm)
- Stat strip: `Phiếu chốt | Tổng tiền giao dịch | Số nông dân`
- Section "Hồ sơ doanh nghiệp": tên DN, MST, địa chỉ làm việc, status verify (✓ Đã duyệt / ⚠ Đang chờ / ✗ Bị từ chối)
- Section "Vai trò" → ẩn (đã là TRADER), thay bằng "Khu vực hoạt động" với map preview nhỏ
- Stats có click-through: tap "Phiếu chốt" → TraderTransactions, tap "Số nông dân" → list

### 2.2. Layout chỉ khác hero + section đặc thù

```
┌─ Hero ──────────────────────────┐
│   [Avatar + verified badge ✓]   │
│   Nguyễn Văn B                  │
│   🚚 Thương lái Gia Thịnh        │
│   ⭐ 4.8 (24 đánh giá)           │  ← rating từ FARMER feedback
└─────────────────────────────────┘

┌─ Stat strip 3 card ────────────┐
│ [127]   [245M]   [38]           │
│ Phiếu   Tổng GD  Nông dân       │
└─────────────────────────────────┘

Hồ sơ doanh nghiệp
┌─ List ─────────────────────────┐
│ [🏢] Gia Thịnh Trading          │
│ [📋] MST 0312345678              │
│ [📍] 123 Lê Lợi, Long An         │
│ [✓ Đã xác minh]                 │  ← chip success
└────────────────────────────────┘

Khu vực hoạt động
┌─ Map preview 160dp ────────────┐
│ [Map snippet 13 markers]        │
│  [Mở bản đồ →]                  │
└────────────────────────────────┘

[Rest same as FarmerProfile but role section removed]
```

---

## 3. TraderTransactionsScreen (Trader-only tab)

### 3.1. Yêu cầu chức năng

Tab "Giao dịch" cho TRADER. Hiển thị các phiếu cân TRADER đã chốt (qua QR scan từ FARMER). Sort by date desc.

Filter: thời gian (hôm nay / 7d / 30d / tất cả), status (đã thanh toán / còn nợ), tìm theo tên FARMER.

### 3.2. Layout

```
┌──────────────────────────────────┐
│ Giao dịch         [🔍] [📊 stats]│
├──────────────────────────────────┤
│  ┌─ Filter strip ─────────────┐  │
│  │ [Hôm nay ▾][Status ▾][Sort]│  │  ← chip filter horizontal scroll
│  └────────────────────────────┘  │
│                                  │
│  ┌─ Summary card 80dp ─────────┐ │
│  │ Tháng 5 / 2026               │ │
│  │ 127 phiếu · 245.000.000 đ    │ │  ← display 24sp
│  │ ── Đã trả: 200M  Còn nợ: 45M│ │  ← progress bar 2 màu
│  └─────────────────────────────┘ │
│                                  │
│  ─── Hôm nay (3 phiếu) ───────  │  ← sticky group header
│  ┌─ Transaction row 72dp ──────┐│
│  │ [Avatar] Anh Tâm             ││
│  │          5 tấn · 41.000.000đ ││
│  │          09:32 ✓ Đã trả      ││  ← status chip
│  └──────────────────────────────┘│
│  ┌─ Transaction row ────────────┐│
│  │ [Avatar] Chị Lan             ││
│  │          3.2 tấn · 26.240.000đ│
│  │          11:15 ⏳ Còn nợ 5M  ││  ← warning chip
│  └──────────────────────────────┘│
│                                  │
│  ─── Hôm qua (5 phiếu) ────────  │
│  ...                             │
│                                  │
│  [Bottom nav]                    │
└──────────────────────────────────┘
```

### 3.3. Transaction row anatomy

- Avatar FARMER + name
- Subtitle: weight + total amount
- Trailing right: time + status chip
- Tap → mở `CardDetail` (read-only cho TRADER, không sửa được)
- Long-press → bottom sheet: [📞 Gọi farmer] [💬 Ghi chú] [🗑 Xoá (sau 24h khoá)]

### 3.4. Summary card

- BG gradient `brand.primary` → darker
- Text on-primary white
- 2 KPI: tổng tiền tháng + breakdown đã trả/còn nợ với progress bar inline
- Tap → mở `TraderHistory` (analytics chi tiết)

### 3.5. States

- **Default** (data đầy đủ + grouping by date)
- **Empty** (no transactions): "Chưa có phiếu nào. Quét QR từ nông dân để bắt đầu."
- **Loading**, **Offline**, **Error**, **Dark**
- **Filter active** (chip "Hôm nay" filled)
- **Search active** (search bar expand thay top bar)

### 3.6. Pain point fix

| ID | Fix |
|---|---|
| #25 | Trader transactions có data structure rõ ràng, sort + filter chuẩn |
| #15 | Filter chip dùng D1 atom |
| #44 | Status chip có icon (✓ / ⏳) không chỉ màu |

---

## 4. TraderHistoryScreen (analytics)

### 4.1. Yêu cầu

Dashboard analytics 30d / 6m / 1y cho TRADER.

KPI:
- Tổng tiền giao dịch
- Số phiếu
- Số nông dân unique
- Trung bình/phiếu
- Loại lúa top 3
- Tỉnh top 3
- Xu hướng (chart line)

### 4.2. Layout đề xuất — dashboard

```
┌──────────────────────────────────┐
│ [← back] Thống kê                │
├──────────────────────────────────┤
│  [Tab: 30d | 6m | 1y]            │  ← segmented control
│                                  │
│  ┌─ Hero KPI card ─────────────┐ │
│  │ Doanh thu 30 ngày qua        │ │
│  │ 245.000.000 đ ↑ 12%          │ │  ← display 28sp + delta
│  │ [▁▂▄▆▇▇▆▄▅▆] chart 60dp     │ │
│  └─────────────────────────────┘ │
│                                  │
│  ┌─ KPI grid 2x2 ──────────────┐ │
│  │ ┌──────────┐  ┌──────────┐  │ │
│  │ │127 phiếu │  │38 nông dân│ │ │
│  │ │↑ 5       │  │↑ 2       │  │ │
│  │ └──────────┘  └──────────┘  │ │
│  │ ┌──────────┐  ┌──────────┐  │ │
│  │ │1.93M/phiếu│ │94% trả đúng│ │
│  │ │↓ 200k    │  │↑ 4%       │ │ │
│  │ └──────────┘  └──────────┘  │ │
│  └─────────────────────────────┘ │
│                                  │
│  Loại lúa top                    │
│  ┌─ Bar chart horizontal ──────┐ │
│  │ OM5451  ████████ 45%         │ │
│  │ OM18    █████    28%         │ │
│  │ Đài Thơm ███     18%          │ │
│  │ Khác    ██       9%          │ │
│  └─────────────────────────────┘ │
│                                  │
│  Tỉnh hoạt động                  │
│  ┌─ Map mini ─────────────────┐  │
│  │  [13 tỉnh ĐBSCL với heatmap]│  │
│  └────────────────────────────┘  │
│                                  │
│  [📥 Xuất báo cáo Excel]          │  ← tertiary
└──────────────────────────────────┘
```

### 4.3. Pain point fix

| ID | Fix |
|---|---|
| #26 | History có cấu trúc dashboard, KPI hierarchy rõ |
| #42 | Chart dùng Vico với multi-line / bar / map types |

---

## 5. RiceMapScreen (Trader-only tab)

### 5.1. Yêu cầu

Bản đồ Google Maps hiển thị:
- Các điểm thu mua đã chốt (cluster marker)
- Các farmer đang post yêu cầu bán
- Vị trí trader hiện tại
- Heatmap khu vực hoạt động

User control: filter by date / loại lúa / radius, tap marker → bottom sheet detail.

### 5.2. Layout

```
┌──────────────────────────────────┐
│ [Map full screen]                │
│                                  │
│  ┌─ Search bar top ───────────┐  │
│  │ [🔍] Tìm theo địa chỉ...   │  │  ← floating glass background
│  │                       [⚙]   │  │
│  └────────────────────────────┘  │
│                                  │
│  ┌─ Filter chip strip ──────────┐│
│  │ [📅 30d][🌾 OM5451][📍 5km] ││  ← horizontal scroll
│  └──────────────────────────────┘│
│                                  │
│           [⊕  marker cluster 12] │  ← cluster với count
│                                  │
│         [👤 me marker pulse]     │  ← own location
│                                  │
│  ┌─ Bottom controls ─────────┐   │
│  │ [📍] Vị trí của tôi        │   │  ← floating action button
│  │ [🔄] Refresh               │   │
│  │ [🗺] Layer (satellite/...) │   │
│  └───────────────────────────┘   │
│                                  │
│  ┌─ Bottom info card 80dp ────┐  │  ← collapsed by default
│  │ ▔▔▔▔▔ drag handle ▔▔▔▔▔     │  │
│  │ 12 điểm thu mua trong vùng  │  │
│  │  [Xem danh sách]             │  │
│  └──────────────────────────────┘ │
└──────────────────────────────────┘
```

### 5.3. Marker types

- 🚚 (filled brand.accent): điểm thu mua của trader
- 🌾 (filled brand.primary): farmer đang post
- 👤 (pulse animation): vị trí mình
- ⊕ Cluster: nhiều marker gần nhau, hiện count, tap → zoom in

### 5.4. Marker tap → bottom sheet

```
┌─ Sheet 240dp ─────────────────┐
│ [Avatar] Anh Tâm              │
│         🌾 Đang bán 5 tấn      │
│         OM5451 · 8.300 đ/kg   │
│         📍 2.3km từ bạn        │
│                                │
│  [📞 Gọi]  [💬 Nhắn]  [🗺 Đến]  │
└────────────────────────────────┘
```

### 5.5. States

- **Default** (zoomed to user location, 5km radius)
- **Loading** (skeleton map placeholder grid + spinner)
- **GPS denied** (banner top "Bật vị trí để xem bản đồ chính xác" + button "Bật")
- **Offline** (cached tiles + banner "Đang offline · chỉ hiển thị điểm đã tải")
- **Empty area** ("Không có điểm thu mua nào trong khu vực. Mở rộng bán kính?")
- **Filter active** (chip filled)
- **Marker sheet open**
- **Dark mode** (map style dark grey, marker contrast)

### 5.6. Pain point fix

| ID | Fix |
|---|---|
| #27 | Map UI có filter chip + cluster + bottom sheet detail thực sự usable |
| #44 | Error / offline banner có icon affordance |

---

## 6. TraderComingSoonScreen (placeholder Sprint 6)

### 6.1. Yêu cầu

Vài feature TRADER chưa hoàn thiện (analytics export advanced, bulk QR scan, chat group). Hiện tại screen cần show "Coming soon" gracefully với ETA.

### 6.2. Layout

```
┌──────────────────────────────────┐
│ [← back]                         │
│                                  │
│       [Illustration 200dp]       │  ← farmer + sprout growing
│                                  │
│       Sắp ra mắt                 │  ← h1
│                                  │
│  Tính năng "Xuất báo cáo nâng    │
│  cao" đang được hoàn thiện.      │  ← body text.secondary
│                                  │
│  Dự kiến: Quý 3 / 2026           │  ← chip status.info
│                                  │
│  Bạn muốn được thông báo?        │
│                                  │
│  ┌─ Primary CTA h56 ──────────┐  │
│  │  🔔 Bật thông báo           │  │
│  └────────────────────────────┘  │
│                                  │
│  ┌─ Tertiary button ──────────┐  │
│  │  💬 Gửi đề xuất tính năng  │  │
│  └────────────────────────────┘  │
└──────────────────────────────────┘
```

### 6.3. Pain point fix

| ID | Fix |
|---|---|
| #23 | ComingSoon có ETA cụ thể + notify CTA + feedback CTA, không placeholder bí ẩn |

---

## 7. Acceptance criteria

- [ ] 6 màn × multi-state (Default / Loading / Empty / Offline / Dark) = ≥30 layout
- [ ] FARMER ↔ TRADER profile rõ khác biệt (badge color + stat content)
- [ ] Edit profile flow có Cancel + dirty check dialog
- [ ] Sign-out có confirm dialog destructive
- [ ] Avatar upload sheet 4 option + crop preview
- [ ] CCCD format 4-4-4 auto + masked
- [ ] Email maxLines 1 + ellipsis
- [ ] TraderTransactions có filter chip + summary card + grouping by date
- [ ] TraderHistory dashboard có hero KPI + grid + chart + map preview
- [ ] RiceMap có search + filter chip + marker types + bottom sheet
- [ ] ComingSoon có ETA + notify + feedback CTA
- [ ] 2 locale showcase (vi + en) cho TraderProfile (text dài nhất)
- [ ] Font scale 1.2× cho hero card FARMER profile
- [ ] Tag fix pain points #3, #12, #13, #25, #26, #27, #33, #34, #35, #15, #44, #23
- [ ] Prototype flow: FarmerProfile → Edit → thay đổi → tap Cancel → dialog → keep editing
- [ ] Prototype flow: FarmerProfile → Sign-out → dialog confirm → Auth screen
- [ ] Prototype flow: TraderTransactions → tap row → CardDetail readonly → back

---

## 8. Timeline

| Tuần | Output |
|---|---|
| **1** | UX flow 2 profile (FARMER + TRADER) + Trader screens. Validation rules. |
| **2** | Hi-fi FarmerProfile dùng D1 token + atom. Avatar upload sheet. |
| **3** | Hi-fi TraderProfile + Transactions + History. Edit flow. |
| **4** | Hi-fi RiceMap + ComingSoon. QA + 2 locale. |
| **5** | Polish edge cases (long name, missing avatar, GPS off, marker overflow). |
| **6** | Idle / hỗ trợ. |

---

## 9. References

- **Spec gốc**: section 6 (Account/Profile), section 8 (Trader screens), section 9 (Map)
- **Files implementation**:
  - `app/src/main/java/com/GiaThinh/canlua/ui/screen/AccountScreen.kt`
  - `app/src/main/java/com/GiaThinh/canlua/ui/screen/TraderTransactionsScreen.kt`
  - `app/src/main/java/com/GiaThinh/canlua/ui/screen/TraderHistoryScreen.kt`
  - `app/src/main/java/com/GiaThinh/canlua/ui/screen/RiceMapScreen.kt`
  - `app/src/main/java/com/GiaThinh/canlua/ui/screen/TraderComingSoonScreen.kt`
- **Inspiration**: Stripe Dashboard (analytics), Uber Driver (map + filter), Linear settings (profile edit flow)
- **Dependencies**: Coil/Glide for avatar, Google Maps Compose, Vico chart, Firebase Storage cho avatar
