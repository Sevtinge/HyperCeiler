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
package com.sevtinge.hyperceiler.hook.module.rules.systemframework.corepatch;

import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils;

import de.robv.android.xposed.XC_MethodHook;

public class ReturnConstant extends XC_MethodHook {
    private final String prefsKey;
    private final boolean defaultEnabled;
    private final Object value;

    public ReturnConstant(String prefsKey, boolean defaultEnabled, Object value) {
        this.prefsKey = prefsKey;
        this.defaultEnabled = defaultEnabled;
        this.value = value;
    }

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        super.beforeHookedMethod(param);
        if (PrefsUtils.mPrefsMap.getBoolean(prefsKey, defaultEnabled)) {
            param.setResult(value);
        }
    }
}
