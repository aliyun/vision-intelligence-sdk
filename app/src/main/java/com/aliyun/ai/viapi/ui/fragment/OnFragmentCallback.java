package com.aliyun.ai.viapi.ui.fragment;

import com.aliyun.ai.viapi.ui.homepage.HomeTabPageId;

/**
 * @author: created by hanbing
 * @date: 2021/3/9
 * @Description:
 */
public interface OnFragmentCallback {

    void openTabPage(HomeTabPageId pageId);

    void bannerClick(int position);
}
