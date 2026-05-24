# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Strip verbose/debug logs trong release. Log.i/w/e/wtf vẫn giữ để Crashlytics
# + adb logcat khi user report bug. Loại bỏ Log.d/v giảm size APK + ẩn thông
# tin nội bộ (paths, document IDs, query params) khỏi release build.
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}