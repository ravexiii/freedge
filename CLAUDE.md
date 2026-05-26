# Freedge

KMP app: photographs fridge → Groq Vision → recipes (+ optional Pexels images). Saved to local Room DB.

## Modules
`shared` = HTTP + parsing. `composeApp` = UI + DB + adapters. `app/` = legacy Android, **do not add code**. `iosApp/` = Swift shell (Xcode via `xcodegen`).

## Commands
```bash
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:assembleDebugXCFramework   # macOS only
cd iosApp && xcodegen generate && open iosApp.xcodeproj
```

## Patterns (non-obvious)

**DI.** No Hilt/Koin. `AppDependencies` built once in `FreedgeApplication.onCreate` (Android) / `MainViewController()` (iOS). Passed via `CompositionLocalProvider(LocalAppDeps provides deps)`. ViewModels get deps via constructor in `viewModel { MyVM(deps.x) }`. Don't add globals except `AppContextHolder`.

**ViewModel.** Extends `androidx.lifecycle.ViewModel` (JetBrains KMP fork — works in commonMain, no expect/actual). Created with `viewModel { ... }` factory inside the composable.

**Navigation.** JetBrains `navigation-compose` KMP. Plain string routes. Start destination depends on `onboardingPrefs.isCompleted: Flow<Boolean?>` — `null` means loading, NavHost returns early (do not "fix" the nullable).

**Two-phase analyze.** `MainViewModel.analyzeImage` does Groq call → saves scan → shows result → THEN fetches Pexels images separately. Don't merge into one loading state.

**ScanEntity.imageFileName** stores just `scan_<ts>.jpg` (no path). Full path assembled by `ImageStorage` per platform.

## Adapters (expect → actual)
| Expect | Android | iOS |
|---|---|---|
| `AppConfig` | `BuildConfig.*` from `local.properties` | `NSBundle.mainBundle.infoDictionary` from `Config.xcconfig` |
| `ConnectivityMonitor` | `ConnectivityManager` | `nw_path_monitor_*` (monitor MUST be retained as field) |
| `Haptics` | `Vibrator` / `VibratorManager` (API 31+) | `UINotificationFeedbackGenerator` (call on main) |
| `ImageStorage` | `filesDir/scans/` | `NSDocumentDirectory/scans/` |
| `ShareController` | `Intent.ACTION_SEND` + FileProvider | `UIActivityViewController` (needs iPad popover anchor) |
| `currentTimeMillis()` | `System.currentTimeMillis()` | `NSDate().timeIntervalSince1970 * 1000` |
| `CameraPreview` | CameraX + Accompanist permission | AVFoundation + `UIKitView` |
| `FreedgeHttpClient` | OkHttp | Darwin |
| `DatabaseFactory.buildRoomDatabase` | `Room.databaseBuilder(context, ...)` | `Room.databaseBuilder<DB>(path).setDriver(BundledSQLiteDriver())` |
| `DataStoreFactory.buildDataStore` | `PreferenceDataStoreFactory.create` | `.createWithPath` (okio path) |
| `isRussian()`, `currentLanguageCode()`, `formatDate(Long)`, `stripMarkdown(String)` | Java/Android | Foundation |

## Anti-patterns that will bite

**commonMain:**
- `System.currentTimeMillis()` → use `kg.freedge.core.currentTimeMillis()`
- suspend call inside `runCatching { }.onSuccess { }` → `onSuccess` is non-suspend; use try/catch in `viewModelScope.launch`
- Android imports in commonMain (only KMP-flagged `androidx.*` work)
- Full file paths in DB entities — store filename only

**iOS:**
- Capture native handles in local val → GC kills callbacks. Promote to `private val`/field.
- UIKit / state updates from AVFoundation delegate queue → wrap body in `dispatch_async(dispatch_get_main_queue())`
- `captureSession.stopRunning()` on main → wrap in global queue
- `memcpy` on `addressOf(0)` of empty `ByteArray` → guard `if (size > 0)`
- `usePinned` only across the cinterop call; don't escape the block
- `@Volatile` works on Kotlin/Native 2.x (don't reach for `AtomicInt`)
- `UIActivityViewController` without `popoverPresentationController.sourceView`/`sourceRect` → iPad crash

**Android:**
- Recreating `AppDependencies` per Activity → DataStore single-process violation. Read from `FreedgeApplication.deps`.
- `suspendCancellableCoroutine` without `invokeOnCancellation` + `isActive` checks in callbacks → resume-after-cancel
- Camera capture triggered in `AndroidView { update = {} }` → fires every recomposition. Use `LaunchedEffect(triggerCapture)`.
- Release builds: ProGuard must keep `io.ktor.**`, `okhttp3.**`, `okio.**` + serializer companions

## Stack
Kotlin 2.2.10, AGP 9.1.0, Compose MP 1.8.0, Room 2.7.1 (KSP on all 4 targets), DataStore 1.1.1, Ktor 3.4.2, Coil 3.1.0 (`coil-network-ktor3`), JetBrains lifecycle/navigation 2.8.4. **Don't bump versions casually** — KSP+Room+Compose+Kotlin form a tight compatibility quad.

## API keys
- Android: `local.properties` (`GROQ_API_KEY`, `PEXELS_API_KEY`) → `BuildConfig`
- iOS: `iosApp/iosApp/Config.xcconfig` (git-ignored) → `Info.plist` `$(...)` substitution → `NSBundle`
