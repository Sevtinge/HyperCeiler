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
package com.sevtinge.hyperceiler.libhook.utils.devices

import android.content.res.Configuration
import android.content.res.Resources

/**
 * 小米设备工具类
 */
private val clazzMiuiBuild: Class<*>? by lazy {
    runCatching {
        Class.forName("miui.os.Build")
    }.getOrNull()
}

private fun getStaticBoolean(clazz: Class<*>?, fieldName: String): Boolean {
    if (clazz == null) return false
    return runCatching {
        val field = clazz.getDeclaredField(fieldName)
        field.isAccessible = true
        field.get(null) as? Boolean ?: false
    }.getOrElse { false }
}

val IS_TABLET by lazy {
    getStaticBoolean(clazzMiuiBuild, "IS_TABLET")
}
val IS_PAD by lazy {
    getStaticBoolean(clazzMiuiBuild, "IS_PAD")
}
val IS_FOLD by lazy {
    getStaticBoolean(clazzMiuiBuild, "IS_FOLD")
}
val IS_INTERNATIONAL_BUILD by lazy {
    getStaticBoolean(clazzMiuiBuild, "IS_INTERNATIONAL_BUILD")
}

/**
 * 函数调用，适用于其他一些需要更高精度判断大屏设备的情况，仅支持小米设备的判断
 * @return 一个 Boolean 值，true 代表是大屏设备，false 代表不是大屏设备
 */
fun isLargeUI(): Boolean {
    return runCatching {
        IS_PAD || IS_FOLD || IS_TABLET
    }.getOrElse {
        isPad()
    }
}

/**
 * 函数调用，适用于其他一些需要判断的情况，仅支持小米设备的判断
 * @return 一个 Boolean 值，true 代表是平板，false 代表不是平板
 */
fun isPad(): Boolean {
    return runCatching {
        IS_TABLET
    }.getOrElse {
        // 使用通用判断
        val res = Resources.getSystem()
        val config = res.configuration
        (config.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }
}

/**
 * 函数调用，适用于其他一些需要判断的情况，仅支持小米设备的判断
 * @return 一个 Boolean 值，true 代表是国际版系统，false 代表不是国际版系统
 */
fun isInternational(): Boolean {
    return runCatching {
        IS_INTERNATIONAL_BUILD
    }.getOrElse {
        false
    }
}
