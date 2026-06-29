# Tính năng phát triển sau (Backlog)

Tài liệu này lưu trữ các tính năng tạm thời hoãn lại trong đợt tái cấu trúc offline-first 2026 của dự án **Cân Lúa Mobile**.

---

## 1. Đồng bộ & Sao lưu tự động lên Đám mây (Firebase)
* **Lý do hoãn:** Hiện tại việc đồng bộ liên tục dữ liệu phiếu cân lớn cùng hàng trăm bao lúa (`WeightEntry`) lên Firestore không hiệu quả về mặt chi phí và hiệu năng mạng trên đồng ruộng.
* **Hướng phát triển sau:**
  * Xây dựng cơ chế sao lưu delta (chỉ đẩy phần thay đổi).
  * Nghiên cứu sử dụng Cloud Storage để lưu file nén DB thay vì ghi trực tiếp từng row vào Firestore.
  * Chỉ mở tính năng sao lưu tự động cho tài khoản Premium để tối ưu chi phí.

## 2. Phương thức xác thực QR Handshake thế hệ mới
* **Lý do hoãn:** Tạm thời tắt tính năng QR Handshake dựa trên Firestore vì đã gỡ bỏ đồng bộ phiếu cân tự động.
* **Hướng phát triển sau:**
  * **Phương án 1 (Offline 100%):** Nén toàn bộ cấu trúc dữ liệu phiếu cân và danh sách bao lúa vào mã QR mật độ cao (sử dụng nén GZIP/Deflate + Protobuf hoặc mã hóa Base64 chuỗi nhị phân). Thương lái quét mã là có ngay dữ liệu mà không cần internet.
  * **Phương án 2 (Transient Cloud):** Sử dụng Firestore làm trung gian tạm thời. Khi bấm chia sẻ QR, dữ liệu được đẩy lên cloud, thương lái quét và kéo về máy local xong thì cloud tự động xóa dữ liệu đó (hoặc tự hủy sau 24 giờ).
