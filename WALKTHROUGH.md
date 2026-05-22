# Walkthrough — Luồng chính của Cân Lúa Mobile

Tài liệu này dùng để kiểm thử thủ công các luồng quan trọng sau khi build app. Nội dung bám theo trạng thái hiện tại của dự án, không còn là log của một phiên sửa lỗi riêng lẻ.

---

## 1. Chuẩn bị môi trường

1. Cấu hình `local.properties`:

   ```properties
   sdk.dir=C:\\Path\\To\\Android\\Sdk
   OPENWEATHER_API_KEY=your_openweather_key
   OPENROUTER_API_KEY=your_openrouter_key
   MAPS_API_KEY=your_google_maps_key
   ```

2. Đặt `google-services.json` vào thư mục `app/` nếu test Firebase thật.
3. Build nhanh:

   ```powershell
   .\gradlew.bat :app:compileDebugKotlin
   .\gradlew.bat :app:assembleDebug
   ```

---

## 2. Auth, profile và vai trò

### Mục tiêu

Đảm bảo Firebase Auth là nguồn trạng thái đăng nhập chính, profile được tạo/cập nhật đúng và UI đổi theo vai trò.

### Các bước test

- [ ] Mở app khi chưa đăng nhập → thấy màn login.
- [ ] Đăng nhập bằng Firebase/Google theo cấu hình hiện có.
- [ ] Nếu là user mới → đi qua setup hồ sơ và chọn vai trò.
- [ ] Đăng xuất → app quay về login, không cần kill app.
- [ ] Vào profile → chỉnh thông tin cá nhân nếu màn hình hỗ trợ.
- [ ] Đổi vai trò farmer/trader → bottom navigation và route cập nhật đúng.
- [ ] Kiểm tra Premium card/badge hiển thị bằng resource, không hardcode sai ngôn ngữ.

---

## 3. Luồng cân lúa offline-first

### Mục tiêu

Tạo phiếu cân, nhập nhiều bao và xác nhận dữ liệu vẫn hoạt động khi offline/online.

### Các bước test

- [ ] Tạo phiếu cân mới với thông tin ruộng, giống lúa, thương lái/nông dân và giá.
- [ ] Nhập nhiều bao cân liên tiếp.
- [ ] Kiểm tra tổng bao, tổng khối lượng, khối lượng thực, tiền tạm tính.
- [ ] Thử tăng font scale trong Settings → layout nhập cân không bị vỡ.
- [ ] Tắt mạng, tạo hoặc sửa phiếu → dữ liệu vẫn lưu local.
- [ ] Bật mạng lại → WorkManager/Firestore sync chạy và dữ liệu xuất hiện ở cloud.
- [ ] Xoá phiếu → kiểm tra luồng khôi phục hoặc xoá vĩnh viễn nếu có.

---

## 4. QR handshake farmer/trader

### Mục tiêu

Đảm bảo phiếu cân có thể được xác thực/gắn giao dịch giữa nông dân và thương lái.

### Các bước test

- [ ] Farmer tạo phiếu và mở màn QR generate.
- [ ] Trader mở màn quét QR bằng CameraX/ML Kit.
- [ ] Quét QR hợp lệ → phiếu được lock/gắn với trader đúng.
- [ ] Quét QR lỗi hoặc không hợp lệ → app báo lỗi rõ, không crash.
- [ ] Kiểm tra trạng thái giao dịch trên danh sách và chi tiết phiếu.

---

## 5. Thị trường, tin tức và thời tiết

### Mục tiêu

Xác nhận tab thị trường hoạt động với dữ liệu realtime, cache và UI cập nhật.

### Các bước test

- [ ] Mở tab Thị Trường → thấy giá lúa, biểu đồ và filter giống lúa.
- [ ] Pull-to-refresh → dữ liệu được refresh, không làm mất trạng thái UI bất thường.
- [ ] Nếu có quyền vị trí và API key → weather hiển thị theo GPS.
- [ ] Tắt mạng → weather/news dùng cache hoặc hiển thị trạng thái thiếu dữ liệu hợp lý.
- [ ] Mở một bài news → Chrome Custom Tabs mở bài, quay lại app ổn định.
- [ ] Với trader, kiểm tra luồng đăng/cập nhật giá nếu màn hình được bật.

