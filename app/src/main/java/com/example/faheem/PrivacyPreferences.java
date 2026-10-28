package com.example.faheem;

import android.content.Context;

public final class PrivacyPreferences {
    private static final String PREFS = "privacy_preferences";
    private static final String KEY_CONSENT = "remote_processing_accepted";

    private PrivacyPreferences() { }

    public static boolean hasRemoteProcessingConsent(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_CONSENT, false);
    }

    public static void acceptRemoteProcessing(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_CONSENT, true)
                .apply();
    }
}
