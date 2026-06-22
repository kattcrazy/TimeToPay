-keepclassmembers class * extends android.app.Service {
    public <init>(...);
}

-keepclassmembers class android.nfc.NfcAdapter {
    public boolean enable();
    public boolean disable(boolean);
}

-keep class kattcrazy.timetopay.TimeToPayAccessibilityService { *; }

-dontwarn android.nfc.NfcAdapter
