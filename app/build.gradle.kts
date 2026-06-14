import java.util.Properties
import java.util.Base64
import java.lang.ProcessBuilder

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.perf)
}

// Load API keys từ local.properties (không commit). Fallback empty string nếu chưa cấu hình.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val mapsApiKey: String = localProps.getProperty("MAPS_API_KEY", "")
val appHandshakeToken: String = localProps.getProperty("APP_HANDSHAKE_TOKEN", "")
val openRouterApiKey: String = localProps.getProperty("OPENROUTER_API_KEY", "")
val openWeatherApiKey: String = localProps.getProperty("OPENWEATHER_API_KEY", "")
val telegramBotToken: String = localProps.getProperty("TELEGRAM_BOT_TOKEN", "")
val telegramAdminChatId: String = localProps.getProperty("TELEGRAM_ADMIN_CHAT_ID", "")

fun encodeBase64(value: String): String {
    return Base64.getEncoder().encodeToString(value.toByteArray())
}

val gitCommitCount = runCatching {
     val process = ProcessBuilder("git", "rev-list", "--count", "HEAD").start()
    val countStr = process.inputStream.bufferedReader().readText().trim()
    if (countStr.isNotEmpty()) countStr.toInt() else 1
}.getOrDefault(1)

android {
    namespace = "com.GiaThinh.canlua"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.GiaThinh.canlua"
        minSdk = 24
        targetSdk = 37
        versionCode = gitCommitCount
        versionName = "1.0.$gitCommitCount"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "MAPS_API_KEY", "\"${encodeBase64(mapsApiKey)}\"")
        buildConfigField("String", "APP_HANDSHAKE_TOKEN", "\"${encodeBase64(appHandshakeToken)}\"")
        buildConfigField("String", "OPENROUTER_API_KEY", "\"${encodeBase64(openRouterApiKey)}\"")
        buildConfigField("String", "OPENWEATHER_API_KEY", "\"${encodeBase64(openWeatherApiKey)}\"")
        buildConfigField("String", "TELEGRAM_BOT_TOKEN", "\"${encodeBase64(telegramBotToken)}\"")
        buildConfigField("String", "TELEGRAM_ADMIN_CHAT_ID", "\"${encodeBase64(telegramAdminChatId)}\"")

        // Maps API key tham chiếu trong AndroidManifest.xml qua placeholder ${MAPS_API_KEY}
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    buildTypes {
        release {
            // R8 minify + shrink resources cho APK gọn 60-70% và obfuscate code.
            // Hilt/Room/Firestore/Coil/Compose có rules trong proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Tắt minify cho debug build nhanh + Crashlytics symbols mapping rõ ràng.
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    androidTestImplementation(libs.androidx.benchmark.traceprocessor)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.core.splashscreen)
    ksp(libs.androidx.room.compiler)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.remoteconfig)
    implementation(libs.firebase.appcheck.debug)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.play.services.auth)

    // Credentials & Identity
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Coroutines
    implementation(libs.kotlinx.coroutines.play.services)

    // WorkManager (auto sync)
    implementation(libs.androidx.work.runtime.ktx)

    // Chart library (Vico - Jetpack Compose native)
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)

    // QR Code generate
    implementation(libs.zxing.core)

    // QR Code scan (ML Kit + CameraX)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Location (GPS for weather + map)
    implementation(libs.play.services.location)

    // Google Maps Compose + Clustering utils
    implementation(libs.maps.compose)
    implementation(libs.maps.compose.utils)
    implementation(libs.play.services.maps)

    // Accompanist Permissions
    implementation(libs.accompanist.permissions)

    // Networking (OpenRouter AI API)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)

    // Markdown renderer for AI responses, including tables
    implementation(libs.markwon.core)
    implementation(libs.markwon.ext.tables)

    // Coil — load thumbnail bài báo trong NewsSection
    implementation(libs.coil.compose)

    // AndroidX Browser — Chrome Custom Tab cho mở bài báo external
    implementation(libs.androidx.browser)

    // Kotlinx Collections Immutable — PersistentList được Compose Compiler nhận diện
    // natively là Stable (không cần @Immutable annotation) → WeightTableCard Skippable hoàn toàn
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.datastore)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

tasks.withType<Test> {
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}