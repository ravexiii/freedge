import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    // alias(libs.plugins.google.services)  // включить после добавления google-services.json
}

val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "kg.freedge.app"
    compileSdk = 35

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    defaultConfig {
        applicationId = "kg.freedge.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val groqKey = localProperties.getProperty("GROQ_API_KEY").orEmpty()
        buildConfigField("String", "GROQ_API_KEY", "\"${groqKey.replace("\"", "\\\"")}\"")

        val pexelsKey = localProperties.getProperty("PEXELS_API_KEY").orEmpty()
        buildConfigField("String", "PEXELS_API_KEY", "\"${pexelsKey.replace("\"", "\\\"")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // CameraXs
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.androidx.exifinterface)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation("com.github.jeziellago:compose-markdown:0.5.4")

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Firebase (включить после добавления google-services.json)
    // implementation(platform(libs.firebase.bom))
    // implementation(libs.firebase.analytics)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.splashscreen)
}
