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
package com.sevtinge.hyperceiler.hook.utils.log;

import static com.sevtinge.hyperceiler.hook.utils.log.LogManager.logLevel;

import android.util.Log;

import io.github.libxposed.api.XposedInterface;

public class XposedLogUtils {
    private static volatile XposedInterface sXposed;

    /**
     * 初始化 libxposed 日志通道（在入口处调用一次即可）。
     */
    public static void init(XposedInterface xposed) {
        sXposed = xposed;
    }

    private static void logRaw(String msg) {
        XposedInterface xposed = sXposed;
        if (xposed != null) {
            xposed.log(msg);
        } else {
            Log.i("HyperCeiler", msg);
        }
    }

    private static void logRaw(Throwable t) {
        if (t == null) return;
        XposedInterface xposed = sXposed;
        if (xposed != null) {
            xposed.log(Log.getStackTraceString(t));
        } else {
            Log.e("HyperCeiler", "", t);
        }
    }

    public static void logI(String msg) {
        if (logLevel < 3) return;
        logRaw("[HyperCeiler][I]: " + msg);
    }

    public static void logI(String tagOpkg, String msg) {
        if (logLevel < 3) return;
        logRaw("[HyperCeiler][I][" + tagOpkg + "]: " + msg);
    }

    public static void logI(String tag, String pkg, String msg) {
        if (logLevel < 3) return;
        logRaw("[HyperCeiler][I][" + pkg + "][" + tag + "]: " + msg);
    }

    public static void logW(String msg) {
        if (logLevel < 2) return;
        logRaw("[HyperCeiler][W]: " + msg);
    }

    public static void logW(String tag, String pkg, String msg) {
        if (logLevel < 2) return;
        logRaw("[HyperCeiler][W][" + pkg + "][" + tag + "]: " + msg);
    }

    public static void logW(String tag, String pkg, Throwable log) {
        if (logLevel < 2) return;
        logRaw("[HyperCeiler][W][" + pkg + "][" + tag + "]: " + log);
    }

    public static void logW(String tag, String pkg, String msg, Exception exp) {
        if (logLevel < 2) return;
        logRaw("[HyperCeiler][W][" + pkg + "][" + tag + "]: " + msg + ", by: " + exp);
    }

    public static void logW(String tag, String pkg, String msg, Throwable log) {
        if (logLevel < 2) return;
        logRaw("[HyperCeiler][W][" + pkg + "][" + tag + "]: " + msg + ", by: " + log);
    }

    public static void logW(String tag, String msg) {
        if (logLevel < 2) return;
        logRaw("[HyperCeiler][W][" + tag + "]: " + msg);
    }

    public static void logW(String tag, Throwable log) {
        if (logLevel < 2) return;
        logRaw("[HyperCeiler][W][" + tag + "]: " + log);
    }

    public static void logW(String tag, String msg, Exception exp) {
        if (logLevel < 2) return;
        logRaw("[HyperCeiler][W][" + tag + "]: " + msg + ", by: " + exp);
    }

    public static void logE(String tag, String msg) {
        if (logLevel < 1) return;
        logRaw("[HyperCeiler][E][" + tag + "]: " + msg);
    }

    public static void logE(String msg) {
        if (logLevel < 1) return;
        logRaw("[HyperCeiler][E]: " + msg);
    }

    public static void logE(String tag, Throwable log) {
        if (logLevel < 1) return;
        logRaw(log);
    }

    public static void logE(String tag, String pkg, String msg) {
        if (logLevel < 1) return;
        logRaw("[HyperCeiler][E][" + pkg + "][" + tag + "]: " + msg);
    }

    public static void logE(String tag, String pkg, Throwable log) {
        if (logLevel < 1) return;
        logRaw(log);
    }

    public static void logE(String tag, String pkg, Exception exp) {
        if (logLevel < 1) return;
        logRaw(exp);
    }

    public static void logE(String tag, String pkg, String msg, Throwable log) {
        if (logLevel < 1) return;
        logRaw("[HyperCeiler][E][" + pkg + "][" + tag + "]: " + msg);
        logRaw(log);
    }

    public static void logE(String tag, String pkg, String msg, Exception exp) {
        if (logLevel < 1) return;
        logRaw("[HyperCeiler][E][" + pkg + "][" + tag + "]: " + msg);
        logRaw(exp);
    }

    public static void logD(String msg) {
        if (logLevel < 4) return;
        logRaw("[HyperCeiler][D]: " + msg);
    }

    public static void logD(String tag, String pkg, String msg) {
        if (logLevel < 4) return;
        logRaw("[HyperCeiler][D][" + pkg + "][" + tag + "]: " + msg);
    }

    public static void logD(String tag, String msg) {
        if (logLevel < 4) return;
        logRaw("[HyperCeiler][D][" + tag + "]: " + msg);
    }

}
