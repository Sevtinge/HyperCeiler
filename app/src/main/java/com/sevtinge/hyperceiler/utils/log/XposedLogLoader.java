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
package com.sevtinge.hyperceiler.utils.log;

import android.content.Context;
import android.util.Log;

import com.fan.common.logviewer.LogEntry;
import com.fan.common.logviewer.LogManager;
import com.sevtinge.hyperceiler.hook.utils.shell.ShellUtils;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 用于加载 Xposed/LSPosed 日志并显示在 LogViewer 中
 */
public class XposedLogLoader {

    private static final String TAG = "XposedLogLoader";

    /**
     * 异步加载 Xposed 日志到 LogManager
     *
     * @param context 上下文
     * @param callback 加载完成回调（可为 null）
     */
    public static void loadLogs(Context context, Runnable callback) {
        LogManager logManager = LogManager.getInstance(context);
        logManager.clearXposedLogs(); // 清除旧的 Xposed 日志

        new Thread(() -> {
            try {
                // 查找最新的 Xposed 日志文件
                String logFileCmd = "ls -t /data/adb/lspd/log/modules_*.log 2>/dev/null | head -n 1";
                String logFilePath = ShellUtils.rootExecCmd(logFileCmd).trim();

                if (logFilePath.isEmpty() || logFilePath.contains("No such file") || logFilePath.contains("ls:")) {
                    logManager.addXposedLog(new LogEntry("W", "XposedLogLoader",
                            "No Xposed log file found. Make sure LSPosed is installed and has generated logs.",
                            "System", true));
                    if (callback != null) callback.run();
                    return;
                }

                // 读取日志文件内容
                String content = ShellUtils.rootExecCmd("cat " + logFilePath);

                if (content == null || content.isEmpty()) {
                    logManager.addXposedLog(new LogEntry("W", "XposedLogLoader",
                            "Xposed log file is empty.", "System", true));
                    if (callback != null) callback.run();
                    return;
                }

                BufferedReader reader = new BufferedReader(new StringReader(content));
                String line;
                List<LogEntry> entries = new ArrayList<>();

                while ((line = reader.readLine()) != null) {
                    // 只处理包含 HyperCeiler 的日志行
                    if (line.contains("HyperCeiler")) {
                        LogEntry entry = parseXposedLogLine(line);
                        if (entry != null) {
                            entries.add(entry);
                        }
                    }
                }
                reader.close();

                if (!entries.isEmpty()) {
                    logManager.addXposedLogs(entries);
                    Log.i(TAG, "Loaded " + entries.size() + " Xposed log entries");
                } else {
                    logManager.addXposedLog(new LogEntry("I", "XposedLogLoader",
                            "No HyperCeiler logs found in Xposed log file.", "System", true));
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to load Xposed logs", e);
                logManager.addXposedLog(new LogEntry("E", "XposedLogLoader",
                        "Failed to load logs: " + e.getMessage(), "System", true));
            }

            if (callback != null) callback.run();
        }).start();
    }

    /**
     * 同步加载 Xposed 日志（阻塞调用）
     *
     * @param context 上下文
     */
    public static void loadLogsSync(Context context) {
        LogManager logManager = LogManager.getInstance(context);
        logManager.clearXposedLogs();

        try {
            String logFileCmd = "ls -t /data/adb/lspd/log/modules_*.log 2>/dev/null | head -n 1";
            String logFilePath = ShellUtils.rootExecCmd(logFileCmd).trim();

            if (logFilePath.isEmpty() || logFilePath.contains("No such file") || logFilePath.contains("ls:")) {
                logManager.addXposedLog(new LogEntry("W", "XposedLogLoader",
                        "No Xposed log file found.", "System", true));
                return;
            }

            String content = ShellUtils.rootExecCmd("cat " + logFilePath);

            if (content == null || content.isEmpty()) {
                logManager.addXposedLog(new LogEntry("W", "XposedLogLoader",
                        "Xposed log file is empty.", "System", true));
                return;
            }

            BufferedReader reader = new BufferedReader(new StringReader(content));
            String line;
            List<LogEntry> entries = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                if (line.contains("HyperCeiler")) {
                    LogEntry entry = parseXposedLogLine(line);
                    if (entry != null) {
                        entries.add(entry);
                    }
                }
            }
            reader.close();

            if (!entries.isEmpty()) {
                logManager.addXposedLogs(entries);
            } else {
                logManager.addXposedLog(new LogEntry("I", "XposedLogLoader",
                        "No HyperCeiler logs found.", "System", true));
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to load Xposed logs", e);
            logManager.addXposedLog(new LogEntry("E", "XposedLogLoader",
                    "Failed to load logs: " + e.getMessage(), "System", true));
        }
    }

    /**
     * 解析 Xposed 日志行
     *
     * @param line 日志行
     * @return LogEntry 对象
     */
    private static LogEntry parseXposedLogLine(String line) {
        // 解析日志等级
        String level = "V";

        // LSPosed 日志格式通常包含 [HyperCeiler][E] 或 [HyperCeiler][I] 等标记
        if (line.contains("[E]") || line.contains("/E]") || line.contains("[E/")) {
            level = "E";
        } else if (line.contains("[W]") || line.contains("/W]") || line.contains("[W/")) {
            level = "W";
        } else if (line.contains("[I]") || line.contains("/I]") || line.contains("[I/")) {
            level = "I";
        } else if (line.contains("[D]") || line.contains("/D]") || line.contains("[D/")) {
            level = "D";
        }

        // 提取消息部分
        String message = line;
        int tagIndex = line.indexOf("[HyperCeiler]");
        if (tagIndex != -1) {
            message = line.substring(tagIndex);
        }

        // 提取模块/标签信息
        String module = "Other";
        // 尝试从日志中提取包名
        // 格式: [HyperCeiler][I][com.xxx.yyy][TagName]: message
        // 等级标记后的第一个中括号内容
        int levelEndIndex = -1;
        for (String lvl : new String[]{"[I]", "[D]", "[W]", "[E]", "[V]"}) {
            int idx = message.indexOf(lvl);
            if (idx != -1) {
                levelEndIndex = idx + lvl.length();
                break;
            }
        }

        if (levelEndIndex != -1 && levelEndIndex < message.length() && message.charAt(levelEndIndex) == '[') {
            int pkgEndIndex = message.indexOf("]", levelEndIndex + 1);
            if (pkgEndIndex != -1) {
                String candidate = message.substring(levelEndIndex + 1, pkgEndIndex);
                // 验证是否为包名格式：
                // 1. "android" 特殊保留
                // 2. 包含点号且符合包名规范 (如 com.xxx.yyy)
                if (isValidPackageName(candidate)) {
                    module = candidate;
                }
            }
        }

        return new LogEntry(level, module, message, "Xposed", true);
    }

    /**
     * 验证是否为有效的包名格式
     *
     * @param name 待验证的字符串
     * @return 是否为包名
     */
    private static boolean isValidPackageName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }

        // "android" 是特殊的系统包名
        if ("android".equals(name)) {
            return true;
        }

        // 包名必须包含点号
        if (!name.contains(".")) {
            return false;
        }

        // 验证包名格式：以字母开头，只包含字母、数字、点、下划线
        // 且每个段都以字母开头
        String[] parts = name.split("\\.");
        for (String part : parts) {
            if (part.isEmpty()) {
                return false;
            }
            // 每个段必须以字母开头
            if (!Character.isLetter(part.charAt(0))) {
                return false;
            }
            // 每个字符只能是字母、数字、下划线
            for (char c : part.toCharArray()) {
                if (!Character.isLetterOrDigit(c) && c != '_') {
                    return false;
                }
            }
        }

        return true;
    }
}
