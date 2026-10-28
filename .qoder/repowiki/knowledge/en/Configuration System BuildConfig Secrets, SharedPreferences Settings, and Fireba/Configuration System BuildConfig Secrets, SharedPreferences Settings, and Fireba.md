---
kind: configuration_system
name: 'Configuration System: BuildConfig Secrets, SharedPreferences Settings, and Firebase Remote Config'
category: configuration_system
scope:
    - '**'
source_files:
    - app/src/main/java/com/giathinh/canlua/CanLuaApplication.kt
    - app/src/main/java/com/giathinh/canlua/util/FirebaseRemoteConfigManager.kt
    - app/src/main/java/com/giathinh/canlua/repository/SettingsRepository.kt
    - app/src/main/java/com/giathinh/canlua/di/SupabaseModule.kt
    - app/src/main/java/com/giathinh/canlua/di/NetworkModule.kt
    - app/src/main/java/com/giathinh/canlua/di/FirebaseModule.kt
    - app/src/main/java/com/giathinh/canlua/data/remote/HttpClient.kt
    - app/src/main/java/com/giathinh/canlua/data/remote/SupabaseClient.kt
    - app/src/main/java/com/giathinh/canlua/util/ApiKeyObfuscator.kt
    - app/src/main/java/com/giathinh/canlua/data/model/AppThemeMode.kt
---

## Overview

The app uses a three-tier configuration system:

1. **Build-time secrets & environment flags** — injected via `BuildConfig` from Gradle (`local.properties`).
2. **Runtime user preferences** — persisted in `SharedPreferences` under key `app_settings`, exposed as reactive `Flow`s.
3. **Remote feature flags & URLs** — served by Firebase Remote Config with local caching.

There is no centralized config file (no `.env`, `.yaml`, or `.toml`) at runtime; configuration values are loaded from these three sources during `Application.onCreate()`.

## Key Files and Packages

- `CanLuaApplication.kt` — single bootstrap point that initializes Firebase App Check, Analytics, PremiumState, Firebase Remote Config, then wires early-adopter premium eligibility and auto-sync behavior.
- `util/FirebaseRemoteConfigManager.kt` — type-safe wrapper around `FirebaseRemoteConfig`; exposes keys as constants and `StateFlow`s for reactive UI consumption.
- `repository/SettingsRepository.kt` — single source of truth for all user settings stored in `SharedPreferences` (`app_settings`), backed by `MutableStateFlow` + `OnSharedPreferenceChangeListener`.
- `di/SupabaseModule.kt` — injects Supabase URL and anon key from `BuildConfig.SUPABASE_URL` / `BuildConfig.SUPABASE_ANON_KEY`.
- `di/NetworkModule.kt` — builds shared `OkHttpClient` with timeouts, logging gated on `BuildConfig.DEBUG`, and a custom retry interceptor (max 3 attempts, exponential backoff).
- `data/remote/HttpClient.kt`, `data/remote/SupabaseClient.kt` — lightweight OkHttp clients that consume the injected `OkHttpClient` and `Gson`.
- `util/ApiKeyObfuscator.kt` — Base64 decoder used to deobfuscate API keys embedded in `BuildConfig` at runtime.
- `data/model/AppThemeMode.kt`, `AppLanguage.kt`, `FontScale.kt` — typed enums with `fromName`/`fromTag` fallbacks for safe deserialization of persisted strings.

## Architecture and Conventions

### Build-time configuration (`BuildConfig`)

Secrets and environment-specific values are defined in Gradle's `local.properties` and surfaced through `BuildConfig`. The following keys are consumed across the codebase:

| Key | Consumed by | Purpose |
|---|---|---|
| `SUPABASE_URL` | `SupabaseModule` | Supabase project base URL |
| `SUPABASE_ANON_KEY` | `SupabaseModule` | Public-read-only Supabase key |
| `OPENROUTER_API_KEY` | `AiChatRepository` | OpenRouter LLM API key (Base64-encoded) |
| `TELEGRAM_BOT_TOKEN` | `FeedbackRepository` | Telegram bot token (Base64-encoded) |
| `TELEGRAM_ADMIN_CHAT_ID` | `FeedbackRepository` | Admin chat ID (Base64-encoded) |
| `MAPS_API_KEY` | `RiceMapScreen` | Google Maps key (Base64-encoded) |
| `DEBUG` | `NetworkModule`, `PerformanceTracker`, `CanLuaApplication` | Toggles logging and debug AppCheck provider |
| `VERSION_NAME` | `SettingsScreen` | Shown in About screen |

