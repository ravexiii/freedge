# Freedge KMP iOS Architecture Plan

## Goal

Build a first-class iOS app with feature parity to the current Android app, using Kotlin Multiplatform as the main application stack. The target is not a temporary SwiftUI demo. The target is a maintainable Android+iOS product architecture with shared product behavior, shared UI where practical, and thin native adapters where the platform genuinely matters.

## Product Parity Target

The iOS app must support the same core flows as Android:

- Onboarding gate.
- Camera capture and camera permission flow.
- Fridge photo analysis through Groq.
- Recipe inspiration image lookup through Pexels.
- Loading, retry, and user-facing error states.
- Result screen with captured photo, markdown recipe text, recipe images, copy, and share.
- Scan history list.
- Scan detail screen.
- Delete saved scan.
- RU/EN behavior matching the current app.
- Haptics on success/error/click where platform-appropriate.

The iOS app may be better than Android where iOS has a native advantage, such as share sheet behavior, haptics, camera permission UX, or image orientation handling.

## Architecture Shape

Use a single shared Compose Multiplatform app layer rather than duplicating the UI in SwiftUI.

```text
composeApp/
  commonMain/
    app/
      App.kt
      AppNavGraph.kt
      theme/
    core/
      config/
      error/
      platform/
      result/
      strings/
    domain/
      model/
      repository/
      usecase/
    data/
      network/
      database/
      preferences/
      repository/
    feature/
      onboarding/
      camera/
      history/
      scanDetail/
  androidMain/
    platform/
    MainActivity.kt
  iosMain/
    platform/
    MainViewController.kt

iosApp/
  iosApp.xcodeproj
  iosApp/
    FreedgeApp.swift
    Info.plist
```

Keep the existing Android `app` module compiling during migration. After the shared app reaches parity, decide whether to replace the old Android UI entrypoint with `composeApp`.

## Dependency Direction

```text
presentation -> domain -> data -> platform
```

Allowed dependencies:

- `feature/*` depends on `domain`, `core`, and shared UI primitives.
- `domain` contains models, repository interfaces, and only meaningful use cases.
- `data` implements repositories with network, database, preferences, and file storage.
- `platform` exposes small `expect` APIs for camera, image processing, haptics, sharing, connectivity, and app config.

Forbidden dependencies:

- `domain` must not import Compose, Ktor, Room, DataStore, Android, UIKit, or Foundation APIs.
- `commonMain` must not import Android-only or iOS-only APIs.
- Native camera code must not leak into feature view models.
- Swift code must not reimplement business logic.

## Clean Architecture Rules

Keep the architecture simple. Do not create use cases for every one-line repository call.

Use cases are justified for product workflows:

- `AnalyzeFridgePhotoUseCase`
- `LoadScanHistoryUseCase`
- `SaveScanUseCase`
- `DeleteScanUseCase`
- `CompleteOnboardingUseCase`
- `ObserveOnboardingStateUseCase`

Avoid:

- One interface per class by default.
- Separate mapper classes for trivial field copies.
- Feature modules before the codebase is large enough to need them.
- Dependency injection frameworks until constructor injection becomes painful.
- A second UI implementation in SwiftUI for screens that can be shared.

## Stable Stack

Use stable, production-oriented dependencies only.

- Kotlin Multiplatform for shared Android/iOS code.
- Compose Multiplatform for shared UI.
- Ktor Client for networking.
- kotlinx.serialization for JSON.
- kotlinx.coroutines and Flow for async/state streams.
- AndroidX Lifecycle/ViewModel KMP if it fits the Compose Multiplatform version set cleanly.
- Room KMP as the preferred persistence path because the Android app already uses Room.
- DataStore Preferences KMP for onboarding/settings if version compatibility is clean.
- Coil/Compose image loading only if the selected version supports the needed targets cleanly. Otherwise isolate image loading behind a small UI adapter.

Avoid alpha/beta libraries unless there is no stable option and the reason is written in this file.

## Platform Adapters

Use `expect/actual` for platform behavior that cannot be shared cleanly:

```text
commonMain/core/platform/
  AppConfig.kt
  CameraController.kt
  ConnectivityMonitor.kt
  Haptics.kt
  ImageProcessor.kt
  ShareController.kt
  TimeProvider.kt
```

Android actual implementations:

- Camera: CameraX.
- Image processing: Bitmap, ExifInterface, JPEG compression.
- Share: Android `Intent.ACTION_SEND`.
- Haptics: `Vibrator` / `HapticFeedback`.
- Connectivity: `ConnectivityManager`.
- Config: Gradle `BuildConfig` or generated config.

iOS actual implementations:

- Camera: AVFoundation or UIKit wrapper embedded into Compose.
- Image processing: UIKit/CoreGraphics with orientation normalization and JPEG compression.
- Share: `UIActivityViewController`.
- Haptics: `UIImpactFeedbackGenerator` and `UINotificationFeedbackGenerator`.
- Connectivity: `NWPathMonitor`.
- Config: xcconfig/Info.plist generated values or local debug config.

