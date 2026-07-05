# Designer 4 — Market & AI Chat

**Phạm vi**: 2 màn hình lớn (Market có 5 section + AiChat có drawer).
- MarketScreen
- AiChatScreen

**Spec gốc đọc trước**: section 5 (Market), section 7 (AI Chat).

**Phụ thuộc**: D1 ship token + chip + chart tokens + sheet + list item (cuối tuần 2).

**Critical pain points fix**: #5 (Market overload — 5 section heavy không hierarchy), #10 (AI chat không persist/delete/rename/regenerate, fake streaming), #15 (chip "Tất cả"), #30 (footer padding overlap nav), #42 (chart simplistic), #44 (error banner thiếu icon affordance), #47 (API key missing không có fallback UI).

---

## 0. Mục tiêu trải nghiệm

Market hiện là 1 trang dài 5 section chồng chất nhau — user phải scroll qua weather, prices, news, ad, bids mà không biết "tin gì quan trọng nhất hôm nay". AI Chat thì hứa nhiều mà ship ít: messages không lưu, session không có, regenerate không có. Redesign:

1. **Market**: 1 hero data point (giá lúa hôm nay) + 4 section phụ. Đừng đối xử cả 5 section ngang nhau.
2. **AI Chat**: như chat app thật — drawer session list, delete, rename, regenerate, real streaming token-by-token.
3. **Offline-first**: weather + prices cache 30 phút; UI hiển thị "cập nhật lúc HH:mm" rõ ràng, không pretend là real-time.
4. **Error gentle**: API key missing → empty state có hướng dẫn, không crash hay snackbar tech.

---

## 1. MarketScreen

### 1.1. Yêu cầu chức năng (giữ nguyên backend)

5 nguồn dữ liệu:
1. **Weather** (OpenWeather) — vị trí + 24h forecast + 5-day forecast
2. **Rice prices** (Firestore `rice_prices`) — giá theo tỉnh + xu hướng 30 ngày
3. **News** (Firestore `news_articles`) — bài viết nông nghiệp curated
4. **Native ad** (Firebase Remote Config / AdMob optional)
5. **Trader bids** (Firestore `trader_bids` — TRADER post offer, FARMER xem)

FARMER thấy 5 section read-only. TRADER thấy thêm nút "Đăng giá mua" mở sheet edit bid.

### 1.2. Layout đề xuất — hero + accordion ngắn

**FIX pain #5**: thay vì 5 section dài flat, dùng cấu trúc **hero + 4 collapsible section** với data point chính ở top.

```
┌─────────────────────────────────────┐
│ [Top bar: Thị trường]   [🔄 refresh] │
│                                     │
│  ┌─ HERO CARD ─────────────────┐    │
│  │ Giá lúa hôm nay              │    │
│  │ 8.200 đ/kg ↑ 200 (24h)       │    │  ← display 32sp 800w
│  │                              │    │     Lúa chín gradient
│  │ Long An · cập nhật 09:32     │    │  ← caption text.tertiary
│  │ [▁▂▃▅▇▆▅▄▃] 30d sparkline    │    │  ← inline mini-chart
│  └──────────────────────────────┘    │
│                                     │
│  Thời tiết hôm nay               ▾  │  ← section header h2, collapsible
│  ┌─ Weather summary ────────────┐   │
│  │ 32°C · Nắng nóng              │   │
│  │ [icon] Long An                │   │
│  │ Sấy lúa tốt sau 10h sáng     │   │  ← advisory dynamic
│  └──────────────────────────────┘   │
│   [Xem 5 ngày →]                    │
│                                     │
│  Giá lúa 13 tỉnh ĐBSCL           ▾  │
│  ┌─ Province price chart ───────┐   │
│  │ [Bar chart 13 tỉnh + value]   │   │
│  │ [Tab: 24h | 7d | 30d]         │   │
│  └──────────────────────────────┘   │
│   [Xem chi tiết theo loại lúa →]    │
│                                     │
│  Tin nông nghiệp                 ▾  │
│  ┌─ News card 1 ───────────────┐    │
│  │ [Image] Title 2 dòng         │    │
│  │ Source · 2h trước             │    │
│  └──────────────────────────────┘    │
│  ┌─ News card 2 ───────────────┐    │
│  │ [Image] Title                │    │
│  │ Source · 5h trước             │    │
│  └──────────────────────────────┘    │
│   [Xem thêm 8 bài →]                │
│                                     │
│  Thương lái thu mua              ▾  │  ← chỉ FARMER
│  ┌─ Bid card ────────────────────┐   │
│  │ Anh Tâm (Trader)             │   │
│  │ 8.300 đ/kg · OM5451           │   │
│  │ 📍 Cách 2.3km · 0903.xxx.xxx │   │
│  │ [Gọi]  [Chỉ đường]            │   │
│  └──────────────────────────────┘   │
│                                     │
│  ─────── ⓘ Quảng cáo ────────       │
│  [Native ad card hoặc nothing]      │
│                                     │
│  [Bottom nav 56dp]                  │
└─────────────────────────────────────┘
```

