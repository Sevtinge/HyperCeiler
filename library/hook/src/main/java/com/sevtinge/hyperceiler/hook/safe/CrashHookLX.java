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

import android.app.ActivityOptions;
import android.app.ApplicationErrorReport;
import android.content.Context;
import android.content.Intent;

import com.sevtinge.hyperceiler.hook.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.hook.utils.log.XposedLogUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedInterface.AfterHookCallback;
import io.github.libxposed.api.XposedInterface.BeforeHookCallback;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.BeforeInvocation;

/**
 * libxposed 版本的 CrashHook：
 * - 监听 system_server 里 AppErrors.handleAppCrashInActivityController
 * - 触发 SafeMode.onHandleCrash
 * - 放行模块自身后台启动 CrashActivity（尽量复刻旧行为）
 */
public final class CrashHookLX {
    private static final String TAG = "CrashHookLX";

    private CrashHookLX() {
    }

    public static void install(XposedInterface xposed, ClassLoader classLoader) {
        if (xposed == null || classLoader == null) return;

        try {
            hookBackgroundActivity(xposed, classLoader);
        } catch (Throwable t) {
            XposedLogUtils.logE(TAG, t);
        }

        try {
            hookCrash(xposed, classLoader);
        } catch (Throwable t) {
            XposedLogUtils.logE(TAG, t);
        }
    }

    private static void hookCrash(XposedInterface xposed, ClassLoader cl) throws Exception {
        Class<?> appErrors = Class.forName("com.android.server.am.AppErrors", false, cl);

        Method target = null;
        for (Method m : appErrors.getDeclaredMethods()) {
            if ("handleAppCrashInActivityController".equals(m.getName())
                && m.getReturnType() == boolean.class) {
                target = m;
                break;
            }
        }
        if (target == null) {
            throw new NoSuchMethodException("AppErrors.handleAppCrashInActivityController(boolean) not found");
        }
        target.setAccessible(true);
        xposed.hook(target, HandleAppCrashHooker.class);
    }

    private static void hookBackgroundActivity(XposedInterface xposed, ClassLoader cl) throws Exception {
        Class<?> activityStarter = Class.forName("com.android.server.wm.ActivityStarter", false, cl);
        // shouldAbortBackgroundActivityStart(int,int,String,int,int,WindowProcessController,PendingIntentRecord,boolean,Intent,ActivityOptions)
        Method target = null;
        for (Method m : activityStarter.getDeclaredMethods()) {
            if (!"shouldAbortBackgroundActivityStart".equals(m.getName())) continue;
            Class<?>[] p = m.getParameterTypes();
            if (p.length == 10 && p[2] == String.class && p[8] == Intent.class && p[9] == ActivityOptions.class) {
                target = m;
                break;
            }
        }
        if (target == null) {
            // 某些版本签名可能变动：找不到就跳过，不影响 crash hook 主功能
            return;
        }
        target.setAccessible(true);
        xposed.hook(target, BackgroundStartHooker.class);
    }

    private static Object getFieldValue(Object target, String fieldName) {
        if (target == null || fieldName == null) return null;
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(target);
        } catch (Throwable t) {
            return null;
        }
    }

    public static final class BackgroundStartHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        public static void before(BeforeHookCallback callback) {
            Object[] args = callback.getArgs();
            if (args == null || args.length < 3) return;
            String pkg = (String) args[2];
            if (pkg == null) return;
            if (ProjectApi.mAppModulePkg.equals(pkg)) {
                callback.returnAndSkip(false);
            }
        }
    }

    public static final class HandleAppCrashHooker implements XposedInterface.Hooker {
        @AfterInvocation
        public static void after(AfterHookCallback callback) {
            try {
                Object[] args = callback.getArgs();
                if (args == null || args.length < 2) return;

                Object proc = args[0];
                ApplicationErrorReport.CrashInfo crashInfo = (ApplicationErrorReport.CrashInfo) args[1];
                String longMsg = args.length > 3 ? (String) args[3] : null;
                String stackTrace = args.length > 4 ? (String) args[4] : null;

                Context context = (Context) getFieldValue(callback.getThisObject(), "mContext");
                if (context == null || crashInfo == null) return;

                SafeMode.INSTANCE.onHandleCrash(
                    longMsg == null ? "" : longMsg,
                    stackTrace == null ? "" : stackTrace,
                    crashInfo.throwClassName,
                    crashInfo.throwFileName,
                    crashInfo.throwLineNumber,
                    crashInfo.throwMethodName
                );
            } catch (Throwable t) {
                XposedLogUtils.logE(TAG, t);
            }
        }
    }
}
