# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Build Commands

```bash
./gradlew assembleDebug        # Build debug APK
./gradlew testDebugUnitTest    # Run JVM unit tests (domain logic)
./gradlew installDebug         # Install on a running device/emulator
./gradlew clean
```

No global `gradle`/`kotlinc` — always use the wrapper (Gradle 9.3.1, pinned).

## Project Overview

**Mood Tracker** — a backend-free Android app for a daily 12-item mood
questionnaire (each item 0–3). Items 1–6 sum to an *anxiety* score, 7–12 to a
*depression* score (each 0–18). Opens on today's questionnaire; every choice is
persisted immediately (no Save button) while a bottom bar shows the two category
gauges live; a day only *counts* once all 12 items are answered. The last 30
days are reviewable/editable with missed days — including partially answered
ones — clearly flagged; trends are a two-line chart with linear interpolation
for missing days and a selectable range.

Package `org.mtopol.moodtracker`. Min SDK 30, compile/target SDK 36, Java 17,
Jetpack Compose + Material 3, AGP 9.0.1, Kotlin (Compose plugin) 2.3.10,
`android.builtInKotlin=true`. No navigation library, no DI framework — matching
the conventions of the sibling `grandfather-clock` project.

## Architecture

Single `ComponentActivity` (`MainActivity`) →
`MoodTrackerTheme { MoodApp(vm) }`. Screen switching is plain Compose state (a
`Tab` enum + a nullable editor date), not a navigation library.

- **`MoodViewModel`** (`AndroidViewModel`) — owns `StateFlow` UI states for the
  questionnaire / history / trends, the selected tab, and the past-day editor
  date. Builds the DB + repository from the application context (no DI).
  `setAnswer` updates the in-memory state (live running scores) and upserts the
  whole day on every tap — there is no `save()`. History/Trends gate on derived
  completeness so partial rows never count or plot.
- **`domain/`** — pure, Android-free, unit-tested: `Scoring` (sums +
  completeness), `Interpolation` (per-day linear interpolation, no extrapolation
  past the first/last real point), `DateRange` (`ChartRange` → epoch-day
  window), `Questions` (the 12 string-resource ids; this one references `R`).
- **`data/`** — Room: `MoodEntry` (PK = `LocalDate.toEpochDay()`, 12 `Int`
  columns that may hold `UNANSWERED` (-1) for an in-progress day; scores **and**
  completeness are derived in the domain, never stored), `MoodDao`,
  `MoodDatabase` (**non-destructive**: single-source `VERSION` + ordered
  `MIGRATIONS`, no `fallbackToDestructiveMigration` — a missing migration fails
  loudly instead of wiping real user data; **`JournalMode.TRUNCATE`** so there
  is no `-wal` sidecar for Auto Backup to capture mid-write), `MoodRepository`
  (the only Room ↔ domain
  boundary; returns Flows; also `exportDays`/`importDays`), `MoodBackup` (the
  portable, schema-independent JSON format — `org.json`, no production dep —
  whose `decode` treats the file as untrusted: validates every field, caps
  size, tolerates unknown/missing keys).
- **`ui/`** — `MoodApp` (Scaffold + NavigationBar), `QuestionnaireScreen` (Today
  tab *and* the full-screen past-day editor; bottom bar = the two always-visible
  `ScorePill` gauges, no Save button), `HistoryScreen`, `TrendsScreen` (Vico),
  `BackupScreen` (Backup tab: share-sheet export via `FileProvider`, SAF
  `OpenDocument` import, Auto Backup explainer), `components/` (`ScoreSelector`,
  `ScorePill`), `theme/` (dynamic color + fixed series colors).
- **`reminder/`** — WorkManager daily local notification: `ReminderScheduler`
  enqueues a self-rescheduling one-shot for the next 20:00; `ReminderWorker`
  posts only if today is incomplete; `Notifications` owns the channel +
  deep-link (`EXTRA_OPEN_TODAY`) back to today's questionnaire.

## Dependencies of note

- **Room 2.8.4** — codegen via **KSP `2.3.8`** (NOT kapt; kapt is incompatible
  with `android.builtInKotlin`). KSP uses unified versioning (tracks Kotlin
  2.3).
- **Vico `com.patrykandpatrick.vico:compose-m3:3.1.0`** — Vico 3.x has no
  separate `:core` artifact. Chart code is isolated to `ui/TrendsScreen.kt`.
- **WorkManager `androidx.work:work-runtime-ktx:2.11.2`**.
- **No production JSON dependency** — backup (de)serialization uses the platform
  `org.json`. `org.json:json` is a **test-only** dependency so `MoodBackupTest`
  can run the parser on the JVM (the `android.jar` stub throws).

## Data portability

Local-only data has a single point of failure (lost/replaced phone), addressed
two ways:

- **Auto Backup** — `android:fullBackupContent` (API 30) +
  `android:dataExtractionRules` (API 31+), scoped to `mood.db`. Restores
  automatically on a new device. Safe because `MoodDatabase` uses
  `JournalMode.TRUNCATE` (single file, no torn `-wal`).
- **Export/import** — user-driven, via the Backup tab. Export writes
  `MoodBackup` JSON to `cacheDir/exports/` and shares it through a
  `FileProvider` (`${applicationId}.fileprovider`, see `res/xml/file_paths.xml`)
  ACTION_SEND chooser — the reliable route to Drive/email. Import picks a file
  via SAF (`OpenDocument`, accepts `*/*` since Drive mislabels JSON; the content
  is validated regardless) and bulk-upserts by epoch-day (same-day rows
  overwritten, others kept; a restored *today* triggers a questionnaire
  reload).

## Known v1 limitations

- A range containing exactly one saved day renders as a near-zero-length line
  (no point markers yet); interpolated multi-point lines are unaffected.
- `today` is captured at ViewModel creation (process is short-lived, relaunched
  daily).
- Each choice upserts the full day; rapid taps rely on last-write-wins on the
  day's single row (the in-memory `StateFlow` is the source of truth and the DB
  converges — no per-write ordering guarantee).

## Schema migrations (the app has live users — data is never wiped)

There is real user data, so destructive migration is **gone**. Every schema
change must:

1. bump `DB_VERSION` in `MoodDatabase.kt`;
2. add a `Migration(n-1, n)` to `MoodDatabase.MIGRATIONS` (ordered; an entry,
   once shipped, is never edited or removed);
3. build, then **commit** the regenerated
   `app/schemas/org.mtopol.moodtracker.data.MoodDatabase/<version>.json`
   (`exportSchema = true`; `schemas/` is intentionally **not** gitignored — it
   is the baseline migrations diff against).

`MigrationGuardTest` (a normal `testDebugUnitTest`) fails the build if a
version bump lands without the exported schema or a registered migration. It
does **not** prove a migration is *correct* — before any release that bumps the
version, a migration must also be validated with an instrumented
`androidx.room.testing.MigrationTestHelper` test on a device/emulator. Never
edit the committed entity/`1.json` baseline in place; never re-add
`fallbackToDestructiveMigration`.

## Testing

`./gradlew testDebugUnitTest` covers the domain (`ScoringTest`,
`InterpolationTest`, `DateRangeTest`), the untrusted backup parser
(`MoodBackupTest`), and the schema-migration tripwire (`MigrationGuardTest`) —
JUnit4 `@Test` + `kotlin.test` assertions, matching the sibling project's
convention. Migration *correctness* (vs. the tripwire's process check) requires
an instrumented `MigrationTestHelper` test and is not part of this task.
