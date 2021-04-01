package com.aliyun.ai.viapi.ui.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aliyun.ai.viapi.R;
import com.aliyun.ai.viapi.ui.homepage.model.HomeTabItem;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: created by hanbing
 * @date: 2021/3/9
 * @Description:
 */
public class HomeAdapter extends RecyclerView.Adapter<HomeAdapter.HomeViewHolder> {
    private List<HomeTabItem> mCardList = new ArrayList<>();
    private OnItemClickListener mOnItemClickListener;

    @NonNull
    @Override
    public HomeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new HomeViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull HomeViewHolder holder, int position) {
        HomeTabItem item = mCardList.get(position);
        HomeViewHolder homeViewHolder = holder;
        homeViewHolder.bindData(item, position);
    }

    @Override
    public int getItemCount() {
        return mCardList.size();
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }

    public void setCardList(List<HomeTabItem> cardList) {
        if (cardList != null && cardList.size() > 0) {
            mCardList.clear();
            mCardList.addAll(cardList);
            notifyDataSetChanged();
        }
    }

    public class HomeViewHolder extends AbsViewHolder<HomeTabItem> {
        ImageView imageView;
        TextView textView;

        HomeViewHolder(@NonNull ViewGroup parent) {
            super(parent, R.layout.layout_homepage_tab_list_item);
        }

        @Override
        public int getType() {
            return 0;
        }

        @Override
        protected void initView(final View itemView) {
            imageView = itemView.findViewById(R.id.home_tab_icon_iv);
            textView = itemView.findViewById(R.id.home_tab_name_tv);
            itemView.setOnClickListener((v) -> {
                HomeTabItem item = (HomeTabItem) itemView.getTag();
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(item);
                }

            });
        }

        @Override
        public void bindData(HomeTabItem data, int position) {
            itemView.setTag(data);
            textView.setText(data.tabNameResId);
            imageView.setImageResource(data.drawableResId);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(HomeTabItem item);
    }
}
