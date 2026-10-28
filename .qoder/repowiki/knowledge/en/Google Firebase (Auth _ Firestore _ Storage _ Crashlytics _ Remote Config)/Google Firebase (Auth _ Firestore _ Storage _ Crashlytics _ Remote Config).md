---
kind: external_dependency
name: Google Firebase (Auth / Firestore / Storage / Crashlytics / Remote Config)
slug: google-firebase
category: external_dependency
category_hints:
    - vendor_identity
    - auth_protocol
scope:
    - '**'
---

### Identity
Google Firebase platform used for four distinct services in Cân Lúa.

### Role & integration points

### Durable usage notes
- Firestore is the canonical remote source of truth; Room is the offline cache. Sync direction and idempotency live in `SyncManager`/`SyncWorker`, not in individual repositories.
- Crashlytics calls are wrapped in `runCatching` so a missing Crashlytics SDK does not crash cold start.
- Verify exact Remote Config keys against the Firebase console before changing feature flags.