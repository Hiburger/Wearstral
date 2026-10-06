-keep class org.vosk.** { *; }

# strip verbose/debug/info logging from release builds; warnings and errors stay
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
