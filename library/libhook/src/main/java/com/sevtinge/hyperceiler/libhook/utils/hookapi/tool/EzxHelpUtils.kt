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
@file:Suppress("UNCHECKED_CAST")

package com.sevtinge.hyperceiler.libhook.utils.hookapi.tool

import com.sevtinge.hyperceiler.libhook.callback.IMethodHook
import com.sevtinge.hyperceiler.libhook.callback.IReplaceHook
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog
import io.github.kyuubiran.ezxhelper.core.ClassLoaderProvider.classLoader
import io.github.kyuubiran.ezxhelper.core.finder.ConstructorFinder
import io.github.kyuubiran.ezxhelper.core.finder.FieldFinder
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder
import io.github.kyuubiran.ezxhelper.core.helper.ObjectHelper.`-Static`.objectHelper
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil
import io.github.kyuubiran.ezxhelper.core.util.ObjectUtil
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import io.github.libxposed.api.XposedInterface.MethodUnhooker
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.WeakHashMap

object EzxHelpUtils {

    private val additionalFields = WeakHashMap<Any, MutableMap<String, Any?>>()

    @JvmStatic
    fun findClass(name: String, classLoader: ClassLoader?): Class<*> {
        return ClassUtil.loadClass(name, classLoader)
    }

    @JvmStatic
    fun findClassIfExists(name: String, classLoader: ClassLoader?): Class<*>? {
        return ClassUtil.loadClassOrNull(name, classLoader)
    }

    @JvmStatic
    fun findField(clazz: Class<*>, fieldName: String): Field {
        return FieldFinder.fromClass(clazz)
            .filterByName(fieldName)
            .first()
    }

    @JvmStatic
    fun findFieldIfExists(clazz: Class<*>, fieldName: String): Field? {
        return FieldFinder.fromClass(clazz)
            .filterByName(fieldName)
            .firstOrNull()
    }

    @JvmStatic
    fun findFirstFieldByExactType(clazz: Class<*>, type: Class<*>): Field {
        return FieldFinder.fromClass(clazz)
            .filterByType(type)
            .first()
    }

    @JvmStatic
    fun getObjectField(instance: Any, fieldName: String): Any? {
        return instance.objectHelper().getObjectOrNull(fieldName)
    }

    @JvmStatic
    fun setObjectField(instance: Any, fieldName: String, value: Any?) {
        instance.objectHelper().setObject(fieldName, value)
    }

    @JvmStatic
    fun getBooleanField(instance: Any, fieldName: String): Boolean {
        return getObjectField(instance, fieldName) as? Boolean ?: false
    }

    @JvmStatic
    fun setBooleanField(instance: Any, fieldName: String, value: Boolean) {
        instance.objectHelper().setObject(fieldName, value)
    }

    @JvmStatic
    fun getIntField(instance: Any, fieldName: String): Int {
        return getObjectField(instance, fieldName) as? Int ?: 0
    }

    @JvmStatic
    fun setIntField(instance: Any, fieldName: String, value: Int) {
        instance.objectHelper().setObject(fieldName, value)
    }

    @JvmStatic
    fun getLongField(instance: Any, fieldName: String): Long {
        return getObjectField(instance, fieldName) as? Long ?: 0L
    }

    @JvmStatic
    fun setLongField(instance: Any, fieldName: String, value: Long) {
        instance.objectHelper().setObject(fieldName, value)
    }

    @JvmStatic
    fun getFloatField(instance: Any, fieldName: String): Float {
        return getObjectField(instance, fieldName) as? Float ?: 0f
    }

    @JvmStatic
    fun setFloatField(instance: Any, fieldName: String, value: Float) {
        instance.objectHelper().setObject(fieldName, value)
    }

    @JvmStatic
    fun getStaticObjectField(clazz: Class<*>, fieldName: String): Any? {
        return ClassUtil.getStaticObjectOrNull(clazz, fieldName)
    }

    @JvmStatic
    fun setStaticObjectField(clazz: Class<*>, fieldName: String, value: Any?) {
        ClassUtil.setStaticObject(clazz, fieldName, value)
    }

