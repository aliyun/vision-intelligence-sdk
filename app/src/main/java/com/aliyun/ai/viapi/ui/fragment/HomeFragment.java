package com.aliyun.ai.viapi.ui.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aliyun.ai.viapi.R;
import com.aliyun.ai.viapi.ui.adapter.HomeAdapter;
import com.aliyun.ai.viapi.ui.homepage.HomeTabPageId;
import com.aliyun.ai.viapi.ui.homepage.model.HomePageModel;
import com.aliyun.ai.viapi.ui.homepage.model.HomeTabItem;
import com.aliyun.ai.viapi.ui.widget.CusConvenientBanner;
import com.aliyun.ai.viapi.ui.widget.BannerViewHolder;
import com.bigkoo.convenientbanner.holder.CBViewHolderCreator;
import com.bigkoo.convenientbanner.holder.Holder;
import com.bigkoo.convenientbanner.listener.OnItemClickListener;
import com.tbruyelle.rxpermissions2.RxPermissions;

/**
 * @author: created by hanbing
 * @date: 2021/3/9
 * @Description:
 */
public class HomeFragment extends Fragment implements OnItemClickListener, HomeAdapter.OnItemClickListener {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;
    private RecyclerView mTabRecyclerView;
    private CusConvenientBanner banner;
    private HomePageModel mHomePageModel;
    private OnFragmentCallback mFragmentCallback;

    public HomeFragment() {
    }

    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getActivity() instanceof OnFragmentCallback) {
            mFragmentCallback = (OnFragmentCallback) getActivity();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mHomePageModel = new HomePageModel();
        initView(view);
        initTab();
    }

    private void initView(View view) {
        banner = view.findViewById(R.id.banner_home);
        mTabRecyclerView = view.findViewById(R.id.home_tab_rv);
        mTabRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 4));
        banner.setPages(new CBViewHolderCreator() {
            @Override
            public Holder createHolder(View itemView) {
                return new BannerViewHolder(itemView);
            }

            @Override
            public int getLayoutId() {
                return R.layout.layout_home_image_item;
            }
        }, mHomePageModel.getHomeBannerItems());
        banner.setOnItemClickListener(this);
        banner.setPageIndicator(new int[]{R.drawable.ic_page_indicator_false, R.drawable.ic_page_indicator});
    }

    private void initTab() {
        HomeAdapter homeAdapter = new HomeAdapter();
        homeAdapter.setOnItemClickListener(this);
        mTabRecyclerView.setAdapter(homeAdapter);
        homeAdapter.setCardList(mHomePageModel.getHomeTabItems());
    }

    @SuppressLint("CheckResult")
    @Override
    public void onItemClick(HomeTabItem item) {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE).subscribe(granted -> {
            if (granted) {
                openTabPage(item.tabPageId);
            } else {
                // 权限被拒绝
                Toast.makeText(getContext(), R.string.refused, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onItemClick(int position) {
        if (mFragmentCallback != null) {
            mFragmentCallback.bannerClick(position);
        }
    }

    private void openTabPage(HomeTabPageId tabPageId) {

        if (mFragmentCallback != null) {
            Log.e("hanbing", "");
            mFragmentCallback.openTabPage(tabPageId);
        }
    }
}