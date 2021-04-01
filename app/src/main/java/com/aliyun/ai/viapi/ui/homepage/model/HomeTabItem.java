package com.aliyun.ai.viapi.ui.homepage.model;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.aliyun.ai.viapi.ui.homepage.HomeTabPageId;

/**
 * @author: created by hanbing
 * @date: 2021/3/9
 * @Description:
 */
public class HomeTabItem {
    @StringRes
    public int tabNameResId;
    @DrawableRes
    public int drawableResId;

    public HomeTabPageId tabPageId;

    public HomeTabItem(int tabNameResId, int drawableResId, HomeTabPageId tabPageId) {
        this.tabNameResId = tabNameResId;
        this.drawableResId = drawableResId;
        this.tabPageId = tabPageId;
    }
}