    @JvmStatic
    fun getStaticBooleanField(clazz: Class<*>, fieldName: String): Boolean {
        return ClassUtil.getStaticObjectOrNull(clazz, fieldName) as? Boolean ?: false
    }

    @JvmStatic
    fun setStaticBooleanField(clazz: Class<*>, fieldName: String, value: Boolean) {
        ClassUtil.setStaticObject(clazz, fieldName, value)
    }

    @JvmStatic
    fun getStaticIntField(clazz: Class<*>, fieldName: String): Int {
        return ClassUtil.getStaticObjectOrNull(clazz, fieldName) as? Int ?: 0
    }

    @JvmStatic
    fun setStaticIntField(clazz: Class<*>, fieldName: String, value: Int) {
        ClassUtil.setStaticObject(clazz, fieldName, value)
    }

    @JvmStatic
    fun getStaticLongField(clazz: Class<*>, fieldName: String): Long {
        return ClassUtil.getStaticObjectOrNull(clazz, fieldName) as? Long ?: 0L
    }

    @JvmStatic
    fun setStaticLongField(clazz: Class<*>, fieldName: String, value: Long) {
        ClassUtil.setStaticObject(clazz, fieldName, value)
    }

    @JvmStatic
    fun callMethod(instance: Any, methodName: String, vararg args: Any?): Any? {
        return ObjectUtil.invokeMethodBestMatch(instance, methodName, null, *args)
    }

    @JvmStatic
    fun callStaticMethod(clazz: Class<*>, methodName: String, vararg args: Any?): Any? {
        return ClassUtil.invokeStaticMethodBestMatch(clazz, methodName, null, *args)
    }

    @JvmStatic
    fun findMethodBestMatch(clazz: Class<*>, methodName: String, vararg args: Any?): Method {
        val parameterTypes = args.map {
            when (it) {
                is Class<*> -> it
                else -> it?.javaClass
            }
        }.toTypedArray()

        return if (parameterTypes.all { it != null }) {
            MethodFinder.fromClass(clazz)
                .filterByName(methodName)
                .filterByParamTypes(*parameterTypes.filterNotNull().toTypedArray())
                .firstOrNull()
                ?: MethodFinder.fromClass(clazz)
                    .filterByName(methodName)
                    .filterByParamCount(parameterTypes.size)
                    .first()
        } else {
            MethodFinder.fromClass(clazz)
                .filterByName(methodName)
                .filterByParamCount(parameterTypes.size)
                .first()
        }
    }

    @JvmStatic
    fun findMethodExact(clazz: Class<*>, methodName: String, vararg parameterTypes: Class<*>): Method {
        return MethodFinder.fromClass(clazz)
            .filterByName(methodName)
            .filterByParamTypes(*parameterTypes)
            .first()
    }

    @JvmStatic
    fun findMethodExactIfExists(clazz: Class<*>, methodName: String, vararg parameterTypes: Class<*>): Method? {
        return MethodFinder.fromClass(clazz)
            .filterByName(methodName)
            .filterByParamTypes(*parameterTypes)
            .firstOrNull()
    }

    @JvmStatic
    fun newInstance(clazz: Class<*>, vararg args: Any?): Any {
        val ctor = findConstructorBestMatch(clazz, *args)
        return ctor.newInstance(*args)
    }

    @JvmStatic
    fun findConstructorExact(clazz: Class<*>, vararg parameterTypes: Class<*>): Constructor<*> {
        return ConstructorFinder.fromClass(clazz)
            .filterByParamTypes(*parameterTypes)
            .first()
    }

    @JvmStatic
    fun findConstructorBestMatch(clazz: Class<*>, vararg args: Any?): Constructor<*> {
        val parameterTypes = args.mapNotNull {
            when (it) {
                is Class<*> -> it
                else -> it?.javaClass
            }
        }.toTypedArray()

        return if (parameterTypes.size == args.size) {
            ConstructorFinder.fromClass(clazz)
                .filterByParamTypes(*parameterTypes)
                .firstOrNull()
                ?: ConstructorFinder.fromClass(clazz)
                    .filterByParamCount(parameterTypes.size)
                    .first()
        } else {
            ConstructorFinder.fromClass(clazz)
                .filterByParamCount(args.size)
                .first()
        }
    }

