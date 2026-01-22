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

import android.content.Context;
import android.content.Intent;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.sevtinge.hyperceiler.provision.fragment.WebFragment;
import com.sevtinge.hyperceiler.provision.utils.OobeUtils;
import com.sevtinge.hyperceiler.provision.widget.WebBottomSheet;

import fan.bottomsheet.BottomSheetBehavior;
import fan.core.utils.HyperMaterialUtils;
import fan.core.utils.RomUtils;

public class TermsTitleSpan extends ClickableSpan {

    private FragmentActivity mContext;
    private int mHiperlinkType;

    public TermsTitleSpan(FragmentActivity context, int type) {
        mContext = context;
        mHiperlinkType = type;
    }

    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
        ds.setUnderlineText(false);
    }

    @Override
    public void onClick(@NonNull View widget) {
        Log.i("TermsAndStatementFragment", " here is TermsTitleSpan onClick ");
        Intent licenseIntent = OobeUtils.getLicenseIntent("");
        if (mHiperlinkType == 2) {
            Log.i("TermsAndStatementFragment", " here is User Agreement click ");
            //licenseIntent.putExtra("android.intent.extra.LICENSE_TYPE", 2);
            showBottomSheetMenu(mContext, "https://gcore.jsdelivr.net/gh/ReChronoRain/website@main/Protocol.md");
            //OobeUtils.startActivity(mContext, OobeUtils.getLicenseIntent("https://gcore.jsdelivr.net/gh/ReChronoRain/website@main/Protocol.md"));
            //OTHelper.rdCountEvent("key_click_terms_license");
        } else if (mHiperlinkType == 1) {
            Log.i("TermsAndStatementFragment", " here is Privacy Policy click ");
            showBottomSheetMenu(mContext, "https://gcore.jsdelivr.net/gh/ReChronoRain/website@main/Privacy.md");
            //licenseIntent.putExtra("android.intent.extra.LICENSE_TYPE", 1);
            //OobeUtils.startActivity(mContext, OobeUtils.getLicenseIntent("https://gcore.jsdelivr.net/gh/ReChronoRain/website@main/Privacy.md"));
            //OTHelper.rdCountEvent("key_click_terms_privacy");
        }
    }

    public void showBottomSheetMenu(FragmentActivity activity, String uri) {
        WebBottomSheet bottomSheetModel = new WebBottomSheet(activity);
        WebFragment webFragment = new WebFragment();
        webFragment.setBottomSheetModal(bottomSheetModel);
        //webFragment.setPeopleAndPetTitle(title);
        //webFragment.setCompleteCallBack(null);
        bottomSheetModel.init(uri, activity);
        bottomSheetModel.setDragHandleViewEnabled(true);
        bottomSheetModel.setCanceledOnTouchOutside(false);
        bottomSheetModel.setDragHandleViewEnabled(false);
        if (HyperMaterialUtils.isFeatureEnable(activity) && RomUtils.getHyperOsVersion() >= 2) {
            bottomSheetModel.getBehavior().setModeConfig(0);
            bottomSheetModel.applyBlur(true);
        }
        bottomSheetModel.show();
        //bottomSheetModel.getBehavior().setBottomModeMaxWidth(ResourceUtils.getDimentionPixelsSize(requireContext(), R.dimen.bottom_sheet_dialog_max_width));
        //bottomSheetModel.setOnDismissListener(new IntentUtil$.ExternalSyntheticLambda0(oncompletecallback, customBottomSheetModel));
    }
}
