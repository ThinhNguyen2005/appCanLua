# Kế hoạch Hoàn thiện UI/UX & Hiệu Ứng Nâng Cao — Trang Tin Tức (MarketScreen News Tab & NewsSection)

> **Mục tiêu:** Nâng cấp toàn diện màn hình Tin tức nông nghiệp (`MarketScreen.kt` & `NewsSection.kt`) theo chuẩn UI/UX Playbook, tối ưu công thái học nông dân/thương lái ($\ge 48\text{dp}$ touch target), khắc phục triệt để các chuỗi hardcode tiếng Việt sang 5 locales, hoàn thiện Empty State với nút CTA thử lại, và bổ sung hiệu ứng chuyển động có mục đích (Purposeful Motion) theo chuẩn `@hieu-ung-nangcao`.
> **Thời gian:** 2026-10-13
> **Trạng thái:** Completed (Đã biên dịch thành công `BUILD SUCCESSFUL`)

---

## 1. Bối cảnh & Phân tích Hiện trạng (Current State Audit)

### 1.1 Vấn đề Phát hiện (Checklist Audit)
- ❌ **Hardcoded Strings & Đa ngôn ngữ:**
  - Tab "Tin tức" và "Bảng giá lúa, gạo" trong `MarketTabRow` bị hardcode tiếng Việt.
  - Các nhãn bộ lọc chủ đề `TopicFilterRow` (Tất cả, Lúa, Gạo, Thị trường) bị hardcode.
  - `EmptyState`: "Chưa có tin tức", "Kéo xuống để cập nhật, hoặc kiểm tra mạng" bị hardcode.
  - `ErrorBanner`: "Không cập nhật được — đang dùng dữ liệu cũ", nút đóng "Đóng" bị hardcode.
  - Nhãn "Thu gọn", "Xem thêm bài viết (X bài khác)" bị hardcode.
  - Hàm tính thời gian tương đối `formatRelativeTime`: "vừa xong", "X phút trước", "X giờ trước", v.v... bị hardcode tiếng Việt.
  - Nút FAB "Đăng giá mới" bị hardcode tiếng Việt.
- 🟡 **Công thái học & Touch Target ($\ge 48\text{dp}$):**
  - Nút đóng trong `ErrorBanner` có kích thước `size(28.dp)` $\rightarrow$ vi phạm quy chuẩn touch target $\ge 48\text{dp}$, khó chạm chính xác ngoài đồng ruộng.
  - Filter chips trong `TopicFilterRow` cần đảm bảo chiều cao tối thiểu $\ge 40-48\text{dp}$.
- 🟡 **Chuẩn 4 UI States:**
  - `EmptyState` hiện tại thiếu nút CTA (Call to Action) "Làm mới tin tức", buộc người dùng phải đoán thao tác kéo vuốt.
- 🟡 **Hiệu ứng chuyển động (Motion Spec `@hieu-ung-nangcao`):**
  - Chuyển màu chip bộ lọc cần dùng `animateColorAsState` mượt mà.
  - Banner cảnh báo lỗi ngoại tuyến cần animation trượt lò xo (`spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)`).
  - Phản hồi rung haptic `HapticUtil.tick(context)` khi nhấn đọc bài viết hoặc làm mới tin tức.

---

## 2. Motion Spec (`@hieu-ung-nangcao`)

### Motion Spec 1: Topic Filter Chip Transition
```text
Intent: Phản hồi thị giác rõ ràng, dịu mắt khi chuyển đổi danh mục tin tức (Tất cả / Lúa / Gạo / Thị trường)
Trigger: Chạm vào chip bộ lọc
Start state: Chip inactive (màu nền SurfaceContainer, text TextSecondary)
End state: Chip active (màu nền GreenPrimary, text Color.White)
User-controlled or automatic: User-controlled
Interruptible: Có
Physics/easing: tween(220, FastOutSlowInEasing) qua animateColorAsState
Feedback: HapticFeedbackType.TextHandleMove
Performance risk: 0 (animateColorAsState cục bộ)
Fallback: Chuyển màu tức thì
```

### Motion Spec 2: Error & Offline Banner Slide
```text
Intent: Báo hiệu tin tức đang dùng cache ngoại tuyến mà không làm giật layout
Trigger: errorMessage != null
Physics: spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow) cho enter; spring(dampingRatio = NoBouncy, stiffness = Medium) cho exit
```

### Motion Spec 3: News Article Tap Micro-interaction
```text
Intent: Phản hồi xúc giác tinh tế khi bấm mở liên kết báo chí qua Custom Tabs
Trigger: Chạm vào NewsCard
Feedback: HapticUtil.tick(context) + ripple bounded chuẩn Material 3
```

---

## 3. Các Bước Thực Thi (Action Breakdown)

### Bước 1: Đồng bộ Chuỗi Đa ngôn ngữ (5 Locales)
- Bổ sung 20 chuỗi mới về Tin tức, Danh mục, Thời gian tương đối, Empty state vào 5 file:
  - `values/strings.xml` (Tiếng Việt)
  - `values-en/strings.xml` (Tiếng Anh)
  - `values-km/strings.xml` (Tiếng Khmer)
  - `values-lo/strings.xml` (Tiếng Lào)
  - `values-zh-rCN/strings.xml` (Tiếng Trung Giản thể)

### Bước 2: Nâng cấp `NewsSection.kt`
- Dùng `stringResource` cho toàn bộ nhãn, bộ lọc, thông báo lỗi và thời gian tương đối.
- Tinh chỉnh `ErrorBanner`: touch target $\ge 48\text{dp}$ cho nút Close, spring animation.
- Tinh chỉnh `TopicFilterRow`: `animateColorAsState`, touch target $\ge 40\text{dp}$.
- Bổ sung nút CTA "Làm mới tin tức" vào `EmptyState` ($\ge 48\text{dp}$).
- Thêm `HapticUtil.tick(context)` khi mở bài viết.

### Bước 3: Nâng cấp `MarketScreen.kt`
- Sử dụng `stringResource` cho các tab `MarketTabRow` và nút FAB "Đăng giá mới".
- Nâng cấp spring animation cho FAB "Đăng giá mới".

### Bước 4: Kiểm tra Biên dịch & Nghiệm thu
- Biên dịch bằng `.\gradlew.bat compileDebugKotlin`.
- Đóng dấu hoàn tất `<!-- GOAL_COMPLETE -->`.
