# appCanLua Agent Guidelines

## 1. Minimal Changes & Clean Code

- Make the smallest change that fully solves the task. Reuse project components and conventions first, then prefer Kotlin or Android platform APIs and existing dependencies. Add abstractions or dependencies only when the task requires them.
- Before editing, inspect affected callers, tests, fixtures, resources, and configuration. For bugs, trace callers and fix the root cause where the behavior is defined. Update affected callers and tests in the same change.
- Give functions one responsibility and use names that reveal intent. Prefer guard clauses to deep nesting. Keep related code together and use comments to explain decisions the code cannot show.
- Preserve unrelated working-tree changes. Keep validation at trust boundaries, error handling that protects user data, and accessibility behavior intact during refactors.
- Verify the changed behavior through its normal entry point. For new nontrivial logic, use a focused regression test or assertion if existing checks leave it uncovered. Run relevant tests and Detekt for the code touched. Report actual results, checks not run, and material risks.
- Record known shortcuts as `shortcut: <limit>, <when to upgrade>`.

## 2. Internationalization & Localization

- Put user-facing text, including accessibility labels and errors, in Android string resources. Reuse existing keys when the meaning matches. Use `stringResource` in Compose and `getString` where an Android context is available.
- Keep the default `res/values/strings.xml` complete for fallback. Update supported locale resources when wording changes and preserve placeholder types and positions. Use formatted string resources for complete sentences and `plurals` for quantity-dependent text.
- Format dates and numbers for the active locale. Check text length and wrapping when changing UI copy. Use start/end alignment and spacing for layouts that support right-to-left locales.

## 3. Session Continuity & Handover

- **Session Start:** Read only the latest summary file in `docs/sessions/` if continuing previous work.
- **Session End (Major Milestones / Handover Only):** When completing a major milestone or when asked to wrap up a session:
  1. Run `git status` and `git diff` to audit all changes across the working tree, including staged changes with `git diff --cached`.
  2. Create or update `docs/sessions/YYYY-MM-DD_<topic>.md` with five concise sections:
     1. Context & Diff Overview
     2. Results & Test/Detekt Status
     3. Lessons Learned
     4. Technical Debt / `shortcut:` ledger
     5. Next Steps
