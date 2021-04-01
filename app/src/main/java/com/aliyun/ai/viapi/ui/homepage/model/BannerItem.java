package com.aliyun.ai.viapi.ui.homepage.model;

import androidx.annotation.DrawableRes;

/**
 * @author: created by hanbing
 * @date: 2021/3/9
 * @Description:
 */
public class BannerItem {
    @DrawableRes
    public int drawableResId;

    public BannerItem(int drawableResId) {
        this.drawableResId = drawableResId;
    }
}
