package com.example.musicplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

public class ThemeHelper {
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME = "selected_theme";

    public static void saveTheme(Context context, String themeName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_THEME, themeName).apply();
    }

    public static String getTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_THEME, "black");
    }

    public static int getThemeDrawable(String themeName) {
        switch (themeName) {
            case "orange": return R.drawable.bg_gradient_orange;
            case "purple": return R.drawable.bg_gradient_purple;
            case "blue": return R.drawable.bg_gradient_blue;
            case "red": return R.drawable.bg_gradient_red;
            case "teal": return R.drawable.bg_gradient_teal;
            case "black":
            default: return R.drawable.bg_gradient_black;
        }
    }

    public static int getHeaderColor(String themeName) {
        switch (themeName) {
            case "orange": return 0xFF2E2016;
            case "purple": return 0xFF24143D;
            case "blue": return 0xFF0D325E;
            case "red": return 0xFF4B0E0E;
            case "teal": return 0xFF142F2B;
            case "black":
            default: return 0xFF121212;
        }
    }

    public static int getAccentColor(String themeName) {
        switch (themeName) {
            case "orange": return 0xFFB69F90;
            case "purple": return 0xFF9B8AB3;
            case "blue": return 0xFF7AA3CC;
            case "red": return 0xFFD47979;
            case "teal": return 0xFF86ADA8;
            case "black":
            default: return 0xFF333333;
        }
    }

    public static void applyTheme(View view) {
        if (view == null) return;
        String theme = getTheme(view.getContext());
        view.setBackgroundResource(getThemeDrawable(theme));

        // Try to find status bar spacer and top bar if they exist
        View statusBarSpacer = view.findViewById(R.id.statusBarSpacer);
        View topBar = view.findViewById(R.id.topBar);
        int headerColor = getHeaderColor(theme);
        
        if (statusBarSpacer != null) statusBarSpacer.setBackgroundColor(headerColor);
        if (topBar != null) topBar.setBackgroundColor(headerColor);

        // Handle buttons or other accented elements
        View btnQuit = view.findViewById(R.id.btnQuit);
        if (btnQuit != null) {
            int accentColor = getAccentColor(theme);
            if (btnQuit.getBackground() != null) {
                btnQuit.getBackground().mutate().setTint(accentColor);
            }
        }
    }
}
