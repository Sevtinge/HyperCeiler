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

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.libhook.provider.SharedPrefsProvider;
import com.sevtinge.hyperceiler.libhook.utils.devices.ProjectApi;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import io.github.libxposed.service.RemotePreferences;

public class PrefsUtils {

    public static SharedPreferences mSharedPreferences = null;
    public static RemotePreferences remotePrefs = null;
    public static PrefsMap<String, Object> mPrefsMap = new PrefsMap<>();

    public static String mPrefsPathCurrent = null;
    public static String mPrefsFileCurrent = null;
    public static String mPrefsName = "hyperceiler_prefs";
    public static String mPrefsPath = "/data/user_de/0/" + ProjectApi.mAppModulePkg + "/shared_prefs";
    public static String mPrefsFile = mPrefsPath + "/" + mPrefsName + ".xml";

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

    public static String getSharedPrefsPath() {
        if (mPrefsPathCurrent == null) try {
            Field mFile = mSharedPreferences.getClass().getDeclaredField("mFile");
            mFile.setAccessible(true);
            mPrefsPathCurrent = ((File) mFile.get(mSharedPreferences)).getParentFile().getAbsolutePath();
            return mPrefsPathCurrent;
        } catch (Throwable t) {
            return mPrefsPath;
        }
        else return mPrefsPathCurrent;
    }

    public static String getSharedPrefsFile() {
        if (mPrefsFileCurrent == null) try {
            Field fFile = mSharedPreferences.getClass().getDeclaredField("mFile");
            fFile.setAccessible(true);
            mPrefsFileCurrent = ((File) fFile.get(mSharedPreferences)).getAbsolutePath();
            return mPrefsFileCurrent;
        } catch (Throwable t) {
            return mPrefsFile;
        }
        else return mPrefsFileCurrent;
    }

    public static boolean contains(String key) {
        return mSharedPreferences.contains(key);
    }

    public static void putString(String key, String defValue) {
        mSharedPreferences.edit().putString(key, defValue).apply();
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
        context = AppsTool.getProtectedContext(context);
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

    /**
     * 获取 StringSet 类型的偏好设置
     */
    public static Set<String> getSharedStringSetPrefs(Context context, String name) {
        try {
            SharedPreferences prefs = getSharedPrefs(context);
            Set<String> result = prefs.getStringSet(name, null);
            if (result != null) {
                return result;
            }
        } catch (Throwable ignored) {
        }

        LinkedHashSet<String> empty = new LinkedHashSet<>();
        if (mPrefsMap.containsKey(name)) {
            return (Set<String>) mPrefsMap.getObject(name, empty);
        } else {
            return empty;
        }
    }

    /**
     * 注册偏好设置变更监听器
     */
    public static void registerOnSharedPreferenceChangeListener(Context context) {
        mSharedPreferences.registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> {
            Log.i("prefs", "Changed: " + key);
            AppsTool.requestBackup(context);
            Object val = sharedPreferences.getAll().get(key);
            String path = "";
            if (val instanceof String)
                path = "string/";
            else if (val instanceof Set<?>)
                path = "stringset/";
            else if (val instanceof Integer)
                path = "integer/";
            else if (val instanceof Boolean)
                path = "boolean/";

            ContentResolver resolver = context.getContentResolver();
            resolver.notifyChange(Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/" + path + key), null);
            if (!path.isEmpty()) resolver.notifyChange(Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/pref/" + path + key), null);
        });
    }
}
