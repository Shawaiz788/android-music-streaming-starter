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
        return prefs.getString(KEY_THEME, "teal");
    }

    public static int getThemeDrawable(String themeName) {
        switch (themeName) {
            case "orange": return R.drawable.bg_gradient_orange;
            case "purple": return R.drawable.bg_gradient_purple;
            case "blue": return R.drawable.bg_gradient_blue;
            case "red": return R.drawable.bg_gradient_red;
            case "black": return R.drawable.bg_gradient_black;
            case "teal":
            default: return R.drawable.bg_gradient_teal;
        }
    }

    public static void applyTheme(View view) {
        if (view == null) return;
        String theme = getTheme(view.getContext());
        view.setBackgroundResource(getThemeDrawable(theme));
    }
}