---

## 6. AI Chat và AI insights

### Mục tiêu

Đảm bảo AI trả lời được render đúng, đặc biệt là Markdown table.

### Prompt test đề xuất

```text
Lập bảng so sánh OM5451, Đài Thơm 8 và ST25 theo năng suất, giá bán, rủi ro sâu bệnh và khuyến nghị mùa vụ.
```

### Các bước test

- [ ] Gửi prompt AI bình thường → nhận được câu trả lời.
- [ ] Câu trả lời có heading/list/link hiển thị dễ đọc.
- [ ] Câu trả lời có bảng Markdown hiển thị được bằng Markwon, không dính raw pipe khó đọc.
- [ ] Bấm link trong câu trả lời nếu có → link hoạt động trong phạm vi TextView.
- [ ] Dùng voice input bằng `SpeechRecognizer` → text được đưa vào ô chat.
- [ ] Mở farmer dashboard/AI insights → bảng hoặc list từ AI cũng render được.
- [ ] Thiếu `OPENROUTER_API_KEY` → app báo trạng thái thiếu cấu hình, không crash.

---

## 7. Bản đồ trader

### Mục tiêu

Xác nhận Google Maps Compose, clustering và bottom sheet marker hoạt động.

### Các bước test

- [ ] Cấp quyền vị trí.
- [ ] Tạo vài phiếu có toạ độ hoặc để app tự lấy GPS khi tạo phiếu.
- [ ] Mở bản đồ trader → marker/cluster xuất hiện đúng.
- [ ] Tap cluster → zoom hoặc mở item theo số lượng điểm.
- [ ] Tap marker → bottom sheet hiển thị thông tin phiếu, khối lượng, bao, giống lúa và CTA phù hợp.
- [ ] Tắt `MAPS_API_KEY` hoặc dùng key sai → app degrade rõ ràng, không crash.

---

## 8. Đa ngôn ngữ

### Mục tiêu

Đảm bảo resource các locale không lỗi placeholder và không còn chuỗi lẫn ngôn ngữ ở màn chính.

### Các bước test

- [ ] Chuyển lần lượt Việt, Anh, Khmer, Lào, Trung.
- [ ] Mở các màn: login, danh sách phiếu, chi tiết phiếu, nhập cân, thị trường, AI, settings, profile.
- [ ] Kiểm tra `%1$s`, `%2$d`, số kg/tạ/đồng hiển thị đúng.
- [ ] Tìm chuỗi còn tiếng Việt trong Khmer/Lào/Trung và ghi lại key cần sửa.
- [ ] Kiểm tra Premium badge, font scale labels và các nút Save/Edit/Role không bị hardcode sai locale.

---

## 9. PDF export và chia sẻ file

### Mục tiêu

Đảm bảo phiếu cân xuất PDF và chia sẻ an toàn qua FileProvider.

### Các bước test

- [ ] Mở phiếu đã có dữ liệu đầy đủ.
- [ ] Xuất PDF.
- [ ] Mở/chia sẻ file qua intent.
- [ ] Kiểm tra app nhận file thấy tên file, nội dung và quyền đọc tạm thời đúng.
- [ ] Đảm bảo không cần lộ đường dẫn private app storage trong UI.

---

## 10. Checklist smoke test trước khi giao

- [ ] `:app:compileDebugKotlin` pass.
- [ ] `:app:assembleDebug` pass nếu có đổi dependency/resource lớn.
- [ ] Login/logout pass.
- [ ] Tạo phiếu + nhập cân pass.
- [ ] QR scan/generate pass.
- [ ] AI Markdown table pass.
- [ ] Chuyển ít nhất 2 locale pass.
- [ ] Map marker/bottom sheet pass nếu có `MAPS_API_KEY`.
- [ ] Không commit API key thật hoặc file cấu hình nhạy cảm.
