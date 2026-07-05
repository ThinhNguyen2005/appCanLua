# Đặc Tả Bảo Mật & Phân Quyền (Security & Authorization Specification)

Tài liệu này định nghĩa chi tiết thiết kế bảo mật, ma trận phân quyền người dùng (Farmer vs Trader) và các quy tắc bảo vệ dữ liệu ở cả tầng thiết bị di động (Local) và tầng đám mây (Cloud Supabase) cho hệ thống **Cân Lúa Mobile**.

---

## 1. Ma Trận Phân Quyền Người Dùng (Authorization Matrix)

Hệ thống phân chia quyền truy cập tài nguyên dựa trên ba vai trò chính: **Nông dân (FARMER)**, **Thương lái (TRADER)**, và **Khách (GUEST)**.

```yaml
authorization_matrix:
  roles:
    GUEST: "Tài khoản khách mặc định khi chưa đăng nhập. Chỉ có quyền thao tác local."
    FARMER: "Nông dân. Có quyền quản lý phiếu cân của chính mình và chia sẻ mã QR."
    TRADER: "Thương lái. Có quyền đăng giá mua, quét QR liên kết phiếu và xem bản đồ."
  
  permissions:
    weighing_cards:
      create:
        GUEST: "Được phép (chỉ lưu local)"
        FARMER: "Được phép"
        TRADER: "Được phép (tạo phiếu đại diện cho farmer)"
      read:
        GUEST: "Chỉ đọc phiếu do mình tạo ở local"
        FARMER: "Được phép đọc phiếu của mình"
        TRADER: "Đọc được phiếu do mình tạo hoặc phiếu được liên kết qua QR Handshake"
      update:
        GUEST: "Được phép (chỉ local, chỉ khi chưa khóa)"
        FARMER: "Được phép (chỉ khi chưa khóa)"
        TRADER: "Được phép cập nhật trạng thái thanh toán; cấm sửa cân nếu phiếu đã khóa"
      delete:
        GUEST: "Được phép (chỉ local, chỉ khi chưa khóa)"
        FARMER: "Được phép (chỉ khi chưa khóa)"
        TRADER: "Bị cấm"

    market_prices:
      read:
        GUEST: "Được phép (đọc bảng giá chung)"
        FARMER: "Được phép"
        TRADER: "Được phép"
      write_bids:
        GUEST: "Bị cấm"
        FARMER: "Bị cấm"
        TRADER: "Được phép đăng/sửa giá thu mua của riêng mình"

    role_upgrade:
      request_trader:
        GUEST: "Bị cấm"
        FARMER: "Được phép gửi yêu cầu (CCCD + Lý do) để Admin duyệt"
        TRADER: "Không áp dụng"
```

---

## 2. Quy Tắc Bảo Mật Tầng Local (Local Security Rules)

### 2.1. Mã Hóa Dữ Liệu Nhạy Cảm (CCCD Encryption)
Để bảo vệ thông tin nhận dạng cá nhân (PII), số CCCD của người dùng bắt buộc phải được mã hóa trước khi ghi vào cơ sở dữ liệu local (Room DB) hoặc truyền lên Supabase.

*   **Thuật toán mã hóa:** `AES/CBC/PKCS5Padding` (Khóa AES-256).
*   **Chi tiết cấu hình mã hóa (`CccdCrypto.kt`):**
    ```yaml
    cccd_encryption_spec:
      algorithm: "AES/CBC/PKCS5Padding"
      key_obfuscated_base64: "Q2FuTHVhQXBwQ2NjZEtleTIwMjZTZWN1cmVLZXlLZXk="
      key_decoded_raw: "CanLuaAppCccdKey2026SecureKeyKey" # 32 bytes (256-bit)
      iv_obfuscated_base64: "Q2FuTHVhQXBwSXYyMDI2IQ=="
      iv_decoded_raw: "CanLuaAppIv2026!" # 16 bytes (128-bit)
    ```
