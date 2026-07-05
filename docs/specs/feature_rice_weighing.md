# Đặc Tả Tính Năng Cân Lúa (Rice Weighing Specification)

Tài liệu này đặc tả chi tiết tính năng cân lúa cốt lõi (Scale Tab) dành cho nông dân và thương lái, bao gồm cấu trúc dữ liệu, các ràng buộc dữ liệu đầu vào và các kịch bản hành vi hệ thống (BDD Gherkin).

---

## 1. Mục Tiêu Tính Năng
*   Cho phép người dùng nhập và lưu trữ khối lượng của từng bao lúa thực tế tại ruộng một cách nhanh chóng, chính xác.
*   Tính toán thời gian thực khối lượng bao bì (theo phương thức tính chuẩn hoặc lấy mẫu), tạp chất, khấu trừ độ ẩm tiêu chuẩn (14%) để đưa ra khối lượng thực tế và tổng tiền thanh toán.
*   Hỗ trợ hoạt động 100% khi ngoại tuyến (Offline-First) và khóa phiếu cân sau khi kết nối QR thành công để bảo vệ tính toàn vẹn của dữ liệu tài chính.

---

## 2. Cấu Trúc Dữ Liệu Liên Quan (Data Schema & Validations)

Cấu trúc thực thể Room Database local và Supabase PostgreSQL được thiết kế đồng bộ với các điều kiện ràng buộc dữ liệu (Validation Rules) sau:

```yaml
feature_weighing_specification:
  version: "2026.06.30"
  entities:
    Card:
      description: "Phiếu cân lúa tổng hợp chứa thông tin lô hàng và thanh toán"
      fields:
        id:
          type: "Long / PRIMARY KEY"
          validation: "Tự động tăng (Room DB local) hoặc UUID (Supabase)"
        ownerUid:
          type: "String"
          validation: "Không rỗng (chỉ định định danh user chủ phiếu)"
        traderName:
          type: "String"
          validation: "Không rỗng, tối đa 100 ký tự"
        pricePerKg:
          type: "Double"
          validation: "Giá trị >= 0.0"
        bagWeight:
          type: "Double"
          validation: "Trọng lượng 1 vỏ bao bì (kg), giá trị >= 0.0"
        impurityWeight:
          type: "Double"
          validation: "Trọng lượng tạp chất trừ trực tiếp (kg), giá trị >= 0.0"
        moisturePercent:
          type: "Double"
          validation: "Phần trăm độ ẩm, giá trị từ 0.0 đến 100.0 (tiêu chuẩn quy đổi về 14.0%)"
        isLocked:
          type: "Boolean"
          validation: "Mặc định FALSE. Nếu TRUE, cấm mọi thao tác thêm/sửa/xóa"
        bagMethodIsSampling:
          type: "Boolean"
          validation: "Mặc định FALSE. Nếu TRUE, tính bao bì theo phương pháp lấy mẫu (Sampling)"
        bagSampleCount:
          type: "Int"
          validation: "Số lượng bao mẫu, giá trị >= 0"
        bagSampleTotalWeight:
          type: "Double"
          validation: "Tổng khối lượng của các bao mẫu (kg), giá trị >= 0.0"
        netWeight:
          type: "Double"
          validation: "Khối lượng thực sau khấu trừ, tính toán tự động"
        totalAmount:
          type: "Double"
          validation: "Tổng số tiền tạm tính, tính toán tự động"
        paidAmount:
          type: "Double"
          validation: "Số tiền đã trả, giá trị >= 0.0"
        remainingAmount:
          type: "Double"
          validation: "Số tiền nợ còn lại, tính toán tự động"

    WeightEntry:
      description: "Chi tiết cân nặng của từng bao lúa riêng lẻ"
      fields:
        id:
          type: "Long / PRIMARY KEY"
          validation: "Tự động tăng"
        cardId:
          type: "Long / FOREIGN KEY"
          validation: "Phải tham chiếu đến một Card ID hợp lệ"
        weight:
          type: "Double"
          validation: "Khối lượng thô của bao lúa. Yêu cầu: > 0.0 và < 200.0 (kg)"
        tableIndex:
          type: "Int"
          validation: "Chỉ số bảng lưới (Bảng 1, Bảng 2...), giá trị >= 0"
        cellIndex:
          type: "Int"
          validation: "Vị trí ô trong lưới 5x5, giá trị từ 0 đến 24"
```

---

## 3. Kịch Bản Hành Vi Hệ Thống (BDD Gherkin Scenarios)

