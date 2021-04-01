package com.aliyun.ai.viapi.ui.model;

/**
 * @author: created by hanbing
 * @date: 2021/3/11
 * @Description:
 */
public class VBNoneCardModel extends VBIconCardModel {

    public VBNoneCardModel(int iconFontName, boolean isSelect) {
        super(iconFontName, VBCardType.NONE, isSelect, VBCardType.NONE.getCardName());
    }
}
