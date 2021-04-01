package com.aliyun.ai.viapi.ui.model;

/**
 * @author: created by hanbing
 * @date: 2021/3/11
 * @Description:
 */
public class VBUserCustomCardModel extends VBNormalCardModel {
    // 加载图片的本地路径
    public String bgImagePath;

    public VBUserCustomCardModel(int drawableResId, boolean isShowDeleteIcon, String bgImagePath, String bgImageName, boolean selected) {
        super(drawableResId, isShowDeleteIcon, bgImageName, VBCardType.USER_CUSTOM_PIC, selected);
        this.bgImagePath = bgImagePath;
    }

    @Override
    public String getBgImagePath() {
        return bgImagePath;
    }
}