**FIX pain #30**: padding bottom 80dp (= nav bar height + safe margin) trên scroll content để bài cuối không bị navbar che.

### 1.3. Hero card — animated value

- Số 8.200 đ/kg ở `display` size, weight 800
- Delta ↑ 200 dùng `sem.success`, ↓ 200 dùng `sem.error`, đứng yên dùng `text.tertiary`
- Sparkline 30d inline ngay dưới — 64dp height, dùng `brand.accent` cho đường, fill 20% opacity
- Tap card → mở rộng → chart full screen với loại lúa breakdown (OM5451 / OM18 / Đài Thơm / IR504...)
- Caption "cập nhật 09:32" — nếu data > 1h cũ, dùng `sem.warning` chip "Có thể chậm"

### 1.4. Weather widget — advisory thay vì dữ liệu thô

User thường không quan tâm °C chính xác — họ cần biết "có sấy lúa được không". Logic:
- Mưa next 6h → "Hoãn phơi lúa, có mưa tới"
- Nắng > 30°C → "Sấy lúa tốt sau 10h sáng"
- Gió > 15km/h → "Gió mạnh, cẩn thận đổ lúa"
- Mặc định "Thời tiết bình thường"

Layout:
```
┌─ Weather card ──────────────────┐
│ [Icon big 48dp] 32°C            │
│                Nắng nóng         │
│                                 │
│ 📍 Long An · 10:32              │
│                                 │
│ 💡 Sấy lúa tốt sau 10h sáng    │  ← advisory chip status.info
│                                 │
│ [Tap → expand 24h hourly]       │
└─────────────────────────────────┘
```

Expanded state hiện:
- Hourly forecast 24h (line chart)
- 5-day mini cards horizontal scroll

### 1.5. Rice price chart — đa chiều thay vì 1 line

**FIX pain #42**: hiện chart chỉ 1 đường, designer thiết kế 3 tab interactive:

```
[Tab 1: 24h | Tab 2: 7d | Tab 3: 30d]  ← Segmented control

┌─ Chart area 200dp height ──────┐
│         ╱╲                     │
│       ╱   ╲╱╲    ← OM5451       │
│   ╱╲╱        ╲╱  ← OM18         │
│ ╱              ╲ ← Đài Thơm     │
│                                │
└────────────────────────────────┘
[Legend dot]
● OM5451  ● OM18  ● Đài Thơm  ← chip toggle để ẩn/hiện
```

- Long-tap chart → tooltip "Ngày 22/05, OM5451: 8.200 đ/kg"
- Lock vertical zoom (only horizontal pan)
- Y-axis dùng `text.tertiary` ngắn (8.0 / 8.2 / 8.4)
- Use Vico (đã có trong stack) — không thêm thư viện mới
- Chart line dùng token `chart.color.1/2/3` (xem D1 spec)

