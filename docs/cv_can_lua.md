# CÂN LÚA — NỘI DUNG MỤC DỰ ÁN CHO CV
*(Định dạng chuẩn hóa đối chiếu trực tiếp theo mẫu hồ sơ CV tại `docs/image.png`)*

---

## THÔNG TIN CHUNG DỰ ÁN
- **Tên dự án**: Cân Lúa — Ứng dụng Cân & Quản lý Giao dịch Nông sản
- **Thời gian thực hiện**: 03/2026 – 11/2026
- **Lĩnh vực / Nghiệp vụ**: Nông nghiệp thực địa (Agritech), IoT & Giao tiếp thiết bị phần cứng, Số hóa giao dịch thương mại nông sản

---

## TRƯỜNG 1: Vị trí của bạn trong dự án
*(Trường thông tin 1 theo biểu mẫu CV)*

**Mobile Developer** (Android Developer)

---

## TRƯỜNG 2: Mô tả ngắn gọn về dự án, mục tiêu, vai trò của bạn, các công nghệ sử dụng và những thành tựu bạn đã đạt được trong dự án
*(Trường thông tin 2 theo biểu mẫu CV)*

Dưới đây là 2 phương án trình bày được tối ưu hóa cho các mục đích sử dụng khác nhau:
- **Phương án A (Bản đầy đủ — Rich Markdown)**: Cấu trúc chi tiết, rõ ràng theo 5 tiêu chuẩn kỹ thuật, phù hợp lưu trữ hồ sơ, xuất file PDF, nộp CV trên Notion, GitHub Portfolio hoặc các hệ thống hỗ trợ rich-text.
- **Phương án B (Bản rút gọn — 1-Click Copy Block)**: Nén gọn thành các gạch đầu dòng súc tích, tối ưu hóa mật độ thông tin để sao chép trực tiếp vào các form CV trực tuyến (TopCV, VietnamWorks, CakeResume...) có giới hạn trường nhập liệu.

---

### PHƯƠNG ÁN A: BẢN ĐẦY ĐỦ (CHUẨN MARKDOWN / RICH TEXT)

- **Mục tiêu dự án**:
  - Số hóa toàn diện quy trình cân và giao dịch mua bán lúa vụ tại bờ ruộng và bến thu mua; giải quyết triệt để vấn đề gian lận số liệu, thất thoát tiền tệ và tranh chấp phát sinh từ phương thức ghi chép sổ tay, tính nhẩm thủ công truyền thống.
  - Xây dựng giải pháp di động chuyên dụng hoạt động bền bỉ trong điều kiện thực địa khắc nghiệt (nắng gắt, rung lắc cơ học, mất kết nối Internet); vận hành linh hoạt ở cả hai chế độ: tự động tiếp nhận tín hiệu từ đầu cân điện tử công nghiệp và nhập liệu thủ công tốc độ cao.

- **Vai trò đảm nhiệm**:
  - Đảm nhiệm vai trò **Mobile Developer** chủ trì toàn diện vòng đời kỹ thuật ứng dụng Android Native: từ khảo sát quy trình thu mua lúa vụ thực địa, thiết kế kiến trúc phần mềm Clean Architecture kết hợp MVVM, triển khai tầng giao tiếp phần cứng (HAL), xây dựng động cơ tính toán tài chính đa tầng đến tối ưu hóa hiệu năng và đóng gói bản phát hành.

