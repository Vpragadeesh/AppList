# AppList

An Android app that lists every installed application (user and system apps) and acts as an
app manager toolkit — inspect apps, launch them, jump to their settings, search, sort,
categorize, favorite, and export/share the list.

Built with Kotlin, Jetpack Compose, and Material 3.

## Features

- **Full app listing** via `PackageManager` (user + system + updated-system apps)
- **Categories**: All / User / System / Updated System / Disabled
- **Real-time search** by app name and package name
- **Sorting** by name, install date, update date, app size, and user/system
- **App details screen**: version, install/update times, size, UID, SDK levels,
  APK/data paths, requested permissions
- **Launch apps** and **open their settings** screens with a tap
- **Favorites** (persisted locally)
- **Statistics** bar (total / user / system counts)
- **Share / export** the app list as CSV or JSON
- **Pull-to-refresh**, **dark mode**, Material 3 cards and adaptive navigation

## Screenshots / structure

```
app/src/main/java/com/example/applist/
├── MainActivity.kt               # Entry point + NavHost
├── data/
│   ├── AppRepository.kt          # PackageManager wrapper (+ AppDetails)
│   └── FavoritesStore.kt         # SharedPreferences-backed favorites
├── model/
│   ├── AppInfo.kt                # Data model for an installed app
│   ├── AppCategory.kt            # Filter categories
│   └── SortOption.kt             # Sort orders
├── ui/
│   ├── AppListViewModel.kt       # State holder (StateFlow)
│   ├── AppListScreen.kt          # List + search + chips + sort + stats
│   └── AppDetailsScreen.kt       # Detail view with actions
├── util/
│   ├── AppFilter.kt              # Pure filter/sort logic (unit tested)
│   └── ExportUtils.kt            # CSV/JSON export + formatters
└── ui/theme/                     # Material 3 theme (colors, type, theme)
```

## Build

Requirements: JDK 17, Android SDK (compileSdk 36, minSdk 24), Gradle wrapper included.

```bash
./gradlew assembleDebug          # build the APK
./gradlew testDebugUnitTest      # run unit tests
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Permissions

- `QUERY_ALL_PACKAGES` — required to enumerate every installed package on Android 11+.
- App-size data and recent-apps/usage features need `PACKAGE_USAGE_STATS`
  (granted from Settings → Special app access); size shows "Unknown" when not granted.
