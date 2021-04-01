package com.aliyun.ai.viapi.ui.widget;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aliyun.ai.viapi.R;
import com.aliyun.ai.viapi.ui.adapter.HumanSegVBAdapter;
import com.aliyun.ai.viapi.ui.model.BaseVBItem;
import com.aliyun.ai.viapi.ui.model.VBUserCustomCardModel;

import java.util.List;

/**
 * @author: created by hanbing
 * @date: 2021/3/11
 * @Description:
 */
public class VBPicturePickView extends ConstraintLayout implements View.OnClickListener, HumanSegVBAdapter.OnItemClickListener, HumanSegVBAdapter.OnCardRemoveListener {
    private static final String TAG = "VBPicturePickView";
    private Context context;
    private RecyclerView mRecyclerView;
    private HumanSegVBAdapter mHumanSegVBAdapter;
    private Button mSetComplete;
    private OnCardItemClickListener mItemClickListener;


    public VBPicturePickView(@NonNull Context context) {
        this(context, null);
    }

    public VBPicturePickView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VBPicturePickView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
        LayoutInflater.from(context).inflate(R.layout.layout_picture_picker, this, true);
        mSetComplete = findViewById(R.id.virtual_bg_set_complete);
        mRecyclerView = findViewById(R.id.virtual_bg_select_rv);
        mHumanSegVBAdapter = new HumanSegVBAdapter(context);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRecyclerView.setLayoutManager(layoutManager);
        int space = getResources().getDimensionPixelOffset(R.dimen.dp1);
        int lastSpace = getResources().getDimensionPixelOffset(R.dimen.dp6);
        mRecyclerView.addItemDecoration(new SpacesItemDecoration(space, lastSpace));
        mRecyclerView.setAdapter(mHumanSegVBAdapter);
        mHumanSegVBAdapter.setOnItemClickListener(this);
        mHumanSegVBAdapter.setOnCardRemoveListener(this);
        mSetComplete.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (mItemClickListener != null) {
            mItemClickListener.onSetComplete();
        }
    }

    @Override
    public void onItemClick(BaseVBItem item) {
        if (mItemClickListener != null) {
            mItemClickListener.onItemClick(item);
        }
    }

    @Override
    public void onRemove(int pos, VBUserCustomCardModel item) {
        if (mItemClickListener != null) {
            mItemClickListener.onRemove(pos, item);
        }
    }

    public void setVBLists(List<BaseVBItem> baseVBItems) {
        if (mHumanSegVBAdapter != null) {
            mHumanSegVBAdapter.setCardList(baseVBItems);
        }
    }

    public void setItemClickListener(OnCardItemClickListener listener) {
        this.mItemClickListener = listener;
    }

    public void updateSelectedBg(String bgImageName) {
        mHumanSegVBAdapter.updateSelectedBg(bgImageName);
    }

    public int getUserSelectCardCount() {
        return mHumanSegVBAdapter.getUserSelectCardCount();
    }

    public void addUserCard(BaseVBItem divideLineCardItem, boolean isRefresh) {
        mHumanSegVBAdapter.addUserCard(divideLineCardItem, isRefresh);
    }

    public void addUserCard(BaseVBItem userCardItem) {
        mHumanSegVBAdapter.addUserCard(userCardItem, true);
    }

    public void removeUserCard(int pos) {
        mHumanSegVBAdapter.removeUserCard(pos);
    }

    public void removeDivide() {
        mHumanSegVBAdapter.removeDivide();
    }

    private class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private int space;
        private int lastSpace;

        SpacesItemDecoration(int space, int lastSpace) {
            this.space = space;
            this.lastSpace = lastSpace;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view,
                                   RecyclerView parent, RecyclerView.State state) {
            outRect.left = 0;
            outRect.right = space;
            outRect.bottom = 0;
            outRect.top = 0;
            int childPos = parent.getChildLayoutPosition(view);
//            if (childPos == 0 || childPos == 1) {
//                outRect.right = space ;
//            }
            if (parent.getChildLayoutPosition(view) == mHumanSegVBAdapter.getItemCount() - 1) {
                outRect.right = lastSpace;
            }
        }
    }

    public interface OnCardItemClickListener {
        void onItemClick(BaseVBItem item);

        void onRemove(int pos, VBUserCustomCardModel item);

        void onSetComplete();
    }

}
