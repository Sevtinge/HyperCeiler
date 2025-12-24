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
package com.sevtinge.hyperceiler.hook.utils;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class AdditionalFieldStore {
    private static final Map<Object, Map<String, Object>> STORE =
        Collections.synchronizedMap(new WeakHashMap<>());

    private AdditionalFieldStore() {
    }

    public static Object get(Object target, String key) {
        if (target == null || key == null) return null;
        Map<String, Object> map = STORE.get(target);
        return map == null ? null : map.get(key);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getAs(Object target, String key, Class<T> clazz) {
        Object value = get(target, key);
        if (value == null) return null;
        if (clazz == null) return (T) value;
        if (!clazz.isInstance(value)) return null;
        return (T) value;
    }

    public static void set(Object target, String key, Object value) {
        if (target == null || key == null) return;
        Map<String, Object> map = STORE.get(target);
        if (map == null) {
            map = new java.util.HashMap<>();
            STORE.put(target, map);
        }
        map.put(key, value);
    }

    public static void remove(Object target, String key) {
        if (target == null || key == null) return;
        Map<String, Object> map = STORE.get(target);
        if (map == null) return;
        map.remove(key);
        if (map.isEmpty()) {
            STORE.remove(target);
        }
    }
}
