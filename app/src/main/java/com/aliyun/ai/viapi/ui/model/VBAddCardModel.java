package com.aliyun.ai.viapi.ui.model;

/**
 * @author: created by hanbing
 * @date: 2021/3/11
 * @Description:
 */
public class VBAddCardModel extends VBIconCardModel {

    public VBAddCardModel(int iconFontName, boolean isSelect) {
        super(iconFontName, VBCardType.ADD_MENU, isSelect, VBCardType.ADD_MENU.getCardName());
    }
}
