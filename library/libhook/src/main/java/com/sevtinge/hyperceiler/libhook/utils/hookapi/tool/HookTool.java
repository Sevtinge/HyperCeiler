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
package com.sevtinge.hyperceiler.libhook.utils.hookapi.tool;

import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedInterface.Hooker;
import io.github.libxposed.api.XposedInterface.MethodUnhooker;

/**
 * Hook 工具类
 *
 * @author HyperCeiler
 */
public final class HookTool {

    private static final String TAG = "HookTool";
    private static XposedInterface sXposed;

    private HookTool() {
    }

    public static void init(XposedInterface xposed) {
        sXposed = xposed;
    }

    public static XposedInterface getXposed() {
        return sXposed;
    }

    public static Class<?> findClass(String className, ClassLoader classLoader) {
        return ReflectUtils.findClass(className, classLoader);
    }

    public static Class<?> findClassIfExists(String className, ClassLoader classLoader) {
        return ReflectUtils.findClassIfExists(className, classLoader);
    }

    public static Object getObjectField(Object obj, String fieldName) {
        return ReflectUtils.getObjectField(obj, fieldName);
    }

    public static Object getObjectFieldSilently(Object obj, String fieldName) {
        try {
            return ReflectUtils.getObjectField(obj, fieldName);
        } catch (Throwable t) {
            XposedLog.e(TAG, "getObjectField failed: " + fieldName, t);
            return null;
        }
    }

    public static void setObjectField(Object obj, String fieldName, Object value) {
        ReflectUtils.setObjectField(obj, fieldName, value);
    }

    public static Object getStaticObjectField(Class<?> clazz, String fieldName) {
        return ReflectUtils.getStaticObjectField(clazz, fieldName);
    }

    public static Object getStaticObjectFieldSilently(Class<?> clazz, String fieldName) {
        try {
            return ReflectUtils.getStaticObjectField(clazz, fieldName);
        } catch (Throwable t) {
            XposedLog.e(TAG, "getStaticObjectField failed: " + clazz.getName() + "#" + fieldName, t);
            return null;
        }
    }

    public static void setStaticObjectField(Class<?> clazz, String fieldName, Object value) {
        ReflectUtils.setStaticObjectField(clazz, fieldName, value);
    }

    public static Object callMethod(Object obj, String methodName, Object... args) {
        return ReflectUtils.callMethod(obj, methodName, args);
    }

