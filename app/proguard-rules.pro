# DarkFrame release rules.
#
# The app has no reflection of its own, so almost nothing needs keeping — the engine is plain
# Kotlin and the UI is inflated from XML, which R8 already understands. The entries below cover the
# two places where something outside our code reaches in.

# Views inflated from layout XML are constructed reflectively by LayoutInflater.
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# AppWidgetProvider subclasses are instantiated by the system from the manifest.
-keep class com.darkframe.icons.widget.** { *; }

# Google Play Billing ships its own consumer rules; this only silences warnings about the optional
# parts of the library that DarkFrame does not use.
-dontwarn com.android.billingclient.**
