# Kế hoạch Hoàn thiện UI/UX & Hiệu Ứng Nâng Cao — CardDetailScreen

> **Mục tiêu:** Nâng cấp toàn diện màn hình Chi tiết phiếu cân (`CardDetailScreen.kt`) theo chuẩn UI/UX Playbook, khắc phục triệt để lỗi thiếu Error State, và bổ sung hiệu ứng chuyển động có mục đích (Purposeful Motion) theo chuẩn `@hieu-ung-nangcao`.
> **Thời gian:** 2026-10-12
> **Trạng thái:** In Progress (Thực thi tự động theo `/goal`)

---

## 1. Bối cảnh & Phân tích Hiện trạng (Current State Audit)

### 1.1 Vấn đề Phát hiện (Checklist Audit)
- ❌ **Thiếu Error / Not Found State:** Khi `currentCard == null` và `!isLoading` (phiếu cân bị xoá hoặc sai ID), hệ thống hiện fallback về `placeholderCard` trống rỗng (id=0), gây hiểu lầm là app bị đơ hoặc lỗi dữ liệu.
- 🟡 **Hiệu ứng chuyển động (Motion & Micro-interactions):** Chuyển tab giữa các bảng bao cân (Bảng 1, Bảng 2...) và vuốt HorizontalPager cần mượt mà, phản hồi rung xúc giác đồng bộ.
- 🟡 **Spacing & Tokens:** Đồng bộ các kích thước khoảng cách và bo góc về `AppDimensions` và `DESIGN.md`.

---

## 2. Motion Spec (`@hieu-ung-nangcao`)

```text
Intent: Cung cấp phản hồi xúc giác và thị giác mượt mà khi xem/chuyển đổi giữa các bảng cân
Trigger: Người dùng vuốt pager hoặc chạm vào tab bảng cân (Bảng 1, Bảng 2,...)
Start state: Tab cũ active (viền xanh 1.5dp, nền GreenSurface)
End state: Tab mới active với viền animate mượt mà, pager cuộn êm ái
User-controlled or automatic: User-controlled
Interruptible: Có
Physics/easing: tween(200, FastOutSlowInEasing)
Performance risk: Thấp (không recompose danh sách, animate qua animateDpAsState)
Fallback: Chuyển đổi tức thì nếu thiết bị bật giảm hiệu ứng chuyển động
```

---

## 3. Các Bước Thực Thi (Action Breakdown)

### Bước 1: Xử lý Trạng thái Lỗi / Not Found chuẩn 4-States
- Trong `CardDetailScreen.kt`:
  - Khi `isLoading`: Hiển thị Skeleton/Loading mượt mà hoặc Header đồng nhất.
  - Khi `!isLoading && currentCard == null`: Hiển thị Error State chuyên biệt (Icon thông tin, thông điệp rõ ràng, nút CTA "Quay lại danh sách" với touch target $\ge 48\text{dp}$).
  - Khi `currentCard != null`: Render giao diện chi tiết đầy đủ.

### Bước 2: Tối ưu Công thái học & Hiệu ứng Nâng cao
- Thêm hiệu ứng xuất hiện tự nhiên cho FAB "Cân lúa".
- Tinh chỉnh các card thông tin (`CardInfoCard`, `WeightSummaryCard`, `FinancialSummaryCard`, `BagEntriesCard`) đồng bộ bo góc `16.dp` và bóng đổ `2.dp` theo `DESIGN.md`.
- Đảm bảo các nút bấm và icon có diện tích bấm $\ge 48\text{dp} \times 48\text{dp}$.

### Bước 3: Đồng bộ Chuỗi Đa ngôn ngữ (i18n)
- Rà soát các chuỗi thông báo, đảm bảo có mặt trong cả 5 ngôn ngữ (`values`, `values-en`, `values-km`, `values-lo`, `values-zh-rCN`).

### Bước 4: Kiểm tra Biên dịch & Nghiệm thu (`/goal`)
- Chạy `.\gradlew compileDebugKotlin` để đảm bảo 0 lỗi biên dịch.
- Đóng dấu hoàn tất `<!-- GOAL_COMPLETE -->`.
