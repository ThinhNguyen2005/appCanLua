# Flavor Isolation Phase 2.1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Firebase-free, offline Lite APK while preserving Full's cloud entry point and behavior.

**Architecture:** `main` holds only offline-safe shared UI/data code. Both `MainActivity` and `CanLuaApplication` are selected from `lite` or `full` source sets by fully qualified name. Online dependencies, permissions, BuildConfig secrets, and implementations exist exclusively in Full.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, Room, Gradle product flavors, Android manifests, PowerShell.

## Global Constraints

- Do not change `RiceCalculator`, Room entities, or migrations.
- Preserve the six existing uncommitted Lite UI/navigation changes.
- Lite must have no Firebase, Maps, location, OkHttp, Gson, Markwon, Coil, Browser, Accompanist Permissions, Google identity, or Play-services auth in its runtime graph.
- Main and Lite source sets must not import `com.google.firebase.*`.
- Do not add broad R8 keep rules in this phase.

---

### Task 1: Add executable flavor-boundary checks

**Files:**
- Create: `scripts/assert-flavor-boundaries.ps1`
- Modify: `build_verify.ps1`

**Consumes:** flavor names `lite` and `full`.

**Produces:** `scripts/assert-flavor-boundaries.ps1`, which exits nonzero if Lite contains a forbidden dependency or sensitive permission.

- [ ] **Step 1: Write the failing boundary check**

Create `scripts/assert-flavor-boundaries.ps1` with these checks: run `:app:dependencies --configuration liteDebugRuntimeClasspath`; reject the case-insensitive tokens `firebase`, `play-services-auth`, `googleid`, `maps`, `location`, `okhttp`, `gson`, `markwon`, `coil`, `browser`, and `accompanist`; inspect the generated Lite merged manifest and reject `INTERNET`, `READ_CONTACTS`, `RECORD_AUDIO`, `ACCESS_FINE_LOCATION`, and `ACCESS_COARSE_LOCATION`.

- [ ] **Step 2: Run it and confirm RED**

Run: `powershell -ExecutionPolicy Bypass -File scripts/assert-flavor-boundaries.ps1`

Expected: FAIL, reporting Firebase and at least one sensitive permission in Lite.

- [ ] **Step 3: Wire it into the verifier**

After Lite assembly in `build_verify.ps1`, invoke the new script and stop on a nonzero `$LASTEXITCODE`. Replace the ambiguous `:app:testDebugUnitTest` invocation with `:app:testLiteDebugUnitTest` then `:app:testFullDebugUnitTest`.

- [ ] **Step 4: Commit the test harness only**

Run: `git add scripts/assert-flavor-boundaries.ps1 build_verify.ps1; git commit -m "test(flavors): assert lite dependency boundary"`

### Task 2: Scope Gradle dependencies and manifests

**Files:**
- Modify: `app/build.gradle.kts:14-80,162-214`
- Modify: `app/src/main/AndroidManifest.xml:5-29`
- Modify: `app/src/full/AndroidManifest.xml:4-29`

**Consumes:** the boundary checks from Task 1.

**Produces:** Lite runtime classpath with only offline dependencies and a Full-only sensitive-permission manifest.

- [ ] **Step 1: Move build configuration and dependencies**

Move Firebase BOM and Firebase artifacts, Play-services auth/location/maps, Credentials/Google ID, coroutines Play Services, OkHttp, Gson, Markwon, Coil, Browser, and Accompanist Permissions to `fullImplementation`. Put `MAPS_API_KEY`, OpenRouter, Supabase, Telegram, and web-client resource values only in `full` flavor configuration; retain shared Compose, Room, Hilt, Navigation, Vico, DataStore, Security Crypto, and WorkManager only if the Lite application still requires it.

- [ ] **Step 2: Separate declarations**

Keep only `VIBRATE` and shared provider/application elements in the main manifest. Add both location permissions to Full alongside its existing network, contacts, microphone, Maps metadata, and app links. Remove `networkSecurityConfig` from main if it only exists for remote traffic; otherwise preserve an offline-safe configuration.

- [ ] **Step 3: Run boundary check and confirm it still fails for source imports**

