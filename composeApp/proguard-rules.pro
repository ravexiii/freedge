-keep class kg.freedge.** { *; }
-keep class androidx.room.** { *; }

# Ktor
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }
-dontwarn io.ktor.**

# OkHttp (engine used by ktor-client-okhttp)
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Okio
-keep class okio.** { *; }
-dontwarn okio.**

# Kotlinx serialization (used by ktor-serialization-kotlinx-json)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class kg.freedge.**$$serializer { *; }
-keepclassmembers class kg.freedge.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

-dontwarn kotlinx.**
