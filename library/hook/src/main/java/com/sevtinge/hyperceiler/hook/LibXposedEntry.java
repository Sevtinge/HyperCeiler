/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hook;

import static com.sevtinge.hyperceiler.hook.module.base.tool.AppsTool.getPackageVersionCode;
import static com.sevtinge.hyperceiler.hook.module.base.tool.AppsTool.getPackageVersionName;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.MiDeviceAppUtilsKt.isPad;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.getAndroidVersion;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.getHyperOSVersion;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isAndroidVersion;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isHyperOSVersion;
import static com.sevtinge.hyperceiler.hook.utils.log.LogManager.logLevelDesc;
import static com.sevtinge.hyperceiler.hook.utils.log.XposedLogUtils.logE;
import static com.sevtinge.hyperceiler.hook.utils.log.XposedLogUtils.logI;
import static com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils.mPrefsMap;

import android.content.SharedPreferences;

import com.sevtinge.hyperceiler.hook.module.app.VariousThirdApps;
import com.sevtinge.hyperceiler.hook.module.base.BaseModule;
import com.sevtinge.hyperceiler.hook.module.base.tool.ResourcesTool;
import com.sevtinge.hyperceiler.hook.module.skip.SystemFrameworkForCorePatch;
import com.sevtinge.hyperceiler.hook.safe.CrashHookLX;
import com.sevtinge.hyperceiler.hook.safe.RescuePartyPlusLX;
import com.sevtinge.hyperceiler.hook.safe.SafeMode;
import com.sevtinge.hyperceiler.hook.utils.log.AndroidLogUtils;
import com.sevtinge.hyperceiler.hook.utils.log.XposedLogUtils;
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils;
import com.sevtinge.hyperceiler.module.base.DataBase;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.kyuubiran.ezxhelper.android.logging.Logger;
import io.github.kyuubiran.ezxhelper.xposed.EzXposed;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public class LibXposedEntry extends XposedModule {

    String processName;
    private static final String TAG = "HyperCeiler";
    public static String mModulePath = null;
    public static ResourcesTool mResHook;
    public final VariousThirdApps mVariousThirdApps = new VariousThirdApps();
    SharedPreferences remotePrefs;
    SharedPreferences.OnSharedPreferenceChangeListener mListener;

    public LibXposedEntry(final XposedInterface base, final XposedModuleInterface.ModuleLoadedParam param) {
        super(base, param);
        XposedLogUtils.init(base);
        this.processName = param.getProcessName();
    }

    @Override
    public void onSystemServerLoaded(final SystemServerLoadedParam lpparam) {
        // load SharePrefs
        loadPreferenceChange();
        if (!mPrefsMap.getBoolean("allow_hook")) return;

        // load EzXHelper
        Logger.INSTANCE.setTag(TAG);
        logI("android", "androidVersion = " + getAndroidVersion() + ", hyperosVersion = " + getHyperOSVersion());

    XC_LoadPackage.LoadPackageParam moduleParam = getSystemParam(lpparam); // TODO: 后续迁移掉 legacy param

        // load CorePatch
        try {
            if (mPrefsMap.getBoolean("system_framework_core_patch_enable")) {
                new SystemFrameworkForCorePatch().handleLoadPackage(moduleParam);
            }
        } catch (Throwable e) {
            AndroidLogUtils.logE("SystemFrameworkForCorePatch", "HyperCeiler core patch load failed", e);
        }

        mModulePath = getApplicationInfo().sourceDir;

        // load ResourcesTool
        mResHook = ResourcesTool.getInstance(getApplicationInfo().sourceDir);

        // load Module hook
        androidCrashEventHook(lpparam.getClassLoader());
        invokeSystemInit(moduleParam);
    }

    @Override
    public void onPackageLoaded(final PackageLoadedParam lpparam) {
        if (!mPrefsMap.getBoolean("allow_hook")) return;
        String packageName = lpparam.getPackageName();
        if (SafeMode.isInSafeModeOnHook(packageName) || SafeMode.isInSafeModeByProp(packageName) || !lpparam.isFirstPackage()) {
            return;
        }

        XC_LoadPackage.LoadPackageParam moduleParam = getParam(lpparam); // Todo: 暂无兼容旧 api 的解决方法

        // load EzXHelper and set log tag
        EzXposed.initHandleLoadPackage(moduleParam);
        Logger.INSTANCE.setTag(TAG);

        // load module
        logI(packageName, "versionName = " + getPackageVersionName(moduleParam) + ", versionCode = " + getPackageVersionCode(moduleParam));

        // 资源注入先延后迁移（按你的要求），这里不再依赖 HookTool
        // load Module hook
        invokeInit(moduleParam);
    }

    private void loadPreferenceChange() {
        HashSet<String> ignoreKeys = new HashSet<>();

        if (mPrefsMap.isEmpty()) {
            mListener = (sharedPreferences, key) -> {
                Object val = sharedPreferences.getAll().get(key);
                if (val == null) {
                    // XposedHelpers.log(processName + " key removed: " + key);
                    mPrefsMap.remove(key);
                }
                else {
                    // XposedHelpers.log(processName + " key changed: " + key);
                    mPrefsMap.put(key, val);
                }
                if (!ignoreKeys.contains(key)) {
                    PrefsUtils.handlePreferenceChanged(key);
                }
            };
            remotePrefs = getRemotePreferences(PrefsUtils.mPrefsName + "_remote");

            Map<String, ?> allPrefs = remotePrefs.getAll();
            if (allPrefs != null && !allPrefs.isEmpty()) {
                mPrefsMap.clear();
                mPrefsMap.putAll(allPrefs);
            }

            remotePrefs.registerOnSharedPreferenceChangeListener(mListener);
        }

    }

    private XC_LoadPackage.LoadPackageParam getSystemParam(SystemServerLoadedParam lpparam) {
        XC_LoadPackage.LoadPackageParam xposedParam;
        try {
            Class<?> cSet = Class.forName("de.robv.android.xposed.XposedBridge$CopyOnWriteSortedSet");
            Object emptySet = cSet.getConstructor().newInstance();
            java.lang.reflect.Constructor<XC_LoadPackage.LoadPackageParam> constructor =
                XC_LoadPackage.LoadPackageParam.class.getDeclaredConstructor(cSet);
            constructor.setAccessible(true);
            xposedParam = constructor.newInstance(emptySet);
        } catch (Exception e) {
            throw new RuntimeException("Cannot be instantiated LoadPackageParam", e);
        }
        xposedParam.classLoader = lpparam.getClassLoader();

        return xposedParam;
    }

    private XC_LoadPackage.LoadPackageParam getParam(PackageLoadedParam lpparam) {
        XC_LoadPackage.LoadPackageParam xposedParam;
        try {
            Class<?> cSet = Class.forName("de.robv.android.xposed.XposedBridge$CopyOnWriteSortedSet");
            Object emptySet = cSet.getConstructor().newInstance();
            java.lang.reflect.Constructor<XC_LoadPackage.LoadPackageParam> constructor =
                XC_LoadPackage.LoadPackageParam.class.getDeclaredConstructor(cSet);
            constructor.setAccessible(true);
            xposedParam = constructor.newInstance(emptySet);
        } catch (Exception e) {
            throw new RuntimeException("Cannot be instantiated LoadPackageParam", e);
        }
        xposedParam.packageName = lpparam.getPackageName();
        xposedParam.classLoader = lpparam.getClassLoader();
        xposedParam.appInfo = lpparam.getApplicationInfo();
        xposedParam.isFirstApplication = lpparam.isFirstPackage();

        return xposedParam;
    }

    private void invokeSystemInit(XC_LoadPackage.LoadPackageParam lpparam) {
        HashMap<String, DataBase> dataMap = DataBase.get();

        dataMap.forEach(new BiConsumer<>() {
            @Override
            public void accept(String s, DataBase dataBase) {
                if (!(dataBase.mTargetSdk == -1) && !isAndroidVersion(dataBase.mTargetSdk))
                    return;
                if (!(dataBase.mTargetOSVersion == -1F) && !(isHyperOSVersion(dataBase.mTargetOSVersion)))
                    return;
                if ((dataBase.isPad == 1 && !isPad()) || (dataBase.isPad == 2 && isPad()))
                    return;

                try {
                    Class<?> clazz = Objects.requireNonNull(getClass().getClassLoader()).loadClass(s);
                    BaseModule module = (BaseModule) clazz.getDeclaredConstructor().newInstance();
                    module.init(lpparam);
                } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                         InstantiationException | InvocationTargetException e) {
                    logE(TAG, e);
                }
            }
        });
    }

    private void invokeInit(XC_LoadPackage.LoadPackageParam lpparam) {
        String mPkgName = lpparam.packageName;
        if (mPkgName == null) return;
        if (isOtherRestrictions(mPkgName)) return;

        HashMap<String, DataBase> dataMap = DataBase.get();
        if (dataMap.values().stream().noneMatch(dataBase -> dataBase.mTargetPackage.equals(mPkgName))) {
            mVariousThirdApps.init(lpparam);
            return;
        }

        dataMap.forEach(new BiConsumer<>() {
            @Override
            public void accept(String s, DataBase dataBase) {
                if (!mPkgName.equals(dataBase.mTargetPackage))
                    return;
                if (!(dataBase.mTargetSdk == -1) && !isAndroidVersion(dataBase.mTargetSdk))
                    return;
                if (!(dataBase.mTargetOSVersion == -1F) && !(isHyperOSVersion(dataBase.mTargetOSVersion)))
                    return;
                if ((dataBase.isPad == 1 && !isPad()) || (dataBase.isPad == 2 && isPad()))
                    return;

                try {
                    Class<?> clazz = Objects.requireNonNull(getClass().getClassLoader()).loadClass(s);
                    BaseModule module = (BaseModule) clazz.getDeclaredConstructor().newInstance();
                    module.init(lpparam);
                } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                         InstantiationException | InvocationTargetException e) {
                    logE(TAG, e);
                }
            }
        });
    }

    private void androidCrashEventHook(ClassLoader classLoader) {
        logI("android", "Log level is " + logLevelDesc());

        // 纯 libxposed 版本：不依赖 HookTool / XposedBridge / 伪造 LoadPackageParam
        CrashHookLX.install(this, classLoader);
        RescuePartyPlusLX.setHandler(SafeMode.INSTANCE::onHandleCrash);
        RescuePartyPlusLX.install(this, classLoader);
    }

    private boolean isOtherRestrictions(String pkg) {
        switch (pkg) {
            case "com.google.android.webview", "com.miui.contentcatcher",
                 "com.miui.catcherpatch" -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    // safe mode 判定统一走 SafeMode.kt（包含 settings + prop 分支）
}
