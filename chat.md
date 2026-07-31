# AppList — Chat / Progress Log

Working directory: `/home/pragadeesh/tmp/AppList`
Repo: git initialized on `master`, no commits yet.

## Objective
Build an Android app ("AppList") that lists all installed apps (user + system),
based on `plan-1.md` (MVP roadmap) and `plan-2.md` (expanded professional vision).

## Environment state
- Java 17 (OpenJDK 17.0.19), `ANDROID_HOME=/home/pragadeesh/Android/Sdk`.
- Platforms android-33/34/35/36 + build-tools installed.
- Gradle wrapper added (Gradle 8.14, generated from cached distribution).
  Builds work: `./gradlew assembleDebug` and `./gradlew testDebugUnitTest`.

## What was done

### Foundation fixes (skeleton -> compiling app)
- `ui/theme/Color.kt`, `Theme.kt`, new `Type.kt`: Material 3 color scheme (purple/teal),
  `AppListTheme` with dark/light + dynamic color support. Fixed undefined `Purple200`,
  `Black`, `Purple500`, `Typography` references.
- `AndroidManifest.xml`: added `QUERY_ALL_PACKAGES` (needed to enumerate all packages on
  Android 11+), `<queries>` for launcher/view intents, `namespace` moved to Gradle DSL.
- Created missing res files: adaptive + legacy vector launcher icons, `backup_rules.xml`,
  `data_extraction_rules.xml`, `proguard-rules.pro`.
- XML themes now use framework `android:Theme.Material*.NoActionBar` parents (the View
  `com.google.android.material:material` lib was dropped; the app is pure Compose).
- `gradle.properties` with `android.useAndroidX=true`.
- Version alignment: Kotlin 1.9.22, Compose BOM 2024.02.01 (Compose 1.6.x),
  Compose compiler 1.5.8, AGP 8.3.0, Gradle wrapper 8.14. Dropped unused appcompat /
  View material / constraintlayout (they forced newer AndroidX needing AGP 8.6+).

### Features implemented
- `data/AppRepository.kt`: `getInstalledApps()` (all packages via PackageManager,
  name/icon/version/install+update time/system+updated-system flags/enabled/uid),
  `getAppDetails()` (target/min SDK, size via StorageStatsManager best-effort, APK/data
  paths, permissions, launchability), `getLaunchIntent()`.
- `data/FavoritesStore.kt`: SharedPreferences-backed favorites.
- `ui/AppListViewModel.kt`: StateFlow state, load/search/category/sort/favorite actions.
- `ui/AppListScreen.kt`: Material 3 list with search field, category FilterChips,
  sort menu (name/install/update/size/user-system), stats bar, cards, badges
  (System/Updated System/Disabled), favorites indicator, pull-to-refresh, loading/error/empty states.
- `ui/AppDetailsScreen.kt`: full app info, Launch (graceful if not launchable),
  App settings, share, favorite toggle.
- `MainActivity.kt`: NavHost routing list -> details.
- `util/AppFilter.kt` + `util/ExportUtils.kt`: pure, unit-tested filter/sort and CSV/JSON export.

### Verification
- `./gradlew :app:assembleDebug` -> BUILD SUCCESSFUL, APK at
  `app/build/outputs/apk/debug/app-debug.apk` (~8.8 MB).
- `./gradlew :app:testDebugUnitTest` -> 18 tests passing (13 AppFilter, 5 ExportUtils).
- Known environment quirk: `StorageStatsManager` is `android.app.usage.*` and
  `StorageManager` is `android.os.storage.*` on this SDK; `UserHandle.getUserId/of` are
  hidden, use `getUserHandleForUid` instead.

## Remaining / next steps (see todo.md and .opencode/plans/TODO.md)
- Instrumented UI tests (needs an emulator/device).
- Power features: new/removed app detection, duplicate detection, comparison.
- Recent apps + widgets (need `PACKAGE_USAGE_STATS` special access).
- APK/Permission analyzers and the rest of the Phase 17+ vision.
- Initial git commit is still pending.

## Reference plan files
- `plan-1.md` — MVP roadmap.
- `plan-2.md` — full AppList vision.
- `.opencode/plans/TODO.md` — merged working implementation plan (Phases 0–17+).
