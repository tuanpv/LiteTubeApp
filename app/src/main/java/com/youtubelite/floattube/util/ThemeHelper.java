package com.youtubelite.floattube.util;

import android.content.Context;
import android.preference.PreferenceManager;

public class ThemeHelper {

    /**
     * Apply the selected theme (on NewPipe settings) in the context
     *
     * @param context context that the theme will be applied
     */
    public static void setTheme(Context context) {

        String themeKey = context.getString(com.youtubelite.floattube.R.string.theme_key);
        String darkTheme = context.getResources().getString(com.youtubelite.floattube.R.string.dark_theme_title);
        String blackTheme = context.getResources().getString(com.youtubelite.floattube.R.string.black_theme_title);

        String sp = PreferenceManager.getDefaultSharedPreferences(context).getString(themeKey, context.getResources().getString(com.youtubelite.floattube.R.string.light_theme_title));

        if (sp.equals(darkTheme)) context.setTheme(com.youtubelite.floattube.R.style.DarkTheme);
        else if (sp.equals(blackTheme)) context.setTheme(com.youtubelite.floattube.R.style.BlackTheme);
        else context.setTheme(com.youtubelite.floattube.R.style.AppTheme);
    }

    /**
     * Return true if the selected theme (on NewPipe settings) is the Light theme
     *
     * @param context context to get the preference
     */
    public static boolean isLightThemeSelected(Context context) {
        String themeKey = context.getString(com.youtubelite.floattube.R.string.theme_key);
        String darkTheme = context.getResources().getString(com.youtubelite.floattube.R.string.dark_theme_title);
        String blackTheme = context.getResources().getString(com.youtubelite.floattube.R.string.black_theme_title);

        String sp = PreferenceManager.getDefaultSharedPreferences(context).getString(themeKey, context.getResources().getString(com.youtubelite.floattube.R.string.light_theme_title));

        return !(sp.equals(darkTheme) || sp.equals(blackTheme));
    }
}