### Kịch bản 1: Thêm bao lúa mới (Add New Bag)
```gherkin
Scenario: Người dùng thêm một bao lúa mới thành công trong màn hình nhập cân
  Given Phiếu cân lúa có ID 1 đang mở ở trạng thái chưa khóa (isLocked = FALSE)
  And Phiếu cân hiện tại đã có 11 bao lúa và tổng khối lượng thô là 525.5 kg
  And Con trỏ đang đứng ở ô nhập của "Bao #12"
  When Người dùng nhập khối lượng bao lúa là "50.5" kg vào bàn phím số
  And Người dùng nhấn nút "Lưu bao & cân tiếp"
  Then Hệ thống sẽ lưu một bản ghi WeightEntry mới vào Room DB với các thông tin:
    | cardId | weight | tableIndex | cellIndex |
    | 1      | 50.5   | 0          | 11        |
  And Cập nhật số bao lúa của phiếu cân lên "12" bao
  And Cập nhật tổng khối lượng thô của phiếu cân lên "576.0" kg (525.5 + 50.5)
  And Hệ thống phát ra phản hồi rung nhẹ (Haptic Tick)
  And Xóa trắng ô nhập cũ và tự động chuyển tiêu điểm (Focus) sang "Bao #13"

Scenario: Người dùng nhập khối lượng bao lúa ngoài phạm vi cho phép
  Given Phiếu cân lúa đang mở ở trạng thái chưa khóa (isLocked = FALSE)
  And Con trỏ đang đứng ở ô nhập của "Bao #12"
  When Người dùng nhập khối lượng bao lúa là "250.0" kg
  Then Hệ thống hiển thị cảnh báo lỗi (inline error) "Khối lượng bao lúa phải nhỏ hơn 200 kg"
  And Vô hiệu hóa nút "Lưu bao & cân tiếp"
  And Không ghi nhận bản ghi mới vào Room DB
```

### Kịch bản 2: Sửa/Xóa bao lúa (Edit/Delete Bag)
```gherkin
Scenario: Người dùng sửa khối lượng một bao lúa đã cân
  Given Phiếu cân lúa có ID 1 đang mở ở trạng thái chưa khóa (isLocked = FALSE)
  And Danh sách bao cân có "Bao #03" với khối lượng cũ là "50.5" kg
  When Người dùng chọn sửa "Bao #03" và nhập khối lượng mới là "49.0" kg
  And Người dùng nhấn nút "Lưu"
  Then Hệ thống sẽ cập nhật lại giá trị weight của WeightEntry trong Room DB thành "49.0"
  And Hệ thống tự động tính toán lại các số liệu tổng quát của phiếu cân
  And Cập nhật tổng khối lượng thô giảm đi "1.5" kg
  And Đóng giao diện chỉnh sửa và cập nhật hiển thị dòng bao lúa trên UI thành "49.0 kg"

Scenario: Người dùng xóa một bao lúa khỏi phiếu cân
  Given Phiếu cân lúa có ID 1 đang mở ở trạng thái chưa khóa (isLocked = FALSE)
  And Danh sách bao lúa hiện tại có 12 bao lúa
  When Người dùng mở chi tiết "Bao #12" và chọn "Xóa bao"
  And Người dùng xác nhận đồng ý xóa ở hộp thoại cảnh báo
  Then Hệ thống sẽ thực hiện xóa bản ghi WeightEntry tương ứng khỏi Room DB
  And Số lượng bao lúa của phiếu cân giảm từ "12" xuống "11"
  And Cập nhật lại tổng khối lượng thô và tổng số tiền của phiếu cân tương ứng
  And Danh sách bao lúa trên giao diện cập nhật ngay lập tức
```

### Kịch bản 3: Tính toán tổng tiền dựa trên đơn giá và khấu trừ bao bì (Total Calculation & Tare Deduction)
```gherkin
Scenario: Tính toán khối lượng thực tế và thành tiền theo phương pháp trừ bì chuẩn (Cách A)
  Given Phiếu cân lúa có đơn giá là "9000" đ/kg
  And Số bao cân lúa hiện có là "20" bao với tổng khối lượng thô là "1000.0" kg
  And Khối lượng một vỏ bao bì mặc định (bagWeight) là "1.0" kg
  And Khấu trừ tạp chất (impurityWeight) là "10.0" kg
  And Độ ẩm lúa (moisturePercent) đo được là "18.0" %
  When Hệ thống kích hoạt hàm tính toán lại của RiceCalculator
  Then Khối lượng bao bì được tính là:
    $$TotalBagWeight = 20 \times 1.0 = 20.0\text{ kg}$$
  And Khối lượng sau khi trừ bao bì là:
    $$GrossAfterBag = 1000.0 - 20.0 = 980.0\text{ kg}$$
  And Khối lượng thô trước quy đổi độ ẩm (trừ tiếp tạp chất) là:
    $$Gross = 980.0 - 10.0 = 970.0\text{ kg}$$
  And Khối lượng thực tế sau khi quy đổi về độ ẩm tiêu chuẩn 14% là:
    $$NetWeight = 970.0 \times \frac{100.0 - 18.0}{100.0 - 14.0} \approx 924.9\text{ kg} \text{ (làm tròn 1 chữ số thập phân)}$$
  And Tổng số tiền thành tiền được tính là:
    $$TotalAmount = 924.9 \times 9000 = 8,324,100\text{ đ}$$
  And Cập nhật các trường netWeight và totalAmount tương ứng của Card vào Room DB
```
