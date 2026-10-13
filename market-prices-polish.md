# Kế hoạch Hoàn thiện UI/UX & Hiệu Ứng Nâng Cao — Tab Bảng Giá Lúa Gạo (MarketScreen PricesPage & RicePriceCard)

> **Mục tiêu:** Nâng cấp toàn diện Tab Bảng giá lúa gạo (`MarketScreen.kt` PricesPage, `RicePriceCard.kt`, `PriceTrendChart.kt`), chuẩn hóa công thái học ($\ge 48\text{dp}$ touch target cho mọi nút và bộ lọc), bản địa hóa 100% sang 5 ngôn ngữ, bổ sung Empty State hoàn chỉnh với nút CTA tải lại, và áp dụng Motion Spec (`@hieu-ung-nangcao`).
> **Thời gian:** 2026-10-13
> **Trạng thái:** Completed (Đã biên dịch thành công `BUILD SUCCESSFUL`)

---

## 1. Bối cảnh & Phân tích Hiện trạng (UI/UX Audit)

### 1.1 Vấn đề Phát hiện (Checklist Audit)
- ❌ **Hardcoded Strings:**
  - `PricesPage`: Tiêu đề & phụ đề "Giá rao của bạn", "Giá thu mua hôm nay", "💡 Dữ liệu mang tính tham khảo..." bị hardcode.
  - `FilterChipsRow`: Nhãn "Tất cả", "Đang tăng", "Ổn định", "Đang giảm" bị hardcode.
  - `MyBidCard`: Nhãn xu hướng, "📍 Khu vực:", nút "Xoá tin", "Sửa giá", giống lúa fallback "Chưa xác định" bị hardcode.
  - `RicePriceCard`: Nhãn cột "Thấp nhất", "Cao nhất", "TB 7 ngày", đơn vị "đ/kg", nhãn badge "Tăng", "Giảm", "Ổn định", thời gian tương đối "Vừa cập nhật", "X phút trước" bị hardcode.
  - `PriceTrendChart`: Tiêu đề "Xu hướng giá", empty state "Chưa có dữ liệu lịch sử", toggle "7n"/"30n", nhãn thống kê "Min", "TB", "Max" bị hardcode.
- 🟡 **Chuẩn 4 UI States (Empty State):**
  - Khi danh sách giá rỗng (`prices.isEmpty() && !isLoading`), `PricesPage` không có Empty State component, chỉ hiện khoảng trắng. Cần có `MarketPricesEmptyState` với Primary CTA Button "Tải lại bảng giá" ($\ge 48\text{dp}$).
- 🟡 **Công thái học & Touch Target ($\ge 48\text{dp}$):**
  - Nút "Sửa giá" và "Xoá tin" trong `MyBidCard` cần chiều cao tối thiểu $\ge 48\text{dp}$.
  - Toggle 7d / 30d trong `PriceTrendChart` cần đạt vùng chạm tối thiểu $\ge 40-48\text{dp}$.
  - Filter chips trong `FilterChipsRow` đảm bảo $\ge 48\text{dp}$.
- 🟡 **Hiệu ứng chuyển động (Motion Spec `@hieu-ung-nangcao`):**
  - Chuyển đổi bộ lọc xu hướng với `animateColorAsState` kết hợp spring animation.
  - Micro-interaction xúc giác `HapticUtil.tick(context)` khi bấm chọn bộ lọc, bấm vào thẻ giá để mở biểu đồ xu hướng, bấm sửa/xóa tin rao, và bấm nút tải lại giá.

---

## 2. Motion Spec (`@hieu-ung-nangcao`)

### Motion Spec 1: Trend Filter Pill Selection
```text
Intent: Phản hồi thị giác tức thời và êm dịu khi lọc theo xu hướng giá (Tăng / Ổn định / Giảm)
Trigger: Chạm vào chip xu hướng
Start state: Unselected (màu viền Divider, nền trong suốt)
End state: Selected (màu viền + nền tinted alpha 0.12f + dot chỉ báo)
Physics: spring(stiffness = Spring.StiffnessMediumLow) qua animateColorAsState
Feedback: HapticUtil.tick(context)
```

### Motion Spec 2: Rice Price Card Press & Sheet Expansion
```text
Intent: Cung cấp phản hồi xúc giác nhẹ nhàng khi người dùng xem chi tiết biến động giá giống lúa
Trigger: Chạm vào RicePriceCard
Feedback: HapticUtil.tick(context) + ModalBottomSheet trượt mượt mà
```

---

## 3. Các Bước Thực Thi (Action Breakdown)

### Bước 1: Đồng bộ Chuỗi Đa ngôn ngữ (5 Locales)
- Thêm 25 chuỗi giao diện về Bảng giá, Bộ lọc xu hướng, Biểu đồ giá, Empty state vào 5 tệp:
  - `values/strings.xml` (Tiếng Việt)
  - `values-en/strings.xml` (Tiếng Anh)
  - `values-km/strings.xml` (Tiếng Khmer)
  - `values-lo/strings.xml` (Tiếng Lào)
  - `values-zh-rCN/strings.xml` (Tiếng Trung Giản thể)

### Bước 2: Nâng cấp `RicePriceCard.kt`
- Dùng `stringResource` cho trend badges, column labels, đơn vị đ/kg, contentDescription.
- Tích hợp `formatRelativeTime` `@Composable` dùng chuỗi chung.
- Tích hợp `HapticUtil.tick(context)` khi chạm vào Card.

### Bước 3: Nâng cấp `PriceTrendChart.kt`
- Dùng `stringResource` cho tiêu đề, empty state, toggle thời gian, nhãn thống kê Min/TB/Max.
- Tích hợp `HapticUtil.tick(context)` khi chuyển đổi khung thời gian 7d/30d.

### Bước 4: Nâng cấp `MarketScreen.kt` (PricesPage & MyBidCard & FilterChipsRow)
- Bổ sung `MarketPricesEmptyState` hoàn chỉnh với nút CTA $\ge 48\text{dp}$.
- Dùng `stringResource` cho các SectionTitle, MyBidCard (nút sửa/xóa $\ge 48\text{dp}$), FooterNote.
- Nâng cấp `FilterChipsRow` với spring animation và haptics.

### Bước 5: Kiểm tra Biên dịch & Nghiệm thu
- Biên dịch bằng `.\gradlew.bat compileDebugKotlin`.
- Đóng dấu hoàn tất `<!-- GOAL_COMPLETE -->`.
