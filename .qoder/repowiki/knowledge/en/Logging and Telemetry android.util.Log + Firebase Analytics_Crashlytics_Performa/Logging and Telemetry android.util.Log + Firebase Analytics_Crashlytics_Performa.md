---
kind: logging_system
name: 'Logging and Telemetry: android.util.Log + Firebase Analytics/Crashlytics/Performance'
category: logging_system
scope:
    - '**'
source_files:
    - app/src/main/java/com/giathinh/canlua/util/AnalyticsHelper.kt
    - app/src/main/java/com/giathinh/canlua/util/PerformanceTracker.kt
    - app/src/main/java/com/giathinh/canlua/repository/AiChatRepository.kt
    - app/src/main/java/com/giathinh/canlua/auth/GoogleSignInHelper.kt
    - app/src/main/java/com/giathinh/canlua/data/location/LocationProvider.kt
    - app/src/main/java/com/giathinh/canlua/data/remote/SupabaseClient.kt
    - app/src/main/java/com/giathinh/canlua/repository/MarketRepository.kt
    - app/src/main/java/com/giathinh/canlua/repository/SupabasePriceRepository.kt
---

## What system/approach is used

The app has **no dedicated logger framework** (no Timber, no SLF4J, no custom `Logger` class). Logging is split across three independent channels:

1. **`android.util.Log`** — raw debug/error traces scattered in a handful of data-layer classes.
2. **Firebase Analytics** — business events via a single wrapper `AnalyticsHelper`.
3. **Firebase Crashlytics** — non-fatal exceptions and breadcrumb logs from repositories.
4. **Firebase Performance Monitoring** — screen/load traces and frame metrics via `PerformanceTracker`.

There is no centralized log-level configuration, no structured JSON formatter, and no unified sink. Each channel is initialized and consumed independently.

## Key files and packages

- `app/src/main/java/com/giathinh/canlua/util/AnalyticsHelper.kt` — singleton wrapper around `FirebaseAnalytics` + `FirebaseCrashlytics`; defines all business event names and user properties.
- `app/src/main/java/com/giathinh/canlua/util/PerformanceTracker.kt` — singleton wrapping `FirebasePerformance` + AndroidX `FrameMetricsAggregator`; measures screen lifecycle, first-render time, slow/frozen frames.
- `app/src/main/java/com/giathinh/canlua/repository/AiChatRepository.kt` — direct `FirebaseCrashlytics.log()` / `recordException()` calls for AI API errors.
- `app/src/main/java/com/giathinh/canlua/auth/GoogleSignInHelper.kt` — `Log.d` usage.
- `app/src/main/java/com/giathinh/canlua/data/location/LocationProvider.kt` — `Log.d` usage.
- `app/src/main/java/com/giathinh/canlua/data/remote/SupabaseClient.kt` — `Log.d` usage.
- `app/src/main/java/com/giathinh/canlua/repository/MarketRepository.kt` — `Log.d` usage.
- `app/src/main/java/com/giathinh/canlua/repository/SupabasePriceRepository.kt` — `Log.d` usage.

## Architecture and conventions

### Business telemetry (`AnalyticsHelper`)

- Single Kotlin `object` exposing semantic methods (`cardCreated`, `premiumPurchased`, `syncSuccess`, `syncFailed`, …) that wrap `analytics.logEvent(eventName, params)`.
- Event names and param names are documented as `snake_case` (Firebase requirement, max 40 chars).
- PII is explicitly excluded: comment states "Không log PII (số CCCD, SĐT, email user) — chỉ uid hash + business metric"; only hashed uid and business metrics are sent.
- User identity and premium state are set via `setUser(uid, role)` and `setPremium(isPremium, plan)`, which write both `FirebaseAnalytics.setUserProperty` and Crashlytics `setCustomKey`.
- Non-fatal errors go through `logNonFatal(throwable, tag)` → `crashlytics.recordException`.
- Debug breadcrumbs use `breadcrumb(message)` → `crashlytics.log`.
- Initialization requires an explicit `init(context)` call from `Application.onCreate` because `FirebaseAnalytics.getInstance` needs a `Context`.

### Performance monitoring (`PerformanceTracker`)

- Tracks per-screen `FirebasePerformance.Trace` named `screen_<route>` and load traces named `load_<route>`.
- Route names are sanitized by `sanitizeRouteName`: strips non-alphanumeric chars, truncates dynamic segments like `_cardId_`, caps at 80 chars.
- Frame metrics come from `androidx.core.app.FrameMetricsAggregator.TOTAL_DURATION`; slow frames > 16 ms, frozen > 700 ms.
- Heavy aggregation runs on `Dispatchers.Default` off the main thread; `stopFrameTracking` resets the aggregator reference before calling `stop()` to avoid stuck state.
- A Composable helper `TrackScreenRender(screenRoute)` stops the load trace when the first composition completes.

### Direct `android.util.Log` usage

Scattered in low-level data classes (`GoogleSignInHelper`, `LocationProvider`, `SupabaseClient`, `MarketRepository`, `SupabasePriceRepository`, `PerformanceTracker`). Uses `Log.d` / `Log.e` with a local `TAG` constant where present. No log-level filtering or formatting beyond the default Android logcat format.

### Crashlytics exception reporting

- `AiChatRepository.callOnce` catches `TimeoutCancellationException`, `HttpException`, `IOException`, and generic `Exception`, each writing a breadcrumb via `crashlytics.log("AI[$context] ...")` and recording the original throwable via `recordException` for unexpected cases.
- Error messages exposed to users are localized Vietnamese strings; the raw error context is only sent to Crashlytics.

## Conventions and constraints

- **No unified logger abstraction**: there is no central `Logger` interface or facade; callers reach directly into `android.util.Log` or Firebase SDKs.
- **Analytics event naming convention** (documented in `AnalyticsHelper` KDoc): event names and parameter names must be `snake_case`, ≤ 40 characters, and must not contain PII (CCCD number, phone, email); only hashed uid and business metrics are allowed.
- **Crashlytics opt-out behavior**: the `AnalyticsHelper` comment documents that Crashlytics collection is auto-disabled on debug builds (Firebase BoM default) and enabled in production, overridable via `FirebaseCrashlytics.setCrashlyticsCollectionEnabled`.
- **Performance trace naming**: route names passed to `PerformanceTracker` are sanitized to `[a-zA-Z0-9_]`, truncated to 80 chars, and dynamic path segments like card IDs are collapsed to `_detail` so Firebase Performance accepts them.
- **Frame-metric thresholds**: slow frames are defined as > 16 ms, frozen frames as > 700 ms (hardcoded in `PerformanceTracker.stopScreenTrace`).
- **Error handling pattern in repositories**: caught exceptions are logged to Crashlytics breadcrumbs and mapped to user-facing Vietnamese `RuntimeException` messages rather than propagated to the UI layer.
- **BuildConfig-guarded verbose logging**: `PerformanceTracker` wraps `Log.e` calls in `if (BuildConfig.DEBUG)` checks, so error traces are stripped from release builds.