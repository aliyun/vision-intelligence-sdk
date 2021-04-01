package com.aliyun.ai.viapi.ui.homepage.model;

import com.aliyun.ai.viapi.ui.model.base.BaseViewModel;

import java.util.List;

/**
 * @author: created by hanbing
 * @date: 2021/3/9
 * @Description:
 */
public class HomePageModel extends BaseViewModel<HomePageDataRepo> {
    public HomePageModel() {
        mDataRepo = new HomePageDataRepo(getCompositeDisposable());
    }

    public List<HomeTabItem> getHomeTabItems() {
        return mDataRepo.getTabItems();
    }

    public List<Integer> getHomeBannerItems() {
        return mDataRepo.getBannerItems();
    }

}
