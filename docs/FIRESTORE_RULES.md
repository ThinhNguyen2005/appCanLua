# Firestore Security Rules — Cân Lúa

Tài liệu này list rule tối thiểu cần deploy lên Firebase Console (Firestore → Rules) trước khi production. Hiện tại app dùng Firestore default rules nên dữ liệu user **đang public** — phải cài rule trước khi mở rộng beta.

## Cài đặt

Copy block dưới vào Firestore Rules tab và Publish.

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // ============ Helpers ============
    function isSignedIn() {
      return request.auth != null;
    }

    function isOwner(uid) {
      return isSignedIn() && request.auth.uid == uid;
    }

    function isAdmin() {
      return isSignedIn() && request.auth.token.admin == true;
    }

    // ============ Cards ============
    // Owner đọc/sửa/xoá phiếu của mình.
    // Trader đã verify QR → có thể đọc + lock (lockedByTraderId == uid).
    match /cards/{cardId} {
      allow read: if isSignedIn() && (
        resource.data.userId == request.auth.uid ||
        resource.data.lockedByTraderId == request.auth.uid
      );

      allow create: if isSignedIn() && request.resource.data.userId == request.auth.uid;

      // Owner update bất kỳ field nào.
      // Trader chỉ update lockedByTraderId + isLocked + syncTimestamp (QR handshake).
      allow update: if isSignedIn() && (
        resource.data.userId == request.auth.uid ||
        (
          request.resource.data.lockedByTraderId == request.auth.uid &&
          request.resource.data.diff(resource.data).affectedKeys()
            .hasOnly(['lockedByTraderId', 'isLocked', 'syncTimestamp'])
        )
      );

      allow delete: if isSignedIn() && resource.data.userId == request.auth.uid;
    }

    // ============ Weight Entries ============
    match /weightEntries/{entryId} {
      allow read, write: if isSignedIn() && (
        resource == null ||
        resource.data.userId == request.auth.uid ||
        request.resource.data.userId == request.auth.uid
      );
    }

    // ============ Transactions ============
    match /transactions/{txId} {
      allow read, write: if isSignedIn() && (
        resource == null ||
        resource.data.userId == request.auth.uid ||
        request.resource.data.userId == request.auth.uid
      );
    }

    // ============ Role Requests ============
    // User submit yêu cầu nâng cấp TRADER, đọc trạng thái của mình.
    // Admin đọc tất cả + duyệt (đổi status APPROVED/REJECTED).
    match /roleRequests/{uid} {
      allow read: if isOwner(uid) || isAdmin();
      allow create: if isOwner(uid)
        && request.resource.data.status == "PENDING"
        && request.resource.data.uid == uid;
      // User KHÔNG được tự đổi status → chỉ admin duyệt.
      allow update: if isAdmin();
      allow delete: if isAdmin();
    }

    // ============ Rice Prices (Trader bids) ============
    // Tất cả user đọc; chỉ trader đăng được.
    match /ricePrices/{priceId} {
      allow read: if isSignedIn();
      allow create, update, delete: if isSignedIn() &&
        request.resource.data.traderId == request.auth.uid;
    }
  }
}
```

## Cấp quyền admin

Custom claim `admin: true` cần set qua Cloud Function hoặc gcloud CLI:

```bash
firebase functions:shell
> admin.auth().setCustomUserClaims('UID_CỦA_ADMIN', { admin: true })
```

User cần sign-out + sign-in lại để token refresh claim.

## Approve role flow (manual MVP)

Sau khi rules deploy:

1. User submit form → doc `roleRequests/{uid}` tạo với `status: PENDING`.
2. Admin xem Firebase Console → Firestore → `roleRequests` collection.
3. Admin verify business info, click doc → đổi field `status` → `APPROVED` + thêm `reviewedAt: <timestamp>` + `reviewerNote: "Đã verify"`.
4. App của user đang chạy: `CanLuaApplication.scheduleAutoApplyRoleApproval()` observe `roleRequests/{uid}` → phát hiện APPROVED → tự update local `Profile.role = "TRADER"` → MainScreen reactive đổi nav graph sang trader.
5. Nếu user offline lúc admin duyệt → lần next online sẽ trigger.

## Roadmap automation

Phase sau: viết Cloud Function `onUpdate` của `roleRequests` — khi status đổi sang APPROVED tự động:

- Set custom claim `role: "TRADER"` cho user qua `admin.auth().setCustomUserClaims(uid, ...)`.
- Update `profiles/{uid}.role = "TRADER"` (server-authoritative, không phụ thuộc app gọi `saveProfile`).
- Send FCM push notification "Yêu cầu trở thành thương lái đã được duyệt".
