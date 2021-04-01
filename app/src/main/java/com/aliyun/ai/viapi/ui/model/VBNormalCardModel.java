package com.aliyun.ai.viapi.ui.model;

import androidx.annotation.DrawableRes;

/**
 * @author: created by hanbing
 * @date: 2021/3/11
 * @Description:
 */
public class VBNormalCardModel extends BaseVBItem {

    @Override
    public VBCardType getType() {
        return type;
    }

    @DrawableRes
    public int drawableResId;
    //是否展示底部图片右上角的删除按钮
    public boolean isShowDeleteIcon;

    public VBNormalCardModel(int drawableResId, boolean isShowDeleteIcon, String bgImageName, VBCardType type, boolean selected) {
        super(type, selected, bgImageName);
        this.drawableResId = drawableResId;
        this.isShowDeleteIcon = isShowDeleteIcon;
        this.bgImageName = bgImageName;
    }

    @Override
    public String toString() {
        return "VBNormalCardModel{" +
                ", drawableResId=" + drawableResId +
                ", isShowDeleteIcon=" + isShowDeleteIcon +
                ", bgImageName='" + bgImageName + '\'' +
                ", type=" + type +
                ", selected=" + selected +
                '}';
    }


}
