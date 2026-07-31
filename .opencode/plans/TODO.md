# AppList Implementation Plan

Based on plan-1.md (MVP) and plan-2.md (full vision)

Status as of the current session: MVP core is implemented and builds green.

## Phase 0: Project Setup
- [x] Install required Android SDK platforms (API 34 as minimum, aim for API 36) — 33–36 installed
- [x] Install Gradle and configure wrapper — Gradle wrapper 8.14 added
- [x] Create basic Android project structure with Kotlin
- [x] Configure Jetpack Compose and Material 3 — Compose BOM 2024.02.01, material3 1.2.1
- [ ] Set up Git repository with initial commit — repo exists, no commits yet
- [ ] Configure basic CI/GitHub Actions (if applicable)

## Phase 1: Learn Android Basics (Knowledge Acquisition)
- [x] Learn Kotlin basics (if needed)
- [x] Learn Android Studio basics
- [x] Understand Android project structure
- [x] Learn Activity lifecycle
- [x] Learn about Permissions
- [x] Learn about Intents
- [x] Learn Jetpack Compose basics
- [x] Learn about ViewModel, StateFlow, Coroutines

## Phase 2: Build a Basic App
- [x] Create app with single screen
- [x] Add a button — action buttons on the details screen
- [x] Implement a scrollable list (LazyColumn)
- [x] Add app icon — adaptive + legacy vector launcher icons
- [x] Implement Material 3 UI

## Phase 3: Learn PackageManager
- [x] Study PackageManager API
- [x] Understand PackageInfo and ApplicationInfo classes
- [x] Learn to retrieve:
  - [x] App name
  - [x] Package name
  - [x] Icon
  - [x] Version
  - [x] Install time
  - [x] Update time

## Phase 4: Get Installed Apps
- [x] Implement function to get all installed apps — AppRepository.getInstalledApps()
- [x] Handle system vs user apps distinction
- [x] Get enabled/disabled status
- [x] Get UID
- [ ] Get install source (where available) — needs InstallSource API (API 30+)

## Phase 5: Show the List
- [x] Display app icon, name, package name in list
- [x] Add search functionality
- [x] Add sorting capability
- [x] Add refresh/pull-to-refresh
- [x] Add loading indicator

## Phase 6: Identify System Apps
- [x] Identify common system apps (Settings, Phone, Contacts, etc.)
- [x] Implement logic to distinguish:
  - [x] User apps
  - [x] System apps
  - [x] Updated system apps

## Phase 7: App Details Screen
- [x] Create details screen showing:
  - [x] Name
  - [x] Icon
  - [x] Package name
  - [x] Version
  - [x] Install date
  - [x] Last update
  - [x] Target SDK
  - [x] Minimum SDK
  - [x] Storage usage (best effort; -1/Unknown without PACKAGE_USAGE_STATS)
  - [x] Permissions
  - [ ] Activities
  - [ ] Services
  - [ ] Receivers
  - [ ] Providers
- [x] Implement app launch functionality
- [x] Handle non-launchable apps gracefully

## Phase 8: Launch Apps
- [x] Implement app launching via Intent
- [x] Check for launchable activity
- [x] Show appropriate message if no launcher activity exists

## Phase 9: Open App Settings
- [x] Implement shortcuts to app info screen
- [ ] Notification settings
- [ ] Battery optimization
- [ ] Permission screen

## Phase 10: Permissions
- [x] QUERY_ALL_PACKAGES for package visibility (Android 11+)
- [ ] Request runtime permissions as needed:
  - [ ] Usage statistics (PACKAGE_USAGE_STATS)
  - [ ] Notification access
  - [ ] Accessibility

## Phase 11: Search
- [x] Implement real-time filtering
- [x] Search by app name
- [x] Search by package name
- [x] Ignore case in search

## Phase 12: Sorting
- [x] Implement sorting by:
  - [x] Name
  - [x] Install date
  - [x] Update date
  - [x] App size (best effort; unknown sizes sorted last)
  - [x] User/System apps
  - [ ] Recently used (requires PACKAGE_USAGE_STATS)

## Phase 13: Categories
- [x] Separate apps into:
  - [x] User Apps
  - [x] System Apps
  - [x] Disabled Apps
  - [x] Updated System Apps

## Phase 14: Nice UI
- [x] Implement Material 3 design
- [x] Use Cards for app items
- [ ] Implement adaptive layouts (tablets/foldables)
- [x] Add pull-to-refresh
- [x] Implement dark mode

## Phase 15: Advanced Features (MVP Extensions)
- [x] Add app statistics: total / user / system counts (stats bar)
- [x] Implement export functionality: CSV + JSON
- [x] Implement share functionality: share list and individual app
- [x] Add favorites/bookmarks feature (SharedPreferences)
- [ ] Add recent apps tracking (needs PACKAGE_USAGE_STATS)
- [ ] Add home screen widgets (basic)

## Phase 16: Power Features
- [x] Implement app list backup — CSV/JSON export acts as backup
- [ ] Detect newly installed apps
- [ ] Detect removed apps
- [ ] Add app comparison feature
- [ ] Add duplicate package detection

## Phase 17+: Full AppList Vision (from plan-2.md)
- [ ] Implement clean architecture refinement (MVVM + Clean Architecture)
- [ ] Create dedicated PackageManager module/repository
- [ ] Implement home-screen dashboard
- [ ] Enhance installed apps screen with infinite scrolling, fast scrolling, multi-selection
- [ ] Add APK analyzer phase
- [ ] Add permission analyzer phase
- [ ] Add storage analyzer phase
- [ ] Add usage statistics phase (using UsageStatsManager)
- [ ] Add security dashboard phase
- [ ] Add device information phase
- [ ] Add battery dashboard phase
- [ ] Add sensor explorer phase
- [ ] Add modern UI animations and effects
- [ ] Add export to TXT/PDF
- [ ] Add favorites notes/tags
- [ ] Add powerful search with multiple filters
- [ ] Add analytics charts and graphs
- [ ] Optimize for performance (paging, caching)
- [ ] Add comprehensive testing (instrumented UI tests)
- [ ] Add documentation updates (architecture diagrams)

## Immediate Next Steps
1. Add instrumented UI tests (needs emulator/device)
2. Power features: new/removed detection, duplicate detection, comparison
3. Recent apps + widgets (PACKAGE_USAGE_STATS)
4. Initial git commit
