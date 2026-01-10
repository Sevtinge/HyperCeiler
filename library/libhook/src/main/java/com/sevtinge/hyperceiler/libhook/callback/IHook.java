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
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.HookTool;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.ResourcesTool;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedInterface.Hooker;
import io.github.libxposed.api.XposedInterface.MethodUnhooker;
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam;

/**
 * Hook 规则接口
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
        return HookTool.getXposed();
    }

    // ==================== 类查找 ====================

    default Class<?> findClass(String className) {
        return HookTool.findClass(className, getClassLoader());
    }

    default Class<?> findClassIfExists(String className) {
        return HookTool.findClassIfExists(className, getClassLoader());
    }

    // ==================== 字段操作 ====================

    default Object getObjectField(Object obj, String fieldName) {
        return HookTool.getObjectField(obj, fieldName);
    }

    default void setObjectField(Object obj, String fieldName, Object value) {
        HookTool.setObjectField(obj, fieldName, value);
    }

    default Object getStaticObjectField(Class<?> clazz, String fieldName) {
        return HookTool.getStaticObjectField(clazz, fieldName);
    }

    default void setStaticObjectField(Class<?> clazz, String fieldName, Object value) {
        HookTool.setStaticObjectField(clazz, fieldName, value);
    }

    // ==================== 方法调用 ====================

    default Object callMethod(Object obj, String methodName, Object... args) {
        return HookTool.callMethod(obj, methodName, args);
    }

    default Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        return HookTool.callStaticMethod(clazz, methodName, args);
    }

    // ==================== Hook 方法 ====================

    default MethodUnhooker<Method> hook(Method method, Class<? extends Hooker> hooker) {
        return HookTool.hook(method, hooker);
    }

    default MethodUnhooker<Constructor<?>> hook(Constructor<?> constructor, Class<? extends Hooker> hooker) {
        return HookTool.hook(constructor, hooker);
    }

    default MethodUnhooker<Method> findAndHookMethod(Class<?> clazz, String methodName, Class<? extends Hooker> hooker, Class<?>... parameterTypes) {
        return HookTool.findAndHookMethod(clazz, methodName, hooker, parameterTypes);
    }

    default MethodUnhooker<Method> findAndHookMethod(String className, String methodName, Class<? extends Hooker> hooker, Class<?>... parameterTypes) {
        return HookTool.findAndHookMethod(className, getClassLoader(), methodName, hooker, parameterTypes);
    }

    default MethodUnhooker<Constructor<?>> findAndHookConstructor(Class<?> clazz, Class<? extends Hooker> hooker, Class<?>... parameterTypes) {
        return HookTool.findAndHookConstructor(clazz, hooker, parameterTypes);
    }

    default MethodUnhooker<Constructor<?>> findAndHookConstructor(String className, Class<? extends Hooker> hooker, Class<?>... parameterTypes) {
        return HookTool.findAndHookConstructor(className, getClassLoader(), hooker, parameterTypes);
    }

    default void hookAllMethods(Class<?> clazz, String methodName, Class<? extends Hooker> hooker) {
        HookTool.hookAllMethods(clazz, methodName, hooker);
    }

    default void hookAllMethods(String className, String methodName, Class<? extends Hooker> hooker) {
        HookTool.hookAllMethods(className, getClassLoader(), methodName, hooker);
    }

    default void hookAllConstructors(Class<?> clazz, Class<? extends Hooker> hooker) {
        HookTool.hookAllConstructors(clazz, hooker);
    }

    default void hookAllConstructors(String className, Class<? extends Hooker> hooker) {
        HookTool.hookAllConstructors(className, getClassLoader(), hooker);
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
