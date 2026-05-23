# Cân Lúa Redesign — Designer Briefs Index

**Phiên bản app**: master @ ec712c4 (2026-05-24)
**Spec gốc**: [../2026-05-24-design-handoff.md](../2026-05-24-design-handoff.md)
**Số designer**: 6 (làm song song)
**Style direction pin**: "Editorial nông nghiệp" (xem section 13.1 của spec gốc)
**Tool**: tool-agnostic (Figma/Sketch/XD đều dùng được — brief tập trung vào requirements + AC, không gắn syntax tool)

---

## Tổng quan project

Cân Lúa là app Android Jetpack Compose số hoá quy trình cân lúa giữa **nông dân (FARMER)** và **thương lái (TRADER)** ở đồng bằng sông Cửu Long. Hiện có 27 màn hình, 4 tab (5 cho TRADER), offline-first với Room + Firestore sync. Mục tiêu redesign: rút gọn information architecture, thống nhất 1 design system, fix 47 pain points (đặc biệt 5 CRITICAL), và tạo brand identity "Editorial nông nghiệp" rõ ràng.

**Đọc trước khi bắt đầu**:
1. Spec gốc — section 1 (Brief), section 2 (Personas), section 11 (Design system hiện tại), section 13 (Direction)
2. Brief riêng của bạn trong thư mục này

---

## Phân công 6 designer

| # | Designer | Phạm vi | File brief | Phụ thuộc |
|---|---|---|---|---|
| **D1** | Foundation | Design tokens, component library atoms+molecules, dark mode base, icon set, illustration style, design system docs | [designer-1-foundation.md](designer-1-foundation.md) | Không phụ thuộc ai. **Bắt buộc xong tuần 1-2 trước khi D2-D6 deep work**. |
| **D2** | Auth & Onboarding | AuthScreen, ProfileSetupScreen, RoleRequestScreen + splash transition | [designer-2-auth-onboarding.md](designer-2-auth-onboarding.md) | D1 (tokens, button, input, dialog) |
| **D3** | Scale tab (FARMER core) | CardListScreen, CardDetailScreen, WeightInputScreen, DeletedCardsScreen | [designer-3-scale-farmer-core.md](designer-3-scale-farmer-core.md) | D1 (card, FAB, chip, sheet, badge) |
| **D4** | Market & AI Chat | MarketScreen (5 section), AiChatScreen (drawer + bubble + markdown) | [designer-4-market-aichat.md](designer-4-market-aichat.md) | D1 (chip, chart tokens, sheet, list item) |
| **D5** | Account & Trader-only | FarmerProfileScreen, TraderProfileScreen, TraderTransactionsScreen, TraderHistoryScreen, RiceMapScreen, TraderComingSoonScreen | [designer-5-account-trader.md](designer-5-account-trader.md) | D1 (avatar, transaction row, map tokens) |
| **D6** | Sub-screens & Motion | SettingsScreen, PremiumScreen, QrGenerateScreen, QrScanScreen, SyncStatusScreen + motion spec toàn app | [designer-6-sub-screens-motion.md](designer-6-sub-screens-motion.md) | D1 (sheet, dialog, status, motion tokens) |

### Hand-off matrix (ai cần gì từ ai)

| Cần từ ↓ \ Cho → | D1 | D2 | D3 | D4 | D5 | D6 |
|---|---|---|---|---|---|---|
| **D1 (Foundation)** | — | Tokens, button, input, social button, OTP field | Tokens, card, FAB, chip, sheet | Tokens, chart tokens, chip, list item | Tokens, avatar, list row | Tokens, sheet, dialog, motion specs |
| **D2 (Auth)** | Feedback về Field component | — | — | — | — | — |
| **D3 (Scale)** | Feedback về Card, FAB | — | — | — | — | — |
| **D4 (Market/AI)** | Feedback về Chip, Chart | — | — | — | — | — |
| **D5 (Profile/Trader)** | Feedback về Avatar, Map tokens | — | — | — | — | — |
| **D6 (Sub-screens)** | Feedback về Sheet, Dialog | — | — | — | — | — |

