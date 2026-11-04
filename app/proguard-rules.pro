# Strip verbose/debug logging from release builds so no notification text can reach logcat.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
