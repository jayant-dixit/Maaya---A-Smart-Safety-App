# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* *;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
