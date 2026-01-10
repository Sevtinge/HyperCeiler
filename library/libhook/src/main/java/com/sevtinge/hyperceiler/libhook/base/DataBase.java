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

import java.util.HashMap;

/**
 * 模块数据库基类
 * <p>
 * 由注解处理器生成具体实现，包含模块和目标包名的映射关系。
 * 具体项目需要由 @HookBase 注解处理器生成 DataBase 类。
 *
 * @author 焕晨HChen
 */
public class DataBase {
    public String mTargetPackage;
    public int mTargetSdk;
    public float mTargetOSVersion;
    public int isPad;

    public DataBase(String targetPackage, int targetSdk, float targetOSVersion, int isPad) {
        this.mTargetPackage = targetPackage;
        this.mTargetSdk = targetSdk;
        this.mTargetOSVersion = targetOSVersion;
        this.isPad = isPad;
    }

    /**
     * 获取模块数据映射
     * <p>
     * 此方法应由注解处理器生成的子类覆盖
     *
     * @return 模块类名到 DataBase 的映射
     */
    public static HashMap<String, DataBase> get() {
        return new HashMap<>();
    }
}

