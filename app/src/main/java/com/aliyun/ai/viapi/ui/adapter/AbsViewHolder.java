package com.aliyun.ai.viapi.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * @author: created by hanbing
 * @date: 2021/3/9
 * @Description:
 */
public abstract class AbsViewHolder<T> extends RecyclerView.ViewHolder {

    AbsViewHolder(@NonNull ViewGroup parent, int layoutId) {
        super(LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false));
        initView(itemView);
    }

    public abstract int getType();

    protected abstract void initView(View itemView);

    public abstract void bindData(T data, int position);
}