    @JvmStatic
    fun getSurroundingThis(obj: Any): Any? {
        return getObjectField(obj, $$"this$0")
    }

    @Synchronized
    fun getAdditionalInstanceField(instance: Any, key: String): Any? =
        additionalFields[instance]?.get(key)

    @Synchronized
    fun setAdditionalInstanceField(instance: Any, key: String, value: Any?): Any? {
        val map = additionalFields.getOrPut(instance) { mutableMapOf() }
        return map.put(key, value)
    }

    @Synchronized
    fun removeAdditionalInstanceField(instance: Any, key: String): Any? =
        additionalFields[instance]?.remove(key)


    // ==================== Hook 方法 ====================

    private const val TAG = "EzxHelpUtils"

    /**
     * 格式化方法签名用于日志输出
     */
    private fun formatMethodSignature(method: Method): String {
        val params = method.parameterTypes.joinToString(", ") { it.simpleName }
        return "${method.declaringClass.simpleName}#${method.name}($params)"
    }

    /**
     * 格式化构造器签名用于日志输出
     */
    private fun formatConstructorSignature(constructor: Constructor<*>): String {
        val params = constructor.parameterTypes.joinToString(", ") { it.simpleName }
        return "${constructor.declaringClass.simpleName}<init>($params)"
    }

    /**
     * Hook 方法
     *
     * @param method 要 Hook 的方法
     * @param callback Hook 回调
     * @return MethodUnhooker 对象，可用于取消 Hook
     */
    @JvmStatic
    fun hookMethod(method: Method, callback: IMethodHook): MethodUnhooker<*> {
        val signature = formatMethodSignature(method)
        return try {
            val unhook = method.createHook {
                before { param ->
                    try {
                        callback.before(param)
                    } catch (t: Throwable) {
                        XposedLog.e(TAG, "[$signature] before callback error", t)
                    }
                }
                after { param ->
                    try {
                        callback.after(param)
                    } catch (t: Throwable) {
                        XposedLog.e(TAG, "[$signature] after callback error", t)
                    }
                }
            }
            // XposedLog.i(TAG, "[$signature] hook success")
            unhook
        } catch (t: Throwable) {
            XposedLog.e(TAG, "[$signature] hook failed", t)
            throw t
        }
    }

    /**
     * Hook 方法（替换模式）
     *
     * @param method 要 Hook 的方法
     * @param callback 替换回调
     * @return MethodUnhooker 对象，可用于取消 Hook
     */
    @JvmStatic
    fun hookMethod(method: Method, callback: IReplaceHook): MethodUnhooker<*> {
        val signature = formatMethodSignature(method)
        return try {
            val unhook = method.createHook {
                replace { param ->
                    try {
                        callback.replace(param)
                    } catch (t: Throwable) {
                        XposedLog.e(TAG, "[$signature] replace callback error", t)
                        throw t
                    }
                }
            }
            // XposedLog.i(TAG, "[$signature] hook (replace) success")
            unhook
        } catch (t: Throwable) {
            XposedLog.e(TAG, "[$signature] hook (replace) failed", t)
            throw t
        }
    }

    /**
     * Hook 构造器
     *
     * @param constructor 要 Hook 的构造器
     * @param callback Hook 回调
     * @return MethodUnhooker 对象，可用于取消 Hook
     */
    @JvmStatic
    fun hookConstructor(constructor: Constructor<*>, callback: IMethodHook): MethodUnhooker<*> {
        val signature = formatConstructorSignature(constructor)
        return try {
            val unhook = constructor.createHook {
                before { param ->
                    try {
                        callback.before(param)
                    } catch (t: Throwable) {
                        XposedLog.e(TAG, "[$signature] before callback error", t)
                    }
                }
                after { param ->
                    try {
                        callback.after(param)
                    } catch (t: Throwable) {
                        XposedLog.e(TAG, "[$signature] after callback error", t)
                    }
                }
            }
            // XposedLog.i(TAG, "[$signature] hook success")
            unhook
        } catch (t: Throwable) {
            XposedLog.e(TAG, "[$signature] hook failed", t)
            throw t
        }
    }