All sensitive keys except Supabase anon key are Base64-encoded in `local.properties` and decoded at runtime via `ApiKeyObfuscator.decode()`. This is documented in `ApiKeyObfuscator`'s KDoc: "Giải mã API key dạng Base64 thành String thô ở runtime. Giúp tránh việc các API key bị lưu plaintext trong DEX".

### Runtime user settings (`SettingsRepository`)

User preferences live in a single `SharedPreferences` instance named `app_settings`. Every setting is:

- Declared as a `private const val KEY_*` in the companion object.
- Exposed as a `Flow<T>` (via `MutableStateFlow`) so UI can observe changes reactively.
- Updated atomically: both the `SharedPreferences` value and the in-memory `StateFlow` are written together inside `prefs.edit { ... }` blocks.
- A global `OnSharedPreferenceChangeListener` keeps the flows in sync when values change externally.

Typed enums (`AppThemeMode`, `AppLanguage`, `FontScale`) provide `fromName`/`fromTag` fallbacks so unknown persisted values don't crash — they fall back to a sensible default (e.g. `AUTO` theme, `VIETNAMESE` language, `NORMAL` font scale).

### Remote configuration (Firebase Remote Config)

`FirebaseRemoteConfigManager` centralizes remote feature flags:

- Keys are declared as `const val` constants (`early_adopter_enabled`, `early_adopter_cutoff_ms`, `tutorial_video_url`, `website_url`).
- Default/fallback values are defined inline alongside each key.
- `init(context)` sets a 30-minute minimum fetch interval (the quota-free minimum per Firebase docs) and registers defaults via `setDefaultsAsync`.
- Values are exposed both as direct getters and as `StateFlow`s so UI can react to updates after `fetchAndActivate()`.
- `forceFetch()` is a suspend function used before critical operations (e.g. premium eligibility checks) to ensure fresh values.

`CanLuaApplication.onCreate()` calls `FirebaseRemoteConfigManager.init()` and then `forceFetch()`; if the fetch succeeds it re-applies early-adopter premium eligibility.

### Dependency Injection of configuration

Hilt modules (`di/*Module.kt`) construct infrastructure objects using values from `BuildConfig`:

- `SupabaseModule` provides `@Named("supabaseUrl")` and `@Named("supabaseAnonKey")` from `BuildConfig`.
- `NetworkModule` provides a singleton `OkHttpClient` configured with connection/read/call timeouts (10/15/90s), a User-Agent header, conditional HTTP logging based on `BuildConfig.DEBUG`, and a custom retry interceptor that retries on network errors and server errors (not 4xx client errors) up to 3 times with exponential backoff.
- `FirebaseModule` provides singleton instances of `FirebaseAuth`, `FirebaseFirestore`, `FirebaseStorage`.

### Application bootstrap order

`CanLuaApplication.onCreate()` runs this sequence:

1. Install Firebase App Check (Play Integrity in release, Debug provider in debug).
2. Launch background scope to initialize `AnalyticsHelper`, `PremiumState`, and `FirebaseRemoteConfigManager`.
3. Apply early-adopter premium eligibility based on install time vs. remote cutoff.
4. Force-fetch Remote Config and re-apply eligibility if successful.
5. Schedule orphan-card claiming and observe auto-sync preference changes.

## Conventions and Constraints

- **Secrets never hard-coded**: All API keys are either Base64-encoded in `local.properties` and decoded at runtime via `ApiKeyObfuscator`, or supplied through `BuildConfig` generated by Gradle. There is no `.env` file checked into source control.
- **BuildConfig.DEBUG gates logging and debug providers**: `NetworkModule` sets `HttpLoggingInterceptor.Level.BASIC` only when `BuildConfig.DEBUG` is true; `CanLuaApplication` switches between `DebugAppCheckProviderFactory` and `PlayIntegrityAppCheckProviderFactory` based on `BuildConfig.DEBUG`.
- **Remote Config keys are centralized**: New remote features must add their key constant and default in `FirebaseRemoteConfigManager` rather than scattering string literals.
- **User settings are typed and reactive**: New settings should be an enum with a `fromName` fallback, exposed as a `Flow` in `SettingsRepository`, and registered in the `OnSharedPreferenceChangeListener`.
- **Remote Config cache policy**: Minimum fetch interval is fixed at 30 minutes (quota-free minimum); zero-hour cache is explicitly discouraged in comments.
- **Supabase anon key is the only non-Base64 secret**: Documented as "safe to bundle, only allows public RLS reads" in `SupabaseModule`.
- **Fallback-first semantics**: Both Remote Config getters and `SettingsRepository` read methods return safe defaults when the remote/local value is missing or invalid (blank URL → default URL; unknown theme/language/font-scale → default enum entry).