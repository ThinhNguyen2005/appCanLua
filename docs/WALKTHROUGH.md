# Walkthrough — Luồng Kiểm Thử Cân Lúa Mobile (Offline-First & Supabase)

Tài liệu này hướng dẫn kiểm thử thủ công các luồng nghiệp vụ cốt lõi sau khi biên dịch ứng dụng. Kịch bản bám sát thiết kế kiến trúc mới (Room DB local-first, không bắt buộc login, kết nối QR handshake ngoại tuyến, đồng bộ ngầm qua Supabase, và Cẩm nang Nông nghiệp offline).

---

## 1. Chuẩn Bị Môi Trường

1.  Cấu hình `local.properties` tại thư mục gốc:

    ```properties
    sdk.dir=C:\\Path\\To\\Android\\Sdk
    OPENWEATHER_API_KEY=your_openweather_key
    MAPS_API_KEY=your_google_maps_key
    SUPABASE_URL=https://your-supabase-project.supabase.co
    SUPABASE_ANON_KEY=your_supabase_anon_key
    ```

2.  Đặt cấu hình Supabase Client trong app phù hợp với môi trường kiểm thử.
3.  Build nhanh thông qua PowerShell:

    ```powershell
    .\gradlew.bat :app:compileDebugKotlin
    .\gradlew.bat :app:assembleDebug
    ```

---

## 2. Hồ Sơ Cục Bộ & Đăng Nhập Mở Rộng (Local Profile & Optional Auth)

### Mục tiêu
Đảm bảo luồng khởi tạo không đăng nhập (Guest Mode) hoạt động mượt mà, và chức năng đăng nhập/sao lưu chỉ đóng vai trò tùy chọn chạy ngầm.

### Các bước kiểm thử
- [ ] **Khởi tạo Guest Mode:** Xóa dữ liệu app và mở app khi không có mạng -> Không hiện màn đăng nhập bắt buộc. Hiện màn hình "Tạo hồ sơ nhanh".
- [ ] **Lưu hồ sơ nội bộ:** Nhập Tên và Vai trò (Farmer/Trader) -> Vào thẳng màn hình chính. Xác nhận dữ liệu được lưu vào Room DB.
- [ ] **Chuyển đổi vai trò:** Vào Cài đặt/Hồ sơ -> Đổi vai trò Farmer/Trader -> Giao diện các tab dưới cập nhật tương ứng.
- [ ] **Đăng nhập tùy chọn (Backup):** Bật mạng -> Vào Settings -> Chọn "Đăng nhập để sao lưu" -> Thực hiện đăng nhập -> Hệ thống tự động đẩy dữ liệu từ Room DB lên Supabase ngầm.
- [ ] **Đăng xuất an toàn:** Nhấn đăng xuất -> App hiện thông báo xác nhận (Sign-out confirm dialog) để tránh vô tình nhấn nhầm -> Đăng xuất quay về trạng thái tài khoản Local Guest, không xóa dữ liệu Room local.

---

## 3. Luồng Cân Lúa Local-First & Nhập Bao Cân Cải Tiến

### Mục tiêu
Tạo phiếu cân, nhập bao cân ngoại tuyến và kiểm thử tính toán thời gian thực theo cấu trúc lưới mới.

### Các bước kiểm thử
- [ ] **Tạo phiếu mới:** Tạo phiếu cân mới khi không có mạng (nhập giống lúa, vụ mùa, đơn giá, khối lượng bì mặc định, trừ tạp chất).
- [ ] **Nhập cân dạng hàng (Row-Major):** Vào màn hình nhập cân. Nhập các ô theo hàng (từ trái qua phải, trên xuống dưới).
- [ ] **Tập trung tự động (Auto-focus):** Gõ 3 ký tự (ví dụ: `505` -> tự động biến đổi thành `50.5` kg) -> Con trỏ tự nhảy sang ô kế tiếp.
- [ ] **Khấu trừ nhất quán:** Nhập độ ẩm (ví dụ: `18.0%`) -> Kiểm tra khối lượng thực (`net_weight`) trừ bì và độ ẩm và số tiền tạm tính hiển thị khớp nhau tại cả màn hình nhập và màn chi tiết.
- [ ] **Kháng Font Scale:** Vào cài đặt hệ thống của Android -> Tăng kích thước phông chữ lên `1.2x` -> Quay lại màn hình lưới 5x5 nhập cân -> Bố cục không bị vỡ hoặc tràn chữ.
- [ ] **Đồng bộ Supabase:** Bật mạng lại -> Kiểm tra xem `WorkManager` có tự động giải phóng hàng đợi và cập nhật trạng thái phiếu cân thành `SYNCED` trên Supabase Database hay không.

---

## 4. Kiểm Thử QR Handshake Ngoại Tuyến

### Mục tiêu
Xác thực phiếu cân và bàn giao dữ liệu an toàn giữa Nông dân và Thương lái khi cả hai thiết bị đều ngoại tuyến.

