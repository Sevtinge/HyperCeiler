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
package com.sevtinge.hyperceiler.libhook.utils.log

import android.util.Log
import io.github.libxposed.api.XposedInterface

/**
 * Xposed 日志工具类
 */
object XposedLog {
    private const val TAG = "HyperCeiler"

    @Volatile
    private var sXposed: XposedInterface? = null

    @Volatile
    @JvmField
    var logLevel: Int = 3

    @JvmStatic
    fun init(xposed: XposedInterface) {
        sXposed = xposed
    }

    @JvmStatic
    fun setLogLevel(level: Int) {
        logLevel = level
    }

    private fun logRaw(msg: String? = null, t: Throwable? = null) {
        if (msg == null && t == null) return
        val xposed = sXposed
        if (msg != null) {
            if (xposed != null) {
                xposed.log(msg)
            } else {
                Log.i(TAG, msg)
            }
        }
        if (t != null) {
            if (xposed != null) {
                xposed.log(Log.getStackTraceString(t))
            } else {
                Log.e(TAG, "", t)
            }
        }
    }

    @JvmStatic
    fun d(msg: String) {
        if (logLevel < 4) return
        logRaw("[$TAG][D]: $msg")
    }

    @JvmStatic
    fun d(tag: String, msg: String) {
        if (logLevel < 4) return
        logRaw("[$TAG][D][$tag]: $msg")
    }

    @JvmStatic
    fun d(tag: String, msg: String, t: Throwable) {
        if (logLevel < 4) return
        logRaw("[$TAG][D][$tag]: $msg", t)
    }

    @JvmStatic
    fun i(msg: String) {
        if (logLevel < 3) return
        logRaw("[$TAG][I]: $msg")
    }

    @JvmStatic
    fun i(tag: String, msg: String) {
        if (logLevel < 3) return
        logRaw("[$TAG][I][$tag]: $msg")
    }

    @JvmStatic
    fun i(tag: String, pkg: String?, msg: String) {
        if (logLevel < 3) return
        if (pkg != null) {
            logRaw("[$TAG][I][$pkg][$tag]: $msg")
        } else {
            logRaw("[$TAG][I][$tag]: $msg")
        }
    }

    @JvmStatic
    fun w(msg: String) {
        if (logLevel < 2) return
        logRaw("[$TAG][W]: $msg")
    }

    @JvmStatic
    fun w(tag: String, msg: String) {
        if (logLevel < 2) return
        logRaw("[$TAG][W][$tag]: $msg")
    }

    @JvmStatic
    fun w(tag: String, msg: String, t: Throwable) {
        if (logLevel < 2) return
        logRaw("[$TAG][W][$tag]: $msg", t)
    }

    @JvmStatic
    fun w(tag: String, pkg: String?, msg: String) {
        if (logLevel < 2) return
        if (pkg != null) {
            logRaw("[$TAG][W][$pkg][$tag]: $msg")
        } else {
            logRaw("[$TAG][W][$tag]: $msg")
        }
    }

    @JvmStatic
    fun w(tag: String, pkg: String?, msg: String, t: Throwable) {
        if (logLevel < 2) return
        if (pkg != null) {
            logRaw("[$TAG][W][$pkg][$tag]: $msg", t)
        } else {
            logRaw("[$TAG][W][$tag]: $msg", t)
        }
    }

    @JvmStatic
    fun e(msg: String) {
        if (logLevel < 1) return
        logRaw("[$TAG][E]: $msg")
    }

    @JvmStatic
    fun e(tag: String, msg: String) {
        if (logLevel < 1) return
        logRaw("[$TAG][E][$tag]: $msg")
    }

    @JvmStatic
    fun e(tag: String, t: Throwable) {
        if (logLevel < 1) return
        logRaw("[$TAG][E][$tag]: ${t.message}", )
    }

    @JvmStatic
    fun e(tag: String, msg: String, t: Throwable) {
        if (logLevel < 1) return
        logRaw("[$TAG][E][$tag]: $msg", t)
    }

    @JvmStatic
    fun e(tag: String, pkg: String?, msg: String) {
        if (logLevel < 1) return
        if (pkg != null) {
            logRaw("[$TAG][E][$pkg][$tag]: $msg")
        } else {
            logRaw("[$TAG][E][$tag]: $msg")
        }
    }

    @JvmStatic
    fun e(tag: String, pkg: String?, msg: String, t: Throwable) {
        if (logLevel < 1) return
        if (pkg != null) {
            logRaw("[$TAG][E][$pkg][$tag]: $msg", t)
        } else {
            logRaw("[$TAG][E][$tag]: $msg", t)
        }
    }


    @JvmStatic
    fun logLevelDesc(): String {
        return when (logLevel) {
            0 -> "Disable"
            1 -> "Error"
            2 -> "Warn"
            3 -> "Info"
            4 -> "Debug"
            else -> "Unknown"
        }
    }
}
