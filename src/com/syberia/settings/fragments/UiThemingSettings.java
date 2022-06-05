/*
 * Copyright © 2018-2021 Syberia Project
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

package com.syberia.settings.fragments;

import static android.os.UserHandle.USER_CURRENT;
import static android.os.UserHandle.USER_SYSTEM;

import android.content.Context;
import android.content.ContentResolver;
import android.content.om.IOverlayManager;
import android.database.ContentObserver;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import com.android.settings.R;
import android.net.Uri;

import com.android.settings.dashboard.DashboardFragment;

import androidx.preference.PreferenceScreen;
import androidx.preference.Preference;
import androidx.preference.ListPreference;
import androidx.preference.SwitchPreference;
import androidx.preference.PreferenceCategory;
import androidx.preference.Preference.OnPreferenceChangeListener;

import android.provider.SearchIndexableResource;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;

import com.android.internal.logging.nano.MetricsProto;

import com.syberia.settings.preference.SystemSettingListPreference;
import com.android.settings.development.OverlayCategoryPreferenceController;

import android.provider.Settings;
import android.os.UserHandle;

import org.json.JSONException;
import org.json.JSONObject;

import net.margaritov.preference.colorpicker.ColorPickerPreference;
import com.syberia.settings.preference.CustomSeekBarPreference;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class UiThemingSettings extends DashboardFragment implements OnPreferenceChangeListener {

    private static final String TAG = "UiThemingSettings";

    private Handler mHandler;
    private IOverlayManager mOverlayManager;
    private IOverlayManager mOverlayService;
    private ListPreference mLockClockStyles;

    private static final String WALLPAPER_KEY = "monet_engine_use_wallpaper_color";
    private static final String COLOR_KEY = "monet_engine_color_override";
    private static final String CHROMA_KEY = "monet_engine_chroma_factor";
    private static final String CUSTOM_CLOCK_FACE = Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK_FACE;
    private static final String DEFAULT_CLOCK = "com.android.keyguard.clock.DefaultClockController";

    SwitchPreference mUseWall;
    ColorPickerPreference mColorOvr;
    CustomSeekBarPreference mChroma;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final PreferenceScreen screen = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        mOverlayService = IOverlayManager.Stub
                .asInterface(ServiceManager.getService(Context.OVERLAY_SERVICE));

        mUseWall = findPreference(WALLPAPER_KEY);
        mColorOvr = findPreference(COLOR_KEY);
        String color = Settings.Secure.getString(resolver, COLOR_KEY);
        boolean useWall = color == null || color.isEmpty();
        mUseWall.setChecked(useWall);
        mColorOvr.setEnabled(!useWall);
        if (!useWall) mColorOvr.setNewPreviewColor(
                ColorPickerPreference.convertToColorInt(color));
        mUseWall.setOnPreferenceChangeListener(this);
        mColorOvr.setOnPreferenceChangeListener(this);

        mChroma = findPreference(CHROMA_KEY);
        float chroma = Settings.Secure.getFloat(resolver, CHROMA_KEY, 1) * 100;
        mChroma.setValue(Math.round(chroma));
        mChroma.setOnPreferenceChangeListener(this);

        mLockClockStyles = (ListPreference) findPreference(CUSTOM_CLOCK_FACE);
        String mLockClockStylesValue = getLockScreenCustomClockFace();
        mLockClockStyles.setValue(mLockClockStylesValue);
        mLockClockStyles.setSummary(mLockClockStyles.getEntry());
        mLockClockStyles.setOnPreferenceChangeListener(this);

    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SYBERIA;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.ui_theming;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mUseWall) {
            boolean value = (Boolean) newValue;
            mColorOvr.setEnabled(!value);
            if (value) Settings.Secure.putString(resolver, COLOR_KEY, "");
            return true;
        } else if (preference == mColorOvr) {
            int value = (Integer) newValue;
            Settings.Secure.putString(resolver, COLOR_KEY,
                    ColorPickerPreference.convertToRGB(value));
            return true;
        } else if (preference == mChroma) {
            int value = (Integer) newValue;
            Settings.Secure.putFloat(resolver, CHROMA_KEY, value / 100f);
            return true;
        } else if (preference == mLockClockStyles) {
            setLockScreenCustomClockFace((String) newValue);
            int index = mLockClockStyles.findIndexOfValue((String) newValue);
            mLockClockStyles.setSummary(mLockClockStyles.getEntries()[index]);
            return true;
        }
        return false;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.ui_theming) {

            };

    private String getLockScreenCustomClockFace() {
        String value = Settings.Secure.getStringForUser(getActivity().getContentResolver(),
                CUSTOM_CLOCK_FACE, USER_CURRENT);

        if (value == null || value.isEmpty()) value = DEFAULT_CLOCK;

        try {
            JSONObject json = new JSONObject(value);
            return json.getString("clock");
        } catch (JSONException ex) {
        }
        return value;
    }

    private void setLockScreenCustomClockFace(String value) {
        try {
            JSONObject json = new JSONObject();
            json.put("clock", value);
            Settings.Secure.putStringForUser(getActivity().getContentResolver(), CUSTOM_CLOCK_FACE,
                    json.toString(), USER_CURRENT);
        } catch (JSONException ex) {
        }
    }

}