    /**
     * 查找并 Hook 方法
     *
     * @param clazz 目标类
     * @param methodName 方法名
     * @param parameterTypes 参数类型
     * @param callback Hook 回调（放在最后以支持 Java lambda）
     * @return Unhook 对象
     */
    @JvmStatic
    fun findAndHookMethod(
        clazz: Class<*>,
        methodName: String,
        vararg parameterTypes: Class<*>,
        callback: IMethodHook
    ): MethodUnhooker<*> {
        val method = findMethodExact(clazz, methodName, *parameterTypes)
        return hookMethod(method, callback)
    }


    @JvmStatic
    fun findAndHookMethod(
        clazzName: String,
        methodName: String,
        vararg args: Any,
    ): MethodUnhooker<*> {
        val clazz = findClass(clazzName, classLoader)
        return findAndHookMethod(clazz, methodName, *args)
    }

    @JvmStatic
    fun findAndHookMethod(
        clazzName: String,
        classLoader: ClassLoader,
        methodName: String,
        vararg args: Any
    ): MethodUnhooker<*> {
        val clazz = findClass(clazzName, classLoader)
        return findAndHookMethod(clazz, methodName, *args)
    }

    /**
     * 查找并 Hook 方法（Java 友好版本，callback 作为最后一个参数）
     *
     * @param clazz 目标类
     * @param methodName 方法名
     * @param args 参数类型数组，最后一个元素必须是 IMethodHook
     * @return MethodUnhooker 对象
     */
    @JvmStatic
    fun findAndHookMethod(clazz: Class<*>, methodName: String, vararg args: Any): MethodUnhooker<*> {
        require(args.isNotEmpty()) { "args must contain at least the callback" }
        val callback = args.last()
        require(callback is IMethodHook) { "Last argument must be IMethodHook" }

        val paramTypes = args.dropLast(1).map {
            require(it is Class<*>) { "Parameter types must be Class<?>" }
            it
        }.toTypedArray()

        val method = findMethodExact(clazz, methodName, *paramTypes)
        return hookMethod(method, callback)
    }

    /**
     * 查找并 Hook 构造器
     *
     * @param clazz 目标类
     * @param parameterTypes 参数类型
     * @param callback Hook 回调
     * @return MethodUnhooker 对象
     */
    @JvmStatic
    fun findAndHookConstructor(
        clazz: Class<*>,
        vararg parameterTypes: Class<*>,
        callback: IMethodHook
    ): MethodUnhooker<*> {
        val constructor = findConstructorExact(clazz, *parameterTypes)
        return hookConstructor(constructor, callback)
    }

    /**
     * 查找并 Hook 构造器（Java 友好版本，callback 作为最后一个参数）
     *
     * @param clazz 目标类
     * @param args 参数类型数组，最后一个元素必须是 IMethodHook
     * @return MethodUnhooker 对象
     */
    @JvmStatic
    fun findAndHookConstructor(clazz: Class<*>, vararg args: Any): MethodUnhooker<*> {
        require(args.isNotEmpty()) { "args must contain at least the callback" }
        val callback = args.last()
        require(callback is IMethodHook) { "Last argument must be IMethodHook" }

        val paramTypes = args.dropLast(1).map {
            require(it is Class<*>) { "Parameter types must be Class<?>" }
            it
        }.toTypedArray()

        val constructor = findConstructorExact(clazz, *paramTypes)
        return hookConstructor(constructor, callback)
    }

    @JvmStatic
    fun hookAllMethods(
        clazzName: String,
        methodName: String,
        callback: IMethodHook
    ): List<MethodUnhooker<*>> {
        val clazz = findClass(clazzName, classLoader)
        return hookAllMethods(clazz, methodName, callback)
    }

