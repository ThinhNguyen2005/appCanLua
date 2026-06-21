<!-- Language: [English](#english) | [Tiếng Việt](#tiếng-việt) -->

<div align="right">
  <a href="#english">🇺🇸 English</a> &nbsp;|&nbsp;
  <a href="#tiếng-việt">🇻🇳 Tiếng Việt</a>
</div>

---

<a name="english"></a>

# 🌾 Cân Lúa Mobile

> Android app for rice farmers in the Mekong Delta. Offline-first. No account required.

## Stack

| Layer | Tech |
|---|---|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture + Hilt |
| Local DB | Room v17 (8 entities, migration chain) |
| Cloud | Firebase Auth · Firestore · WorkManager sync |
| Network | OkHttp + Gson |
| AI | OpenRouter (Gemini Flash) — RAG with rice price context |
| Charts | Vico Compose |
| Maps | Google Maps Compose 6.4.1 + Clustering |
| News | RSS XML parser + Chrome Custom Tabs |

## Features

- **Weigh cards** — multi-bag entry, real-time net weight calc (tare, impurity, moisture)
- **Market prices** — Firestore-backed live rice prices, 7-day trend chart
- **News feed** — RSS from agricultural sources, Room-cached, auto-categorized
- **AI assistant** — OpenRouter/Gemini with RAG-injected prices + agronomy knowledge base
- **Season dashboard** — aggregate stats per crop season, AI-generated insights
- **Google Maps** — purchase map with marker clustering, GPS auto-capture

## Net Weight Formula

```
netWeight = totalWeight
          - (bagCount × bagWeightPerUnit)
          - impurityKg
          - totalWeight × (moisturePercent / 100)
```

## Setup

```properties
# local.properties
sdk.dir=...
OPENROUTER_API_KEY=...
MAPS_API_KEY=...
```

Place `google-services.json` in `app/`.

```bash
./gradlew :app:assembleDebug
```

## Structure

```
app/src/main/java/com/GiaThinh/canlua/
├── data/          # Room entities, DAOs, Firestore models, remote clients
├── repository/    # Business logic, sync, AI, news, market
├── ui/
│   ├── screen/    # Compose screens (weighing, market, news, AI, dashboard, map)
│   ├── component/ # Reusable Compose components
│   ├── viewmodel/ # MVVM state holders
│   └── theme/     # Color tokens, typography, Material 3 theme
├── util/          # RiceCalculator, formatters, TTS/STT, PDF export
└── di/            # Hilt modules
```

---

<a name="tiếng-việt"></a>

# 🌾 Cân Lúa Mobile

> Ứng dụng Android dành cho nông dân ĐBSCL. Offline-first. Không cần tài khoản.

## Stack

| Lớp | Công nghệ |
|---|---|
| Ngôn ngữ | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Kiến trúc | MVVM + Clean Architecture + Hilt |
| Local DB | Room v17 (8 entity, migration chain) |
| Cloud | Firebase Auth · Firestore · WorkManager sync |
| Network | OkHttp + Gson |
| AI | OpenRouter (Gemini Flash) — RAG với context giá lúa |
| Chart | Vico Compose |
| Bản đồ | Google Maps Compose 6.4.1 + Clustering |
| Tin tức | RSS XML parser + Chrome Custom Tabs |

## Tính năng

- **Phiếu cân** — nhập nhiều bao, tính KG thực real-time (bì, tạp chất, độ ẩm)
- **Giá thị trường** — giá lúa realtime từ Firestore, biểu đồ xu hướng 7 ngày
- **Tin tức** — RSS từ báo nông nghiệp, cache Room, tự phân loại
- **AI khuyến nông** — OpenRouter/Gemini, RAG inject giá lúa + knowledge base canh tác
- **Dashboard vụ mùa** — thống kê aggregate theo vụ, AI insights
- **Google Maps** — bản đồ thu mua, clustering marker, GPS tự động

## Công thức KG Thực

```
kgThực = tổngCân
       - (sốBao × kgBì)
       - tạpChấtKg
       - tổngCân × (độẨm / 100)
```

## Cài đặt

```properties
# local.properties
sdk.dir=...
OPENROUTER_API_KEY=...
MAPS_API_KEY=...
```

Đặt `google-services.json` vào thư mục `app/`.

```bash
./gradlew :app:assembleDebug
```

## Cấu trúc

```
app/src/main/java/com/GiaThinh/canlua/
├── data/          # Room entity, DAO, Firestore model, remote client
├── repository/    # Business logic, sync, AI, news, market
├── ui/
│   ├── screen/    # Màn hình Compose (cân, thị trường, tin, AI, dashboard, map)
│   ├── component/ # Component dùng lại
│   ├── viewmodel/ # MVVM state holder
│   └── theme/     # Color token, typography, Material 3 theme
├── util/          # RiceCalculator, formatter, TTS/STT, PDF export
└── di/            # Hilt module
```

---

*Built by Gia Thịnh*
