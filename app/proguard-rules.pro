# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# === Attributes — Giữ generic signatures và annotations để Gson/Room không bị ClassCastException/reflection error ===
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*,SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# === Google Sign-In & Credential Manager ===
# Giữ các class của Credential Manager và Google ID Token để không bị tối ưu hóa/strip mất ở release build.
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class androidx.credentials.** { *; }

# === Firestore / Gson model classes ===
# Giữ nguyên toàn bộ class name và field name trong package model và firestore.
# Giúp Gson, Room và Firestore hoạt động bằng reflection ổn định ở bản release.
-keep class com.giathinh.canlua.data.firestore.** { *; }
-keep class com.giathinh.canlua.data.model.** { *; }

# === DTOs & Models used for JSON serialization/deserialization ===
-keep class com.giathinh.canlua.data.remote.ai.** { *; }
-keep class com.giathinh.canlua.repository.RicePriceDto { *; }
-keep class com.giathinh.canlua.repository.NewsArticleDto { *; }
-keep class com.giathinh.canlua.repository.KnowledgeBaseRepository$KnowledgeEntry { *; }
-keep class com.giathinh.canlua.repository.KnowledgeBaseRepository$KbFile { *; }
-keep class com.giathinh.canlua.util.BackupManager$CardBackupWrapper { *; }
-keep class com.giathinh.canlua.util.BackupManager$BackupPayload { *; }

# === Room entity & DAO ===
# Room tạo implementation bằng annotation processor — không cần keep DAO interface
# nhưng cần giữ tên Entity class và field có @ColumnInfo / @PrimaryKey.
-keepclassmembers @androidx.room.Entity class * {
    <fields>;
    <init>(...);
}

# === Hilt / Dagger ===
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
}

# === Kotlin Serialization ===
-dontnote kotlinx.serialization.AnnotationsKt

# === Markwon (Markdown renderer cho AI chat) ===
# Markwon dùng reflection để scan classpath tìm Linkify/Strikethrough extensions
# và nội bộ TablePlugin cũng reflection. Nếu R8 obfuscate →
# NoSuchMethodError/ClassNotFoundException → crash app khi render AI response ở release.
-keep class io.noties.markwon.** { *; }
-keep interface io.noties.markwon.** { *; }
-dontwarn io.noties.markwon.**


# === Strip verbose debug logs trong release ===
# Log.i/w/e/wtf vẫn giữ để Crashlytics + adb logcat khi user report bug.
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}
