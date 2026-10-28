---
kind: external_dependency
name: OpenRouter (AI model proxy, free tier)
slug: openrouter
category: external_dependency
category_hints:
    - vendor_identity
    - client_constraint
scope:
    - '**'
---

### Identity
OpenRouter (`https://openrouter.ai/api/v1/chat/completions`) is used as an OpenAI-compatible proxy to serve the `openrouter/free` model for Cân Lúa's AI assistant.

### Role & integration points
- Response headers include `HTTP-Referer: https://canlua.app` and `X-Title: CanLua`.

### Stable constraints
- Single-model policy: only `openrouter/free` is used intentionally to keep cost predictable; no fallback model exists.
- Calls have a 30-second timeout and zero retries — every error maps to a Vietnamese user-facing message rather than surfacing raw codes.
- Responses are sanitized (control chars stripped, hard-capped at 8000 chars) before rendering in Markwon.
- Verify exact OpenRouter billing/throttling behavior against their docs when changing models or enabling paid tiers.