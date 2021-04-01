package com.aliyun.ai.viapi.ui.model;


import androidx.annotation.DrawableRes;

/**
 * @author: created by hanbing
 * @date: 2021/3/11
 * @Description:
 */
public abstract class VBIconCardModel extends BaseVBItem {

    @DrawableRes
    public int iconResId;

    public VBIconCardModel(int iconFontName, VBCardType type, boolean isSelect, String bgImageNam) {
        super(type, isSelect, bgImageNam);
        this.iconResId = iconFontName;
    }
}