## Migration Phases

### Phase 0: Branch Baseline

Purpose: make the branch safe to work in.

Tasks:

- Create a dedicated branch, for example `codex/kmp-ios-compose-app`.
- Record baseline Android build commands.
- Keep the current `app` module untouched except where wiring is deliberate.

Verification:

- `./gradlew :app:compileDebugKotlin`
- `./gradlew :shared:compileKotlinMetadata`

### Phase 1: Shared Domain And Data Core

Purpose: move business behavior out of Android-only code.

Tasks:

- Move shared models and error types into KMP common code.
- Move Groq/Pexels DTOs to common code using kotlinx.serialization.
- Move prompt generation and image query parsing to common code.
- Add focused common tests for parser and error mapping.
- Keep Android repository behavior unchanged while common code stabilizes.

Verification:

- Common tests pass.
- Android still analyzes a fridge photo through the existing UI.

### Phase 2: App State And View Models

Purpose: make Android and iOS share screen state and business orchestration.

Tasks:

- Introduce shared state models for onboarding, camera/result, history, and detail.
- Move `MainViewModel` logic into common code, excluding Android `Application`, `BuildConfig`, and network checks.
- Move history orchestration into common code.
- Create platform adapters for config, network status, analytics hooks, and haptics.

Verification:

- Android UI can consume the shared view models or shared state facades.
- No Android imports exist in common view model files.

### Phase 3: Shared Compose UI

Purpose: get one UI implementation for Android and iOS.

Tasks:

- Create `composeApp` with shared `FreedgeApp`.
- Port theme, onboarding, camera shell, result, history, and detail screens to common Compose.
- Replace Android-specific composables with adapter interfaces where needed.
- Keep platform camera preview isolated behind a composable adapter.

Verification:

- Android can launch the shared Compose UI.
- iOS simulator can launch the shared Compose UI from `iosApp`.

### Phase 4: Persistence

Purpose: make history and onboarding cross-platform.

Tasks:

- Migrate scan entity and DAO to Room KMP, or use SQLDelight only if Room KMP blocks the migration.
- Store image files through a common repository with platform file-system actuals.
- Move onboarding preference to DataStore KMP if compatible.

Verification:

- Android history still reads existing or migrated scans.
- iOS can save, list, view, and delete scans.

### Phase 5: Camera And Image Pipeline

Purpose: make the scan input behavior reliable on both platforms.

Tasks:

- Keep CameraX on Android.
- Implement iOS camera capture with AVFoundation/UIKit interop.
- Normalize image orientation on both platforms.
- Compress uploaded images consistently before Groq calls.
- Add manual test cases for portrait, landscape, retake, permission denied, and poor network.

Verification:

- Same photo produces comparable upload size and expected orientation on both platforms.
- Real iPhone scan works end to end.

### Phase 6: Polish And Release Readiness

Purpose: make iOS feel complete, not merely functional.

Tasks:

- Match loading animation, copy/share behavior, haptics, errors, and localization.
- Audit accessibility labels and dynamic type behavior.
- Add privacy strings for camera/photo usage.
- Document Mac build and device test flow.

Verification:

- Real iPhone smoke test passes.
- Android smoke test passes.
- No unstable dependency remains without an explicit note.

## Manual Acceptance Checklist

Run this after each major phase:

- Fresh install opens onboarding when onboarding is not complete.
- Onboarding completion navigates to camera.
- Camera permission denied state is understandable and recoverable.
- Capturing a photo shows loading and then a result or user-facing error.
- API auth/rate/network failures show the intended messages.
- Recipe images load when Pexels key exists.
- Copy puts recipe text on clipboard.
- Share opens the native platform share UI.
- History shows saved scans.
- Scan detail opens the selected scan.
- Delete removes the scan and image file.
- RU/EN behavior matches device language expectations.

## Self-Review Checklist

Review every implementation step against this list:

- Did this change keep Android buildable?
- Did this change move real shared behavior into common code?
- Did any Android/iOS API leak into `commonMain`?
- Did I add a layer because it solves a real problem, not because architecture diagrams like boxes?
- Is this dependency stable and current enough for production?
- Can this be manually tested on Android and iOS with a short script?
- Are errors mapped once and displayed consistently?
- Is image orientation/compression handled before upload?
- Is Swift limited to app bootstrap and native platform integrations?
- Would a teammate understand where to add the next feature?

## Current Repo Notes

The repository currently has:

- `app`: Android-only app with Compose, CameraX, Room, DataStore, Retrofit/Gson, Coil, and feature screens.
- `shared`: early KMP module with iOS targets and Ktor-based network/business logic.
- `docs/ios-kmp.md`: minimal framework build/integration note.

The next implementation step should not be a bigger Swift host demo. It should be either:

- turn `shared` into the real shared app module, or
- introduce `composeApp` and migrate the useful code from `shared` into it.

Prefer `composeApp` if the branch is allowed to be a clean migration branch. Keep `shared` only if we want a smaller incremental path.
