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
    alias(libs.plugins.detekt)
}

// Load API keys từ local.properties (không commit). Fallback empty string nếu chưa cấu hình.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val mapsApiKey: String = localProps.getProperty("MAPS_API_KEY", "")
val appHandshakeToken: String = localProps.getProperty("APP_HANDSHAKE_TOKEN", "")
val openRouterApiKey: String = localProps.getProperty("OPENROUTER_API_KEY", "")
val telegramBotToken: String = localProps.getProperty("TELEGRAM_BOT_TOKEN", "")
val telegramAdminChatId: String = localProps.getProperty("TELEGRAM_ADMIN_CHAT_ID", "")
val supabaseUrl: String = localProps.getProperty("SUPABASE_URL", "")
val supabaseAnonKey: String = localProps.getProperty("SUPABASE_ANON_KEY", "")

// AdMob — App ID + Native Ad Unit ID THẬT cho release (debug luôn dùng test ID chính thức của Google).
val admobAppId: String = localProps.getProperty("ADMOB_APP_ID", "")
val admobNativeUnitId: String = localProps.getProperty("ADMOB_NATIVE_UNIT_ID", "")
val googleTestAdmobAppId = "ca-app-pub-3940256099942544~3347511713"

fun encodeBase64(value: String): String {
    return Base64.getEncoder().encodeToString(value.toByteArray())
}

val gitCommitCount = runCatching {
     val process = ProcessBuilder("git", "rev-list", "--count", "HEAD").start()
    val countStr = process.inputStream.bufferedReader().readText().trim()
    if (countStr.isNotEmpty()) countStr.toInt() else 1
}.getOrDefault(1)

android {
    namespace = "com.giathinh.canlua"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.giathinh.canlua"
        minSdk = 24
        targetSdk = 37
        versionCode = gitCommitCount
        versionName = "1.0.$gitCommitCount"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "MAPS_API_KEY", "\"${encodeBase64(mapsApiKey)}\"")
        buildConfigField("String", "APP_HANDSHAKE_TOKEN", "\"${encodeBase64(appHandshakeToken)}\"")
        buildConfigField("String", "OPENROUTER_API_KEY", "\"${encodeBase64(openRouterApiKey)}\"")
        buildConfigField("String", "TELEGRAM_BOT_TOKEN", "\"${encodeBase64(telegramBotToken)}\"")
        buildConfigField("String", "TELEGRAM_ADMIN_CHAT_ID", "\"${encodeBase64(telegramAdminChatId)}\"")
        // Supabase — anon key is public (RLS enforced server-side), no need to base64 encode
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")

        // AdMob IDs — giá trị thật được chọn theo build type (xem buildTypes bên dưới).
        // Mặc định = test ID (debug); release override bằng ID thật từ local.properties.
        buildConfigField("String", "ADMOB_APP_ID", "\"$googleTestAdmobAppId\"")
        buildConfigField("String", "ADMOB_NATIVE_UNIT_ID", "\"ca-app-pub-3940256099942544/2247696110\"")

        // Maps API key tham chiếu trong AndroidManifest.xml qua placeholder ${MAPS_API_KEY}
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        // App ID trong manifest PHẢI trùng app ID truyền vào MobileAds.initialize (Next-Gen SDK yêu cầu).
        manifestPlaceholders["ADMOB_APP_ID"] = googleTestAdmobAppId
    }

    buildTypes {
        release {
            // R8 minify + shrink resources cho APK gọn 60-70% và obfuscate code.
            // Hilt/Room/Firestore/Coil/Compose có rules trong proguard-rules.pro.
            //
            // isShrinkResources tắt vì resource shrinker có thể xóa asset JSON trong assets/
            // (agronomy_knowledge.json, trader_knowledge.json) — dữ liệu RAG cho AI chat.
            // Vẫn giữ isMinifyEnabled = true để code minify/obfuscate.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")

            // Release dùng ID THẬT từ local.properties (ADMOB_APP_ID / ADMOB_NATIVE_UNIT_ID).
            buildConfigField("String", "ADMOB_APP_ID", "\"$admobAppId\"")
            buildConfigField("String", "ADMOB_NATIVE_UNIT_ID", "\"$admobNativeUnitId\"")
            manifestPlaceholders["ADMOB_APP_ID"] = admobAppId
        }
        debug {
            // Tắt minify cho debug build nhanh + Crashlytics symbols mapping rõ ràng.
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    testOptions {
        unitTests.all {
            it.jvmArgs("-Djdk.attach.allowAttachSelf=true", "-XX:+EnableDynamicAgentLoading")
        }
        // android.util.Log trong unit test trả giá trị mặc định thay vì ném "not mocked".
        unitTests.isReturnDefaultValues = true
    }
}

// Room schema export (v20+) — phục vụ migration test và review schema.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
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

    // Credentials & Identity (Modern replacement for legacy GoogleSignIn)
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

    // Google Mobile Ads (GMA) Next-Gen SDK & User Messaging Platform (UMP)
    implementation(libs.ads.mobile.sdk)
    implementation(libs.user.messaging.platform)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    detektPlugins(libs.compose.rules.detekt)
}

// Guard: bản release KHÔNG được build với AdMob ID thiếu/placeholder/test ID.
// Vô hiệu hóa mọi task assembleRelease (kể cả bundleRelease) cho tới khi cấu hình đúng.
val googleTestNativeUnitId = "ca-app-pub-3940256099942544/2247696110"
val admobReleaseIdCheck = tasks.register("checkAdmobReleaseIds") {
    description = "Chuc minh ADMOB_APP_ID / ADMOB_NATIVE_UNIT_ID that trong local.properties cho release."
    doLast {
        fun invalid(value: String, name: String): String? = when {
            value.isBlank() -> "$name chua duoc cau hinh trong local.properties."
            value.startsWith("YOUR_") || value.contains("XXXX", ignoreCase = true) || value.contains("PLACEHOLDER", ignoreCase = true) -> "$name van con gia tri placeholder."
            else -> null
        }
        val errors = listOfNotNull(
            invalid(admobAppId, "ADMOB_APP_ID"),
            invalid(admobNativeUnitId, "ADMOB_NATIVE_UNIT_ID"),
            if (admobAppId == googleTestAdmobAppId) "ADMOB_APP_ID la test ID cua Google — khong duoc dung ban release." else null,
            if (admobNativeUnitId == googleTestNativeUnitId) "ADMOB_NATIVE_UNIT_ID la test ID cua Google — khong duoc dung ban release." else null,
        )
        if (errors.isNotEmpty()) {
            throw GradleException(
                "AdMob release config missing:\n  - " + errors.joinToString("\n  - ") +
                    "\nAdd ADMOB_APP_ID / ADMOB_NATIVE_UNIT_ID into local.properties."
            )
        }
    }
}
tasks.matching { it.name.startsWith("assembleRelease") || it.name.startsWith("bundleRelease") }.configureEach {
    dependsOn(admobReleaseIdCheck)
}

tasks.withType<Test> {
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}

detekt {
    buildUponDefaultConfig.set(true)
    allRules.set(false)
    parallel.set(true)
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    baseline.set(rootProject.file("config/detekt/baseline.xml"))
    basePath.set(rootProject.layout.projectDirectory)
}

tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
    reports {
        html.required.set(true)
        checkstyle.required.set(false)
        sarif.required.set(false)
        markdown.required.set(false)
    }
}