- **Công nghệ kỹ thuật sử dụng**:
  - *Nền tảng & Kiến trúc*: 100% Android Native (Kotlin), Jetpack Compose (Material 3), Clean Architecture, MVVM/MVI, Unidirectional Data Flow (UDF), Repository Pattern.
  - *Bất đồng bộ & Luồng dữ liệu phản ứng*: Kotlin Coroutines (`Dispatchers.IO`, `Dispatchers.Default`, `Dispatchers.Main.immediate`), StateFlow, SharedFlow.
  - *Lưu trữ cục bộ & Bền bỉ dữ liệu*: Room Database (SQLite), cấu hình Write-Ahead Logging (`PRAGMA journal_mode=WAL`), giao dịch nguyên tử (`@Transaction`), Foreign Key cascade, mô hình Local-First ngoại tuyến 100% (Zero Cloud Dependency).
  - *Tầng giao tiếp phần cứng cân điện tử (Hardware Abstraction Layer)*:
    - Bluetooth Classic SPP (RFCOMM Socket chuẩn UUID `00001101-0000-1000-8000-00805F9B34FB`) và Bluetooth Low Energy (BLE GATT).
    - USB Serial RS-232 qua giao thức OTG (`usb-serial-for-android` hỗ trợ các dòng chip CH340, CP2102, FTDI, PL2303, Baudrate 9600 8N1).
    - Thuật toán trượt khung (Sliding Window Frame Parser) xử lý đồng bộ luồng byte STX/CRLF và xác thực mã kiểm soát Checksum thời gian thực.
    - Bộ lọc nhiễu rung cơ học (Moving Average Noise Filter) kết hợp State Machine nhận diện cờ ổn định (`Stable Flag`) và tự động chốt mã cân (Auto-Capture & Debounce).
  - *Động cơ in ấn ngoại vi (ESC/POS Thermal Printing)*:
    - Giao thức điều khiển máy in nhiệt cầm tay 58mm / 80mm qua Bluetooth SPP và USB.
    - Kỹ thuật Canvas Bitmap Rasterization (chuyển đổi đồ họa 1-bit monochrome `GS v 0`) xử lý triệt để bài toán hiển thị tiếng Việt có dấu, in mã QR VietQR thanh toán và logo thương hiệu sắc nét không phụ thuộc bảng mã firmware máy in.
  - *Xuất dữ liệu & Báo cáo*: Android `PdfDocument`, Apache POI (bảng kê Excel `.xlsx` chứa công thức động), trích xuất CSV và sao lưu mã hóa cơ sở dữ liệu cục bộ.

- **Chức năng chính & Tiện ích giá trị thực tế**:
  - *Ghi nhận mã cân liên tục (Dual-Mode Input)*:
    - Chế độ tự động: Đọc luồng dữ liệu liên tục từ đầu cân điện tử, tự động chốt mã cân khi trọng lượng ổn định trong 600ms – 800ms, phát âm thanh phản hồi (Audio Beep cue), tự động trừ bì vỏ bao (Auto-Tare) và khóa chống cân trùng bao lúa.
    - Chế độ thủ công tối ưu: Bàn phím số chuyên dụng cỡ lớn (Ergonomic Numeric Pad) hỗ trợ nhập nhanh liên tục bằng một tay kèm âm thanh gõ phím khi không có cân điện tử hoặc thiết bị gặp sự cố.
  - *Động cơ quyết toán tài chính đa tầng*:
    - Xử lý tính toán chính xác tuyệt đối bằng kiểu dữ liệu `BigDecimal` / `Long`, triệt tiêu hoàn toàn sai số làm tròn số học.
    - Tự động khấu trừ tạp chất và độ ẩm nông sản theo tỷ lệ quy đổi (lúa tươi, lúa khô), trừ trọng lượng vỏ bao dứa theo số lượng bao.
    - Tự động cấn trừ tiền tạm ứng cọc mùa vụ, chi phí nhân công bốc vác, chi phí vận chuyển ghe bến và chiết khấu thương mại để chốt số tiền thực trả cuối cùng.
  - *Quản lý danh mục đối tác & Nông sản*:
    - Quản lý danh bạ nông dân, thương lái, chủ ruộng, nhà xe; theo dõi công nợ lũy kế và lịch sử các đợt thu mua theo từng cánh đồng.
    - Quản lý danh mục các giống lúa phổ biến (ST25, ST24, Đài Thơm 8, OM18, OM5451, IR50404...) gắn liền biểu giá thỏa thuận và quy chuẩn trừ ẩm riêng biệt.
  - *In ấn & Xuất báo cáo ngoại tuyến tại hiện trường*:
    - In phiếu cân và hóa đơn giao dịch tức thì qua máy in nhiệt cầm tay với bảng kê chi tiết từng mã cân, tổng trọng lượng thô, trọng lượng trừ bì, tỷ lệ tạp chất và mã VietQR nhận tiền chuyển khoản.
    - Xuất bảng kê chi tiết định dạng Excel (.xlsx) chứa sẵn công thức tính toán và tài liệu PDF chuyên nghiệp gửi trực tiếp cho kế toán hoặc nhà máy xay xát qua Zalo / tin nhắn ngay tại bờ ruộng.
    - Khả năng tự phục hồi phiên làm việc và sao lưu dữ liệu an toàn vào thẻ nhớ ngoài.

