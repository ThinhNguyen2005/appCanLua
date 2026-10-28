---
kind: external_dependency
name: Google Play Services (Maps, AdMob, Google Sign-In)
slug: google-play-services
category: external_dependency
category_hints:
    - vendor_identity
    - auth_protocol
scope:
    - '**'
---

### Identity
Three Google Play Services surface areas are wired into Cân Lúa:
- **Google Maps**: API key supplied via `com.google.android.geo.API_KEY` meta-data (`MAPS_API_KEY` from `local.properties`).
- **AdMob**: App ID set to test value `ca-app-pub-3940256099942544~3347511713`; ad loading gated by `AdConsentManager` (TCF v2 compliance).
- **Google Sign-In**: handled by `auth/GoogleSignInHelper.kt` and integrated with Firebase Auth.

### Durable notes
- Deep links for shareable cards target `canluavn.web.app` and `canluavn.firebaseapp.com` under `/share`.
- AdMob App ID is a development/test ID — replace with production ID before release.
- Map API key is injected at build time via Gradle properties; do not commit real keys.