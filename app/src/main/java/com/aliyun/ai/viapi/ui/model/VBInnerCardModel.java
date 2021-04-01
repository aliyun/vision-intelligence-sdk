package com.aliyun.ai.viapi.ui.model;

/**
 * @author: created by hanbing
 * @date: 2021/3/11
 * @Description:
 */
public class VBInnerCardModel extends VBNormalCardModel {

    public VBInnerCardModel(int drawableResId, boolean isShowDeleteIcon, String bgImageName, boolean selected) {
        super(drawableResId, isShowDeleteIcon, bgImageName, VBCardType.INNER_PIC, selected);
    }
}
