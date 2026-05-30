package com.example.ticktok;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;


public class TickTokApplication extends Application {

    public static final String PREFS_SETTINGS = "prefs_settings";
    public static final String KEY_THEME_MODE = "theme_mode";

    // Persisted values
    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    @Override
    public void onCreate() {
        super.onCreate();
        applyThemeFromPreferences();
    }

    private void applyThemeFromPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE);
        int themeMode = prefs.getInt(KEY_THEME_MODE, THEME_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(mapToNightMode(themeMode));
    }

    public static int mapToNightMode(int themeMode) {
        if (themeMode == THEME_LIGHT) {
            return AppCompatDelegate.MODE_NIGHT_NO;
        }
        if (themeMode == THEME_DARK) {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }
}

