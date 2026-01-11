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
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.ReflectUtils;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.ResourcesTool;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

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
        return BaseLoad.getXposed();
    }

    // ==================== 类查找 ====================

    default Class<?> findClass(String className) {
        return ReflectUtils.findClass(className, getClassLoader());
    }

    default Class<?> findClassIfExists(String className) {
        return ReflectUtils.findClassIfExists(className, getClassLoader());
    }

    // ==================== 字段操作 ====================

    default Object getObjectField(Object obj, String fieldName) {
        return ReflectUtils.getObjectField(obj, fieldName);
    }

    default void setObjectField(Object obj, String fieldName, Object value) {
        ReflectUtils.setObjectField(obj, fieldName, value);
    }

    default Object getStaticObjectField(Class<?> clazz, String fieldName) {
        return ReflectUtils.getStaticObjectField(clazz, fieldName);
    }

    default void setStaticObjectField(Class<?> clazz, String fieldName, Object value) {
        ReflectUtils.setStaticObjectField(clazz, fieldName, value);
    }

    // ==================== 方法调用 ====================

    default Object callMethod(Object obj, String methodName, Object... args) {
        return ReflectUtils.callMethod(obj, methodName, args);
    }

    default Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        return ReflectUtils.callStaticMethod(clazz, methodName, args);
    }

    // ==================== Hook 方法 ====================

    default MethodUnhooker<Method> hook(Method method, Class<? extends Hooker> hooker) {
        if (xposed() == null) throw new IllegalStateException("HookTool not initialized");
        return xposed().hook(method, hooker);
    }

    @SuppressWarnings("unchecked")
    default MethodUnhooker<Constructor<?>> hook(Constructor<?> constructor, Class<? extends Hooker> hooker) {
        if (xposed() == null) throw new IllegalStateException("HookTool not initialized");
        return (MethodUnhooker<Constructor<?>>) (MethodUnhooker<?>) xposed().hook(constructor, hooker);
    }

    default MethodUnhooker<Method> findAndHookMethod(Class<?> clazz, String methodName, Class<? extends Hooker> hooker, Class<?>... parameterTypes) {
        if (clazz == null) throw new IllegalArgumentException("clazz == null");
        if (xposed() == null) throw new IllegalStateException("HookTool not initialized");

        Method target = findMethodInHierarchy(clazz, methodName, parameterTypes);
        if (target == null) {
            throw new NoSuchMethodError(clazz.getName() + "#" + methodName + Arrays.toString(parameterTypes));
        }
        return xposed().hook(target, hooker);
    }

    default MethodUnhooker<Method> findAndHookMethod(String className, String methodName, Class<? extends Hooker> hooker, Class<?>... parameterTypes) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            throw new IllegalArgumentException("Class not found: " + className);
        }
        return findAndHookMethod(clazz, methodName, hooker, parameterTypes);
    }

    @SuppressWarnings("unchecked")
    default MethodUnhooker<Constructor<?>> findAndHookConstructor(Class<?> clazz, Class<? extends Hooker> hooker, Class<?>... parameterTypes) {
        if (clazz == null) throw new IllegalArgumentException("clazz == null");
        if (xposed() == null) throw new IllegalStateException("HookTool not initialized");

        Constructor<?> ctor = findConstructorExact(clazz, parameterTypes);
        if (ctor == null) {
            throw new NoSuchMethodError(clazz.getName() + "<init>" + Arrays.toString(parameterTypes));
        }
        return (MethodUnhooker<Constructor<?>>) (MethodUnhooker<?>) xposed().hook(ctor, hooker);
    }

    default MethodUnhooker<Constructor<?>> findAndHookConstructor(String className, Class<? extends Hooker> hooker, Class<?>... parameterTypes) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            throw new IllegalArgumentException("Class not found: " + className);
        }
        return findAndHookConstructor(clazz, hooker, parameterTypes);
    }

    default void hookAllMethods(Class<?> clazz, String methodName, Class<? extends Hooker> hooker) {
        if (clazz == null || xposed() == null) return;
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                try {
                    xposed().hook(m, hooker);
                } catch (Throwable t) {
                    XposedLog.e("IHook", "hookAllMethods failed: " + clazz.getName() + "#" + methodName, t);
                }
            }
        }
    }

    default void hookAllMethods(String className, String methodName, Class<? extends Hooker> hooker) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w("IHook", "hookAllMethods: class not found: " + className);
            return;
        }
        hookAllMethods(clazz, methodName, hooker);
    }

    default void hookAllConstructors(Class<?> clazz, Class<? extends Hooker> hooker) {
        if (clazz == null || xposed() == null) return;
        for (Constructor<?> c : clazz.getDeclaredConstructors()) {
            try {
                xposed().hook(c, hooker);
            } catch (Throwable t) {
                XposedLog.e("IHook", "hookAllConstructors failed: " + clazz.getName(), t);
            }
        }
    }

    default void hookAllConstructors(String className, Class<? extends Hooker> hooker) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w("IHook", "hookAllConstructors: class not found: " + className);
            return;
        }
        hookAllConstructors(clazz, hooker);
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

    private static Method findMethodInHierarchy(Class<?> clazz, String methodName, Class<?>[] paramTypes) {
        Class<?> c = clazz;
        while (c != null) {
            try {
                Method m = c.getDeclaredMethod(methodName, paramTypes);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private static Constructor<?> findConstructorExact(Class<?> clazz, Class<?>[] paramTypes) {
        try {
            Constructor<?> c = clazz.getDeclaredConstructor(paramTypes);
            c.setAccessible(true);
            return c;
        } catch (NoSuchMethodException ignored) {
        }
        return null;
    }
}
