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

import static com.sevtinge.hyperceiler.libhook.utils.devices.MiDeviceAppUtilsKt.isPad;
import static com.sevtinge.hyperceiler.libhook.utils.devices.SystemSDKKt.isAndroidVersion;
import static com.sevtinge.hyperceiler.libhook.utils.devices.SystemSDKKt.isHyperOSVersion;
import static com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils.mPrefsMap;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.HookTool;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.utils.pkg.DebugModeUtils;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

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
    protected String processName;
    protected SharedPreferences remotePrefs;
    protected SharedPreferences.OnSharedPreferenceChangeListener mListener;

    public XposedInitEntry(@NonNull XposedInterface base, @NonNull ModuleLoadedParam param) {
        super(base, param);
        processName = param.getProcessName();
        XposedLog.init(base);
        HookTool.init(base);
        EzXposed.initXposedModule(base);
    }

    @Override
    public void onSystemServerLoaded(@NonNull final SystemServerLoadedParam lpparam) {
        // load preferences
        initPrefs();

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
        if (dataMap.values().stream().noneMatch(data -> data.mTargetPackage.equals(packageName))) {
            onNoMatchedPackage(lpparam);
            return;
        }

        dataMap.forEach((className, data) -> {
            if (!packageName.equals(data.mTargetPackage)) return;
            if (data.mTargetSdk != -1 && !isAndroidVersion(data.mTargetSdk)) return;
            if (data.mTargetOSVersion != -1F && !isHyperOSVersion(data.mTargetOSVersion)) return;

            int debugMode = DebugModeUtils.INSTANCE.getChooseResult(packageName);
            if (debugMode != 0) {
                if (data.isPad != debugMode) return;
            } else {
                if ((data.isPad == 1 && !isPad()) || (data.isPad == 2 && isPad())) return;
            }

            loadModule(className, lpparam);
        });
    }

    protected void onNoMatchedPackage(PackageLoadedParam lpparam) {
    }

    protected void loadModule(String className, PackageLoadedParam lpparam) {
        try {
            Class<?> clazz = Objects.requireNonNull(getClass().getClassLoader()).loadClass(className);
            BaseLoad module = (BaseLoad) clazz.getDeclaredConstructor().newInstance();
            module.onLoad(lpparam);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                 InstantiationException | InvocationTargetException e) {
            XposedLog.e(TAG, "Failed to load module: " + className, e);
        }
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
