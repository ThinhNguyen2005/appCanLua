# Cân Lúa Mobile

Ứng dụng Android native cho nông dân và thương lái số hoá quy trình cân lúa tại ruộng: tạo phiếu cân, nhập cân nhiều bao, chốt giao dịch bằng QR, theo dõi thị trường, hỏi đáp AI nông nghiệp, xem bản đồ điểm thu mua và xuất PDF.

## Tính năng chính

### Cân lúa offline-first

- Tạo và quản lý phiếu cân lúa cho cả vai trò `FARMER` và `TRADER`.
- Nhập nhiều bao cân, tính khối lượng thực theo bì, tạp chất và độ ẩm.
- Room Database lưu cục bộ, Firestore đồng bộ nền qua WorkManager khi có mạng.
- Khôi phục/xoá vĩnh viễn phiếu đã xoá.
- QR handshake bằng SHA-256 để xác thực giao dịch giữa nông dân và thương lái.

### Hồ sơ, vai trò và Premium

- Firebase Auth làm nguồn trạng thái đăng nhập chính.
- Hồ sơ người dùng có chỉnh sửa thông tin cá nhân, chuyển vai trò, trạng thái Premium.
- Farmer profile có dashboard mùa vụ, KPI, biểu đồ so sánh vụ, AI insights và lịch sử thương lái.
- Trader profile có các luồng riêng cho giao dịch, bản đồ và thị trường.
- Font scale, TTS khi nhập cân, đồng bộ/backup và cài đặt ngôn ngữ trong Settings.

### Thị trường, tin tức và thời tiết

- Giá lúa realtime từ Firestore, lọc theo giống lúa.
- Biểu đồ xu hướng bằng Vico Compose.
- Weather GPS + OpenWeatherMap, cache offline-first.
- News feed RSS đa nguồn, cache Room, mở bài bằng Chrome Custom Tabs.

### AI nông nghiệp

- AI Chat dùng OpenRouter/Gemini, có context giá lúa, hồ sơ và knowledge base canh tác.
- Voice input bằng Android `SpeechRecognizer`.
- Markdown AI render bằng Markwon, hỗ trợ heading, list, link và bảng Markdown.
- AI dashboard insights dùng cùng renderer nên bảng so sánh từ AI xem được trong app.

### Bản đồ và QR

- Google Maps Compose 6.x với clustering điểm cân/thu mua.
- CameraX + ML Kit để quét QR.
- Tự ghi GPS khi tạo phiếu nếu có quyền vị trí.

### Đa ngôn ngữ

- Resource locale hiện có: `values` mặc định tiếng Việt, `values-en`, `values-km`, `values-lo`, `values-zh-rCN`.
- Khmer/Lào/Trung đang được cập nhật dần; cần kiểm tra placeholder `%1$s`, `%2$d`, XML escape và đơn vị tiền/tạ/kg sau mỗi lượt dịch.

## Tech stack

| Lớp | Công nghệ |
|---|---|
| Ngôn ngữ | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Kiến trúc | MVVM, Repository, Hilt |
| Local DB | Room |
| Cloud | Firebase Auth, Firestore, Storage, Analytics, Crashlytics |
| Background | WorkManager |
| Network | OkHttp, Gson |
| AI | OpenRouter/Gemini, Markwon Markdown renderer |
| Map | Google Maps Compose, maps-compose-utils clustering |
| QR | ZXing, CameraX, ML Kit Barcode |
| Chart | Vico Compose |
| Image | Coil Compose |
| Browser | AndroidX Browser / Chrome Custom Tabs |

## Cấu hình local

Tạo hoặc cập nhật `local.properties` ở root project:

```properties
sdk.dir=C:\\Path\\To\\Android\\Sdk
OPENWEATHER_API_KEY=your_openweather_key
OPENROUTER_API_KEY=your_openrouter_key
MAPS_API_KEY=your_google_maps_key
```

Đặt `google-services.json` vào thư mục `app/` để dùng Firebase.

App vẫn build được khi API key rỗng, nhưng các module Weather, AI hoặc Map sẽ degrade theo trạng thái thiếu cấu hình.

## Build và kiểm tra

```powershell
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebug
```

Lệnh nhanh trên Windows:

```powershell
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:assembleDebug
```

## Cấu trúc chính

```text
app/src/main/java/com/GiaThinh/canlua/
├── auth/                 # Google/Firebase sign-in helpers
├── data/                 # Room entities, DAO, database, location, remote models
├── di/                   # Hilt modules
├── repository/           # Single source of truth, sync, AI, news, market
├── ui/
│   ├── component/        # Reusable Compose components
│   ├── navigation/       # NavHost, bottom nav, transitions
│   ├── screen/           # Feature screens
│   ├── theme/            # AppColors, typography, Theme
│   ├── util/             # Markdown, scroll behavior, formatting
│   └── viewmodel/        # ViewModels
└── util/                 # Premium state, locale, TTS/STT helpers
```

## Các luồng cần test thủ công

- Đăng nhập/đăng xuất Firebase và Google.
- Tạo phiếu cân, nhập cân nhiều bao, khoá giao dịch, quét QR.
- Đồng bộ Firestore khi offline → online.
- AI Chat với câu trả lời có bảng Markdown.
- Chuyển ngôn ngữ Việt/Anh/Khmer/Lào/Trung.
- Profile farmer: dashboard mùa vụ, Premium card, lịch sử thương lái.
- Map trader: cấp quyền vị trí, clustering, mở bottom sheet marker.
- Xuất PDF và chia sẻ file.

## Ghi chú bảo mật

- Không commit `local.properties`, API key thật hoặc `google-services.json` nếu không được phép.
- Client Android không phải boundary bảo mật; mọi quyền quan trọng cần được Firestore Rules/Firebase backend kiểm soát.
- Nội dung AI là dữ liệu không tin cậy; app chỉ render Markdown bằng Markwon, không bật WebView hoặc thực thi script.

## Tác giả

Phát triển bởi Gia Thịnh. Dự án phục vụ số hoá quy trình cân lúa và giao dịch nông nghiệp thực địa.