**Luật**: D1 ship draft tokens + atoms tuần 1, D2-D6 bắt đầu wireframe & flow tuần 1-2 nhưng visual hi-fi phải đợi D1 final tuần 2.

---

## Style direction tóm tắt (pin cho cả 6 designer)

> "Editorial nông nghiệp" — typography mạnh kiểu báo, lưới editorial breaking, palette xanh-vàng-đất, ảnh thật, ít animation phù phiếm, đáng tin cậy như sổ kế toán mini.

**Có**:
- Typography đặc trưng (sans-serif weight 700-800 cho display, 600 headline, body super-readable hỗ trợ đầy đủ dấu tiếng Việt)
- Palette "Lúa xanh" (#1F6B27) + "Lúa chín" (#F59E0B) + đất + neutral ấm
- Hero data point per screen (1 focal point, không 4 card cùng cấp)
- Surface tone contrast thay vì card-in-card
- Real photography texture (ruộng, lúa, máy gặt)
- Number-first cho mọi screen tài chính (tổng KG, doanh thu, công nợ)
- Touch ≥ 48dp (≥ 56dp cho action chính)
- Contrast AA mọi text; AAA cho số liệu tiền

**Không**:
- Glassmorphism, neon gradient, dark mode mặc định
- "Smart", "AI-powered" buzzword visual
- Cute/playful, mascot trẻ con
- Bo tròn quá mức (>32dp) trừ pill nav
- Gradient kim loại, holographic
- Generic Material defaults (Material You wallpaper override)

---

## Constraints chung (đọc 1 lần, áp dụng tất cả)

### Bắt buộc
1. **Touch target** ≥ 48dp; action chính ("Cân lúa", "Tạo phiếu", "Đồng bộ", "Đăng nhập") ≥ 56dp
2. **Contrast WCAG**: AA cho text thường, AAA cho số liệu tài chính (totalAmount, remaining, revenue)
3. **Font scale 0.9× → 1.2×** không vỡ layout — test trên mọi screen ở 1.2×
4. **5 locale** vi / en / km (Khmer) / lo (Lào) / zh-CN. Không hardcode text, không width fixed cho label
5. **Reduced motion** (System setting) phải tắt được pulse + infinite animation
6. **Dark mode** đầy đủ — không phải afterthought
7. **Color-blind safe**: Success/Error/Warning không chỉ dựa màu, luôn kèm icon hoặc label
8. **Offline state** là first-class — mọi screen có data từ server phải có variant offline thiện cảm

### Phạm vi viewport
- Min: 360 × 640 dp (small Android phone)
- Default: 412 × 915 dp (Pixel-class)
- Large: 480 × 1024 dp (phablet)
- Foldable / tablet: **không required** ở Phase 1 (note as future-work)

### Không gian an toàn
- Status bar: 24-32dp (per device)
- Navigation bar: 24-48dp (gesture vs button)
- Notch / cutout: respect insets
- IME keyboard: Compose sẽ `imePadding()` — designer cần show 2 state (keyboard up vs down) cho mọi form

### Brand identity bất khả xâm phạm
- Logo Eco icon — đã có, có thể refine, không bỏ
- Primary color anchor — phải là 1 sắc xanh lúa (`#1F6B27` đề xuất, có thể adjust ±5% lightness)
- Accent color anchor — phải là 1 sắc vàng lúa chín ấm
- Không dùng đỏ làm primary (đỏ chỉ dùng cho Error, công nợ chưa trả)

---

## Shared deliverables (D1 lead, mọi designer dùng)

Sau khi D1 ship, các designer khác kế thừa:

1. **Token file** (JSON / Figma Tokens / Style Dictionary) — color, typography, spacing, radius, elevation, motion
2. **Atom library** — Button (5 variants × 4 state), Input (4 variants × 4 state), Chip, Badge, Avatar, Icon, Divider
3. **Molecule library** — Card (3 variants), List row, Status indicator, Empty state, Skeleton, Banner, Dialog, Sheet, FAB
4. **Theme variants** — Light + Dark, đầy đủ token coverage
5. **Icon set** — 80+ icon đã inventory (xem brief D1)
6. **Illustration style guide** — empty state, error, splash, premium upsell, role variants
7. **Photography direction** — moodboard với 30+ ảnh thực ruộng/lúa được chấp nhận, 10 ảnh bị reject (counter-example)

---

## Acceptance criteria toàn project (gate to ship)

Mỗi designer phải đảm bảo deliverable của mình đạt:

- [ ] Mọi screen có **3 state**: Loading (skeleton) + Empty + Error/Offline + Default
- [ ] Mọi form có **2 state IME**: keyboard up + keyboard down
- [ ] Mọi screen có **2 theme**: Light + Dark
- [ ] Mọi text dùng token typography, **không hardcode font size**
- [ ] Mọi màu dùng token, **không hardcode hex** (trừ illustration)
- [ ] Mọi corner radius dùng token (xs/sm/md/lg/xl)
- [ ] Mọi touch target ≥ 48dp, action chính ≥ 56dp
- [ ] Layout không vỡ ở **font scale 1.2×** (showcase trên 1 screen tiêu biểu)
- [ ] Showcase **2 locale** vi + en cho 1 screen có text dài nhất của mình
- [ ] **Motion spec**: mọi animation custom phải kèm duration + easing + property
- [ ] **Pain point map**: tag mỗi pain point từ spec (ID 1-47) được fix bởi screen nào trong scope của mình
- [ ] **Acceptance criteria riêng** từng brief đều pass (xem từng file)

---

## Timeline tổng (gợi ý)

| Tuần | D1 Foundation | D2 Auth | D3 Scale | D4 Market/AI | D5 Profile/Trader | D6 Sub/Motion |
|---|---|---|---|---|---|---|
| **1** | Token draft + atom WIP | UX flow + wireframe | UX flow + wireframe | UX flow + wireframe | UX flow + wireframe | UX flow + motion principles |
| **2** | Token final + atom + molecule | Hi-fi visual | Hi-fi visual (FARMER) | Hi-fi visual (Market) | Hi-fi visual (Farmer Profile) | Hi-fi visual (Settings, Premium) |
| **3** | Dark mode + icon + illustration | Prototype + 2 locale | Hi-fi visual (Detail, WeightInput) | Hi-fi visual (AI Chat) | Hi-fi visual (Trader Profile, Map) | Hi-fi visual (QR, Sync) |
| **4** | QA hỗ trợ 5 designer | QA + hand-off | QA + hand-off | QA + hand-off | QA + hand-off | Motion spec + Prototype global |
| **5** | Polish + design system docs | (done) | Polish + edge cases | Polish + AI Chat states | Polish + Trader edge cases | Polish + motion library |
| **6** | **Final hand-off**: token v1.0, library, dark mode complete, illustration set, prototype global, animation library | | | | | |

**Tổng**: 6 tuần. Designer tuần đầu chạy độc lập (UX/flow), bắt đầu hi-fi tuần 2 khi D1 có token draft.

---

## Files trong thư mục này

- [README.md](README.md) ← bạn đang đọc, đọc đầu tiên
- [designer-1-foundation.md](designer-1-foundation.md)
- [designer-2-auth-onboarding.md](designer-2-auth-onboarding.md)
- [designer-3-scale-farmer-core.md](designer-3-scale-farmer-core.md)
- [designer-4-market-aichat.md](designer-4-market-aichat.md)
- [designer-5-account-trader.md](designer-5-account-trader.md)
- [designer-6-sub-screens-motion.md](designer-6-sub-screens-motion.md)

---

## Liên lạc & process

- **Design lead** (nếu có): điền tên + Slack
- **Tech lead** (dev side): điền tên + Slack
- **Daily sync**: 15p sáng, 6 designer + tech lead
- **Weekly review**: thứ Sáu, demo 1 screen tiêu biểu mỗi designer
- **Token freeze**: cuối tuần 2 — sau tuần 2 mọi thay đổi token cần lead approval
- **Hand-off format**: Figma frame + spec file Markdown, ảnh export 3× cho icon/illustration, JSON cho token

---

**Bắt đầu**: D1 đọc spec gốc section 11 + 13 trước, rồi mở brief riêng. Các designer khác đọc brief riêng + 2 section trên rồi bắt đầu UX flow trong khi đợi token draft.
