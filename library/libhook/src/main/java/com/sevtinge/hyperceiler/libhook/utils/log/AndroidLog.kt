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

/**
 * Android 日志工具类
 *
 * @author HyperCeiler
 */
object AndroidLog {
    private const val TAG = "HyperCeiler"

    @Volatile
    @JvmField
    var logLevel: Int = 3

    interface LogListener {
        fun onLog(level: String, tag: String, message: String)
    }

    @Volatile
    private var sLogListener: LogListener? = null

    @JvmStatic
    fun setLogListener(listener: LogListener?) {
        sLogListener = listener
    }

    private fun notifyListener(level: String, tag: String, message: String) {
        try {
            sLogListener?.onLog(level, tag, message)
        } catch (_: Throwable) {
        }
    }

    @JvmStatic
    fun d(msg: String) {
        if (logLevel < 4) return
        Log.d(TAG, "[$TAG][D]: $msg")
        notifyListener("D", TAG, msg)
    }

    @JvmStatic
    fun d(tag: String, msg: String) {
        if (logLevel < 4) return
        Log.d(tag, "[$TAG][D][$tag]: $msg")
        notifyListener("D", tag, msg)
    }

    @JvmStatic
    fun d(tag: String, msg: String, t: Throwable?) {
        if (logLevel < 4) return
        Log.d(tag, "[$TAG][D][$tag]: $msg", t)
        notifyListener("D", tag, msg + if (t != null) "\n$t" else "")
    }

    @JvmStatic
    fun i(msg: String) {
        if (logLevel < 3) return
        Log.i(TAG, "[$TAG][I]: $msg")
        notifyListener("I", TAG, msg)
    }

    @JvmStatic
    fun i(tag: String, msg: String) {
        if (logLevel < 3) return
        Log.i(tag, "[$TAG][I][$tag]: $msg")
        notifyListener("I", tag, msg)
    }

    @JvmStatic
    fun i(tag: String, pkg: String?, msg: String) {
        if (logLevel < 3) return
        if (pkg != null) {
            Log.i(tag, "[$TAG][I][$pkg][$tag]: $msg")
        } else {
            Log.i(tag, "[$TAG][I][$tag]: $msg")
        }
        notifyListener("I", tag, msg)
    }

    @JvmStatic
    fun w(msg: String) {
        if (logLevel < 2) return
        Log.w(TAG, "[$TAG][W]: $msg")
        notifyListener("W", TAG, msg)
    }

    @JvmStatic
    fun w(tag: String, msg: String) {
        if (logLevel < 2) return
        Log.w(tag, "[$TAG][W][$tag]: $msg")
        notifyListener("W", tag, msg)
    }

    @JvmStatic
    fun w(tag: String, msg: String, t: Throwable?) {
        if (logLevel < 2) return
        Log.w(tag, "[$TAG][W][$tag]: $msg", t)
        notifyListener("W", tag, msg + if (t != null) "\n$t" else "")
    }

    @JvmStatic
    fun w(tag: String, pkg: String?, msg: String) {
        if (logLevel < 2) return
        if (pkg != null) {
            Log.w(tag, "[$TAG][W][$pkg][$tag]: $msg")
        } else {
            Log.w(tag, "[$TAG][W][$tag]: $msg")
        }
        notifyListener("W", tag, msg)
    }

    @JvmStatic
    fun w(tag: String, pkg: String?, msg: String, t: Throwable?) {
        if (logLevel < 2) return
        if (pkg != null) {
            Log.w(tag, "[$TAG][W][$pkg][$tag]: $msg", t)
        } else {
            Log.w(tag, "[$TAG][W][$tag]: $msg", t)
        }
        notifyListener("W", tag, msg + if (t != null) "\n$t" else "")
    }

    @JvmStatic
    fun e(msg: String) {
        if (logLevel < 1) return
        Log.e(TAG, "[$TAG][E]: $msg")
        notifyListener("E", TAG, msg)
    }

    @JvmStatic
    fun e(tag: String, msg: String) {
        if (logLevel < 1) return
        Log.e(tag, "[$TAG][E][$tag]: $msg")
        notifyListener("E", tag, msg)
    }

    @JvmStatic
    fun e(tag: String, t: Throwable) {
        if (logLevel < 1) return
        Log.e(tag, "[$TAG][E][$tag]: ${t.message}", t)
        notifyListener("E", tag, t.message ?: t.toString())
    }

    @JvmStatic
    fun e(tag: String, msg: String, t: Throwable?) {
        if (logLevel < 1) return
        Log.e(tag, "[$TAG][E][$tag]: $msg", t)
        notifyListener("E", tag, msg + if (t != null) "\n$t" else "")
    }

    @JvmStatic
    fun e(tag: String, pkg: String?, msg: String) {
        if (logLevel < 1) return
        if (pkg != null) {
            Log.e(tag, "[$TAG][E][$pkg][$tag]: $msg")
        } else {
            Log.e(tag, "[$TAG][E][$tag]: $msg")
        }
        notifyListener("E", tag, msg)
    }

    @JvmStatic
    fun e(tag: String, pkg: String?, msg: String, t: Throwable?) {
        if (logLevel < 1) return
        if (pkg != null) {
            Log.e(tag, "[$TAG][E][$pkg][$tag]: $msg", t)
        } else {
            Log.e(tag, "[$TAG][E][$tag]: $msg", t)
        }
        notifyListener("E", tag, msg + if (t != null) "\n$t" else "")
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
