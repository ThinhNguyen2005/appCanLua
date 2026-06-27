import java.util.Properties
import java.util.Base64
import java.lang.ProcessBuilder

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
}

// Offline Only

val gitCommitCount = runCatching {
     val process = ProcessBuilder("git", "rev-list", "--count", "HEAD").start()
    val countStr = process.inputStream.bufferedReader().readText().trim()
    if (countStr.isNotEmpty()) countStr.toInt() else 1
}.getOrDefault(1)

android {
    namespace = "com.giathinh.canlua"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.giathinh.canlua"
        minSdk = 24
        targetSdk = 37
        versionCode = gitCommitCount
        versionName = "1.0.$gitCommitCount"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"


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
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
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



    // WorkManager (auto sync)
    implementation(libs.androidx.work.runtime.ktx)

    // Chart library (Vico - Jetpack Compose native)
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)





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