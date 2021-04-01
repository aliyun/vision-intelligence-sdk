package com.aliyun.ai.viapi.ui.widget;

import android.view.View;
import android.widget.ImageView;

import com.aliyun.ai.viapi.R;
import com.bigkoo.convenientbanner.holder.Holder;

public class BannerViewHolder extends Holder<Integer> {
    private ImageView mImageView;

    public BannerViewHolder(View itemView) {
        super(itemView);
    }

    @Override
    protected void initView(View itemView) {
        mImageView = itemView.findViewById(R.id.item_image);
    }

    @Override
    public void updateUI(Integer data) {
        mImageView.setImageResource(data);
    }
}
