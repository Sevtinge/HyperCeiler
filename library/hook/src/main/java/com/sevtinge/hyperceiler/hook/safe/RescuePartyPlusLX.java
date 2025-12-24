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
 */
package com.sevtinge.hyperceiler.hook.safe;

import android.content.Context;
import android.content.pm.VersionedPackage;
import android.os.SystemProperties;
import android.provider.Settings;

import com.sevtinge.hyperceiler.hook.utils.AdditionalFieldStore;
import com.sevtinge.hyperceiler.hook.utils.hidden.PackageWatchdog;
import com.sevtinge.hyperceiler.hook.utils.log.XposedLogUtils;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedInterface.BeforeHookCallback;
import io.github.libxposed.api.annotations.BeforeInvocation;

/**
 * libxposed 版本的 RescuePartyPlus 入口。
 *
 * 目标：保留 SafeMode / RescuePartyPlus 链路，不依赖 de.robv.android.xposed / HookTool。
 */
public final class RescuePartyPlusLX {
    private static final String TAG = "RescuePartyPlusLX";
    private static volatile CrashHandler handler;
    private static volatile ClassLoader classLoader;

    private RescuePartyPlusLX() {
    }

    public interface CrashHandler {
        boolean onHandleCrash(Context context, String pkgName, int mitigationCount);
    }

    public static void setHandler(CrashHandler crashHandler) {
        handler = crashHandler;
    }

    public static void install(XposedInterface xposed, ClassLoader cl) {
        if (xposed == null || cl == null) return;
        classLoader = cl;
        PackageWatchdog.INSTANCE.setClassLoader(cl);

        try {
            Class<?> impl = Class.forName("com.android.server.PackageWatchdogImpl", false, cl);
            Method setCrashApplicationLevel = findMethod(
                impl,
                "setCrashApplicationLevel",
                int.class,
                VersionedPackage.class,
                Context.class
            );
            xposed.hook(setCrashApplicationLevel, SetCrashApplicationLevelHooker.class);

            Method doStepNew = findMethodIfExists(
                impl,
                "doRescuePartyPlusStepNew",
                int.class,
                VersionedPackage.class,
                Context.class
            );
            if (doStepNew != null) {
                xposed.hook(doStepNew, DoRescuePartyPlusStepHooker.class);
            }

            Method doStep = findMethodIfExists(
                impl,
                "doRescuePartyPlusStep",
                int.class,
                VersionedPackage.class,
                Context.class
            );
            if (doStep != null) {
                xposed.hook(doStep, DoRescuePartyPlusStepHooker.class);
            }
        } catch (Throwable t) {
            XposedLogUtils.logE(TAG, t);
        }
    }

    private static boolean checkDisableRescuePartyPlus() {
        if (SystemProperties.getBoolean("persist.sys.rescuepartyplus.disable", false)) {
            return true;
        }

        // 兼容原逻辑：enable=false => 不强制禁用
        return false;
    }

    private static void putGlobalSettings(Context context, String key, int value) {
        Settings.Global.putInt(context.getContentResolver(), key, value);
    }

    private static void onAfterSetAppCrashLevel(Context context, String packageName, Object watchdog) {
        SystemProperties.set("sys.set_app_crash_level.flag", "true");
        PackageWatchdog.INSTANCE.clearRecord(context, packageName);
        AdditionalFieldStore.set(watchdog, "flag", packageName);
    }

    private static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) throws NoSuchMethodException {
        Method m = clazz.getDeclaredMethod(name, paramTypes);
        m.setAccessible(true);
        return m;
    }

    private static Method findMethodIfExists(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            return findMethod(clazz, name, paramTypes);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static final class SetCrashApplicationLevelHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        public static void before(BeforeHookCallback callback) {
            CrashHandler crashHandler = handler;
            if (crashHandler == null) return;

            Object[] args = callback.getArgs();
            if (args == null || args.length < 3) return;

            int mitigationCount = (int) args[0];
            VersionedPackage versionedPackage = (VersionedPackage) args[1];
            Context context = (Context) args[2];

            if (checkDisableRescuePartyPlus() || versionedPackage == null) {
                callback.returnAndSkip(false);
                return;
            }

            String packageName = versionedPackage.getPackageName();
            if (packageName == null) return;

            switch (packageName) {
                case com.sevtinge.hyperceiler.hook.module.HostConstant.HOST_SYSTEM_UI -> {
                    if (crashHandler.onHandleCrash(context, packageName, mitigationCount)) {
                        putGlobalSettings(context, "sys.rescueparty.systemui.level", 0);
                        putGlobalSettings(context, "sys.anr.rescue.systemui.level", 0);
                        onAfterSetAppCrashLevel(context, packageName, callback.getThisObject());
                        callback.returnAndSkip(true);
                    }
                }
                case com.sevtinge.hyperceiler.hook.module.HostConstant.HOST_HOME -> {
                    if (crashHandler.onHandleCrash(context, packageName, mitigationCount)) {
                        putGlobalSettings(context, "sys.rescueparty.home.level", 0);
                        onAfterSetAppCrashLevel(context, packageName, callback.getThisObject());
                        callback.returnAndSkip(true);
                    }
                }
            }
        }
    }

    public static final class DoRescuePartyPlusStepHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        public static void before(BeforeHookCallback callback) {
            Object watchdog = callback.getThisObject();
            String flag = AdditionalFieldStore.getAs(watchdog, "flag", String.class);
            if (flag == null) return;
            AdditionalFieldStore.remove(watchdog, "flag");

            Object[] args = callback.getArgs();
            if (args == null || args.length < 3) return;

            VersionedPackage versionedPackage = (VersionedPackage) args[1];
            if (versionedPackage == null) {
                callback.returnAndSkip(false);
                return;
            }

            int mitigationCount = (int) args[0];
            String packageName = versionedPackage.getPackageName();
            if (packageName == null) return;

            if (!packageName.equals(flag)) {
                return;
            }

            // 对齐原逻辑：mitigationCount > 1 时移除上一条消息
            if (mitigationCount > 1) {
                try {
                    int what = mitigationCount <= 7 ? (mitigationCount - 1) : 7;
                    Method removeMessage = watchdog.getClass().getDeclaredMethod("removeMessage", int.class, String.class);
                    removeMessage.setAccessible(true);
                    removeMessage.invoke(watchdog, what, packageName);
                } catch (Throwable t) {
                    XposedLogUtils.logE(TAG, t);
                }
            }

            callback.returnAndSkip(true);
        }
    }
}
