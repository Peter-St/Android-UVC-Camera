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
-dontwarn java.awt.*
-keep class com.sun.jna.* { *; }
-keepclassmembers class * extends com.sun.jna.* { public *; }
-dontwarn android.support.**
-keep class org.webrtc.**  { *; }
-keep class org.appspot.apprtc.**  { *; }
-keep class de.tavendo.autobahn.**  { *; }
-keep class de.tavendo.autobanh.**  { *; }
-keepclasseswithmembernames class * { native <methods>; }

# Play Console Warning
-keep class humer.UvcCamera.** { *; }
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose
-keep class com.example.androidthings.videortc.** { *; }
-keep class com.freeapps.hosamazzam.androidchangelanguage.** { *; }
-keep class com.sample.timelapse.** { *; }
-keep class noman.zoomtextview.** { *; }

-printmapping mapping.txt