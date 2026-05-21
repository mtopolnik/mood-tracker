import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Release/upload signing credentials live in a local, git-ignored
// keystore.properties (never committed, never passed through chat).
// When the file is absent (e.g. on a fresh checkout), release falls back to
// the debug key so the project still builds — but Play rejects that AAB.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
}
val hasReleaseKeystore = keystorePropertiesFile.exists()

android {
    namespace = "org.mtopol.moodtracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.mtopol.moodtracker"
        minSdk = 30
        targetSdk = 36
        versionCode = 10
        versionName = "1.8"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (hasReleaseKeystore) {
            create("release") {
                // storeFile may be absolute or relative to the project root.
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName(
                if (hasReleaseKeystore) "release" else "debug"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // Ship the exported Room schema JSONs (room.schemaLocation, below) as
    // androidTest assets so MigrationTestHelper can load each version's
    // baseline on-device.
    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

// Export the Room schema so future migrations have a baseline to diff against.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.withType<Test> {
    testLogging {
        showStandardStreams = true
    }
}

dependencies {
    constraints {
        // The rationale lives in because() so it travels with the resolution
        // graph (visible in `dependencyInsight`), not just in source comments —
        // this pin looks unused (no app code touches serialization) and its
        // only failure mode is a device-only androidTest, so it is dangerously
        // easy to delete without it.
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1") {
            because(
                "room-migration 2.8.4 (MigrationTestHelper) pulls " +
                    "kotlinx-serialization-json 1.8.1, while androidx.lifecycle/" +
                    "savedstate pin serialization-core to 1.7.3. AGP consistent " +
                    "resolution stamps 1.7.3 onto the androidTest classpath, so " +
                    "json 1.8.1 runs against core 1.7.3 and the Room schema " +
                    "parser dies with AbstractMethodError. Keep until Room and " +
                    "androidx.lifecycle agree on a serialization-core version.",
            )
        }
    }

    implementation(platform("androidx.compose:compose-bom:2026.03.00"))
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Room — local SQLite store; Flow/suspend DAOs via room-ktx, codegen via KSP (not kapt).
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // Vico 3.x — Compose Material3 line chart for the Trends screen.
    // 3.x has no separate :core artifact; compose-m3 transitively pulls :compose.
    implementation("com.patrykandpatrick.vico:compose-m3:3.1.0")

    // WorkManager — daily local reminder notification (pulls work-runtime → CoroutineWorker).
    implementation("androidx.work:work-runtime-ktx:2.11.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation(kotlin("test"))
    // Test-only: the real org.json so MoodBackup's untrusted-import parser can
    // be unit-tested on the JVM (the android.jar stub throws). Not shipped.
    testImplementation("org.json:json:20240303")

    // Instrumented migration-correctness harness (device/emulator only).
    // room-testing provides MigrationTestHelper, which validates that each
    // registered Migration actually transforms vN into the exported vN+1
    // schema. Run via `./gradlew connectedDebugAndroidTest`.
    androidTestImplementation("androidx.room:room-testing:2.8.4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
}
