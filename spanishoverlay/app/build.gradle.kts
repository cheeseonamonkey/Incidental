plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.spanishoverlay"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.spanishoverlay"
        minSdk = 22; targetSdk = 34
        versionCode = 1; versionName = "1.0"
    }
    buildFeatures { compose = true; viewBinding = true; buildConfig = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.8" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildTypes {
        debug {
            isDebuggable = true
            buildConfigField("Boolean", "STRICT_MODE", "true")
        }
        release { isMinifyEnabled = false }
    }
}
dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.core:core-ktx:1.12.0")
    val bom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(bom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.11.1")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