Run: `powershell -ExecutionPolicy Bypass -File scripts/assert-flavor-boundaries.ps1`

Expected: dependency and manifest checks PASS; Kotlin compilation is not yet expected to pass because online classes remain in `main`/`lite`.

### Task 3: Split Android entry points and online source ownership

**Files:**
- Delete: `app/src/main/java/com/giathinh/canlua/MainActivity.kt`
- Delete: `app/src/main/java/com/giathinh/canlua/CanLuaApplication.kt`
- Create: `app/src/lite/java/com/giathinh/canlua/MainActivity.kt`
- Create: `app/src/full/java/com/giathinh/canlua/MainActivity.kt`
- Create: `app/src/lite/java/com/giathinh/canlua/CanLuaApplication.kt`
- Create: `app/src/full/java/com/giathinh/canlua/CanLuaApplication.kt`
- Move to `src/full`: `data/remote/HttpClient.kt`, `data/location/LocationProvider.kt`, `util/AnalyticsHelper.kt`, `util/CustomTabsLauncher.kt`, `util/FirebaseRemoteConfigManager.kt`, `ui/util/MarkdownText.kt`, `ui/component/profile/GradientProfileHeader.kt`, `util/PerformanceTracker.kt`
- Modify or delete: `app/src/lite/java/com/giathinh/canlua/repository/AuthManager.kt`

**Consumes:** Task 2's flavor classpaths.

**Produces:** a Lite startup path that never references auth, Firebase, runtime sensitive permissions, or Full-only libraries.

- [ ] **Step 1: Preserve Full behavior**

Move the current main `MainActivity` and `CanLuaApplication` verbatim into `src/full` before editing. Keep the Full permission launcher, auth/profile gate, Hilt worker configuration, Firebase initialization, and `PerformanceTracker` invocation.

- [ ] **Step 2: Implement Lite entry points**

Create a minimal `@HiltAndroidApp` Lite application. Create Lite `MainActivity` with `InitViewModel`, splash keep condition, `SettingsViewModel`, locale/theme collection, and direct `MainScreen()` composition. Do not import `Manifest`, `AuthViewModel`, `AuthScreen`, `ProfileSetupScreen`, Firebase, or `PerformanceTracker`.

- [ ] **Step 3: Eliminate remaining Lite-incompatible symbols**

Move the listed online files to Full only when their callers are Full. For every shared caller, provide the smallest Lite-local no-op or remove the unreachable Full call. Replace the Lite `AuthManager` FirebaseUser signature with no Firebase type, or remove it if no Lite caller remains. Verify with:

`rg -n -g '*.kt' 'com\\.google\\.firebase|FirebaseUser|okhttp3|io\\.noties\\.markwon|coil\\.compose|androidx\\.browser|accompanist\\.permissions|com\\.google\\.android\\.gms|com\\.google\\.maps' app/src/main app/src/lite`

Expected: no matches.

- [ ] **Step 4: Verify GREEN**

Run: `./gradlew.bat :app:compileLiteDebugKotlin :app:compileFullDebugKotlin`

Expected: both tasks `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit source-set isolation**

Run: `git add app/build.gradle.kts app/src/main app/src/lite app/src/full; git commit -m "feat(flavors): isolate lite entry points and dependencies"`

### Task 4: Assemble and verify both products

**Files:**
- Modify only if verification exposes an error: exact smallest affected file from Tasks 1-3.

- [ ] **Step 1: Run full acceptance checks**

Run: `powershell -ExecutionPolicy Bypass -File build_verify.ps1`

Expected: Lite and Full debug assemblies plus both flavor-qualified unit-test tasks pass, followed by the Lite boundary check.

- [ ] **Step 2: Inspect artifacts**

Run: `Get-ChildItem app/build/outputs/apk/lite/debug/app-lite-debug.apk, app/build/outputs/apk/full/debug/app-full-debug.apk | Select-Object Name,Length`

Record sizes only; do not claim the Lite `<10 MB` release target until `assembleLiteRelease` is verified in Phase 2.4.

- [ ] **Step 3: Check the final diff**

Run: `git diff --check HEAD~2..HEAD; git status --short`

Expected: no whitespace errors and no staging/commit of the pre-existing UI files.
