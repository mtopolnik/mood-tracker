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
*depression* score (each 0–18). The grouping is never labelled in the UI — only
a larger gap separates the two groups. Opens on today's questionnaire; every
choice is persisted immediately (no Save button) while a bottom bar shows the
two category gauges live; a day only *counts* once all 12 items are answered.
The last 30 days are reviewable/editable with missed days — including
partially answered ones — clearly flagged; trends are a two-line chart with
linear interpolation for missing days and a selectable range.

Package `org.mtopol.moodtracker`. Min SDK 30, compile/target SDK 36, Java 17,
Jetpack Compose + Material 3, AGP 9.0.1, Kotlin (Compose plugin) 2.3.10,
`android.builtInKotlin=true`. No navigation library, no DI framework — matching
the conventions of the sibling `grandfather-clock` project.

## Architecture

Single `ComponentActivity` (`MainActivity`) → `MoodTrackerTheme { MoodApp(vm) }`.
Screen switching is plain Compose state (a `Tab` enum + a nullable editor date),
not a navigation library.

- **`MoodViewModel`** (`AndroidViewModel`) — owns `StateFlow` UI states for the
  questionnaire / history / trends, the selected tab, and the past-day editor
  date. Builds the DB + repository from the application context (no DI).
  `setAnswer` updates the in-memory state (live running scores) and upserts the
  whole day on every tap — there is no `save()`. History/Trends gate on derived
  completeness so partial rows never count or plot.
- **`domain/`** — pure, Android-free, unit-tested: `Scoring` (sums + completeness),
  `Interpolation` (per-day linear interpolation, no extrapolation past the
  first/last real point), `DateRange` (`ChartRange` → epoch-day window),
  `Questions` (the 12 string-resource ids; this one references `R`).
- **`data/`** — Room: `MoodEntry` (PK = `LocalDate.toEpochDay()`, 12 `Int`
  columns that may hold `UNANSWERED` (-1) for an in-progress day; scores **and**
  completeness are derived in the domain, never stored), `MoodDao`, `MoodDatabase`
  (v1, `fallbackToDestructiveMigration(dropAllTables = true)` — pre-1.0 only),
  `MoodRepository` (the only Room ↔ domain boundary; returns Flows).
- **`ui/`** — `MoodApp` (Scaffold + NavigationBar), `QuestionnaireScreen`
  (Today tab *and* the full-screen past-day editor; bottom bar = the two
  always-visible `ScorePill` gauges, no Save button), `HistoryScreen`,
  `TrendsScreen` (Vico), `components/` (`ScoreSelector`, `ScorePill`),
  `theme/` (dynamic color + fixed series colors).
- **`reminder/`** — WorkManager daily local notification: `ReminderScheduler`
  enqueues a self-rescheduling one-shot for the next 20:00; `ReminderWorker`
  posts only if today is incomplete; `Notifications` owns the channel + deep-link
  (`EXTRA_OPEN_TODAY`) back to today's questionnaire.

## Dependencies of note

- **Room 2.8.4** — codegen via **KSP `2.3.8`** (NOT kapt; kapt is incompatible
  with `android.builtInKotlin`). KSP uses unified versioning (tracks Kotlin 2.3).
- **Vico `com.patrykandpatrick.vico:compose-m3:3.1.0`** — Vico 3.x has no
  separate `:core` artifact. Chart code is isolated to `ui/TrendsScreen.kt`.
- **WorkManager `androidx.work:work-runtime-ktx:2.11.2`**.

## Known v1 limitations

- A range containing exactly one saved day renders as a near-zero-length line
  (no point markers yet); interpolated multi-point lines are unaffected.
- `today` is captured at ViewModel creation (process is short-lived, relaunched
  daily).
- Each choice upserts the full day; rapid taps rely on last-write-wins on the
  day's single row (the in-memory `StateFlow` is the source of truth and the
  DB converges — no per-write ordering guarantee).
- Pre-1.0 schema changes are destructive (bump `MoodDatabase.version`).

## Testing

`./gradlew testDebugUnitTest` covers `ScoringTest`, `InterpolationTest`,
`DateRangeTest` (JUnit4 `@Test` + `kotlin.test` assertions, matching the
sibling project's convention).
