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

import static com.sevtinge.hyperceiler.libhook.utils.log.XposedLog.e;
import static com.sevtinge.hyperceiler.libhook.utils.log.XposedLog.w;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.loader.ResourcesLoader;
import android.content.res.loader.ResourcesProvider;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Pair;
import android.util.TypedValue;

import com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedInterface.BeforeHookCallback;
import io.github.libxposed.api.XposedInterface.Hooker;
import io.github.libxposed.api.XposedInterface.MethodUnhooker;

/**
 * 重写资源钩子，希望本钩子能有更好的生命力。
 *
 * @author 焕晨HChen
 * @co-author Ling Qiqi
 */
public class ResourcesTool {
    private static final String TAG = "ResourcesTool";
    private static volatile ResourcesTool sInstance = null;

    private final String mModulePath;
    private volatile boolean hooksApplied = false;
    private volatile boolean isInit = false;
    private Handler mHandler = null;
    private volatile ResourcesLoader resourcesLoader;

    private final CopyOnWriteArrayList<Resources> resourcesArrayList = new CopyOnWriteArrayList<>();
    private final java.util.Set<Integer> resMap = ConcurrentHashMap.newKeySet();
    private final CopyOnWriteArrayList<MethodUnhooker<?>> unhooks = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<ResKey, Pair<ReplacementType, Object>> replacements = new ConcurrentHashMap<>();

    private record ResKey(String pkg, String type, String name) {
    }

    protected enum ReplacementType {
        ID,
        DENSITY,
        OBJECT
    }

    private ResourcesTool(String modulePath) {
        this.mModulePath = modulePath;
        resourcesArrayList.clear();
        resMap.clear();
        unhooks.clear();
        applyHooks();
        isInit = true;
    }

    public static ResourcesTool getInstance(String modulePath) {
        if (sInstance == null) {
            synchronized (ResourcesTool.class) {
                if (sInstance == null) {
                    sInstance = new ResourcesTool(modulePath);
                }
            }
        }
        return sInstance;
    }

    public static synchronized ResourcesTool getInstance() {
        if (sInstance == null) {
            e(TAG, "ResourcesTool not initialized. Call getInstance(String modulePath) first.");
        }
        return sInstance;
    }

    public boolean isInit() {
        return isInit;
    }

    public static int getFakeResId(String resourceName) {
        return 0x7e00f000 | (resourceName.hashCode() & 0x00ffffff);
    }

    public Resources loadModuleRes(Resources resources, boolean doOnMainLooper) {
        if (resources == null) {
            w(TAG, "Context can't be null!");
            return null;
        }
        boolean loaded = loadResAboveApi30(resources, doOnMainLooper);
        if (loaded) {
            if (!resourcesArrayList.contains(resources)) {
                resourcesArrayList.add(resources);
            }
        } else {
            w(TAG, "loadModuleRes: failed to load resources: " + resources);
        }
        return resources;
    }

    public Resources loadModuleRes(Resources resources) {
        return loadModuleRes(resources, false);
    }

    public Resources loadModuleRes(Context context, boolean doOnMainLooper) {
        return loadModuleRes(context.getResources(), doOnMainLooper);
    }

    public Resources loadModuleRes(Context context) {
        return loadModuleRes(context, false);
    }