    @JvmStatic
    fun hookAllMethods(
        clazzName: String,
        classLoader: ClassLoader,
        methodName: String,
        callback: IMethodHook
    ): List<MethodUnhooker<*>> {
        val clazz = findClass(clazzName, classLoader)
        return hookAllMethods(clazz, methodName, callback)
    }

    /**
     * Hook 类中所有指定名称的方法
     *
     * @param clazz 目标类
     * @param methodName 方法名
     * @param callback Hook 回调
     * @return MethodUnhooker 对象列表
     */
    @JvmStatic
    fun hookAllMethods(clazz: Class<*>, methodName: String, callback: IMethodHook): List<MethodUnhooker<*>> {
        val methods = MethodFinder.fromClass(clazz)
            .filterByName(methodName)
            .toList()

        if (methods.isEmpty()) {
            XposedLog.w(TAG, "[${clazz.simpleName}#$methodName] no methods found")
            return emptyList()
        }

        return methods.mapNotNull { method ->
            try {
                hookMethod(method, callback)
            } catch (t: Throwable) {
                null
            }
        }
    }

    /**
     * Hook 类中所有构造器
     *
     * @param clazz 目标类
     * @param callback Hook 回调
     * @return MethodUnhooker 对象列表
     */
    @JvmStatic
    fun hookAllConstructors(clazz: Class<*>, callback: IMethodHook): List<MethodUnhooker<*>> {
        val constructors = ConstructorFinder.fromClass(clazz).toList()

        if (constructors.isEmpty()) {
            XposedLog.w(TAG, "[${clazz.simpleName}<init>] no constructors found")
            return emptyList()
        }

        return constructors.mapNotNull { constructor ->
            try {
                hookConstructor(constructor, callback)
            } catch (t: Throwable) {
                null
            }
        }
    }

    // ==================== 便捷 Hook 工具 ====================

    /**
     * 创建一个返回常量值的 Hook 回调
     *
     * @param result 要返回的常量值
     * @return IMethodHook 实例
     */
    @JvmStatic
    fun returnConstant(result: Any?): IMethodHook {
        return object : IMethodHook {
            override fun before(param: BeforeHookParam) {
                param.result = result
            }
        }
    }

    /**
     * 什么都不做的 Hook（阻止原方法执行，返回 null）
     */
    @JvmField
    val DO_NOTHING: IMethodHook = object : IMethodHook {
        override fun before(param: BeforeHookParam) {
            param.result = null
        }
    }
}

// ==================== Any 扩展函数 ====================

fun Any.getObjectField(fieldName: String): Any? = EzxHelpUtils.getObjectField(this, fieldName)
fun <T> Any.getObjectFieldAs(fieldName: String): T = getObjectField(fieldName) as T
fun Any.setObjectField(fieldName: String, value: Any?) = EzxHelpUtils.setObjectField(this, fieldName, value)

fun Any.getBooleanField(fieldName: String): Boolean = EzxHelpUtils.getBooleanField(this, fieldName)
fun Any.setBooleanField(fieldName: String, value: Boolean) = EzxHelpUtils.setBooleanField(this, fieldName, value)
fun Any.getIntField(fieldName: String): Int = EzxHelpUtils.getIntField(this, fieldName)
fun Any.setIntField(fieldName: String, value: Int) = EzxHelpUtils.setIntField(this, fieldName, value)
fun Any.getLongField(fieldName: String): Long = EzxHelpUtils.getLongField(this, fieldName)
fun Any.setLongField(fieldName: String, value: Long) = EzxHelpUtils.setLongField(this, fieldName, value)
fun Any.getFloatField(fieldName: String): Float = EzxHelpUtils.getFloatField(this, fieldName)
fun Any.setFloatField(fieldName: String, value: Float) = EzxHelpUtils.setFloatField(this, fieldName, value)

fun Any.callMethod(methodName: String, vararg args: Any?): Any? = EzxHelpUtils.callMethod(this, methodName, *args)
fun <T> Any.callMethodAs(methodName: String, vararg args: Any?): T = callMethod(methodName, *args) as T

