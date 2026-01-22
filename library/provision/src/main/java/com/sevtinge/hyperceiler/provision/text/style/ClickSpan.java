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
package com.sevtinge.hyperceiler.provision.text.style;

import android.app.Activity;
import android.content.Context;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.sevtinge.hyperceiler.provision.fragment.WebFragment;
import com.sevtinge.hyperceiler.provision.utils.OobeUtils;
import com.sevtinge.hyperceiler.provision.widget.WebBottomSheet;

import java.util.HashMap;

import fan.bottomsheet.BottomSheetBehavior;

public class ClickSpan extends ClickableSpan {

    private FragmentActivity mContext;
    private HashMap<String, Integer> mPrivacyTypeMap;

    public ClickSpan(FragmentActivity context, HashMap<String, Integer> typeMap) {
        this(typeMap);
        mContext = context;
    }

    private ClickSpan(HashMap<String, Integer> typeMap) {
        mPrivacyTypeMap = typeMap;
    }

    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
        ds.setUnderlineText(false);
        super.updateDrawState(ds);
    }

    @Override
    public void onClick(@NonNull View widget) {
        Spanned spanned = (Spanned) ((TextView) widget).getText();
        int spanStart = spanned.getSpanStart(this);
        int spanEnd = spanned.getSpanEnd(this);
        showBottomSheetMenu(mContext, "Test");
        //OobeUtils.startActivity(mContext, OobeUtils.getLicenseIntent("https://limestart.cn/"));
    }

    public void showBottomSheetMenu(FragmentActivity activity, String uri) {
        WebBottomSheet bottomSheetModel = new WebBottomSheet(activity);
        WebFragment webFragment = new WebFragment();
        webFragment.setBottomSheetModal(bottomSheetModel);
        //webFragment.setPeopleAndPetTitle(title);
        //webFragment.setCompleteCallBack(null);
        bottomSheetModel.init(uri, activity);
        bottomSheetModel.setCanceledOnTouchOutside(false);
        bottomSheetModel.setDragHandleViewEnabled(false);
        /*if (HyperMaterialUtils.isFeatureEnable(customOrderFragment.getActivity()) && RomUtils.getHyperOsVersion() >= 2) {
            bottomSheetModel.getBehavior().setModeConfig(0);
            bottomSheetModel.applyBlur(true);
        }*/
        bottomSheetModel.show();
        bottomSheetModel.setDragHandleViewEnabled(true);
        //bottomSheetModel.getBehavior().setBottomModeMaxWidth(ResourceUtils.getDimentionPixelsSize(requireContext(), R.dimen.bottom_sheet_dialog_max_width));
        //bottomSheetModel.setOnDismissListener(new IntentUtil$.ExternalSyntheticLambda0(oncompletecallback, customBottomSheetModel));
    }
}