    private boolean loadResAboveApi30(Resources resources, boolean doOnMainLooper) {
        if (resourcesLoader == null) {
            synchronized (this) {
                if (resourcesLoader == null) {
                    try (ParcelFileDescriptor pfd = ParcelFileDescriptor.open(new File(mModulePath),
                            ParcelFileDescriptor.MODE_READ_ONLY)) {
                        ResourcesProvider provider = ResourcesProvider.loadFromApk(pfd);
                        ResourcesLoader loader = new ResourcesLoader();
                        loader.addProvider(provider);
                        resourcesLoader = loader;
                    } catch (IOException ex) {
                        e(TAG, "Failed to add resource! debug: above api 30.", ex);
                        return false;
                    }
                }
            }
        }
        if (doOnMainLooper) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                return addLoaders(resources);
            } else {
                synchronized (this) {
                    if (mHandler == null) {
                        mHandler = new Handler(Looper.getMainLooper());
                    }
                }
                mHandler.post(() -> addLoaders(resources));
                return true;
            }
        } else {
            return addLoaders(resources);
        }
    }

    private boolean addLoaders(Resources resources) {
        try {
            resources.addLoaders(resourcesLoader);
        } catch (IllegalArgumentException ex) {
            String expected1 = "Cannot modify resource loaders of ResourcesImpl not registered with ResourcesManager";
            if (expected1.equals(ex.getMessage())) {
                return loadResBelowApi30(resources);
            } else {
                e(TAG, "Failed to add loaders!", ex);
                return false;
            }
        }
        return true;
    }

    @SuppressLint("DiscouragedPrivateApi")
    private boolean loadResBelowApi30(Resources resources) {
        try {
            AssetManager assets = resources.getAssets();
            Method addAssetPath = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
            addAssetPath.setAccessible(true);
            Integer cookie = (Integer) addAssetPath.invoke(assets, mModulePath);
            if (cookie == null || cookie == 0) {
                w(TAG, "Method 'addAssetPath' result 0, maybe load res failed!");
                return false;
            }
        } catch (Throwable ex) {
            e(TAG, "Failed to add resource! debug: below api 30.", ex);
            return false;
        }
        return true;
    }

    private synchronized void applyHooks() {
        if (hooksApplied) return;
        hooksApplied = true;

        XposedInterface xposed = HookTool.getXposed();
        if (xposed == null) {
            e(TAG, "XposedInterface not initialized!");
            return;
        }

        Method[] resMethods = Resources.class.getDeclaredMethods();
        for (Method method : resMethods) {
            String name = method.getName();
            Class<?>[] paramTypes = method.getParameterTypes();
            switch (name) {
                case "getInteger", "getLayout", "getBoolean", "getDimension",
                     "getDimensionPixelOffset", "getDimensionPixelSize", "getText", "getFloat",
                     "getIntArray", "getStringArray", "getTextArray", "getAnimation" -> {
                    if (paramTypes.length == 1 && paramTypes[0].equals(int.class)) {
                        try {
                            MethodUnhooker<?> unhook = xposed.hook(method, ResHooker.class);
                            unhooks.add(unhook);
                        } catch (Throwable t) {
                            e(TAG, "Failed to hook " + name, t);
                        }
                    }
                }
                case "getColor" -> {
                    if (paramTypes.length == 2 && paramTypes[0].equals(int.class)) {
                        try {
                            MethodUnhooker<?> unhook = xposed.hook(method, ResHooker.class);
                            unhooks.add(unhook);
                        } catch (Throwable t) {
                            e(TAG, "Failed to hook " + name, t);
                        }
                    }
                }
                case "getFraction", "getDrawableForDensity" -> {
                    if (paramTypes.length == 3 && paramTypes[0].equals(int.class)) {
                        try {
                            MethodUnhooker<?> unhook = xposed.hook(method, ResHooker.class);
                            unhooks.add(unhook);
                        } catch (Throwable t) {
                            e(TAG, "Failed to hook " + name, t);
                        }
                    }
                }
            }
        }

        Method[] typedMethods = TypedArray.class.getDeclaredMethods();
        for (Method method : typedMethods) {
            if (method.getName().equals("getColor")) {
                Class<?>[] paramTypes = method.getParameterTypes();
                if (paramTypes.length == 2 && paramTypes[0].equals(int.class) && paramTypes[1].equals(int.class)) {
                    try {
                        MethodUnhooker<?> unhook = xposed.hook(method, TypedArrayHooker.class);
                        unhooks.add(unhook);
                    } catch (Throwable t) {
                        e(TAG, "Failed to hook TypedArray.getColor", t);
                    }
                }
            }
        }
    }

    public void unHookRes() {
        if (unhooks.isEmpty()) {
            isInit = false;
            return;
        }
        for (MethodUnhooker<?> unhook : unhooks) {
            unhook.unhook();
        }
        unhooks.clear();
        isInit = false;
        hooksApplied = false;
    }

    public static class TypedArrayHooker implements Hooker {
        public static void before(BeforeHookCallback callback) {
            ResourcesTool instance = getInstance();
            if (instance == null) return;

            Object[] args = callback.getArgs();
            if (args == null || args.length < 1) return;

            int index = (int) args[0];
            Object thisObject = callback.getThisObject();

            int[] mData = (int[]) ReflectUtils.getObjectField(thisObject, "mData");
            if (mData == null || index + 3 >= mData.length) return;

            int type = mData[index];
            int id = mData[index + 3];

            if (id != 0 && (type != TypedValue.TYPE_NULL)) {
                Resources mResources = (Resources) ReflectUtils.getObjectField(thisObject, "mResources");
                Object value = instance.getTypedArrayReplacement(mResources, id);
                if (value != null) {
                    callback.returnAndSkip(value);
                }
            }
        }
    }

    public static class ResHooker implements Hooker {
        public static void before(BeforeHookCallback callback) {
            ResourcesTool instance = getInstance();
            if (instance == null) return;

            if (instance.resourcesArrayList.isEmpty()) {
                Context context = ContextUtils.getContextNoError(ContextUtils.FLAG_CURRENT_APP);
                if (context != null) {
                    Resources resources = instance.loadModuleRes(context);
                    if (resources != null) {
                        instance.resourcesArrayList.add(resources);
                    }
                }
            }

            Object[] args = callback.getArgs();
            if (args == null || args.length < 1) return;

            int reqId = (int) args[0];
            if (instance.resMap.contains(reqId)) {
                return;
            }

            Resources thisRes = (Resources) callback.getThisObject();
            String methodName = callback.getMember().getName();

            for (Resources resources : instance.resourcesArrayList) {
                if (resources == null) continue;
                Object value;
                try {
                    value = instance.getResourceReplacement(resources, thisRes, methodName, args);
                } catch (Resources.NotFoundException ex) {
                    continue;
                }
                if (value != null) {
                    Object finalResult = null;

                    switch (methodName) {
                        case "getInteger", "getColor", "getDimensionPixelOffset", "getDimensionPixelSize" -> {
                            if (value instanceof Number) {
                                finalResult = Math.round(((Number) value).floatValue());
                            }
                        }
                        case "getDimension", "getFloat" -> {
                            if (value instanceof Number) {
                                finalResult = ((Number) value).floatValue();
                            }
                        }
                        case "getText" -> {
                            if (value instanceof CharSequence) {
                                finalResult = value;
                            }
                        }
                        case "getBoolean" -> {
                            if (value instanceof Boolean) {
                                finalResult = value;
                            }
                        }
                        default -> finalResult = value;
                    }

                    if (finalResult != null) {
                        callback.returnAndSkip(finalResult);
                    } else {
                        w(TAG, "Mismatched replacement type for method " + methodName + ". Got " + value.getClass().getName());
                    }
                    break;
                }
            }
        }
    }

    public void setResReplacement(String pkg, String type, String name, int replacementResId) {
        try {
            applyHooks();
            replacements.put(new ResKey(pkg, type, name), new Pair<>(ReplacementType.ID, replacementResId));
        } catch (Throwable t) {
            e(TAG, "setResReplacement failed", t);
        }
    }

    public void setDensityReplacement(String pkg, String type, String name, float replacementResValue) {
        try {
            applyHooks();
            replacements.put(new ResKey(pkg, type, name), new Pair<>(ReplacementType.DENSITY, replacementResValue));
        } catch (Throwable t) {
            e(TAG, "setDensityReplacement failed", t);
        }
    }

    public void setObjectReplacement(String pkg, String type, String name, Object replacementResValue) {
        try {
            applyHooks();
            replacements.put(new ResKey(pkg, type, name), new Pair<>(ReplacementType.OBJECT, replacementResValue));
        } catch (Throwable t) {
            e(TAG, "setObjectReplacement failed", t);
        }
    }

    private Object getResourceReplacement(Resources resources, Resources res, String method, Object[] args) throws Resources.NotFoundException {
        if (resources == null) return null;
        String pkgName;
        String resType;
        String resName;
        try {
            int resId = (int) args[0];
            if (resId == 0) return null;
            pkgName = res.getResourcePackageName(resId);
            resType = res.getResourceTypeName(resId);
            resName = res.getResourceEntryName(resId);
        } catch (Throwable ignore) {
            return null;
        }

        if (pkgName == null || resType == null || resName == null) return null;

        ResKey resFullNameKey = new ResKey(pkgName, resType, resName);
        ResKey resAnyPkgNameKey = new ResKey("*", resType, resName);

        Pair<ReplacementType, Object> replacement = replacements.get(resFullNameKey);
        if (replacement == null) {
            replacement = replacements.get(resAnyPkgNameKey);
        }

        if (replacement != null) {
            switch (replacement.first) {
                case OBJECT:
                    if ("getText".equals(method) && !(replacement.second instanceof CharSequence)) {
                        w(TAG, "Mismatched type: OBJECT replacement is not a CharSequence for getText method.");
                        return null;
                    }
                    return replacement.second;
                case DENSITY: {
                    if ("getText".equals(method)) {
                        w(TAG, "Mismatched type: DENSITY replacement cannot be used for getText method.");
                        return null;
                    }
                    Object repl = replacement.second;
                    if (repl instanceof Number) {
                        return ((Number) repl).floatValue() * res.getDisplayMetrics().density;
                    } else if (repl instanceof String) {
                        try {
                            return Float.parseFloat((String) repl) * res.getDisplayMetrics().density;
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    w(TAG, "Invalid DENSITY replacement type: " + repl);
                    return null;
                }
                case ID: {
                    if (!(replacement.second instanceof Number)) return null;
                    Integer modResId = ((Number) replacement.second).intValue();
                    if (modResId == 0) return null;
                    try {
                        resources.getResourceName(modResId);
                    } catch (Resources.NotFoundException n) {
                        throw n;
                    }
                    if (method == null) return null;
                    resMap.add(modResId);
                    Object value;
                    try {
                        if ("getDrawable".equals(method) && args.length >= 2) {
                            value = ReflectUtils.callMethod(resources, method, modResId, args[1]);
                        } else if (("getDrawableForDensity".equals(method) || "getFraction".equals(method)) && args.length >= 3) {
                            value = ReflectUtils.callMethod(resources, method, modResId, args[1], args[2]);
                        } else {
                            value = ReflectUtils.callMethod(resources, method, modResId);
                        }
                    } finally {
                        resMap.remove(modResId);
                    }
                    return value;
                }
            }
        }
        return null;
    }

    private Object getTypedArrayReplacement(Resources resources, int id) {
        if (id == 0) return null;
        String pkgName;
        String resType;
        String resName;
        try {
            pkgName = resources.getResourcePackageName(id);
            resType = resources.getResourceTypeName(id);
            resName = resources.getResourceEntryName(id);
        } catch (Throwable ignore) {
            return null;
        }
        if (pkgName == null || resType == null || resName == null) return null;

        try {
            ResKey resFullNameKey = new ResKey(pkgName, resType, resName);
            ResKey resAnyPkgNameKey = new ResKey("*", resType, resName);

            Pair<ReplacementType, Object> replacement = replacements.get(resFullNameKey);
            if (replacement == null) {
                replacement = replacements.get(resAnyPkgNameKey);
            }

            if (replacement != null && replacement.first == ReplacementType.OBJECT) {
                return replacement.second;
            }
        } catch (Throwable ex) {
            e(TAG, "getTypedArrayReplacement failed", ex);
        }
        return null;
    }
}
