# Flavor Entry-Point Isolation Design

## Goal

Make `lite` a self-contained offline application and retain the complete online
experience in `full`, while keeping both Kotlin flavor variants compilable after
their dependencies and manifests are separated.

## Scope

Phase 2.1 covers Gradle dependency scoping, manifest separation, and the app
entry point. It deliberately does not refactor `CanLuaApplication` or cloud
service DI; those belong to Phase 2.2.

## Architecture

`src/main` retains only offline-safe shared code: Room startup, Compose theme,
`MainScreen`, and the base application manifest with `VIBRATE` plus shared
providers. It must not reference Firebase-only classes, online permissions, or
the Full authentication flow.

Each flavor owns its own `MainActivity` using the same fully qualified class
name. Android's variant source-set precedence selects exactly one activity at
compile time:

- `src/lite/.../MainActivity.kt` waits for `InitViewModel` to warm Room, then
  renders `MainScreen` directly. It contains no permission requests,
  `AuthViewModel`, login, profile setup, Firebase app-link, or cloud imports.
- `src/full/.../MainActivity.kt` owns the current runtime permission launcher
  and the `login -> profile_setup -> main` gate. Its matching manifest retains
  network, contacts, microphone, location, Maps key, and app-link declarations.

The main manifest only retains `VIBRATE` and shared application/provider
declarations. Runtime permissions declared only in `full` are requested only by
the Full activity.

## Dependency Boundaries

The `lite` graph excludes Firebase, Google identity and Play services auth,
Maps/location, OkHttp, Gson, Markwon, Coil, Browser, and Accompanist
Permissions. These dependencies move from `implementation` to
`fullImplementation`, including the Firebase BOM.

Room, Compose, Material 3, Hilt, Navigation, Vico, DataStore, Security Crypto,
and other demonstrated offline requirements remain shared. Release R8 rules are
not broadened in this phase; rules are added only when a verified build or R8
diagnostic demonstrates a reflection requirement.

## Verification

1. Compile `:app:compileLiteDebugKotlin` and `:app:compileFullDebugKotlin`.
2. Assemble `:app:assembleLiteDebug` and `:app:assembleFullDebug`.
3. Update `build_verify.ps1` to invoke flavor-qualified unit-test tasks rather
   than ambiguous `testDebugUnitTest`.
4. Inspect merged manifests: Lite declares only `VIBRATE` among the listed
   sensitive permissions; Full retains internet, contacts, microphone, and
   location.

## Non-Goals

- No changes to `RiceCalculator`, Room entities, or migrations.
- No modification of the existing uncommitted Lite UI/navigation work.
- No `CanLuaApplication` initializer/DI refactor until Phase 2.2.
