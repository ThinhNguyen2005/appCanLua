# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Cân Lúa — Android app (Kotlin/Jetpack Compose) for digitizing field rice weighing between farmers and traders. Offline-first with Room → Firestore sync. Module `:app` is the only Gradle module; web landing page lives in `public/` (deployed via Firebase Hosting to `canluavn.web.app`).

- `namespace` / `applicationId`: `com.GiaThinh.canlua`
- `minSdk` 24, `compileSdk` / `targetSdk` 36, JVM target 11
- Kotlin 2.2.10, AGP 9.1.1, Compose BOM 2025.09.01, Room 2.8.1, Hilt 2.57.2

## Build & run commands

PowerShell (Windows-first toolchain):

```powershell
.\gradlew.bat :app:compileDebugKotlin       # fast typecheck
.\gradlew.bat :app:assembleDebug            # debug APK
.\gradlew.bat :app:assembleRelease          # R8 minify + resource shrink
.\gradlew.bat :app:installDebug             # install on connected device
.\gradlew.bat :app:test                     # JVM unit tests
.\gradlew.bat :app:connectedAndroidTest     # instrumented tests (device/emulator required)
.\gradlew.bat :app:lint                     # Android lint
```

Run a single unit test: `.\gradlew.bat :app:testDebugUnitTest --tests "com.GiaThinh.canlua.SomeTest.someMethod"`.

### Required local config (not committed)

Create `local.properties` at repo root before building (app builds with empty values but Weather/AI/Map features degrade silently):

```properties
sdk.dir=...
OPENWEATHER_API_KEY=...
OPENROUTER_API_KEY=...
MAPS_API_KEY=...
```

These flow through `app/build.gradle.kts` into `BuildConfig` fields and into `AndroidManifest.xml` via `manifestPlaceholders["MAPS_API_KEY"]`. Drop `google-services.json` into `app/` for Firebase (Auth/Firestore/Crashlytics/Perf/Storage).

### Web landing page

`public/` is deployed via `firebase deploy --only hosting`. `public/.well-known/assetlinks.json` is required for Android App Links auto-verification of `https://canluavn.web.app/share/{cardId}` deep links wired in `MainActivity.kt`.

## Architecture

MVVM + Clean Architecture, single-Activity Compose app. Dependency flow: `UI (Compose) → ViewModel (StateFlow) → Repository → Data Source (Room + Firestore + OkHttp/REST)`. Hilt wires everything; see `di/DatabaseModule.kt` and `di/FirebaseModule.kt`.

### Two NavHosts, not one

`MainActivity` owns the **root** NavHost with routes `splash | login | profile_setup | role_request | main?cardId={cardId}`. `AuthViewModel.uiState` drives navigation via `LaunchedEffect`; never hard-code post-auth destinations. The deep-link pattern `https://canluavn.web.app/share/{cardId}` resolves into `main?cardId=...` and `AppNavHost` forwards to `card_detail/{cardId}`.

`MainScreen` mounts the **app** `AppNavHost` (`ui/navigation/AppNavHost.kt`) with 4 tabs (`SCALE / MARKET / AI_CHAT / ACCOUNT`) plus shared sub-routes (`card_detail`, `weight_input`, `qr_*`, `trader_*`, `premium`). FARMER and TRADER share tab routes — each screen reads `profile.role` to render its variant. Default screen transitions are `FadeScale*` from `NavTransitions.kt`.

### Offline-first sync model

`CardRepository` writes only to Room. `SyncableCardRepository` decorates it — every mutation also calls `SyncManager` to push to Firestore (online) or `scheduleImmediateSync()` (offline → `SyncWorker` retries on connectivity). Keep this split: do **not** introduce direct Firestore calls inside `CardRepository`, that creates a circular DI with `SyncManager`.

Cross-device pull dedup hinges on `firestoreId` columns added in `MIGRATION_12_13` on `cards`, `weight_entries`, `transactions`. Conflict resolution compares Room `lastModifiedMs` vs cloud `syncTimestamp` (added in `MIGRATION_13_14`). Deletes write a tombstone row in `deleted_cards` (`MIGRATION_14_15`) so pull cannot "resurrect" deleted cards.