### 1.6. News section — list nhỏ, không heavy

3-5 bài news card thumbnail nhỏ. Tap mở ChromeCustomTab (in-app browser), không WebView raw (tốc độ).

```
┌─ News card 80dp ───────────────┐
│ [Image 80×80] Title 2 dòng      │
│               Source · 2h ago    │
└────────────────────────────────┘
```

Source label dùng pill chip surface.2 nhỏ. Image lazy-load với placeholder `surface.3`.

### 1.7. Trader bids — empty state + filter

```
┌─ Filter bar ──────────────────┐
│ [Bán kính: 5km ▾] [Loại lúa ▾] [Giá ▾] │
└───────────────────────────────┘

┌─ Bid card ──────────────────────┐
│ [Avatar] Anh Tâm                │
│         8.300 đ/kg · OM5451     │
│         📍 2.3km · ⭐ 4.8        │
│  [📞 Gọi]  [🗺 Chỉ đường]        │
└─────────────────────────────────┘
```

Empty state: "Chưa có thương lái nào ở khu vực bạn. Bật thông báo để biết khi có offer mới [Bật]"

### 1.8. TRADER variant — extra bid editor entry

TRADER thấy thêm card đầu section:
```
┌─ Trader's own bid ──────────────┐
│ 🟢 Đang đăng                     │
│ 8.300 đ/kg · OM5451 · 5 tấn     │
│ Hết hạn sau 2 ngày               │
│  [Chỉnh sửa]  [Tạm ẩn]           │
└─────────────────────────────────┘
```

Tap "Chỉnh sửa" → mở `BidEditorSheet` bottom sheet với form (giá, loại, quantity, expire, vị trí).

### 1.9. States cần spec

