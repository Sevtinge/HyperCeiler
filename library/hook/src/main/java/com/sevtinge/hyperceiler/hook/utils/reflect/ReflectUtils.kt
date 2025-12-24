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
 */
@file:Suppress("UNCHECKED_CAST")

package com.sevtinge.hyperceiler.hook.utils.reflect

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

object ReflectUtils {
    @JvmStatic
    fun findClass(name: String, classLoader: ClassLoader?): Class<*> {
        return Class.forName(name, false, classLoader)
    }

    @JvmStatic
    fun findClassIfExists(name: String, classLoader: ClassLoader?): Class<*>? {
        return try {
            findClass(name, classLoader)
        } catch (_: Throwable) {
            null
        }
    }

    @JvmStatic
    fun getObjectField(instance: Any, fieldName: String): Any? {
        val field = findField(instance.javaClass, fieldName) ?: throw NoSuchFieldException(fieldName)
        return field.get(instance)
    }

    @JvmStatic
    fun setObjectField(instance: Any, fieldName: String, value: Any?) {
        val field = findField(instance.javaClass, fieldName) ?: throw NoSuchFieldException(fieldName)
        field.set(instance, value)
    }

    @JvmStatic
    fun getStaticObjectField(clazz: Class<*>, fieldName: String): Any? {
        val field = findField(clazz, fieldName) ?: throw NoSuchFieldException(fieldName)
        return field.get(null)
    }

    @JvmStatic
    fun setStaticObjectField(clazz: Class<*>, fieldName: String, value: Any?) {
        val field = findField(clazz, fieldName) ?: throw NoSuchFieldException(fieldName)
        field.set(null, value)
    }

    @JvmStatic
    fun callMethod(instance: Any, methodName: String, vararg args: Any?): Any? {
        val method = findBestMethod(instance.javaClass, methodName, args)
            ?: throw NoSuchMethodException("${instance.javaClass.name}#$methodName")
        return method.invoke(instance, *args)
    }

    @JvmStatic
    fun findMethodBestMatch(clazz: Class<*>, methodName: String, vararg args: Any?): Method {
        return findBestMethod(clazz, methodName, args)
            ?: throw NoSuchMethodException("${clazz.name}#$methodName")
    }

    @JvmStatic
    fun callStaticMethod(clazz: Class<*>, methodName: String, vararg args: Any?): Any? {
        val method = findBestMethod(clazz, methodName, args)
            ?: throw NoSuchMethodException("${clazz.name}#$methodName")
        return method.invoke(null, *args)
    }

    @JvmStatic
    fun newInstance(clazz: Class<*>, vararg args: Any?): Any {
        val ctor = findBestConstructor(clazz, args) ?: throw NoSuchMethodException("${clazz.name}<init>")
        return ctor.newInstance(*args)
    }

    private fun findField(start: Class<*>, fieldName: String): Field? {
        var c: Class<*>? = start
        while (c != null) {
            try {
                val f = c.getDeclaredField(fieldName)
                f.isAccessible = true
                return f
            } catch (_: Throwable) {
            }
            c = c.superclass
        }
        return null
    }

    private fun findBestMethod(start: Class<*>, methodName: String, args: Array<out Any?>): Method? {
        var c: Class<*>? = start
        val argTypes = arrayOfNulls<Class<*>>(args.size)
        for (i in args.indices) {
            argTypes[i] = args[i]?.javaClass
        }
        while (c != null) {
            val candidates = c.declaredMethods.filter { it.name == methodName && it.parameterTypes.size == args.size }
            val best = candidates.firstOrNull { isApplicable(it.parameterTypes, argTypes) }
            if (best != null) {
                best.isAccessible = true
                return best
            }
            c = c.superclass
        }
        return null
    }

    private fun findBestConstructor(clazz: Class<*>, args: Array<out Any?>): Constructor<*>? {
        val argTypes = arrayOfNulls<Class<*>>(args.size)
        for (i in args.indices) {
            argTypes[i] = args[i]?.javaClass
        }
        val candidates = clazz.declaredConstructors.filter { it.parameterTypes.size == args.size }
        val best = candidates.firstOrNull { isApplicable(it.parameterTypes, argTypes) }
        if (best != null) {
            best.isAccessible = true
            return best
        }
        return null
    }

    private fun isApplicable(params: Array<Class<*>>, args: Array<Class<*>?>): Boolean {
        for (i in params.indices) {
            val p = params[i]
            val a = args[i] ?: continue
            if (p.isPrimitive) {
                if (!isBoxedTypeOf(a, p)) return false
            } else {
                if (!p.isAssignableFrom(a)) return false
            }
        }
        return true
    }

    private fun isBoxedTypeOf(arg: Class<*>, primitive: Class<*>): Boolean {
        return when (primitive) {
            java.lang.Boolean.TYPE -> arg == java.lang.Boolean::class.java
            java.lang.Byte.TYPE -> arg == java.lang.Byte::class.java
            java.lang.Short.TYPE -> arg == java.lang.Short::class.java
            java.lang.Character.TYPE -> arg == java.lang.Character::class.java
            java.lang.Integer.TYPE -> arg == java.lang.Integer::class.java
            java.lang.Long.TYPE -> arg == java.lang.Long::class.java
            java.lang.Float.TYPE -> arg == java.lang.Float::class.java
            java.lang.Double.TYPE -> arg == java.lang.Double::class.java
            else -> false
        }
    }
}

fun Any.getObjectField(fieldName: String): Any? = ReflectUtils.getObjectField(this, fieldName)
fun <T> Any.getObjectFieldAs(fieldName: String): T = getObjectField(fieldName) as T

fun Any.callMethod(methodName: String, vararg args: Any?): Any? = ReflectUtils.callMethod(this, methodName, *args)
fun <T> Any.callMethodAs(methodName: String, vararg args: Any?): T = callMethod(methodName, *args) as T

fun Class<*>.callStaticMethod(methodName: String, vararg args: Any?): Any? = ReflectUtils.callStaticMethod(this, methodName, *args)
fun <T> Class<*>.callStaticMethodAs(methodName: String, vararg args: Any?): T = callStaticMethod(methodName, *args) as T