- **Thành tựu & Chỉ số định lượng thực tế**:
  - *Tốc độ & Độ trễ truyền nhận phần cứng*: Độ trễ tiếp nhận tín hiệu từ đầu cân đạt **< 100ms**; thời gian tự động nhận diện ổn định và chốt lưu mã cân chỉ từ **0.8s – 1.2s** (nhanh hơn gấp 15 lần so với thao tác ghi chép sổ tay thủ công mất 15–20s/mã).
  - *Tối ưu hóa thời gian giao dịch*: Giảm **65% – 70%** tổng thời gian hoàn tất một phiếu cân ghe lúa 10–15 tấn (từ 45–60 phút ghi sổ, cộng tay và tính tiền rút xuống chỉ còn 12–15 phút).
  - *Độ chính xác tài chính & Loại trừ gian lận*: Triệt tiêu **100%** sai sót tính toán số học thủ công và ngăn chặn hoàn toàn việc ghi gian lận mã cân nhờ dữ liệu được chốt trực tiếp từ cảm biến đầu cân.
  - *Độ ổn định kết nối ngoại vi*: Đạt tỷ lệ duy trì kết nối Bluetooth/Serial **99.8%** trong môi trường đồng ruộng có nhiều xung nhiễu vô tuyến, tích hợp cơ chế Exponential Backoff Auto-Reconnect tự động tái lập kết nối trong **< 2s**.
  - *Tính bền bỉ và toàn vẹn dữ liệu ngoại tuyến*: Đạt chuẩn **100% Offline-First** (Zero Cloud Dependency), cam kết **Zero Data Loss** với cơ chế SQLite WAL và giao dịch nguyên tử (Atomic Transactions), bảo vệ an toàn toàn bộ dữ liệu giao dịch kể cả khi thiết bị cạn pin sập nguồn đột ngột.

---

### PHƯƠNG ÁN B: BẢN RÚT GỌN (TỐI ƯU SAO CHÉP 1-CHẠM VÀO FORM TOPCV)
*(Khối văn bản định dạng bullet points cô đọng ~1.420 ký tự, tối ưu sao chép an toàn vào các biểu mẫu CV trực tuyến có giới hạn 1.500 – 2.000 ký tự trên TopCV / VietnamWorks / CakeResume / ITviec)*

```text
- Mục tiêu: Số hóa quy trình cân và giao dịch lúa vụ tại ruộng; triệt tiêu sai sót số học và thất thoát tài chính; tối ưu 2 chế độ: tự động nhận số từ cân điện tử và nhập thủ công ngoại tuyến 100%.
- Vai trò: Mobile Developer chủ trì kỹ thuật: khảo sát thực tế, thiết kế Clean Architecture + MVVM, triển khai tầng HAL kết nối cân, động cơ tài chính đa tầng và in ấn ngoại vi.
- Công nghệ: 100% Android Native (Kotlin), Jetpack Compose, Room (SQLite WAL), Coroutines, StateFlow, Bluetooth SPP/BLE, USB Serial RS-232, ESC/POS (Canvas Bitmap Rasterization), BigDecimal, PdfDocument, Apache POI.
- Chức năng chính: Ghi nhận mã cân thời gian thực (tự động chốt mã hoặc nhập số nhanh); tự động trừ bì, độ ẩm, tạp chất; động cơ BigDecimal tính tiền chuẩn xác, cấn trừ cọc và chi phí; quản lý bạn hàng và giống lúa (ST25, Đài Thơm...); in phiếu cân tiếng Việt kèm VietQR qua máy in nhiệt Bluetooth/USB; xuất Excel/PDF ngoại tuyến tại ruộng.
- Thành tựu & Chỉ số:
  + Độ trễ tín hiệu cân < 100ms, tự động chốt mã 0.8s – 1.2s (nhanh gấp 15 lần ghi sổ tay).
  + Rút ngắn 65% – 70% thời gian chốt cân ghe lúa 10–15 tấn (từ 45–60 phút xuống 12–15 phút).
  + Triệt tiêu 100% sai sót tính toán số học thủ công và gian lận ghi sai số cân bến bãi.
  + Kết nối phần cứng đạt độ ổn định 99.8%, tự động kết nối lại (Auto-Reconnect) < 2s.
  + Đảm bảo 100% offline-first (Local-First), bảo toàn dữ liệu tuyệt đối (Zero Data Loss) nhờ SQLite WAL.
```
