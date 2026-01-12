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
package com.sevtinge.hyperceiler.libhook.base;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isAndroidVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isHyperOSVersion;
import static com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils.mPrefsMap;
import static java.util.Arrays.asList;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.libhook.safecrash.CrashHook;
import com.sevtinge.hyperceiler.libhook.safecrash.RescuePartyPlus;
import com.sevtinge.hyperceiler.libhook.safecrash.SafeMode;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.ResourcesTool;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.utils.pkg.CheckModifyUtils;
import com.sevtinge.hyperceiler.libhook.utils.pkg.DebugModeUtils;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;
import com.sevtinge.hyperceiler.module.base.DataBase;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import io.github.kyuubiran.ezxhelper.xposed.EzXposed;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

/**
 * Xposed 模块入口基类
 *
 * @author HyperCeiler
 */
public class XposedInitEntry extends XposedModule {

    private static final String TAG = "HyperCeiler";
    private final ArrayList<String> checkList = new ArrayList<>(asList(
        "com.miui.securitycenter",
        "com.android.camera",
        "com.miui.home"
    ));
    public static ResourcesTool mResHook;
    protected String processName;
    protected SharedPreferences remotePrefs;
    protected SharedPreferences.OnSharedPreferenceChangeListener mListener;

    public XposedInitEntry(@NonNull XposedInterface base, @NonNull ModuleLoadedParam param) {
        super(base, param);
        processName = param.getProcessName();
        XposedLog.init(base);
        BaseLoad.init(base);
        mResHook = ResourcesTool.getInstance(base.getApplicationInfo().sourceDir);
        EzXposed.initXposedModule(base);
    }

    @Override
    public void onSystemServerLoaded(@NonNull final SystemServerLoadedParam lpparam) {
        // load preferences
        initPrefs();

        // load CrashHook
        try {
            new CrashHook(lpparam);
            RescuePartyPlus rescuePartyPlus = RescuePartyPlus.INSTANCE;
            rescuePartyPlus.setHandler(SafeMode.INSTANCE);
            rescuePartyPlus.init();
        } catch (Exception e) {
            XposedLog.e(TAG, "Crash Hook load failed, " + e);
        }

        // Sync preferences changes
        loadPreferenceChange();
    }

    @Override
    public void onPackageLoaded(@NonNull final PackageLoadedParam lpparam) {
        super.onPackageLoaded(lpparam);
        if (!lpparam.isFirstPackage()) return;
        // load preferences
        initPrefs();
        // load EzXposed
        EzXposed.initOnPackageLoaded(lpparam);
        // invoke module
        invokeInit(lpparam);
        // Sync preferences changes
        loadPreferenceChange();
    }

    protected void invokeInit(PackageLoadedParam lpparam) {
        String packageName = lpparam.getPackageName();
        HashMap<String, DataBase> dataMap = DataBase.get();
        HashSet<String> checkSet = new HashSet<>(checkList);
        boolean isDebug = mPrefsMap.getBoolean("development_debug_mode");

        ClassLoader loader = getClass().getClassLoader();
        if (loader == null) {
            XposedLog.e(TAG, "ClassLoader is null, skip loading modules for: " + packageName);
            return;
        }

        if (dataMap.values().stream().noneMatch(data -> data.mTargetPackage.equals(packageName))) {
            onNoMatchedPackage(lpparam);
            return;
        }

        dataMap.forEach((className, data) -> {
            if (!packageName.equals(data.mTargetPackage)) return;
            if (data.mTargetSdk != -1 && !isAndroidVersion(data.mTargetSdk)) return;
            if (data.mTargetOSVersion != -1F && !isHyperOSVersion(data.mTargetOSVersion)) return;

            if (checkSet.contains(packageName)) {
                boolean check = CheckModifyUtils.INSTANCE.getCheckResult(packageName);
                boolean isVersion = DebugModeUtils.INSTANCE.getChooseResult(packageName) == 0;
                if (check && !isDebug && isVersion) return;
            }

            try {
                Class<?> clazz = loader.loadClass(className);
                BaseLoad module = (BaseLoad) clazz.getDeclaredConstructor().newInstance();
                module.onLoad(lpparam);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                     InstantiationException | InvocationTargetException e) {
                XposedLog.e(TAG, "Failed to load module: " + className, e);
            }
        });
    }

    protected void onNoMatchedPackage(PackageLoadedParam lpparam) {
    }


    protected void initPrefs() {
        SharedPreferences readPrefs = getRemotePreferences(PrefsUtils.mPrefsName + "_remote");
        Map<String, ?> allPrefs = readPrefs.getAll();
        if (allPrefs != null && !allPrefs.isEmpty()) {
            mPrefsMap.putAll(allPrefs);
        }
    }

    protected void loadPreferenceChange() {
        HashSet<String> ignoreKeys = new HashSet<>();

        mListener = (sharedPreferences, key) -> {
            Object val = sharedPreferences.getAll().get(key);
            if (val == null) {
                mPrefsMap.remove(key);
            } else {
                mPrefsMap.put(key, val);
            }
            if (!ignoreKeys.contains(key)) {
                PrefsUtils.handlePreferenceChanged(key);
            }
        };
        remotePrefs = getRemotePreferences(PrefsUtils.mPrefsName + "_remote");
        remotePrefs.registerOnSharedPreferenceChangeListener(mListener);
    }
}
