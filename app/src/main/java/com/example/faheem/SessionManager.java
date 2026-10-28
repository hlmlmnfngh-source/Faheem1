package com.example.faheem;

import android.content.Context;
import android.content.SharedPreferences;

/** Stores only the identifier of the authenticated local user. */
public final class SessionManager {
    private static final String PREFS = "faheem_session";
    private static final String KEY_USER_ID = "user_id";
    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void startSession(int userId) { preferences.edit().putInt(KEY_USER_ID, userId).apply(); }
    public int getUserId() { return preferences.getInt(KEY_USER_ID, -1); }
    public void clearSession() { preferences.edit().remove(KEY_USER_ID).apply(); }
}
