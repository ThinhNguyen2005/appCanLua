# Walkthrough — Tab Tài Khoản, Lịch Sử Thương Lái, Map Address & Fix Crash

## Tổng quan

4 việc đã làm trong session này:

| # | Vấn đề | Kết quả |
|---|--------|---------|
| 1 | Crash khi đăng nhập Google (`GetCredentialCancellationException` / "[16] Account reauth failed") | App không còn văng — hiển thị toast nhẹ "Bạn đã huỷ đăng nhập Google." |
| 2 | Map thương lái không hiện địa chỉ ruộng + SĐT | Bottom sheet trên `RiceMapScreen` giờ có 2 row `ContactInfoRow` cho địa chỉ và SĐT |
| 3 | Farmer chưa có tab "Tài khoản" để chỉnh thông tin / đổi vai trò | Thêm tab "Tài khoản" trong farmer nav (route `account` → `FarmerProfileScreen`) |
| 4 | Chưa có lịch sử thương lái đã mua ruộng | Aggregate query `getTraderHistory()` từ bảng `cards`, hiển thị reactive trong tab Tài khoản |

> [!NOTE]
> Không cần migrate DB — query tận dụng các cột đã có (`traderName`, `traderPhone`, `totalAmount`, `netWeight`, `date`).

## Files thay đổi

### Auth
- [GoogleSignInHelper.kt](file:///d:/appCanLua/app/src/main/java/com/GiaThinh/canlua/auth/GoogleSignInHelper.kt) — bắt riêng `GetCredentialCancellationException` ở cả `requestIdToken` và `trySilent`; mở rộng `humanizeError` cho code `[16]`.

### Map (Trader)
- [RiceMapScreen.kt](file:///d:/appCanLua/app/src/main/java/com/GiaThinh/canlua/ui/screen/map/RiceMapScreen.kt) — thêm `ContactInfoRow` reusable, hiển thị `fieldAddress` + `traderPhone` trong bottom sheet.

### Profile / Account Tab
- [Profile.kt](file:///d:/appCanLua/app/src/main/java/com/GiaThinh/canlua/data/model/Profile.kt) — không đổi schema.
- [SeasonStats.kt](file:///d:/appCanLua/app/src/main/java/com/GiaThinh/canlua/data/model/SeasonStats.kt) — thêm `data class TraderHistoryItem`.
- [CardDao.kt](file:///d:/appCanLua/app/src/main/java/com/GiaThinh/canlua/data/dao/CardDao.kt) — thêm query `getTraderHistory()` group theo (traderName, traderPhone).
- [CardRepository.kt](file:///d:/appCanLua/app/src/main/java/com/GiaThinh/canlua/repository/CardRepository.kt) — forward `getTraderHistory()`.
- [ProfileViewModel.kt](file:///d:/appCanLua/app/src/main/java/com/GiaThinh/canlua/ui/viewmodel/ProfileViewModel.kt) — inject `CardRepository`, expose `traderHistory: StateFlow`, thêm `updateProfile()` cho inline edit.
- [FarmerProfileScreen.kt](file:///d:/appCanLua/app/src/main/java/com/GiaThinh/canlua/ui/screen/FarmerProfileScreen.kt) **[NEW]** — màn hình mới, gồm header avatar/role badge, info card inline-edit, role switcher có confirm dialog, list trader history, button đăng xuất.

### Navigation
- [BottomNavItem.kt](file:///d:/appCanLua/app/src/main/java/com/GiaThinh/canlua/ui/navigation/BottomNavItem.kt) — thêm `ACCOUNT` BottomNavItem; `farmerNavItems` giờ có 5 tab.
- [AppNavHost.kt](file:///d:/appCanLua/app/src/main/java/com/GiaThinh/canlua/ui/navigation/AppNavHost.kt) — wire route `"account"` → `FarmerProfileScreen()`.

## Quyết định thiết kế

**1. Lịch sử thương lái = aggregate query, không phải bảng riêng.**
Mỗi `Card` đã có `traderName + traderPhone + totalAmount + netWeight + date`, đủ để rollup. Tạo bảng riêng `trader_history` sẽ gây duplicate data và phải đồng bộ trên 2 nguồn → bỏ.

**2. Group theo `(traderName, traderPhone)`** thay vì chỉ tên — vì 2 thương lái có thể trùng tên ("Anh Tâm"). `COALESCE(traderPhone, '')` để cards cũ chưa có SDT vẫn group được.

**3. Đổi vai trò là phép toán "lành" — vẫn confirm.**
Đổi role chỉ là update 1 row Profile, nhưng nó re-route toàn bộ nav graph (`MainScreen.isTrader`). Nếu user bấm nhầm sẽ thấy app "biến hình", nên mình bắt qua `AlertDialog` confirm với mô tả rõ "sẽ chuyển sang giao diện THƯƠNG LÁI".

**4. Crash đăng nhập Google.**
`GetCredentialCancellationException` là subclass của `GetCredentialException`. Catch riêng nó **trước** generic catch để:
- Không log `Log.e` (tránh spam Crashlytics với happy-path "user huỷ").
- Trong `trySilent()`, ném tiếp lên caller chứ không fallback explicit (vì user vừa đóng dialog xong, fallback sẽ mở thêm dialog → spam).

## Build & Verify

```
> Task :app:compileDebugKotlin
BUILD SUCCESSFUL in 37s
Exit code: 0
```

Đã chạy `compileDebugKotlin` và `assembleDebug` — cả hai pass clean.

## Manual test cần làm

> [!IMPORTANT]
> Vì không deploy được tự động lên thiết bị, vui lòng test các flow sau:

- [ ] Đăng xuất → mở app → bấm "Đăng nhập Google" → đóng dialog ngay → kỳ vọng: thấy toast/error nhẹ, **không crash**.
- [ ] Tạo phiếu cân với địa chỉ + SĐT thương lái → vào tab "Nguồn cung" (trader) → tap pin → kỳ vọng: bottom sheet hiện địa chỉ ruộng và SĐT.
- [ ] Vào tab "Tài khoản" mới (farmer) → bấm icon edit → đổi tên/SĐT → bấm save → reload app → kỳ vọng: data được lưu.
- [ ] Bấm chip "Thương lái" → confirm → kỳ vọng: bottom nav chuyển ngay sang nav graph thương lái (Rao mua / Sổ giao dịch / Nguồn cung / Cá nhân).
- [ ] Tạo vài card với tên thương lái khác nhau → kỳ vọng: tab Tài khoản → "Lịch sử thương lái" cập nhật real-time, đúng số lần và tổng tiền.

## Open items

- `FarmerProfileScreen` chưa có chức năng tap vào 1 trader để xem chi tiết các card đã giao dịch với họ. Cần discuss với user xem có cần feature này không (sẽ cần thêm 1 detail screen + DAO query filter by traderName).
- Trader cũng có nhu cầu chỉnh tên/SĐT, hiện `TraderProfileScreen` chỉ là read-only. Có thể refactor để chia sẻ UI với `FarmerProfileScreen` ở phần info card.
