plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android {
    namespace = "dev.podcast.nativeapp"
    compileSdk = 36
    defaultConfig { applicationId = "dev.podcast.nativeapp"; minSdk = 31; targetSdk = 36; versionCode = 10200; versionName = "1.2.0" }
    buildTypes {
        create("performance") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            matchingFallbacks += listOf("release")
        }
        create("profileLab") {
            initWith(getByName("performance"))
            applicationIdSuffix = ".profilelab"
            isDebuggable = true // Allow local inspection of the system's private profiling output with run-as.
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    implementation("dev.chrisbanes.haze:haze:1.3.1")
    implementation("androidx.palette:palette-ktx:1.1.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    // Align Lifecycle with the version selected by Activity Compose.
    implementation("androidx.lifecycle:lifecycle-runtime-compose")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-session:1.11.1")
}