Per-user isolation: every card has `ownerUid` (`MIGRATION_11_12`). On first sign-in after upgrade, `CanLuaApplication.scheduleOrphanClaim()` calls `CardRepository.claimOrphanCardsForCurrentUser()` once to assign legacy `ownerUid = ''` rows. Profiles are keyed by Firebase UID (`MIGRATION_10_11`) — dropping the old `id INTEGER` key was the only fix for cross-account role bleed.

### Room database

`data/database/AppDatabase.kt` — single class with all migrations inline. **Bump `version`, append a `MIGRATION_N_N+1`, register it in `addMigrations(...)`** in the same change; do not rely on `fallbackToDestructiveMigration`. Current version is **15**. WAL journaling is on. `exportSchema = false` (no `schemas/` dir to update).

Entities: `Card`, `WeightEntry`, `Transaction`, `Profile`, `RicePrice`, `PricePoint`, `WeatherCache`, `NewsArticle`, `DeletedCard`. DAOs in `data/dao/`.

### Auth state as single source of truth

`AuthManager.authStateFlow` is wired into `FirebaseAuth.AuthStateListener`. Sign-in/out updates propagate via `AuthViewModel` → root NavHost auto-routes — never call `recreate()` or kill the activity to "reset" auth. `CanLuaApplication.scheduleAutoPullOnSignIn()` uses `distinctUntilChanged()` on `uid` so token refresh does not retrigger pull.

### WorkManager + Hilt

`CanLuaApplication` implements `Configuration.Provider` and supplies `HiltWorkerFactory`. **The default `androidx.startup` WorkManager initializer is disabled in `AndroidManifest.xml`** via `tools:node="remove"` — without that, `SyncWorker` (a Hilt `@HiltWorker`) crashes with `NoSuchMethodException` because the default factory cannot inject its dependencies. Don't re-enable startup init.

### AI Chat

`AiChatRepository` calls OpenRouter (Gemini Flash free). System prompt is composed RAG-style: top-5 current rice prices from `RicePriceRepository` + relevant entries from `assets/agronomy_knowledge.json` (`KnowledgeBaseRepository`) + user context (name, GPS, tracked variety). Markdown is rendered with Markwon (`core` + `ext-tables`) — table rendering matters, do not swap to a plain TextView.

### IME handling

`MainActivity` uses `enableEdgeToEdge()` + `android:windowSoftInputMode="adjustResize"` and screens use `imePadding()` on the Column root (especially `AiChatScreen`). Bottom bar visibility tracks `WindowInsets.isImeVisible`. If you add a new full-screen text-entry screen, mirror this pattern or expect keyboard-overlap regressions.

### Performance tracking

`util/PerformanceTracker` is hooked into both NavHosts via `addOnDestinationChangedListener` to record screen render metrics. `MainActivity.onCreate` calls `setActivity(this)` and `onDestroy` calls `clearActivity()` — preserve those calls if you refactor `MainActivity`.

### Locale & font scale

Both are user-settable via `SettingsViewModel` and applied per-recomposition: `LocaleUtil.applyLanguage(this, language)` runs inside `setContent` (not just `attachBaseContext`), and `CanLuaTheme(fontScale = fontScale)` controls font scaling. Supported locales include vi, en, km, lo, zh — keep parity in `res/values-*/strings.xml` when adding strings.

## Conventions worth knowing

- Migrations are append-only and inline in `AppDatabase.kt`. Comment the **why** above each (existing migrations document the bugs they fix — match that style).
- Firestore writes go through `SyncableCardRepository` / `SyncManager`. UI/ViewModel layer should never import `com.google.firebase.firestore` directly.
- Use `Flow` + `StateFlow` for reactive streams; `combine` / `flatMapLatest` for cross-stream joins (see `observeMyTraderCards` + `observeTransactionsForCards` in trader screens).
- Premium gating: `PremiumState` is initialized in `Application.onCreate` and applies "early adopter" by `firstInstallTime` from `PackageManager` — do not move that init later or fake-install detection breaks.
- Default to existing Hilt-provided singletons; do not instantiate repositories directly.
