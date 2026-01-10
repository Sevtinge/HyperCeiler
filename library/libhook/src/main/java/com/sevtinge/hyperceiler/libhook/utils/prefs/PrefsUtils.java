/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.utils.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.util.HashSet;

import io.github.libxposed.service.RemotePreferences;

public class PrefsUtils {

    public static SharedPreferences mSharedPreferences = null;
    public static RemotePreferences remotePrefs = null;
    public static PrefsMap<String, Object> mPrefsMap = new PrefsMap<>();
    public static String mPrefsName = "hyperceiler_prefs";
    private static final HashSet<PreferenceObserver> prefObservers = new HashSet<>();

    public interface PreferenceObserver {
        void onChange(String key);
    }

    public static void observePreferenceChange(PreferenceObserver prefObserver) {
        prefObservers.add(prefObserver);
    }

    public static void handlePreferenceChanged(@Nullable String key) {
        for (PreferenceObserver prefObserver : prefObservers) {
            prefObserver.onChange(key);
        }
    }

    /**
     * 获取 SharedPreferences Editor
     */
    public static SharedPreferences.Editor editor() {
        if (mSharedPreferences != null) {
            return mSharedPreferences.edit();
        }
        throw new IllegalStateException("SharedPreferences not initialized");
    }

    /**
     * 获取 SharedPreferences
     */
    public static SharedPreferences getSharedPrefs(Context context, boolean multiProcess) {
        try {
            return context.getSharedPreferences(mPrefsName, multiProcess ? Context.MODE_MULTI_PROCESS | Context.MODE_WORLD_READABLE : Context.MODE_WORLD_READABLE);
        } catch (Throwable t) {
            return context.getSharedPreferences(mPrefsName, multiProcess ? Context.MODE_MULTI_PROCESS | Context.MODE_PRIVATE : Context.MODE_PRIVATE);
        }
    }

    public static SharedPreferences getSharedPrefs(Context context) {
        return getSharedPrefs(context, false);
    }

    /**
     * 获取 Boolean 类型的偏好设置
     */
    public static boolean getSharedBoolPrefs(Context context, String name, boolean defValue) {
        try {
            SharedPreferences prefs = getSharedPrefs(context);
            return prefs.getBoolean(name, defValue);
        } catch (Throwable t) {
            if (mPrefsMap.containsKey(name))
                return (boolean) mPrefsMap.getObject(name, defValue);
            else
                return defValue;
        }
    }

    /**
     * 获取 String 类型的偏好设置
     */
    public static String getSharedStringPrefs(Context context, String name, String defValue) {
        try {
            SharedPreferences prefs = getSharedPrefs(context);
            return prefs.getString(name, defValue);
        } catch (Throwable t) {
            if (mPrefsMap.containsKey(name))
                return (String) mPrefsMap.getObject(name, defValue);
            else
                return defValue;
        }
    }

    /**
     * 获取 Int 类型的偏好设置
     */
    public static int getSharedIntPrefs(Context context, String name, int defValue) {
        try {
            SharedPreferences prefs = getSharedPrefs(context);
            return prefs.getInt(name, defValue);
        } catch (Throwable t) {
            if (mPrefsMap.containsKey(name))
                return (int) mPrefsMap.getObject(name, defValue);
            else
                return defValue;
        }
    }
}
