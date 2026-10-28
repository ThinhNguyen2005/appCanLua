---
kind: error_handling
name: 'Error Handling: Result<T>, Domain Exceptions, and AppToast Feedback'
category: error_handling
scope:
    - '**'
source_files:
    - app/src/main/java/com/giathinh/canlua/data/remote/HttpClient.kt
    - app/src/main/java/com/giathinh/canlua/data/remote/SupabaseClient.kt
    - app/src/main/java/com/giathinh/canlua/repository/AuthManager.kt
    - app/src/main/java/com/giathinh/canlua/repository/FirestoreRepository.kt
    - app/src/main/java/com/giathinh/canlua/repository/AiChatRepository.kt
    - app/src/main/java/com/giathinh/canlua/data/location/LocationProvider.kt
    - app/src/main/java/com/giathinh/canlua/ui/feedback/AppToast.kt
---

## Approach

The codebase uses a layered error model with no global exception middleware:

1. **Repository layer** returns `kotlin.Result<T>` — every suspend API in `AuthManager`, `FirestoreRepository`, `AiChatRepository`, `FeedbackRepository`, `MarketRepository` wraps its body in `try { Result.success(...) } catch (e: Exception) { Result.failure(e) }`. This is the primary contract between repository and ViewModel.
2. **Remote/data layer** throws domain-specific checked exceptions that extend `RuntimeException`: `HttpException(code, errorBody)` from `HttpClient.kt` and `SupabaseException(code, errorBody)` from `SupabaseClient.kt`. These are thrown on non-2xx HTTP responses or JSON parse failures.
3. **UI layer** surfaces errors through `AppToast` (`ui/feedback/AppToast.kt`) — a Compose pill snackbar with four semantic variants: `SUCCESS`, `INFO`, `WARNING`, `ERROR`. It is exposed via a `CompositionLocal<AppToastController>` so any screen can call `appToast.error(...)` / `.warning(...)` / `.success(...)` / `.info(...)` without passing state down.
4. **Silent-failure pattern**: some operations intentionally swallow errors and return `null` or an empty collection rather than propagating them — e.g. `LocationProvider.getCurrentLocation()` and `reverseGeocode()` log via `Log.w` and return `null`; `SupabaseClient.getList()` catches `JsonSyntaxException` and returns `emptyList()`; `RssFetcher` comments say "Bỏ qua bài lỗi parse — không throw để 1 item hỏng không phá toàn bộ feed".
5. **Crash reporting**: `FirebaseCrashlytics` is accessed lazily via `runCatching { FirebaseCrashlytics.getInstance() }.getOrNull()` so crashlytics initialization failure does not break normal flows. Errors are logged with contextual tags like `AI[$context] http_${e.code}: ...`.

## Key Files

- `data/remote/HttpClient.kt` — defines `HttpException`, throws on non-2xx and JSON parse errors.
- `data/remote/SupabaseClient.kt` — defines `SupabaseException`, logs + throws on failures.
- `repository/AuthManager.kt` — wraps FirebaseAuth calls in `Result<FirebaseUser>`.
- `repository/FirestoreRepository.kt` — all Firestore ops return `Result<Unit>` / `Result<List<T>>` / `Result<String>`.
- `repository/AiChatRepository.kt` — maps `TimeoutCancellationException`, `HttpException`, `IOException` to user-friendly Vietnamese messages; reports to Crashlytics.
- `data/location/LocationProvider.kt` — silent-fallback pattern (returns `null`, logs).
- `ui/feedback/AppToast.kt` — Compose toast host + controller with `AppToastVariant` enum.

## Architecture & Conventions

- **No sealed-error hierarchy**: each subsystem defines its own small exception class (`HttpException`, `SupabaseException`). There is no shared base error type beyond `RuntimeException`.
- **Repository boundary**: repositories never let raw exceptions escape to ViewModels; they convert everything to `Result.failure(exception)`. ViewModels then decide whether to surface it as UI feedback or treat it as a transient condition.
- **User-facing messages are localized at the source**: `AiChatRepository.callOnce` translates HTTP status codes into Vietnamese strings like "Không kết nối được AI (quá thời gian phản hồi). Bà con thử lại sau." — the repository owns the UX copy for its domain.
- **Haptics tied to severity**: `AppToast.triggerHaptic` maps `SUCCESS` → `LongPress`, `ERROR`/`WARNING` → `TextHandleMove` (lighter vibration), per the comment explaining the rationale.
- **Accessibility**: `AppToast` sets `role = Role.Button`, `contentDescription` including dismissal hint, and `liveRegion = LiveRegionMode.Polite` so TalkBack announces without interrupting speech.
- **No `panic`/`recover` equivalent**: Kotlin coroutines use structured concurrency; uncaught exceptions bubble up to the coroutine scope. The app relies on `runCatching` around optional features (Crashlytics, haptics) rather than global handlers.

## Conventions Observed

- Every suspend repository method follows the shape: `suspend fun x(): Result<T> = withContext(Dispatchers.IO) { try { ... Result.success(...) } catch (e: Exception) { Result.failure(e) } }`.
- Network clients throw typed exceptions (`HttpException`, `SupabaseException`) carrying both the HTTP code and a truncated error body (`raw.take(500)` / `body.take(200)`).
- Non-critical I/O (location, geocoding, RSS parsing) prefers returning `null` over throwing, logging warnings via `Log.w(TAG, ...)`.
- Crashlytics access is always wrapped in `runCatching { ... }.getOrNull()` to avoid crashing the app if Crashlytics is unavailable.
- User-visible error text is written in Vietnamese targeting the farmer audience ("bà con"), not technical stack traces.