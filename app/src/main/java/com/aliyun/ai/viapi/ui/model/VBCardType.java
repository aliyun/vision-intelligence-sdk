package com.aliyun.ai.viapi.ui.model;

/**
 * @author: created by hanbing
 * @date: 2021/3/11
 * @Description:
 */
public enum VBCardType {
    NONE(0, "none"),
    ADD_MENU(1, "add_menu"),
    BLUR_PIC(2, "blue_pic"),
    DIVIDING_LINE(3, "dividing_line"),
    USER_CUSTOM_PIC(4, "user_custom_pic"),
    INNER_PIC(5, "inner_pic");

    private int typeId;
    private String name;

    VBCardType(int type, String name) {
        this.typeId = type;
        this.name = name;
    }

    public int getTypeId() {
        return typeId;
    }

    public String getCardName() {
        return name;
    }
}
