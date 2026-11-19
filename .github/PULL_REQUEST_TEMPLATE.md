## Mô tả thay đổi
<!-- Tóm tắt ngắn gọn mục đích, bối cảnh và những gì PR này thay đổi -->


## Loại thay đổi
- [ ] Sửa lỗi (Bug fix)
- [ ] Tính năng mới (Feature)
- [ ] Tái cấu trúc (Refactor)
- [ ] CI/CD / Cấu hình / Tài liệu

## Checklist kiểm chứng
- [ ] Đã chạy `./gradlew testDebugUnitTest` và pass 100% (không sửa assertion/expected value để che bug)
- [ ] Đã chạy `./gradlew detekt lintDebug` và không phát sinh cảnh báo/lỗi mới
- [ ] Nếu sửa UI: đã kiểm tra Dark Mode, FontScale lớn, vùng chạm `>= 48.dp` và không hardcode chuỗi hiển thị tiếng Việt
- [ ] Nếu thay đổi Room DB (`@Entity` / `@Database` / version): đã viết `Migration` an toàn, export schema vào `app/schemas/` và có migration test
