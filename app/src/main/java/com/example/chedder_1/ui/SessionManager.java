package com.example.chedder_1.ui;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREFS = "session_prefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_ROLE = "role";

    private final SharedPreferences sp;

    public SessionManager(Context context) {
        sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void saveSession(int userId, String role) {
        sp.edit()
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_ROLE, role)
                .apply();
    }

    public int getUserId() {
        return sp.getInt(KEY_USER_ID, -1);
    }

    public String getRole() {
        return sp.getString(KEY_ROLE, null);
    }

    public boolean isLoggedIn() {
        return getUserId() != -1;
    }

    public void clear() {
        sp.edit().clear().apply();
    }
}
