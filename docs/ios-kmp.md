# iOS Kotlin Multiplatform

The project now has a `shared` Kotlin Multiplatform module that builds an iOS framework named `FreedgeShared`.

Build the XCFramework on macOS:

```bash
./gradlew :shared:assembleFreedgeSharedXCFramework
```

The framework output is generated under:

```text
shared/build/XCFrameworks/
```

Minimal Swift usage:

```swift
import FreedgeShared

extension Data {
    func toKotlinByteArray() -> KotlinByteArray {
        let result = KotlinByteArray(size: Int32(count))
        for (index, byte) in enumerated() {
            result.set(index: Int32(index), value: Int8(bitPattern: byte))
        }
        return result
    }
}

let client = FreedgeSharedClient()

do {
    let result = try await client.analyzeImageWithRecipeImages(
        imageBytes: imageData.toKotlinByteArray(),
        groqApiKey: groqApiKey,
        pexelsApiKey: pexelsApiKey,
        languageCode: Locale.current.language.languageCode?.identifier ?? "en"
    )
    print(result.analysis.displayText)
} catch {
    print(error)
}
```

The shared module contains network and business logic only. Camera capture, image compression, local storage, onboarding, and UI still need native iOS implementations or a later Compose Multiplatform UI migration.
