package com.aliyun.ai.viapi.ui.homepage.model;

import androidx.annotation.NonNull;

import com.aliyun.ai.viapi.R;
import com.aliyun.ai.viapi.ui.model.base.BaseDataRepo;
import com.aliyun.ai.viapi.ui.homepage.HomeTabPageId;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.CompositeDisposable;

/**
 * @author: created by hanbing
 * @date: 2021/3/9
 * @Description:
 */
public class HomePageDataRepo extends BaseDataRepo {
    public HomePageDataRepo(CompositeDisposable compositeDisposable) {
        super(compositeDisposable);
    }

    @NonNull
    public List<HomeTabItem> getTabItems() {
        List<HomeTabItem> tabItems = new ArrayList<>();
        HomeTabItem model;
        model = new HomeTabItem(R.string.home_page_tab_video_segment, R.drawable.ic_human_segment, HomeTabPageId.HUMAN_SEGMENT_PAGE);
        tabItems.add(model);
        model = new HomeTabItem(R.string.home_page_tab_picture_segment, R.drawable.ic_human_segment, HomeTabPageId.PICTRRE_SEGMENT_PAGE);
        tabItems.add(model);
        model = new HomeTabItem(R.string.home_page_tab_facebeauty, R.drawable.ic_human_segment, HomeTabPageId.FACEBEAUTY_PAGE);
        tabItems.add(model);
        model = new HomeTabItem(R.string.home_page_tab_face_landmask, R.drawable.ic_human_segment, HomeTabPageId.FACE_LANDMASK_PAGE);
        tabItems.add(model);
        model = new HomeTabItem(R.string.home_page_tab_human_body_landmask, R.drawable.ic_human_segment, HomeTabPageId.HUMAN_BODY_LANDMASK_PAGE);
        tabItems.add(model);
        return tabItems;
    }

    @NonNull
    public List<Integer> getBannerItems() {
        List<Integer> bannerItems = new ArrayList<>();
        bannerItems.add(R.drawable.ic_banner_1);
        bannerItems.add(R.drawable.ic_banner_2);
        bannerItems.add(R.drawable.ic_banner_3);
        return bannerItems;
    }
}
