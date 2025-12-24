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
package com.sevtinge.hyperceiler.hook.module.base.tool;

import com.sevtinge.hyperceiler.hook.BuildConfig;
import com.sevtinge.hyperceiler.hook.utils.log.XposedLogUtils;
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsMap;
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils;
import com.sevtinge.hyperceiler.hook.utils.reflect.ReflectUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookTool extends XposedLogUtils {
    public static final PrefsMap<String, Object> mPrefsMap = PrefsUtils.mPrefsMap;
    private final String TAG = getClass().getSimpleName();

    public XC_LoadPackage.LoadPackageParam lpparam;

    public void setLoadPackageParam(XC_LoadPackage.LoadPackageParam param) {
    }

    public Class<?> findClass(String className) {
        return findClass(className, lpparam.classLoader);
    }

    public Class<?> findClass(String className, ClassLoader classLoader) {
        try {
            return ReflectUtils.findClass(className, classLoader);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public Class<?> findClassIfExists(String className) {
        try {
            return findClass(className);
        } catch (Throwable e) {
            if (BuildConfig.DEBUG)
                logE(TAG, "find " + className + " is Null: " + e);
        }
        return null;
    }

    public Class<?> findClassIfExists(String newClassName, String oldClassName) {
        try {
            return findClass(findClassIfExists(newClassName) != null ? newClassName : oldClassName);
        } catch (Throwable e) {
            if (BuildConfig.DEBUG)
                logE(TAG, "find " + newClassName + " and " + oldClassName + " is Null: " + e);

        }
        return null;
    }

    public Class<?> findClassIfExists(String className, ClassLoader classLoader) {
        try {
            return findClass(className, classLoader);
        } catch (Throwable e) {
            if (BuildConfig.DEBUG)
                logE(TAG, "find " + className + " is Null: " + e);
        }
        return null;
    }

    public static class MethodHook extends XC_MethodHook {

        protected void before(MethodHookParam param) throws Throwable {
        }

        protected void after(MethodHookParam param) throws Throwable {
        }

        public MethodHook() {
            super();
        }

        public MethodHook(int priority) {
            super(priority);
        }

        public static MethodHook returnConstant(final Object result) {
            return new MethodHook(PRIORITY_DEFAULT) {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(result);
                }
            };
        }

        public static final MethodHook DO_NOTHING = new MethodHook(PRIORITY_HIGHEST * 2) {
            @Override
            protected void before(MethodHookParam param) {
                param.setResult(null);
            }
        };

        @Override
        public void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                this.before(param);
            } catch (Throwable t) {
                // logE("BeforeHook", t);
            }
        }

        @Override
        public void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                this.after(param);
            } catch (Throwable t) {
                // logE("AfterHook", t);
            }
        }
    }

    /*用于替换方法*/
    public abstract static class replaceHookedMethod extends MethodHook {

        public replaceHookedMethod() {
            super();
        }

        public replaceHookedMethod(int priority) {
            super(priority);
        }

        protected abstract Object replace(MethodHookParam param) throws Throwable;

        @Override
        public void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                Object result = replace(param);
                param.setResult(result);
            } catch (Throwable t) {
                param.setThrowable(t);
            }
        }
    }

    public static void hookMethod(Method method, MethodHook callback) {
        XposedBridge.hookMethod(method, callback);
    }

    public static Object getObjectFieldSilently(Object obj, String fieldName) {
        try {
            return ReflectUtils.getObjectField(obj, fieldName);
        } catch (Throwable t) {
            return "ObjectFieldNotExist";
        }
    }

    public void safeHookMethod(Method method, MethodHook callback) {
        try {
            hookMethod(method, callback);
        } catch (Throwable e) {
            logE(TAG, "safeHookMethod failed: " + e);
        }
    }

    public static XC_MethodHook.Unhook findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        if (clazz == null) throw new IllegalArgumentException("clazz == null");
        if (parameterTypesAndCallback == null || parameterTypesAndCallback.length == 0) {
            throw new IllegalArgumentException("parameterTypesAndCallback is empty");
        }

        Object last = parameterTypesAndCallback[parameterTypesAndCallback.length - 1];
        if (!(last instanceof XC_MethodHook)) {
            throw new IllegalArgumentException("Last argument must be XC_MethodHook");
        }
        XC_MethodHook callback = (XC_MethodHook) last;

        ClassLoader cl = clazz.getClassLoader();
        Class<?>[] paramTypes = resolveParamTypes(cl, parameterTypesAndCallback, 0, parameterTypesAndCallback.length - 1);

        Method target = findMethodInHierarchy(clazz, methodName, paramTypes);
        if (target == null) {
            throw new NoSuchMethodError(clazz.getName() + "#" + methodName + Arrays.toString(paramTypes));
        }
        return XposedBridge.hookMethod(target, callback);
    }

    public void findAndHookMethod(String className, String methodName, Object... parameterTypesAndCallback) {
        findAndHookMethod(findClassIfExists(className), methodName, parameterTypesAndCallback);
    }

    public void safeFindAndHookMethod(String className, String methodName, Object... parameterTypesAndCallback) {
        try {
            findAndHookMethod(className, methodName, parameterTypesAndCallback);
        } catch (Throwable e) {
            logE(TAG, "safeFindAndHookMethod failed: " + e);
        }
    }

    public static void findAndHookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        Class<?> clazz = null;
        try {
            clazz = ReflectUtils.findClassIfExists(className, classLoader);
        } catch (Throwable ignored) {
        }
        if (clazz == null) return;
        findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
    }

    public XC_MethodHook.Unhook findAndHookMethodUseUnhook(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        try {
            Class<?> clazz = ReflectUtils.findClassIfExists(className, classLoader);
            if (clazz == null) return null;
            return findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
        } catch (Throwable t) {
            // logE("findAndHookMethodUseUnhook", "Failed to hook " + methodName + " method in " + className);
            return null;
        }
    }

    public XC_MethodHook.Unhook findAndHookMethodUseUnhook(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        try {
            return findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
        } catch (Throwable t) {
            // logE("findAndHookMethodUseUnhook", "Failed to hook " + methodName + " method in " + clazz.getCanonicalName());
            return null;
        }
    }

    public boolean findAndHookMethodSilently(String className, String methodName, Object... parameterTypesAndCallback) {
        try {
            findAndHookMethod(className, methodName, parameterTypesAndCallback);
            return true;
        } catch (Throwable t) {
            // logE("findAndHookMethodSilently", className + methodName + " is null: " + t);
            return false;
        }
    }

    public boolean findAndHookMethodSilently(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        try {
            findAndHookMethod(className, classLoader, methodName, parameterTypesAndCallback);
            return true;
        } catch (Throwable t) {
            // logE("findAndHookMethodSilently", className + methodName + " is null: " + t);
            return false;
        }
    }

    public boolean findAndHookMethodSilently(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        try {
            findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
            return true;
        } catch (Throwable t) {
            // logE("findAndHookMethodSilently", clazz + methodName + " is null: " + t);
            return false;
        }
    }

    public void findAndHookConstructor(String className, Object... parameterTypesAndCallback) {
        findAndHookConstructor(findClassIfExists(className), parameterTypesAndCallback);
    }

    public void findAndHookConstructor(Class<?> hookClass, Object... parameterTypesAndCallback) {
        if (hookClass == null) throw new IllegalArgumentException("hookClass == null");
        if (parameterTypesAndCallback == null || parameterTypesAndCallback.length == 0) {
            throw new IllegalArgumentException("parameterTypesAndCallback is empty");
        }

        Object last = parameterTypesAndCallback[parameterTypesAndCallback.length - 1];
        if (!(last instanceof XC_MethodHook)) {
            throw new IllegalArgumentException("Last argument must be XC_MethodHook");
        }
        XC_MethodHook callback = (XC_MethodHook) last;
        Class<?>[] paramTypes = resolveParamTypes(hookClass.getClassLoader(), parameterTypesAndCallback, 0, parameterTypesAndCallback.length - 1);

        java.lang.reflect.Constructor<?> ctor = findConstructorInHierarchy(hookClass, paramTypes);
        if (ctor == null) {
            throw new NoSuchMethodError(hookClass.getName() + "<init>" + Arrays.toString(paramTypes));
        }
        XposedBridge.hookMethod(ctor, callback);
    }

    public static void findAndHookConstructor(String className, ClassLoader classLoader, Object... parameterTypesAndCallback) {
        Class<?> hookClass = ReflectUtils.findClassIfExists(className, classLoader);
        if (hookClass == null) return;
        new HookTool().findAndHookConstructor(hookClass, parameterTypesAndCallback);
    }

    public void hookAllMethods(String className, String methodName, MethodHook callback) {
        Class<?> hookClass = findClassIfExists(className);
        if (hookClass != null) {
            XposedBridge.hookAllMethods(hookClass, methodName, callback);
        }
    }

    public static void hookAllMethods(Class<?> hookClass, String methodName, MethodHook callback) {
        XposedBridge.hookAllMethods(hookClass, methodName, callback);
    }

    public static void hookAllMethods(String className, ClassLoader classLoader, String methodName, MethodHook callback) {
        Class<?> hookClass = ReflectUtils.findClassIfExists(className, classLoader);
        if (hookClass != null) {
            XposedBridge.hookAllMethods(hookClass, methodName, callback);
        }
    }

    public void hookAllMethodsSilently(String className, String methodName, MethodHook callback) {
        try {
            Class<?> hookClass = findClassIfExists(className);
            if (hookClass != null) {
                XposedBridge.hookAllMethods(hookClass, methodName, callback);
            }
        } catch (Throwable ignored) {
        }
    }

    public void hookAllMethodsSilently(Class<?> hookClass, String methodName, MethodHook callback) {
        try {
            if (hookClass != null) {
                XposedBridge.hookAllMethods(hookClass, methodName, callback);
            }
        } catch (Throwable ignored) {
        }
    }

    public boolean hookAllMethodsBoolean(String className, String methodName, MethodHook callback) {
        try {
            Class<?> hookClass = findClassIfExists(className);
            if (hookClass != null) {
                return !XposedBridge.hookAllMethods(hookClass, methodName, callback).isEmpty();
            }
        } catch (Throwable ignored) {
            return false;
        }
        return false;
    }

    public boolean hookAllMethodsBoolean(Class<?> hookClass, String methodName, MethodHook callback) {
        try {
            if (hookClass != null) {
                return !XposedBridge.hookAllMethods(hookClass, methodName, callback).isEmpty();
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    public void hookAllConstructors(String className, MethodHook callback) {
        Class<?> hookClass = findClassIfExists(className);
        if (hookClass != null) {
            XposedBridge.hookAllConstructors(hookClass, callback);
        }
    }

    public void hookAllConstructors(Class<?> hookClass, MethodHook callback) {
        XposedBridge.hookAllConstructors(hookClass, callback);
    }

    public void hookAllConstructors(String className, ClassLoader classLoader, MethodHook callback) {
        Class<?> hookClass = ReflectUtils.findClassIfExists(className, classLoader);
        if (hookClass != null) {
            XposedBridge.hookAllConstructors(hookClass, callback);
        }
    }

    public Object getStaticObjectFieldSilently(Class<?> clazz, String fieldName) {
        try {
            return ReflectUtils.getStaticObjectField(clazz, fieldName);
        } catch (Throwable t) {
            return null;
        }
    }

    public Object proxySystemProperties(String method, String prop, int val, ClassLoader classLoader) {
        Class<?> sp = findClassIfExists("android.os.SystemProperties", classLoader);
        if (sp == null) return null;
        try {
            return ReflectUtils.callStaticMethod(sp, method, prop, val);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Class<?>[] resolveParamTypes(ClassLoader classLoader, Object[] types, int from, int to) {
        int len = Math.max(0, to - from);
        Class<?>[] out = new Class<?>[len];
        for (int i = 0; i < len; i++) {
            Object t = types[from + i];
            if (t instanceof Class<?>) {
                out[i] = (Class<?>) t;
            } else if (t instanceof String) {
                out[i] = ReflectUtils.findClassIfExists((String) t, classLoader);
            } else {
                out[i] = null;
            }
            if (out[i] == null) {
                throw new IllegalArgumentException("Bad parameter type at index " + i + ": " + t);
            }
        }
        return out;
    }

    private static Method findMethodInHierarchy(Class<?> clazz, String methodName, Class<?>[] paramTypes) {
        Class<?> c = clazz;
        while (c != null) {
            try {
                Method m = c.getDeclaredMethod(methodName, paramTypes);
                m.setAccessible(true);
                return m;
            } catch (Throwable ignored) {
            }
            c = c.getSuperclass();
        }
        // fallback: try any matching name/arity (best-effort)
        c = clazz;
        while (c != null) {
            for (Method m : c.getDeclaredMethods()) {
                if (!m.getName().equals(methodName)) continue;
                if (m.getParameterTypes().length != paramTypes.length) continue;
                m.setAccessible(true);
                return m;
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private static java.lang.reflect.Constructor<?> findConstructorInHierarchy(Class<?> clazz, Class<?>[] paramTypes) {
        try {
            java.lang.reflect.Constructor<?> c = clazz.getDeclaredConstructor(paramTypes);
            c.setAccessible(true);
            return c;
        } catch (Throwable ignored) {
        }
        // fallback: try any matching arity
        for (java.lang.reflect.Constructor<?> c : clazz.getDeclaredConstructors()) {
            if (c.getParameterTypes().length != paramTypes.length) continue;
            c.setAccessible(true);
            return c;
        }
        return null;
    }

    public Method getDeclaredMethod(String className, String method, Object... type) throws NoSuchMethodException {
        return getDeclaredMethod(findClassIfExists(className), method, type);
    }

    public Method getDeclaredMethod(Class<?> clazz, String method, Object... type) throws NoSuchMethodException {
        String tag = "getDeclaredMethod";
        ArrayList<Method> haveMethod = new ArrayList<>();
        Method hqMethod = null;
        int methodNum;
        if (clazz == null) {
            logE(tag, "find class is null: " + method);
            throw new NoSuchMethodException("find class is null");
        }
        for (Method getMethod : clazz.getDeclaredMethods()) {
            if (getMethod.getName().equals(method)) {
                haveMethod.add(getMethod);
            }
        }
        if (haveMethod.isEmpty()) {
            logE(tag, "find method is null: " + method);
            throw new NoSuchMethodException("find method is null");
        }
        methodNum = haveMethod.size();
        if (type != null) {
            Class<?>[] classes = new Class<?>[type.length];
            Class<?> newclass = null;
            Object getType;
            for (int i = 0; i < type.length; i++) {
                getType = type[i];
                if (getType instanceof Class<?>) {
                    newclass = (Class<?>) getType;
                }
                if (getType instanceof String) {
                    newclass = findClassIfExists((String) getType);
                    if (newclass == null) {
                        logE(tag, "get class error: " + i);
                        throw new NoSuchMethodException("get class error");
                    }
                }
                classes[i] = newclass;
            }
            boolean noError = true;
            for (int i = 0; i < methodNum; i++) {
                hqMethod = haveMethod.get(i);
                boolean allHave = true;
                if (hqMethod.getParameterTypes().length != classes.length) {
                    if (methodNum - 1 == i) {
                        logE(tag, "class length bad: " + Arrays.toString(hqMethod.getParameterTypes()));
                        throw new NoSuchMethodException("class length bad");
                    } else {
                        noError = false;
                        continue;
                    }
                }
                for (int t = 0; t < hqMethod.getParameterTypes().length; t++) {
                    Class<?> getClass = hqMethod.getParameterTypes()[t];
                    if (!getClass.getSimpleName().equals(classes[t].getSimpleName())) {
                        allHave = false;
                        break;
                    }
                }
                if (!allHave) {
                    if (methodNum - 1 == i) {
                        logE(tag, "type bad: " + Arrays.toString(hqMethod.getParameterTypes())
                                + " input: " + Arrays.toString(classes));
                        throw new NoSuchMethodException("type bad");
                    } else {
                        noError = false;
                        continue;
                    }
                }
                if (noError) {
                    break;
                }
            }
            return hqMethod;
        } else {
            if (methodNum > 1) {
                logE(tag, "no type method must only have one: " + haveMethod);
                throw new NoSuchMethodException("no type method must only have one");
            }
        }
        return haveMethod.get(0);
    }

    public void setDeclaredField(XC_MethodHook.MethodHookParam param, String iNeedString, Object iNeedTo) {
        if (param != null) {
            try {
                Field setString = param.thisObject.getClass().getDeclaredField(iNeedString);
                setString.setAccessible(true);
                try {
                    setString.set(param.thisObject, iNeedTo);
                    Object result = setString.get(param.thisObject);
                    checkLast("getDeclaredField", iNeedString, iNeedTo, result);
                } catch (IllegalAccessException e) {
                    // logE("IllegalAccessException to: " + iNeedString + " need to: " + iNeedTo + " code: " + e);
                }
            } catch (NoSuchFieldException e) {
                // logE("No such the: " + iNeedString + " code: " + e);
            }
        } else {
            // logE("Param is null Field: " + iNeedString + " to: " + iNeedTo);
        }
    }

    public void checkLast(String setObject, Object fieldName, Object value, Object last) {
        if (value.equals(last)) {
            logI(setObject + " Success! set " + fieldName + " to " + value);
        } else {
            // logE(setObject + " Failed! set " + fieldName + " to " + value + " hope: " + value + " but: " + last);
        }
    }

    // resHook 相关方法
    public static int getFakeResId(String resourceName) {
        return ResourcesTool.getFakeResId(resourceName);
    }

    /**
     * 设置资源 ID 类型的替换
     */
    public static void setResReplacement(String pkg, String type, String name, int replacementResId) {
        ResourcesTool resTool = ResourcesTool.getInstance();
        if (resTool != null) {
            resTool.setResReplacement(pkg, type, name, replacementResId);
        }
    }

    /**
     * 设置密度类型的资源
     */
    public static void setDensityReplacement(String pkg, String type, String name, float replacementResValue) {
        ResourcesTool resTool = ResourcesTool.getInstance();
        if (resTool != null) {
            resTool.setDensityReplacement(pkg, type, name, replacementResValue);
        }
    }

    /**
     * 设置 Object 类型的资源
     */
    public static void setObjectReplacement(String pkg, String type, String name, Object replacementResValue) {
        ResourcesTool resTool = ResourcesTool.getInstance();
        if (resTool != null) {
            resTool.setObjectReplacement(pkg, type, name, replacementResValue);
        }
    }
}
