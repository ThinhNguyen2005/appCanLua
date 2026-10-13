---
name: Can Lua Mobile Design System
version: 1.0.0
description: Design tokens and ergonomics standards for Can Lua Mobile Android application (Rice weighing for Farmers and Traders).
colors:
  primary: "#2E7D32"
  primary-light: "#4CAF50"
  primary-surface: "#E8F5E9"
  primary-dark: "#1B5E20"
  accent-gold: "#F9A825"
  accent-gold-light: "#FFF8E1"
  background: "#F8FAF8"
  surface: "#FFFFFF"
  card-bg: "#FFFFFF"
  text-primary: "#1A1A1A"
  text-secondary: "#424242"
  text-hint: "#757575"
  divider: "#D0D0D0"
  divider-strong: "#B8B8B8"
  success: "#43A047"
  warning: "#FF8F00"
  error: "#E53935"
  info: "#1976D2"
  locked-surface: "#FFF1F3"
  paid-surface: "#F1F8F2"
rounded:
  xs: 4px
  sm: 6px
  md: 12px
  lg: 16px
  xl: 20px
  full: 9999px
spacing:
  xxs: 2px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
components:
  grid-cell:
    min-height: 60px
    rounded: "{rounded.sm}"
    border-width: 1.5px
  weight-card:
    backgroundColor: "{colors.card-bg}"
    rounded: "{rounded.lg}"
    elevation: 2px
  primary-button:
    backgroundColor: "{colors.primary}"
    textColor: "#FFFFFF"
    rounded: "{rounded.md}"
    min-height: 48px
---

# Cân Lúa Mobile Design System

## Overview
Cân Lúa Mobile là hệ thống giao diện Android Native phục vụ cho người nông dân và thương lái tại ruộng lúa Việt Nam. Thiết kế đặt trọng tâm vào **công thái học ngoài trời nắng gắt**, **thao tác một tay (Thumb Zone)**, **vùng chạm lớn ($\ge 48\text{dp}$)** và khả năng nhận diện dữ liệu nhanh chóng, chống lóa và chống nhầm lẫn.

## Colors
- **Primary (Xanh lá lúa - `#2E7D32`)**: Màu chủ đạo cho các hành động tương tác chính (Primary CTA, lưu cân, FAB).
- **Primary Surface (`#E8F5E9`)**: Nền cho các ô đã nhập cân, chip trạng thái active.
- **Accent Gold (`#F9A825`)**: Tượng trưng cho hạt lúa vàng, dùng cho số liệu tổng tiền và điểm nhấn quan trọng.
- **Surface & Background (`#F8FAF8`, `#FFFFFF`)**: Nền sáng dịu mắt, độ tương phản văn bản đạt chuẩn WCAG AA $\ge 4.5:1$.
- **Status Colors**: Xanh (`Success`), Cam (`Warning`), Đỏ (`Error`), Xanh dương (`Info`). Không bao giờ truyền tải trạng thái bằng màu sắc đơn độc mà luôn kèm nhãn và icon.

## Typography
- Hệ thống phông chữ chuẩn Material 3 (Roboto / System Default), tối đa 1 font family để tối ưu hiệu năng và độ quen thuộc.
- Hỗ trợ Dynamic Type / Accessibility Font Scale ($1.3\times - 2.0\times$): Không cố định `height` ở các phần tử chứa văn bản tiếng Việt để chống cụt dấu.

## Layout & Spacing
- Hệ thống lưới Spacing tuân thủ nghiêm ngặt thang đo bội số 4/8 (`AppDimensions`):
  - `SpacingXs`: 4dp
  - `SpacingSm`: 8dp
  - `SpacingMd`: 16dp
  - `SpacingLg`: 24dp
  - `SpacingXl`: 32dp
- Vùng chạm tối thiểu (Touch target) cho mọi icon và nút bấm $\ge 48\text{dp} \times 48\text{dp}$.

## Elevation & Depth
- Độ cao bóng đổ mềm mại, tự nhiên: Card chính `2.dp`, Floating Action Button `6.dp`.
- Tránh dùng đường viền dày đen; ưu tiên viền mỏng tinh tế (`1.dp` - `1.5.dp`) kết hợp nền phân lớp để tăng độ rõ nét ngoài trời nắng.

## Shapes
- Bo góc nhất quán theo phân cấp:
  - Ô lưới nhập liệu: `8.dp`
  - Thẻ thông tin Card: `16.dp`
  - Dialog & Hộp thoại: `20.dp`

## Components & 4 UI States
1. **Loading State**: Skeleton loading mượt mà hoặc CircularProgressIndicator có màu chủ đạo.
2. **Empty State**: Icon minh họa trực quan + Tiêu đề + Hướng dẫn gợi ý + Nút kêu gọi hành động (Primary CTA).
3. **Error State**: Thông báo lỗi rõ ràng bằng tiếng Việt tự nhiên + Nút Thử lại hoặc Quay lại, tuyệt đối không để người dùng kẹt ở màn hình vô tận.
4. **Offline State**: Chỉ báo trạng thái ngoại tuyến rõ ràng, dữ liệu hiển thị từ bộ nhớ đệm Room.

## Do's and Don'ts
- **DO**: Luôn đảm bảo vùng chạm $\ge 48\text{dp}$.
- **DO**: Đẩy các CTA chính (Cân lúa, Thêm bao, Tạo phiếu) vào tầm với ngón cái (Thumb Zone).
- **DON'T**: Không bao giờ để spinner tải vô tận khi dữ liệu `card == null`.
- **DON'T**: Không dùng tím làm màu chủ đạo nếu không có lý do nhận diện thương hiệu.
- **DON'T**: Không hardcode chuỗi ký tự hiển thị ra giao diện.
