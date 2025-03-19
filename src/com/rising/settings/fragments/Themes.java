/*
 * Copyright (C) 2023-2024 the risingOS Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rising.settings.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.internal.logging.nano.MetricsProto;
import androidx.fragment.app.Fragment;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.utils.SystemRestartUtils;
import com.android.settingslib.search.SearchIndexable;
import com.android.settings.dashboard.DashboardFragment;

import com.android.internal.util.android.ThemeUtils;

import java.util.List;

@SearchIndexable
public class Themes extends DashboardFragment implements
        Preference.OnPreferenceChangeListener {

    public static final String TAG = "Themes";
    private static final String KEY_PGB_STYLE = "progress_bar_style";
    private static final String KEY_NOTIF_STYLE = "notification_style";
    private static final String KEY_POWERMENU_STYLE = "powermenu_style";
    private static final String KEY_HIDE_IME_STYLE = "hide_ime_space_style";
    private static final String CUSTOM_FONT_PREFERENCE_KEY = "custom_font_preference";

    private static final String[] POWER_MENU_OVERLAYS = {
            "com.android.theme.powermenu.cyberpunk",
            "com.android.theme.powermenu.duoline",
            "com.android.theme.powermenu.ios",
            "com.android.theme.powermenu.layers"
    };

    private static final String[] NOTIF_OVERLAYS = {
            "com.android.theme.notification.cyberpunk",
            "com.android.theme.notification.duoline",
            "com.android.theme.notification.ios",
            "com.android.theme.notification.layers"
    };

    private static final String[] PROGRESS_BAR_OVERLAYS = {
            "com.android.theme.progressbar.blocky_thumb",
            "com.android.theme.progressbar.minimal_thumb",
            "com.android.theme.progressbar.outline_thumb",
            "com.android.theme.progressbar.shishu"
    };

    private static final String[] HIDE_IME_OVERLAYS = {
            "com.android.system.theme.hide_ime_space_narrow",
            "com.android.system.theme.hide_ime_space_no_space",
    };

    private ThemeUtils mThemeUtils;
    private Preference mProgressBarPref;
    private Preference mNotificationStylePref;
    private Preference mPowerMenuStylePref;
    private Preference mHideImePref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rising_settings_themes);
        mThemeUtils = ThemeUtils.getInstance(getActivity());

        mProgressBarPref = findPreference(KEY_PGB_STYLE);
        mProgressBarPref.setOnPreferenceChangeListener(this);

        mNotificationStylePref = findPreference(KEY_NOTIF_STYLE);
        mNotificationStylePref.setOnPreferenceChangeListener(this);

        mPowerMenuStylePref = findPreference(KEY_POWERMENU_STYLE);
        mPowerMenuStylePref.setOnPreferenceChangeListener(this);

        mHideImePref = findPreference(KEY_HIDE_IME_STYLE);
        mHideImePref.setOnPreferenceChangeListener(this);

        com.android.settingslib.widget.LayoutPreference highlightPref = getPreferenceScreen().findPreference("themes_highlight_dashboard");
        if (highlightPref != null) {
            java.util.Map<Integer, String> highlightClickMap = new java.util.HashMap<>();
            highlightClickMap.put(R.id.boot_styles_tile, "PersonalizationsBSActivity");
            highlightClickMap.put(R.id.icon_pack_tile, "PersonalizationsIconPackActivity");
            highlightClickMap.put(R.id.settings_tile, "PersonalizationsSettingsUIActivity");
            highlightClickMap.put(R.id.wallpaper_styles_tile, "PersonalizationsWSActivity");
            com.android.settings.utils.HighlightPrefUtils.Companion.setupHighlightPref(getContext(), highlightPref, highlightClickMap);
        }
    }

    private void updateStyle(String key, String category, String target, 
            int defaultValue, String[] overlayPackages, boolean restartSystemUI) {
        final int style = Settings.System.getIntForUser(
                getContext().getContentResolver(),
                key,
                defaultValue,
                UserHandle.USER_CURRENT
        );
        if (mThemeUtils == null) {
            mThemeUtils = ThemeUtils.getInstance(getContext());
        }
        mThemeUtils.setOverlayEnabled(category, target, target);
        if (style > 0 && style <= overlayPackages.length) {
            mThemeUtils.setOverlayEnabled(category, overlayPackages[style - 1], target);
        }
        if (restartSystemUI) {
            SystemRestartUtils.restartSystemUI(getContext());
        }
    }

    private void updatePowerMenuStyle() {
        updateStyle(KEY_POWERMENU_STYLE, "android.theme.customization.powermenu", "com.android.systemui", 0, POWER_MENU_OVERLAYS, false);
    }

    private void updateNotifStyle() {
        updateStyle(KEY_NOTIF_STYLE, "android.theme.customization.notification", "com.android.systemui", 0, NOTIF_OVERLAYS, true);
    }

    private void updateProgressBarStyle() {
        updateStyle(KEY_PGB_STYLE, "android.theme.customization.progress_bar", "android", 0, PROGRESS_BAR_OVERLAYS, false);
    }

    private void updateHideImeSpaceStyle() {
        updateStyle(KEY_HIDE_IME_STYLE, "android.theme.customization.hide_ime_space", "android", 0, HIDE_IME_OVERLAYS, false);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int value = Integer.parseInt((String) newValue);

        if (preference == mProgressBarPref) {
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    KEY_PGB_STYLE, value, UserHandle.USER_CURRENT);
            updateProgressBarStyle();
            return true;
        } else if (preference == mNotificationStylePref) {
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    KEY_NOTIF_STYLE, value, UserHandle.USER_CURRENT);
            updateNotifStyle();
            return true;
        } else if (preference == mPowerMenuStylePref) {
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    KEY_POWERMENU_STYLE, value, UserHandle.USER_CURRENT);
            updatePowerMenuStyle();
            return true;
        } else if (preference == mHideImePref) {
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    KEY_HIDE_IME_STYLE, value, UserHandle.USER_CURRENT);
            updateHideImeSpaceStyle();
            return true;
        }
        return false;
    }


    @Override
     protected String getLogTag() {
         return TAG;
     }

     @Override
     protected int getPreferenceScreenResId() {
         return R.xml.rising_settings_themes;
     }

     @Override
     protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
         return buildPreferenceControllers(context, getSettingsLifecycle(), this);
     }

     private static List<AbstractPreferenceController> buildPreferenceControllers(
             Context context, Lifecycle lifecycle, ThemeSettings fragment) {
         final List<AbstractPreferenceController> controllers = new ArrayList<>();
         controllers.add(new CustomFontPreferenceController(context, CUSTOM_FONT_PREFERENCE_KEY, fragment, lifecycle));
         return controllers;
     }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }

    /**
     * For search
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.rising_settings_themes) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

                    return keys;
                }
            };
}