    public static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        return ReflectUtils.callStaticMethod(clazz, methodName, args);
    }

    public static MethodUnhooker<Method> hook(Method method, Class<? extends Hooker> hooker) {
        if (sXposed == null) throw new IllegalStateException("HookTool not initialized");
        return sXposed.hook(method, hooker);
    }

    @SuppressWarnings("unchecked")
    public static MethodUnhooker<Constructor<?>> hook(Constructor<?> constructor, Class<? extends Hooker> hooker) {
        if (sXposed == null) throw new IllegalStateException("HookTool not initialized");
        return (MethodUnhooker<Constructor<?>>) (MethodUnhooker<?>) sXposed.hook(constructor, hooker);
    }

    public static MethodUnhooker<Method> findAndHookMethod(Class<?> clazz, String methodName, Class<? extends Hooker> hooker, Class<?>... parameterTypes) {
        if (clazz == null) throw new IllegalArgumentException("clazz == null");
        if (sXposed == null) throw new IllegalStateException("HookTool not initialized");

        Method target = findMethodInHierarchy(clazz, methodName, parameterTypes);
        if (target == null) {
            throw new NoSuchMethodError(clazz.getName() + "#" + methodName + Arrays.toString(parameterTypes));
        }
        return sXposed.hook(target, hooker);
    }

    public static MethodUnhooker<Method> findAndHookMethod(String className, ClassLoader classLoader, String methodName, Class<? extends Hooker> hooker, Class<?>... parameterTypes) {
        Class<?> clazz = findClassIfExists(className, classLoader);
        if (clazz == null) {
            throw new IllegalArgumentException("Class not found: " + className);
        }
        return findAndHookMethod(clazz, methodName, hooker, parameterTypes);
    }

    public static MethodUnhooker<Method> findAndHookMethodSilently(Class<?> clazz, String methodName, Class<? extends Hooker> hooker, Class<?>... parameterTypes) {
        try {
            return findAndHookMethod(clazz, methodName, hooker, parameterTypes);
        } catch (Throwable t) {
            XposedLog.e(TAG, "findAndHookMethod failed: " + clazz.getName() + "#" + methodName, t);
            return null;
        }
    }

    public static MethodUnhooker<Method> findAndHookMethodSilently(String className, ClassLoader classLoader, String methodName, Class<? extends Hooker> hooker, Class<?>... parameterTypes) {
        try {
            return findAndHookMethod(className, classLoader, methodName, hooker, parameterTypes);
        } catch (Throwable t) {
            XposedLog.e(TAG, "findAndHookMethod failed: " + className + "#" + methodName, t);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static MethodUnhooker<Constructor<?>> findAndHookConstructor(Class<?> clazz, Class<? extends Hooker> hooker, Class<?>... parameterTypes) {
        if (clazz == null) throw new IllegalArgumentException("clazz == null");
        if (sXposed == null) throw new IllegalStateException("HookTool not initialized");

        Constructor<?> ctor = findConstructorExact(clazz, parameterTypes);
        if (ctor == null) {
            throw new NoSuchMethodError(clazz.getName() + "<init>" + Arrays.toString(parameterTypes));
        }
        return (MethodUnhooker<Constructor<?>>) (MethodUnhooker<?>) sXposed.hook(ctor, hooker);
    }

    public static MethodUnhooker<Constructor<?>> findAndHookConstructor(String className, ClassLoader classLoader, Class<? extends Hooker> hooker, Class<?>... parameterTypes) {
        Class<?> clazz = findClassIfExists(className, classLoader);
        if (clazz == null) {
            throw new IllegalArgumentException("Class not found: " + className);
        }
        return findAndHookConstructor(clazz, hooker, parameterTypes);
    }

    public static MethodUnhooker<Constructor<?>> findAndHookConstructorSilently(Class<?> clazz, Class<? extends Hooker> hooker, Class<?>... parameterTypes) {
        try {
            return findAndHookConstructor(clazz, hooker, parameterTypes);
        } catch (Throwable t) {
            XposedLog.e(TAG, "findAndHookConstructor failed: " + clazz.getName(), t);
            return null;
        }
    }

    public static void hookAllMethods(Class<?> clazz, String methodName, Class<? extends Hooker> hooker) {
        if (clazz == null || sXposed == null) return;
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                try {
                    sXposed.hook(m, hooker);
                } catch (Throwable t) {
                    XposedLog.e(TAG, "hookAllMethods failed: " + clazz.getName() + "#" + methodName, t);
                }
            }
        }
    }

    public static void hookAllMethods(String className, ClassLoader classLoader, String methodName, Class<? extends Hooker> hooker) {
        Class<?> clazz = findClassIfExists(className, classLoader);
        if (clazz == null) {
            XposedLog.w(TAG, "hookAllMethods: class not found: " + className);
            return;
        }
        hookAllMethods(clazz, methodName, hooker);
    }

    public static void hookAllConstructors(Class<?> clazz, Class<? extends Hooker> hooker) {
        if (clazz == null || sXposed == null) return;
        for (Constructor<?> c : clazz.getDeclaredConstructors()) {
            try {
                sXposed.hook(c, hooker);
            } catch (Throwable t) {
                XposedLog.e(TAG, "hookAllConstructors failed: " + clazz.getName(), t);
            }
        }
    }

    public static void hookAllConstructors(String className, ClassLoader classLoader, Class<? extends Hooker> hooker) {
        Class<?> clazz = findClassIfExists(className, classLoader);
        if (clazz == null) {
            XposedLog.w(TAG, "hookAllConstructors: class not found: " + className);
            return;
        }
        hookAllConstructors(clazz, hooker);
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