*   **Quy trình:**
    *   **Ghi dữ liệu:** Văn bản CCCD thô -> UTF-8 bytes -> Mã hóa AES -> Chuỗi Base64 -> Lưu Database.
    *   **Đọc dữ liệu:** Chuỗi mã hóa Base64 -> Giải mã Base64 -> Giải mã AES -> Hiển thị text thô lên UI. Nếu giải mã lỗi, tự động trả về bản thô để đảm bảo khả năng tương thích ngược.

### 2.2. Xáo Trộn API Key tại Runtime (API Key Obfuscation)
Để tránh các cuộc tấn công dịch ngược gói tin APK để đánh cắp khóa API (Google Maps, OpenWeather, Supabase Anon Key):
*   Các khóa API nhạy cảm được mã hóa Base64 trước khi đặt vào file cấu hình.
*   **Giải mã động (`ApiKeyObfuscator.kt`):** Ứng dụng giải mã Base64 sang chuỗi String thô trực tiếp ở bộ nhớ RAM tại runtime khi gọi API, ngăn chặn việc quét chuỗi plaintext tĩnh trong file `classes.dex`.

### 2.3. Cô Lập Dữ Liệu Đa Người Dùng (Local Multi-Tenant Isolation)
*   Khi nhiều tài khoản đăng nhập trên cùng một thiết bị di động, dữ liệu trong Room DB được cô lập tuyệt đối bằng trường `ownerUid`.
*   Mọi câu truy vấn SQL trong DAO bắt buộc phải có điều kiện lọc: `WHERE ownerUid = :currentUid` để tránh rò rỉ dữ liệu chéo giữa các tài khoản.

---

## 3. Quy Tắc Bảo Mật Tầng Mạng & Cloud (Cloud Security Config)

### 3.1. Cấu Hình An Toàn Giao Tiếp Mạng (`network_security_config.xml`)
Ứng dụng thực thi chính sách cấm hoàn toàn truyền thông tin dạng văn bản rõ (Cleartext HTTP) để ngăn chặn tấn công nghe lén (Man-in-the-Middle):
*   **Chính sách mặc định:** `cleartextTrafficPermitted="false"` (Ép buộc HTTPS cho toàn bộ API như Supabase, Firebase, Google Maps).
*   **Danh sách Whitelist cho CDN tin tức:** Cho phép HTTP (`cleartextTrafficPermitted="true"`) đối với các tên miền CDN chứa ảnh tin tức nông nghiệp của Việt Nam không hỗ trợ chứng chỉ SSL:
    ```yaml
    http_whitelisted_domains:
      - "*.danviet.vn"
      - "*.nongnghiep.vn"
      - "*.nongnghiepmedia.vn"
      - "*.vnecdn.net"
      - "*.mediacdn.vn"
      - "*.tuoitre.vn"
      - "*.thanhnien.vn"
      - "*.baomoi.com"
    ```

### 3.2. Chính Sách Row-Level Security (RLS) trên Supabase Database
Supabase PostgreSQL chặn mọi hành vi đọc/ghi dữ liệu ở tầng database bằng cơ chế RLS. Client App chỉ được phép thao tác thông qua Token JWT xác thực.

```yaml
supabase_rls_specification:
  profiles:
    select_policy: "auth.uid() = id OR (role = 'FARMER' AND EXISTS (SELECT 1 FROM cards WHERE handshake_trader_id = auth.uid() AND owner_uid = id))"
    insert_policy: "auth.uid() = id"
    update_policy: "auth.uid() = id"
  
  cards:
    select_policy: "auth.uid() = owner_uid OR auth.uid() = handshake_trader_id"
    insert_policy: "auth.uid() = owner_uid"
    update_policy: "(auth.uid() = owner_uid AND NOT is_locked) OR (auth.uid() = handshake_trader_id)"
    delete_policy: "auth.uid() = owner_uid AND NOT is_locked"

  weight_entries:
    select_policy: "EXISTS (SELECT 1 FROM cards WHERE id = card_id AND (owner_uid = auth.uid() OR handshake_trader_id = auth.uid()))"
    insert_policy: "EXISTS (SELECT 1 FROM cards WHERE id = card_id AND owner_uid = auth.uid() AND NOT is_locked)"
    delete_policy: "EXISTS (SELECT 1 FROM cards WHERE id = card_id AND owner_uid = auth.uid() AND NOT is_locked)"
```
