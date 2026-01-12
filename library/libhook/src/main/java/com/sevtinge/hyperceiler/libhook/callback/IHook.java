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
package com.sevtinge.hyperceiler.libhook.callback;

import com.sevtinge.hyperceiler.libhook.base.BaseLoad;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.ResourcesTool;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.lang.reflect.Method;
import java.util.List;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedInterface.MethodUnhooker;
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam;

/**
 * Hook 规则接口
 * <p>
 * 提供 Java 常用的 Hook 工具方法，Kotlin 建议直接使用 ezxhelper API。
 *
 * @author HyperCeiler
 */
public interface IHook {
    void init();

    default ClassLoader getClassLoader() {
        return BaseLoad.getClassLoader();
    }

    default String getPackageName() {
        return BaseLoad.getPackageName();
    }

    default PackageLoadedParam getLpparam() {
        return BaseLoad.getLpparam();
    }

    default XposedInterface xposed() {
        return BaseLoad.getXposed();
    }

    // ==================== 类查找 ====================

    default Class<?> findClass(String className) {
        return EzxHelpUtils.findClass(className, getClassLoader());
    }

    default Class<?> findClassIfExists(String className) {
        return EzxHelpUtils.findClassIfExists(className, getClassLoader());
    }

    // ==================== 字段操作 ====================

    default Object getObjectField(Object obj, String fieldName) {
        return EzxHelpUtils.getObjectField(obj, fieldName);
    }

    default void setObjectField(Object obj, String fieldName, Object value) {
        EzxHelpUtils.setObjectField(obj, fieldName, value);
    }

    default Object getStaticObjectField(Class<?> clazz, String fieldName) {
        return EzxHelpUtils.getStaticObjectField(clazz, fieldName);
    }

    default void setStaticObjectField(Class<?> clazz, String fieldName, Object value) {
        EzxHelpUtils.setStaticObjectField(clazz, fieldName, value);
    }

    // ==================== 方法调用 ====================

    default Object callMethod(Object obj, String methodName, Object... args) {
        return EzxHelpUtils.callMethod(obj, methodName, args);
    }

    default Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        return EzxHelpUtils.callStaticMethod(clazz, methodName, args);
    }

    // ==================== Hook 方法 ====================

    /**
     * Hook 方法
     *
     * @param method   要 Hook 的方法
     * @param callback Hook 回调
     * @return MethodUnhooker 对象
     */
    default MethodUnhooker<?> hookMethod(Method method, IMethodHook callback) {
        return EzxHelpUtils.hookMethod(method, callback);
    }

    /**
     * 查找并 Hook 方法
     * <p>
     * 最后一个参数必须是 IMethodHook，前面的参数是参数类型
     *
     * @param clazz      目标类
     * @param methodName 方法名
     * @param args       参数类型 + IMethodHook 回调
     * @return MethodUnhooker 对象
     */
    default MethodUnhooker<?> findAndHookMethod(Class<?> clazz, String methodName, Object... args) {
        return EzxHelpUtils.findAndHookMethod(clazz, methodName, args);
    }

    /**
     * Hook 类中所有指定名称的方法
     *
     * @param clazz      目标类
     * @param methodName 方法名
     * @param callback   Hook 回调
     * @return MethodUnhooker 对象列表
     */
    default List<MethodUnhooker<?>> hookAllMethods(Class<?> clazz, String methodName, IMethodHook callback) {
        return EzxHelpUtils.hookAllMethods(clazz, methodName, callback);
    }

    /**
     * Hook 类中所有指定名称的方法（通过类名）
     *
     * @param className  类名
     * @param methodName 方法名
     * @param callback   Hook 回调
     * @return MethodUnhooker 对象列表
     */
    default List<MethodUnhooker<?>> hookAllMethods(String className, String methodName, IMethodHook callback) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w("IHook", "hookAllMethods: class not found: " + className);
            return java.util.Collections.emptyList();
        }
        return EzxHelpUtils.hookAllMethods(clazz, methodName, callback);
    }

    /**
     * Hook 类中所有构造器
     *
     * @param clazz    目标类
     * @param callback Hook 回调
     * @return MethodUnhooker 对象列表
     */
    default List<MethodUnhooker<?>> hookAllConstructors(Class<?> clazz, IMethodHook callback) {
        return EzxHelpUtils.hookAllConstructors(clazz, callback);
    }

    // ==================== 便捷 Hook 工具 ====================

    /**
     * 创建一个返回常量值的 Hook 回调
     */
    default IMethodHook returnConstant(Object result) {
        return EzxHelpUtils.returnConstant(result);
    }

    /**
     * 获取阻止原方法执行的 Hook（返回 null）
     */
    default IMethodHook doNothing() {
        return EzxHelpUtils.DO_NOTHING;
    }

    // ==================== 资源 Hook ====================

    default int getFakeResId(String resourceName) {
        return ResourcesTool.getFakeResId(resourceName);
    }

    default void setResReplacement(String pkg, String type, String name, int replacementResId) {
        ResourcesTool resTool = ResourcesTool.getInstance();
        if (resTool != null) {
            resTool.setResReplacement(pkg, type, name, replacementResId);
        }
    }

    default void setDensityReplacement(String pkg, String type, String name, float replacementResValue) {
        ResourcesTool resTool = ResourcesTool.getInstance();
        if (resTool != null) {
            resTool.setDensityReplacement(pkg, type, name, replacementResValue);
        }
    }

    default void setObjectReplacement(String pkg, String type, String name, Object replacementResValue) {
        ResourcesTool resTool = ResourcesTool.getInstance();
        if (resTool != null) {
            resTool.setObjectReplacement(pkg, type, name, replacementResValue);
        }
    }
}
