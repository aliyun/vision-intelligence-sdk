package com.aliyun.ai.viapi.ui.model;

/**
 * @author: created by hanbing
 * @date: 2021/3/11
 * @Description:
 */
public abstract class BaseVBItem {
    protected VBCardType type;
    public boolean selected;

    public String bgImageName;

    public BaseVBItem(VBCardType type, boolean selected, String bgImageNam) {
        this.type = type;
        this.selected = selected;
        this.bgImageName = bgImageNam;
    }

    public VBCardType getType() {
        return type;
    }

    public String getBgImageName() {
        return bgImageName;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getBgImagePath() {
        return "";
    }
}
