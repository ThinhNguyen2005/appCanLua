---
kind: external_dependency
name: Supabase (REST API via custom OkHttp client)
slug: supabase
category: external_dependency
category_hints:
    - vendor_identity
    - sdk_real_api
scope:
    - '**'
---

### Identity
Supabase Postgres instance accessed through its public REST endpoint (`https://{ref}.supabase.co/rest/v1/{table}`), NOT the official Supabase Kotlin SDK.

### Role & integration points
- Credentials (`SUPABASE_URL`, `SUPABASE_ANON_KEY`) come from `BuildConfig` (backed by `local.properties`) and are injected via `SupabaseModule`.
- Only price data is currently fetched from Supabase (`repository/SupabasePriceRepository.kt`); it acts as a parallel source merged into the offline-first pipeline.

### Stable integration shape
- Reads use the anon key; writes require the service_role key and should only be called from admin/GAS, not the app (see `SupabaseClient.upsert` comment).
- No retry/fallback is built into `SupabaseClient` — callers must handle `SupabaseException`.
- Verify exact REST query params and table schemas against the Supabase project's RLS policies before adding new endpoints.