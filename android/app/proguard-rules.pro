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

# Preserve line number information for crash stack traces.
-keepattributes SourceFile,LineNumberTable

# Hide original source file name in stack traces.
-renamesourcefileattribute SourceFile

# Strip debug and verbose log calls in release builds.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

# Keep Room entities and DAOs.
-keep class com.ethiobalance.app.data.** { *; }

# Keep Kotlin metadata for Hilt and Room annotation processing.
-keepattributes *Annotation*
-keep class kotlin.Metadata { *; }
