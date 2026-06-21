# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# === Source info — giữ để Crashlytics stack trace có line numbers ===
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# === Firestore / Gson model classes ===
# Firestore dùng reflection để deserialize — phải giữ tên field gốc.
# Áp dụng cho tất cả class trong package data.firestore và data.model
# có annotation @SerializedName hoặc field public.
-keep class com.giathinh.canlua.data.firestore.** { *; }
-keepclassmembers class com.giathinh.canlua.data.model.** {
    <init>();
    <fields>;
}

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
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# === Markwon (Markdown renderer cho AI chat) ===
# Markwon dùng reflection để scan classpath tìm Linkify/Strikethrough extensions
# và nội bộ TablePlugin cũng reflection. Nếu R8 obfuscate →
# NoSuchMethodError/ClassNotFoundException → crash app khi render AI response ở release.
-keep class io.noties.markwon.** { *; }
-keep interface io.noties.markwon.** { *; }
-dontwarn io.noties.markwon.**

# === OpenRouter AI (data.remote.ai.*) ===
# Gson reflection dùng tên field gốc để parse response từ OpenRouter.
# Nếu không keep, R8 obfuscate field name (content → a, choices → b) →
# Gson đọc field lỗi → choices = emptyList() → AI trả message rỗng.
-keep class com.giathinh.canlua.data.remote.ai.** { *; }
-keepclassmembers class com.giathinh.canlua.data.remote.ai.** {
    <init>();
    <fields>;
}

# === Strip verbose debug logs trong release ===
# Log.i/w/e/wtf vẫn giữ để Crashlytics + adb logcat khi user report bug.
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}
