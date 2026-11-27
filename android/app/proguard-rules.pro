# ProGuard rules for OpenTTD Android port
-keep class org.libsdl.app.** { *; }
-keep class org.openttd.openttd.** { *; }
-keepclassmembers class * {
    native <methods>;
}

