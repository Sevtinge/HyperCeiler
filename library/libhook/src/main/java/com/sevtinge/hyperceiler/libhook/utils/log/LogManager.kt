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
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Hardware.getSerial
import com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi.isBeta
import com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi.isCanary
import com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi.isRelease
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils.mPrefsMap
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils.mSharedPreferences
import com.sevtinge.hyperceiler.libhook.utils.shell.ShellUtils.rootExecCmd
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.InputStreamReader
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.StandardOpenOption
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * 日志管理器
 *
 */
object LogManager {

    @JvmField
    var IS_LOGGER_ALIVE: Boolean = false

    @JvmField
    val logLevel: Int = getLogLevel()

    @JvmField
    var LOGGER_CHECKER_ERR_CODE: String? = null

    private const val LOG_CONFIG_PATH = "/files/log_config"

    @JvmStatic
    fun init() {
        IS_LOGGER_ALIVE = isLoggerAlive()
        // 同步日志级别到其他日志类
        XposedLog.logLevel = logLevel
        AndroidLog.logLevel = logLevel
    }

    @JvmStatic
    fun setLogLevel() {
        val logLevel = mSharedPreferences.getString("prefs_key_log_level", "3")!!.toInt()
        val effectiveLogLevel: Int =
            getEffectiveLogLevel(logLevel)
        writeLogLevelToFile(null, effectiveLogLevel)
    }

    @JvmStatic
    fun setLogLevel(level: Int, basePath: String?) {
        val effectiveLogLevel = getEffectiveLogLevel(level)
        writeLogLevelToFile(basePath, effectiveLogLevel)
    }

    /**
     * 根据构建类型获取有效的日志等级
     * Release: 0 (Disable) 或 1 (Error)
     * Beta: 1 (Error) 或 4 (Debug)
     * Canary: 3 (Info) 或 4 (Debug)
     * Debug: 0-4 全部
     */
    private fun getEffectiveLogLevel(level: Int): Int {
        if (isRelease()) {
            return if (level == 0) 0 else 1
        } else if (isBeta()) {
            return if (level == 1) 1 else 4
        } else if (isCanary()) {
            return if (level == 4) 4 else 3
        }
        return level
    }

    private fun writeLogLevelToFile(basePath: String?, level: Int) {
        try {
            val configPath = (basePath ?: "") + LOG_CONFIG_PATH
            val configFile = File(configPath)
            val configDir = configFile.parentFile

            if (configDir != null && !configDir.exists()) {
                configDir.mkdirs()
            }

            // 使用 FileLock 进行多进程安全读写
            FileChannel.open(
                configFile.toPath(),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
            ).use { channel ->
                val lock: FileLock = channel.lock()
                try {
                    FileWriter(configFile).use { writer ->
                        writer.write(level.toString())
                        writer.flush()
                    }
                } finally {
                    lock.release()
                }
            }
        } catch (e: Exception) {
            Log.e("LogManager", "Failed to write log level to file: ", e)
        }
    }

    @JvmStatic
    fun readLogLevelFromFile(basePath: String?): Int {
        try {
            val configPath = (basePath ?: "") + LOG_CONFIG_PATH
            val configFile = File(configPath)

            if (configFile.exists()) {
                BufferedReader(FileReader(configFile)).use { reader ->
                    val line = reader.readLine()
                    if (line != null) {
                        try {
                            val level = line.trim().toInt()
                            if (level in 0..4) {
                                return level
                            }
                        } catch (_: NumberFormatException) {
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LogManager", "Failed to read log level from file: ", e)
        }

        // Default fallback
        val level = mPrefsMap.getStringAsInt("log_level", 3)
        return getEffectiveLogLevel(level)
    }

    @JvmStatic
    fun getLogLevel(): Int {
        val level = mPrefsMap.getStringAsInt("log_level", 3)
        return getEffectiveLogLevel(level)
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

    @JvmStatic
    fun isLoggerAlive(): Boolean {
        try {
            val lsposedLogDirExists = rootExecCmd("ls -d /data/adb/lspd/log/ 2>/dev/null")?.isNotEmpty() == true

            if (lsposedLogDirExists) {
                val latestLogFile = rootExecCmd("ls -t /data/adb/lspd/log/modules_*.log 2>/dev/null | head -n 1")?.trim() ?: ""

                if (latestLogFile.isNotEmpty() && !latestLogFile.contains("No such file")) {
                    val grepOutput = rootExecCmd("grep -i -q 'HyperCeiler' $latestLogFile && echo 'FOUND' || echo 'EMPTY'")
                    if (grepOutput?.trim() == "EMPTY") {
                        LOGGER_CHECKER_ERR_CODE = "EMPTY_XPOSED_LOG_FILE"
                        return false
                    }
                } else {
                    LOGGER_CHECKER_ERR_CODE = "NO_XPOSED_LOG_FILE"
                    return false
                }
            }
        } catch (e: Exception) {
            LOGGER_CHECKER_ERR_CODE = e.toString()
        }

        val tag = "HyperCeilerLogManager"
        val message = "LOGGER_ALIVE_SYMBOL_${getSerial()}"
        val timeout = 5
        Log.d(tag, message)

        val executor = Executors.newSingleThreadExecutor()
        val future = executor.submit<Boolean> {
            var process: Process? = null
            try {
                process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "brief", "-s", "$tag:D"))

                BufferedReader(InputStreamReader(process.inputStream)).use { bufferedReader ->
                    var line: String?
                    while (bufferedReader.readLine().also { line = it } != null) {
                        if (line?.contains(message) == true) {
                            LOGGER_CHECKER_ERR_CODE = "SUCCESS"
                            return@submit true
                        }
                    }
                }
            } catch (e: Exception) {
                LOGGER_CHECKER_ERR_CODE = e.toString()
            } finally {
                process?.destroy()
            }
            LOGGER_CHECKER_ERR_CODE = "NO_SUCH_LOG"
            false
        }

        try {
            return future.get(timeout.toLong(), TimeUnit.SECONDS)
        } catch (_: TimeoutException) {
            LOGGER_CHECKER_ERR_CODE = "TIME_OUT"
            future.cancel(true)
        } catch (e: Exception) {
            LOGGER_CHECKER_ERR_CODE = e.toString()
        } finally {
            executor.shutdownNow()
        }

        LOGGER_CHECKER_ERR_CODE = "WITHOUT_CODE"
        return false
    }

    @JvmStatic
    fun fixLSPosedLogService(): String {
        return try {
            rootExecCmd("resetprop -n persist.log.tag.LSPosed V")
            rootExecCmd("resetprop -n persist.log.tag.LSPosed-Bridge V")
            "SUCCESS"
        } catch (e: Exception) {
            e.toString()
        }
    }
}
