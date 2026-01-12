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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.safecrash

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemProperties
import com.sevtinge.hyperceiler.libhook.safecrash.CrashData.scopeData
import com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi.isDebug
import com.sevtinge.hyperceiler.libhook.utils.api.PropUtils
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils
import kotlin.properties.Delegates

object SafeMode : RescuePartyPlus.CrashHandler {
    private lateinit var longMsg: String
    private lateinit var throwMethodName: String
    private var throwLineNumber by Delegates.notNull<Int>()
    private lateinit var throwFileName: String
    private lateinit var throwClassName: String
    private lateinit var stackTrace: String
    const val PROP_REPORT_PACKAGE = "persist.service.hyperceiler.crash.report"

    private fun onSafeMode(context: Context, pkgName: String): Boolean {
        if (isInSafeModeOnHook(pkgName) || isInSafeModeByProp(pkgName)) {
            return false
        }

        val alias = scopeData()[pkgName] ?: return false

        runCatching {
            context.startActivity(Intent().apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                component = ComponentName(
                    "com.sevtinge.hyperceiler",
                    "com.sevtinge.hyperceiler.safemode.CrashActivity"
                )

                putExtra("key_is_need_set_prop", true)
                putExtra("key_longMsg", longMsg)
                putExtra("key_stackTrace", stackTrace)
                putExtra("key_throwClassName", throwClassName)
                putExtra("key_throwFileName", throwFileName)
                putExtra("key_throwLineNumber", throwLineNumber)
                putExtra("key_throwMethodName", throwMethodName)
                putExtra("key_pkg", alias)
            })
        }.onFailure {
            SystemProperties.set(PROP_REPORT_PACKAGE, alias)
            XposedLog.d("SafeMode", "start CrashActivity failed, try set prop")
        }

        return true
    }

    override fun onHandleCrash(context: Context, pkgName: String, mitigationCount: Int): Boolean {
        if (pkgName != "com.android.systemui" && pkgName != "com.miui.home") {
            return false
        }

        if (mitigationCount < 1) {
            return false
        }

        return onSafeMode(context, pkgName)
    }

    @JvmStatic
    fun isInSafeModeOnHook(pkgName: String): Boolean = when (pkgName) {
        "com.android.systemui" -> PrefsUtils.mPrefsMap.getBoolean("system_ui_safe_mode_enable")
        "com.miui.home" -> PrefsUtils.mPrefsMap.getBoolean("home_safe_mode_enable")
        "com.android.settings" -> PrefsUtils.mPrefsMap.getBoolean("settings_safe_mode_enable")
        "com.miui.securitycenter" -> PrefsUtils.mPrefsMap.getBoolean("security_center_safe_mode_enable")
        else -> false
    }

    @JvmStatic
    fun isInSafeModeByProp(pkgName: String): Boolean {
        val data = PropUtils.getProp(PROP_REPORT_PACKAGE, "")
        if (pkgName.isEmpty()) {
            return false
        }
        val alias = scopeData()[pkgName] ?: return false
        if (isDebug()) XposedLog.d("SafeMode", "data: $data, alias: $alias")
        return data.split(",").toHashSet().contains(alias)
    }

    fun onHandleCrash(
        longMsg: String,
        stackTrace: String,
        throwClassName: String,
        throwFileName: String,
        throwLineNumber: Int,
        throwMethodName: String
    ) {
        this.longMsg = longMsg
        this.stackTrace = stackTrace
        this.throwClassName = throwClassName
        this.throwFileName = throwFileName
        this.throwLineNumber = throwLineNumber
        this.throwMethodName = throwMethodName
    }
}