### Các bước kiểm thử
- [ ] **Tạo mã QR (Farmer):** Trên máy Farmer (đang đóng vai trò nông dân), mở phiếu cân và chọn "Tạo mã xác thực QR" -> Mã QR hiển thị rõ ràng, không bị mờ nhòe.
- [ ] **Quét mã QR (Trader):** Trên máy Trader (đang đóng vai trò thương lái), mở chức năng "Quét mã giao dịch" -> CameraX kích hoạt và tự động nhận diện mã QR của Farmer.
- [ ] **Ký số & Khóa phiếu (LOCKED):** Sau khi quét thành công -> Phiếu cân trên cả hai máy tự động chuyển sang trạng thái **LOCKED** (Khóa).
- [ ] **Bảo vệ dữ liệu khóa:** Thử nhấn nút Sửa hoặc nhập bao cân trên phiếu đã khóa -> App báo lỗi hoặc khóa nút, không cho phép thay đổi dữ liệu đã chốt.

---

## 5. Thị Trường & Cẩm Nang Nông Nghiệp Offline

### Mục tiêu
Kiểm tra bảng giá lúa thời gian thực từ Supabase và chức năng Cẩm nang tĩnh khi không có mạng.

### Các bước kiểm thử
- [ ] **Bảng giá thu mua:** Mở tab Thị trường -> Bảng giá lúa đồng bộ từ Supabase hiển thị đúng xu hướng tăng/giảm/ổn định.
- [ ] **Biểu đồ xu hướng:** Bấm vào một dòng giá lúa -> Chart xu hướng (Vico LineChart) hiển thị đúng biên độ dao động, có hiển thị trục tọa độ và mức giá Min/Max/TB rõ ràng.
- [ ] **Cẩm nang tĩnh ngoại tuyến:** Tắt kết nối mạng -> Vào tab "Tri thức/Cẩm nang" -> Danh sách bài viết kỹ thuật nông học (phòng sâu bệnh, khuyến khuyến nghị mùa vụ) đọc từ file JSON tĩnh vẫn hiển thị và xem được chi tiết bình thường.
- [ ] **Degrade tin tức:** Khi tắt mạng, mục tin tức (RSS cache) vẫn hiển thị các bài viết đã tải trước đó cùng với cảnh báo "Đang sử dụng dữ liệu cũ".

---

## 6. Bản Đồ Trader (Trader Map)

### Mục tiêu
Xác nhận bản đồ Google Maps hiển thị các điểm thu mua và cụm điểm cân lúa (clustering).

### Các bước kiểm thử
- [ ] **Cấp quyền vị trí:** Cấp quyền GPS khi ứng dụng yêu cầu.
- [ ] **Hiển thị Marker & Cluster:** Mở tab Bản đồ (với vai trò Trader) -> Thấy các điểm thu mua hiển thị dưới dạng icon lúa. Các điểm gần nhau tự động gom thành vòng tròn số (Clustering).
- [ ] **Bottom Sheet Chi tiết:** Chạm vào một marker điểm cân lúa -> Bottom sheet trượt lên hiển thị thông tin: Tên đối tác, giống lúa, khối lượng, và số điện thoại (bấm vào biểu tượng gọi điện để thực hiện cuộc gọi nhanh).

---

## 7. Đa Ngôn Ngữ & Xuất Bản PDF

### Mục tiêu
Xác minh giao diện dịch thuật không bị lỗi placeholder và chia sẻ PDF an toàn.

### Các bước kiểm thử
- [ ] **Kiểm thử Locale vùng biên:** Chuyển đổi ngôn ngữ thiết bị lần lượt sang: Khmer (km), Lào (lo), Trung giản thể (zh-CN) -> Kiểm tra các nhãn nút và thông số hiển thị không bị dịch thiếu hoặc chèn ký tự lạ.
- [ ] **Xuất PDF phiếu cân:** Chọn một phiếu cân đã hoàn thành -> Nhấn "Xuất PDF" -> Ứng dụng tạo tệp PDF lưu trữ nội bộ.
- [ ] **Chia sẻ PDF:** Nhấn nút "Chia sẻ" -> App mở danh sách ứng dụng chia sẻ (Zalo, Messenger, Gmail) thông qua `FileProvider` an toàn, không làm lộ đường dẫn lưu trữ riêng tư của thiết bị.

---

## 8. Danh Sách Kiểm Tra Nhanh Trước Khi Phát Hành (Smoke Test)

- [ ] Thực hiện chạy lệnh `:app:compileDebugKotlin` thành công không có lỗi biên dịch.
- [ ] Thực hiện chạy lệnh `:app:assembleDebug` thành công tạo ra tệp APK.
- [ ] Khởi động app ngoại tuyến -> Vào thẳng ứng dụng (Guest Mode thành công).
- [ ] Nhập cân bao lúa -> Tự động tính đúng khối lượng thực và tiền tạm tính.
- [ ] Hiển thị cẩm nang tĩnh nông học khi không có kết nối mạng.
- [ ] Kiểm tra không commit tài khoản Supabase Admin Key hoặc Maps API Key thật lên git repository.
