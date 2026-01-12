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

import static com.sevtinge.hyperceiler.libhook.base.XposedInitEntry.mResHook;

import com.sevtinge.hyperceiler.libhook.callback.IHook;
import com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils;
import com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKit;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsMap;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam;

/**
 * 应用模块基类
 * <p>
 * 每个目标应用对应一个 BaseLoad 子类，负责管理该应用下的所有 Hook 规则。
 * 通过静态方法暴露 ClassLoader、PackageName 等资源供具体 Hook 使用。
 *
 * @author HyperCeiler
 */
public abstract class BaseLoad {
    private static final Object sLock = new Object();
    private static volatile ClassLoader sClassLoader;
    private static volatile String sPackageName;
    private static volatile PackageLoadedParam sLpparam;
    private static volatile XposedInterface sXposed;

    public static void init(XposedInterface xposed) {
        sXposed = xposed;
    }

    public static XposedInterface getXposed() {
        return sXposed;
    }

    public static ClassLoader getClassLoader() {
        synchronized (sLock) {
            return sClassLoader;
        }
    }

    public static String getPackageName() {
        synchronized (sLock) {
            return sPackageName;
        }
    }

    public static PackageLoadedParam getLpparam() {
        synchronized (sLock) {
            return sLpparam;
        }
    }

    protected String TAG = "BaseLoad";
    protected final PrefsMap<String, Object> mPrefsMap = PrefsUtils.mPrefsMap;

    public abstract void onPackageLoaded();

    protected boolean needDexKit() {
        return false;
    }

    public void onLoad(PackageLoadedParam lpparam) {
        if (lpparam == null) return;

        // 设置静态资源（线程安全）
        synchronized (sLock) {
            sClassLoader = lpparam.getClassLoader();
            sPackageName = lpparam.getPackageName();
            sLpparam = lpparam;
        }

        // 把模块资源加载到目标应用
        try {
            if (!Objects.equals(ProjectApi.mAppModulePkg, sPackageName)) {
                boolean isAndroid = "android".equals(sPackageName);
                ContextUtils.getWaitContext(context -> {
                    if (context != null) {
                        mResHook.loadModuleRes(context);
                    }
                }, isAndroid);
            }
        } catch (Throwable e) {
            XposedLog.e(TAG, "get context failed! " + e);
        }

        try {
            // 按需初始化 DexKit
            if (needDexKit()) {
                DexKit.ready(lpparam, TAG);
            }

            // 执行具体 Hook 逻辑
            onPackageLoaded();
        } finally {
            // 关闭 DexKit
            if (needDexKit()) {
                DexKit.close();
            }
        }
    }

    protected void initHook(IHook hook) {
        initHook(hook, () -> true);
    }

    protected void initHook(IHook hook, boolean isInit) {
        initHook(hook, () -> isInit);
    }

    protected void initHook(IHook hook, BooleanSupplier condition) {
        if (hook == null) return;
        TAG = hook.getClass().getSimpleName();

        try {
            if (condition.getAsBoolean()) {
                hook.init();
                XposedLog.i(TAG, "Hook Success");
            }
        } catch (Throwable t) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            XposedLog.e(TAG, "Hook Failed: " + sw);
        }
    }

}