fun Any.getFirstFieldByExactType(type: Class<*>): Any? =
    javaClass.findFirstFieldByExactType(type).get(this)
fun <T> Any.getFirstFieldByExactTypeAs(type: Class<*>) =
    javaClass.findFirstFieldByExactType(type).get(this) as? T

fun Any.getAdditionalInstanceField(field: String): Any? = EzxHelpUtils.getAdditionalInstanceField(this, field)

fun <T> Any.getAdditionalInstanceFieldAs(field: String) = EzxHelpUtils.getAdditionalInstanceField(this, field) as T
fun Any.setAdditionalInstanceField(
    field: String,
    value: Any?
): Any? = EzxHelpUtils.setAdditionalInstanceField(this, field, value)
fun Any.removeAdditionalInstanceField(
    field: String
): Any? = EzxHelpUtils.removeAdditionalInstanceField(this, field)

// ==================== Class 扩展函数 ====================

fun Class<*>.getStaticObjectField(fieldName: String): Any? = EzxHelpUtils.getStaticObjectField(this, fieldName)
fun <T> Class<*>.getStaticObjectFieldAs(fieldName: String): T = getStaticObjectField(fieldName) as T
fun Class<*>.setStaticObjectField(fieldName: String, value: Any?) = EzxHelpUtils.setStaticObjectField(this, fieldName, value)

fun Class<*>.getStaticBooleanField(fieldName: String): Boolean = EzxHelpUtils.getStaticBooleanField(this, fieldName)
fun Class<*>.setStaticBooleanField(fieldName: String, value: Boolean) = EzxHelpUtils.setStaticBooleanField(this, fieldName, value)
fun Class<*>.getStaticIntField(fieldName: String): Int = EzxHelpUtils.getStaticIntField(this, fieldName)
fun Class<*>.setStaticIntField(fieldName: String, value: Int) = EzxHelpUtils.setStaticIntField(this, fieldName, value)
fun Class<*>.getStaticLongField(fieldName: String): Long = EzxHelpUtils.getStaticLongField(this, fieldName)
fun Class<*>.setStaticLongField(fieldName: String, value: Long) = EzxHelpUtils.setStaticLongField(this, fieldName, value)

fun Class<*>.callStaticMethod(methodName: String, vararg args: Any?): Any? = EzxHelpUtils.callStaticMethod(this, methodName, *args)
fun <T> Class<*>.callStaticMethodAs(methodName: String, vararg args: Any?): T = callStaticMethod(methodName, *args) as T

fun <T> Class<*>.newInstance(vararg args: Any?): T = EzxHelpUtils.newInstance(this, *args) as T

fun Class<*>.findFieldByExactType(type: Class<*>): Field = EzxHelpUtils.findFirstFieldByExactType(this, type)
fun Class<*>.findFirstFieldByExactType(type: Class<*>): Field =
    EzxHelpUtils.findFirstFieldByExactType(this, type)


// ==================== String 扩展函数 ====================

fun String.toClass(classLoader: ClassLoader?): Class<*> = EzxHelpUtils.findClass(this, classLoader)
fun String.toClassOrNull(classLoader: ClassLoader?): Class<*>? = EzxHelpUtils.findClassIfExists(this, classLoader)

// ==================== Hook 扩展函数 ====================

fun Method.hook(callback: IMethodHook): MethodUnhooker<*> = EzxHelpUtils.hookMethod(this, callback)
fun Method.hookReplace(callback: IReplaceHook): MethodUnhooker<*> = EzxHelpUtils.hookMethod(this, callback)
fun Constructor<*>.hook(callback: IMethodHook): MethodUnhooker<*> = EzxHelpUtils.hookConstructor(this, callback)

fun Class<*>.hookAllMethods(methodName: String, callback: IMethodHook): List<MethodUnhooker<*>> =
    EzxHelpUtils.hookAllMethods(this, methodName, callback)

fun Class<*>.hookAllConstructors(callback: IMethodHook): List<MethodUnhooker<*>> =
    EzxHelpUtils.hookAllConstructors(this, callback)