- **Default** (5 section đầy đủ data)
- **Loading** (skeleton cho mỗi section, không spinner full screen)
- **Empty per section** (weather GPS denied / prices network / news empty / bids 0)
- **Offline banner** ở top: "Dữ liệu offline · cập nhật lúc HH:mm" (FIX pain #44 — icon `offline` 16dp leading)
- **API key missing** (FIX pain #47): weather section show "Bật vị trí để xem thời tiết" + nút Setting; prices section show "Đang cập nhật dữ liệu, thử lại sau"
- **Pull-to-refresh** trigger
- **Section collapsed** (chevron rotate)
- **Dark mode**
- **Font scale 1.2×** (display number không tràn)

### 1.10. Pain point fix

| ID | Fix |
|---|---|
| #5 | Hero data point + 4 section collapsible thay vì 5 section flat |
| #15 | Filter chip dùng atom D1 (filled + outline rõ ràng) |
| #30 | Padding bottom 80dp tránh navbar overlap |
| #42 | Chart 3-tab (24h/7d/30d) + multi-line + tooltip |
| #44 | Offline banner có icon `offline` leading + caption "cập nhật HH:mm" |
| #47 | Empty state hướng dẫn user thay vì crash/error tech |

---

## 2. AiChatScreen

### 2.1. Yêu cầu chức năng nâng cấp (cần dev co-design)

**Hiện tại** (cần FIX):
- Messages không persist (mất khi xoay screen)
- 1 session duy nhất (không có history)
- Streaming là fake (delay artificial)
- Không delete / rename / regenerate

**Sau redesign** (yêu cầu backend support):
- Multiple sessions (drawer left, sort by lastMessageMs)
- Persist trong Room (`chat_sessions` + `chat_messages` table mới)
- Real streaming SSE từ OpenRouter (event `message_delta`)
- Long-press message → menu: copy / regenerate / delete from here
- Long-press session → menu: rename / delete / pin

Backend đã có `AiChatRepository` — chỉ cần thêm table + DAO + repo method. D4 designer spec UI, dev sẽ wire.

### 2.2. Layout đề xuất

**Main view**:
```
┌──────────────────────────────────┐
│ [☰] Tư vấn AI    [🔄][Trợ lý ▾]  │  ← top bar
├──────────────────────────────────┤
│                                  │
│ ┌─ Bubble AI (left) ────────┐    │
│ │ Chào anh! Em là Cân Lúa AI.│    │  ← bg surface.2
│ │ Em có thể giúp tư vấn về:  │    │     border-radius lg
│ │ • Sâu bệnh lúa             │    │
│ │ • Kỹ thuật canh tác        │    │
│ │ • Giá thị trường           │    │
│ │ Anh hỏi gì nhé?            │    │
│ │                09:32        │    │  ← caption text.tertiary
│ └────────────────────────────┘    │
│                                  │
│      ┌─ Bubble user (right) ──┐  │
│      │ Lúa em bị đốm vàng,    │  │  ← bg brand.primary
│      │ phải làm sao?           │  │     text on-primary
│      │           09:33   ✓     │  │     border-radius lg with tail
│      └────────────────────────┘  │
│                                  │
│ ┌─ Bubble AI (streaming) ────┐    │
│ │ Anh ơi, đốm vàng có thể do  │    │
│ │ **bệnh đạo ôn** hoặc thiếu  │    │  ← markdown rendered
│ │ kali. Cách phân biệt:       │    │
│ │ |Triệu chứng|Đạo ôn|Kali|   │    │  ← table support Markwon
│ │ | Vết đốm  |hình thoi|...|   │    │
│ │  ▌ │ ← cursor blinking      │    │  ← streaming indicator
│ └────────────────────────────┘    │
│                                  │
│ ┌─ Suggestion chips ──────────┐   │
│ │ [Cách trị đạo ôn] [Bón kali]│   │  ← horizontal scroll
│ └─────────────────────────────┘   │
│                                  │
├──────────────────────────────────┤
│ ┌─ Composer ────────────────┐    │
│ │ [📎] Hỏi gì đó...    [🎤] │    │  ← attach optional + voice
│ └───────────────────────────┘    │
│                          [Gửi]   │
│  ⓘ AI có thể sai. Kiểm tra lại.  │  ← caption text.tertiary
└──────────────────────────────────┘
```

### 2.3. Drawer left — Session list

Tap `☰` → mở drawer:
```
┌─ Drawer 280dp wide ───────────┐
│ Trợ lý AI                     │
│ [+ Chat mới]                  │
│                               │
│ ─── Hôm nay ───                │
│ ┌─ Session row ──────────────┐│
│ │ 📌 Tư vấn đạo ôn           ││  ← pinned icon
│ │ Vừa xong · 12 tin nhắn     ││
│ └────────────────────────────┘│
│ ┌─ Session row ──────────────┐│
│ │ Giá lúa hôm nay            ││
│ │ 2h trước · 4 tin           ││
│ └────────────────────────────┘│
│                               │
│ ─── 7 ngày qua ───             │
│ Bón phân OM5451 · 3d trước    │
│ Sâu cuốn lá · 5d trước        │
│                               │
│ ─── Trước đó ───               │
│ ...                           │
│                               │
│ ───────────────────────────── │
│ [Settings] [Phản hồi]         │
└───────────────────────────────┘
```

- Long-press session: bottom sheet với [Đổi tên / 📌 Ghim / 🗑 Xoá]
- Empty: "Chưa có cuộc trò chuyện. [Bắt đầu chat mới]"
- Search bar top: "Tìm trong lịch sử..."

### 2.4. Message bubble anatomy

**AI bubble** (left):
- BG `surface.2`, border-radius `lg` (16dp) đều 4 góc, không tail (đỡ rối)
- Padding 12dp × 16dp
- Max width 85% screen
- Footer: timestamp `caption` `text.tertiary`
- Long-press → menu: Copy / Regenerate / Đọc to (TTS optional)

**User bubble** (right):
- BG `brand.primary`, text `text.on-primary`
- Border-radius `lg` all corners, tail nhẹ ở bottom-right
- Same padding
- Footer: timestamp + checkmark icon
  - `✓` đã gửi
  - `✓✓` AI đã đọc (= request response started)
- Long-press → menu: Copy / Sửa và gửi lại / Xoá

**System bubble** (centered, no avatar):
- Status messages: "Đã chuyển sang model Gemini Flash"
- Style: text caption text.tertiary, centered

### 2.5. Streaming UX

**FIX pain #10**: streaming phải là **thật**.
- Token xuất hiện token-by-token theo SSE event
- Cursor `▌` blinking 600ms cuối message khi đang stream
- Stop button thay Send khi đang stream → user có thể ngắt
- Sau stop: message giữ nguyên + chip "Đã dừng" + [Tiếp tục] button
- Spinner state khi vừa send chưa nhận token đầu: 3 dot pulse "..."

### 2.6. Markdown rendering (Markwon)

Support:
- Bold / italic / code inline / code block
- Bullet / numbered list
- Table (Markwon ext-tables — đã có dep)
- Link (mở ChromeCustomTab)
- H1-H3 (nhỏ hơn body, không phá flow chat)
- Blockquote (border-left 3dp `brand.accent`)

**Không** support:
- Hình ảnh inline (security)
- Iframe / HTML raw

### 2.7. Composer states

- **Empty** (placeholder "Hỏi gì đó...", Send disabled grey)
- **Filled** (Send enabled brand.primary)
- **Sending** (spinner thay Send icon, disabled composer)
- **Streaming response** (Send → Stop button red)
- **Error** (composer khôi phục text, error banner top "Mạng lỗi, thử lại")
- **Voice mode** (optional Sprint 6): tap mic → waveform animation, tap lại để stop, auto-fill text

### 2.8. Suggestion chips (sau mỗi AI response)

AI trả về kèm 2-3 follow-up question dạng chip:
```
┌─ Chips horizontal scroll ─┐
│ [Cách trị đạo ôn]         │
│ [Liều lượng kali]         │
│ [Phun thuốc bao lâu 1 lần?]│
└───────────────────────────┘
```
Tap chip = auto-send câu hỏi. Chip style: outline `border.primary` 1dp, padding 8×16, radius `xl`.

### 2.9. RAG context indicator

Khi AI có context (giá lúa real-time / knowledge base), show **subtle hint** dưới bubble:
```
┌─ AI bubble ────────────────┐
│ ...response text...        │
│                            │
│ [💎 Có tham khảo: Giá hôm nay │  ← chip xs, surface.3
│  · KB Đạo ôn]                │
│                            09:33 │
└────────────────────────────┘
```

Tap chip → expand panel show sources used (transparency).

### 2.10. States cần spec

- **Empty session** (greeting + 4 chip suggestion "Hỏi gì nhé?")
- **Single user message** (chờ AI)
- **AI streaming** (cursor + stop button)
- **Multi-turn conversation** (5-10 messages scroll)
- **Long message** (overflow scroll bubble, không truncate)
- **Code block message** (monospace + horizontal scroll)
- **Table message** (Markwon table with scroll horizontal nếu rộng)
- **Error message** (banner top + retry button trong message)
- **Offline** (banner "Cần mạng để chat. Bật để xem lịch sử cũ.")
- **API key missing** (FIX pain #47): empty state full-screen "Tính năng AI cần cấu hình. Liên hệ admin." (không snackbar crash)
- **Long-press menu** message context
- **Drawer open** (overlay 60% black)
- **Drawer empty**
- **Drawer long-press session menu**
- **Rename session dialog**
- **Delete confirm dialog**
- **IME open** (composer + last 3 messages visible, không scroll bị che)
- **Dark mode** (bubble surface darker, contrast giữ AA)

### 2.11. Performance

- Chat list dùng `LazyColumn` reverse layout (newest bottom)
- Markdown render lazy — chỉ render visible bubbles
- Image / table render cache (Markwon Image Plugin off để tránh dom XSS)
- Session list trong drawer paginated 20/lần

### 2.12. Pain point fix

| ID | Fix |
|---|---|
| #10 | Session persist (Room) + drawer history + delete/rename/regenerate menu + real SSE streaming |
| #41 | Markdown render dùng Markwon (đã có), không strip text. Table + code block support. |
| #44 | Error banner có icon alert leading + retry CTA |
| #47 | API key missing → empty state hướng dẫn, không snackbar tech |

---

## 3. Cross-cutting concerns

### 3.1. Refresh behavior
- Market: pull-to-refresh top → refetch 5 section parallel, snackbar "Đã cập nhật" 2s
- AI Chat: không pull-to-refresh (gây nhầm scroll lên history). Refresh = nút regenerate per message.

### 3.2. Offline strategy
- Market: hiện cached data, banner top "Đang offline · cập nhật 09:32"; section nào không có cache hiện empty với CTA "Thử lại"
- AI Chat: drawer hiện lịch sử cũ; chat mới disabled với banner "Cần mạng để chat"

### 3.3. Reduced motion
- Market sparkline: tắt animation draw-in, static line
- AI streaming cursor: tắt blinking, dùng static "▌"
- Drawer slide-in: dùng fade thay slide

---

## 4. Acceptance criteria

- [ ] MarketScreen có 6 state: Default, Loading skeleton, Empty, Offline, API-missing, Pull-refresh
- [ ] AiChatScreen có 8 state main + drawer 4 state + dialog 2 state
- [ ] Light + Dark cả 2 màn
- [ ] Showcase 2 locale vi + en cho Market hero card + 1 AI message dài
- [ ] Font scale 1.2× cho Market hero number + AI bubble
- [ ] IME state cho AiChat composer
- [ ] Tag fix pain points #5, #10, #15, #30, #42, #44, #47
- [ ] Prototype flow: Market scroll → tap rice price hero → expand chart full screen → back
- [ ] Prototype flow: AiChat new session → send message → streaming → long-press → regenerate
- [ ] Prototype flow: AiChat drawer open → long-press session → rename → confirm
- [ ] Markdown sample: 1 message có **bold** + bullet + table + code block + link
- [ ] Hand-off Markdown spec: list components dùng từ D1, table schema chat_sessions/messages cho dev, SSE event contract gợi ý

---

## 5. Timeline

| Tuần | Output |
|---|---|
| **1** | UX flow Market (hierarchy 1+4 section), AiChat (drawer + multi-session pattern). Validation rules. |
| **2** | Hi-fi visual Market dùng D1 token + chart tokens. AiChat composer + bubble. |
| **3** | Hi-fi AiChat full (drawer + menu + dialog + streaming). Iterate Market chart 3-tab. |
| **4** | QA + 2 locale showcase + hand-off. |
| **5** | Polish AI chat edge cases (long message, table overflow, code block). |
| **6** | Idle / hỗ trợ. |

---

## 6. References

- **Spec gốc**: section 5 (Market), section 7 (AI Chat), section 11.5 (Market UI patterns)
- **Files implementation**:
  - `app/src/main/java/com/GiaThinh/canlua/ui/screen/MarketScreen.kt`
  - `app/src/main/java/com/GiaThinh/canlua/ui/screen/AiChatScreen.kt`
  - `app/src/main/java/com/GiaThinh/canlua/data/repository/AiChatRepository.kt`
  - `app/src/main/java/com/GiaThinh/canlua/data/repository/KnowledgeBaseRepository.kt`
- **Inspiration**:
  - Market: Bloomberg mobile (hero data + collapsible sections), Apple Stocks (compact chart with tabs)
  - AI Chat: Claude mobile (clean bubble + drawer history), Perplexity (RAG source chips), ChatGPT iOS (streaming UX)
- **Lib có sẵn**: Markwon `4.6.2` + ext-tables, Vico chart, OpenRouter API (Gemini Flash free model)
