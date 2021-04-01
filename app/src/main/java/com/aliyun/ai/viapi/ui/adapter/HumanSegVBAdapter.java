package com.aliyun.ai.viapi.ui.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aliyun.ai.viapi.R;
import com.aliyun.ai.viapi.ui.model.BaseVBItem;
import com.aliyun.ai.viapi.ui.model.VBAddCardModel;
import com.aliyun.ai.viapi.ui.model.VBCardType;
import com.aliyun.ai.viapi.ui.model.VBIconCardModel;
import com.aliyun.ai.viapi.ui.model.VBInnerCardModel;
import com.aliyun.ai.viapi.ui.model.VBNoneCardModel;
import com.aliyun.ai.viapi.ui.model.VBNormalCardModel;
import com.aliyun.ai.viapi.ui.model.VBUserCustomCardModel;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: created by hanbing
 * @date: 2021/3/11
 * @Description:
 */
public class HumanSegVBAdapter extends RecyclerView.Adapter<HumanSegVBAdapter.AbsViewHolder> {
    private static List<BaseVBItem> mCardList = new ArrayList<>();
    private static final int USER_CARD_POS = 2;
    private OnItemClickListener mOnItemClickListener;
    private OnCardRemoveListener mOnCardRemoveListener;
    private Context context;

    public HumanSegVBAdapter(Context context) {
        this.context = context;
    }

    @Override
    public AbsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        AbsViewHolder holder = new InnerCardViewHolder(parent);
        if (viewType == VBCardType.NONE.getTypeId()) {
            holder = new NoneCardViewHolder(parent);
        } else if (viewType == VBCardType.ADD_MENU.getTypeId()) {
            holder = new AddMenuViewHolder(parent);
        } else if (viewType == VBCardType.USER_CUSTOM_PIC.getTypeId()) {
            holder = new UserCustomViewHolder(parent);
        } else if (viewType == VBCardType.DIVIDING_LINE.getTypeId()) {
            holder = new DividingViewHolder(parent);
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(AbsViewHolder holder, int position) {
        if (getItemViewType(position) == VBCardType.NONE.getTypeId()) {
            VBNoneCardModel vbNoneCardModel = (VBNoneCardModel) mCardList.get(position);
            NoneCardViewHolder noneCardViewHolder = (NoneCardViewHolder) holder;
            noneCardViewHolder.bindData(vbNoneCardModel, position);
        } else if (getItemViewType(position) == VBCardType.ADD_MENU.getTypeId()) {
            VBAddCardModel vbAddCardModel = (VBAddCardModel) mCardList.get(position);
            AddMenuViewHolder addMenuViewHolder = (AddMenuViewHolder) holder;
            addMenuViewHolder.bindData(vbAddCardModel, position);
        } else if (getItemViewType(position) == VBCardType.USER_CUSTOM_PIC.getTypeId()) {
            VBNormalCardModel vbUserCustomCardModel = (VBNormalCardModel) mCardList.get(position);
            NormalCardViewHolder userCustomViewHolder = (NormalCardViewHolder) holder;
            userCustomViewHolder.bindData(vbUserCustomCardModel, position);
        } else if (getItemViewType(position) == VBCardType.INNER_PIC.getTypeId()) {
            VBNormalCardModel innerCardModel = (VBNormalCardModel) mCardList.get(position);
            NormalCardViewHolder innerCardViewHolder = (NormalCardViewHolder) holder;
            innerCardViewHolder.bindData(innerCardModel, position);
        }
    }

    @Override
    public int getItemCount() {
        return mCardList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mCardList.get(position).getType().getTypeId();
    }

    public void setCardList(List<BaseVBItem> cardList) {
        if (cardList != null && cardList.size() > 0) {
            mCardList.clear();
            mCardList.addAll(cardList);
            notifyDataSetChanged();
        }
    }

    private void addUserCard(int position, BaseVBItem item) {
        mCardList.add(position, item);
//        notifyItemInserted(position);
    }

    public void addUserCard(BaseVBItem item) {
        addUserCard(item, true);
    }

    public void addUserCard(BaseVBItem item, boolean isRefresh) {
        addUserCard(USER_CARD_POS, item);
        if (item.selected) {
            updateSelectedBg(item.getBgImageName());
        } else {
            if (isRefresh) {
                notifyDataSetChanged();
            }
        }
    }

    public void removeUserCard(int position) {
        BaseVBItem item = mCardList.remove(position);
        if (item.selected) {
            mCardList.get(0).setSelected(true);
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(mCardList.get(0));
            }
        } else {
            // notifyItemRemoved(position);
            notifyDataSetChanged();
        }
    }

    public void removeDivide() {
        for (int i = 0; i < mCardList.size(); i++) {
            BaseVBItem data = mCardList.get(i);
            if (VBCardType.DIVIDING_LINE.equals(data.getType())) {
                mCardList.remove(data);
                notifyDataSetChanged();
                break;
            }
        }
    }

    public int getUserSelectCardCount() {
        int count = 0;
        if (mCardList != null) {
            for (int i = 0; i < mCardList.size(); i++) {
                BaseVBItem data = mCardList.get(i);
                if (VBCardType.USER_CUSTOM_PIC.equals(data.getType())) {
                    count++;
                }
            }
        }
        return count;
    }

    public int updateSelectedBg(final String bgImageName) {
        int pos = 0;
        if (mCardList != null) {
            for (int i = 0; i < mCardList.size(); i++) {
                BaseVBItem data = mCardList.get(i);
                if (bgImageName.equals(data.bgImageName) && !VBCardType.ADD_MENU.getCardName().equals(data.bgImageName)) {
                    data.selected = true;
                    pos = i;
                } else {
                    data.selected = false;
                }
            }
            notifyDataSetChanged();
        }
        return pos;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }

    public void setOnCardRemoveListener(OnCardRemoveListener cardRemoveListener) {
        this.mOnCardRemoveListener = cardRemoveListener;
    }

    public class NoneCardViewHolder extends IconFontViewHolder {

        NoneCardViewHolder(@NonNull ViewGroup parent) {
            super(parent);
        }

        @Override
        public int getType() {
            return VBCardType.NONE.getTypeId();
        }
    }

    public class AddMenuViewHolder extends IconFontViewHolder {

        AddMenuViewHolder(@NonNull ViewGroup parent) {
            super(parent);
        }

        @Override
        protected void initView(View itemView) {
            super.initView(itemView);
            iconFontCardContainer.setBackgroundResource(R.color.transparent);
        }

        @Override
        public int getType() {
            return VBCardType.ADD_MENU.getTypeId();
        }
    }

    public class UserCustomViewHolder extends NormalCardViewHolder {

        UserCustomViewHolder(@NonNull ViewGroup parent) {
            super(parent);
        }

        @Override
        protected void initView(final View itemView) {
            super.initView(itemView);
            deleteIcon.setVisibility(View.VISIBLE);
            deleteIcon.setOnClickListener(v -> {
                BaseVBItem item = (BaseVBItem) itemView.getTag();
                int pos = (int) itemView.getTag(R.id.vb_card_pos_tag_key);
                if (item instanceof VBUserCustomCardModel) {
                    if (mOnCardRemoveListener != null) {
                        mOnCardRemoveListener.onRemove(pos, (VBUserCustomCardModel) item);
                    }
                }
            });
        }

        @Override
        public int getType() {
            return VBCardType.USER_CUSTOM_PIC.getTypeId();
        }
    }

    public class InnerCardViewHolder extends NormalCardViewHolder {

        InnerCardViewHolder(@NonNull ViewGroup parent) {
            super(parent);
        }

        @Override
        protected void initView(View itemView) {
            super.initView(itemView);
            deleteIcon.setVisibility(View.GONE);
        }

        @Override
        public int getType() {
            return VBCardType.INNER_PIC.getTypeId();
        }
    }

    public abstract class NormalCardViewHolder extends AbsViewHolder<VBNormalCardModel> {
        RelativeLayout normalCardContainer;
        View deleteIcon;
        ImageView bgIcon;

        NormalCardViewHolder(@NonNull ViewGroup parent) {
            super(parent, R.layout.layout_vg_normal_card_item);
        }

        @Override
        protected void initView(final View itemView) {
            deleteIcon = itemView.findViewById(R.id.delete_icon);
            bgIcon = itemView.findViewById(R.id.bg_icon);
            normalCardContainer = itemView.findViewById(R.id.normal_card_container);
            normalCardContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BaseVBItem item = (BaseVBItem) itemView.getTag();
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemClick(item);
                    }
                }
            });
        }

        @Override
        public void bindData(VBNormalCardModel data, int position) {
            normalCardContainer.setSelected(data.selected);
            itemView.setTag(data);
            itemView.setTag(R.id.vb_card_pos_tag_key, position);
            if (data instanceof VBInnerCardModel) {
                bgIcon.setImageResource(data.drawableResId);
                RequestOptions options = new RequestOptions();
                options.centerCrop();
                Glide.with(context).load(data.drawableResId).apply(options).into(bgIcon);
            } else if (data instanceof VBUserCustomCardModel) {
                Glide.with(context).load(Uri.fromFile(new File(data.getBgImagePath()))).into(bgIcon);
            }
            if (data.isShowDeleteIcon) {
                deleteIcon.setVisibility(View.VISIBLE);
            } else {
                deleteIcon.setVisibility(View.GONE);
            }
        }
    }

    public static class DividingViewHolder extends AbsViewHolder<VBIconCardModel> {

        DividingViewHolder(@NonNull ViewGroup parent) {
            super(parent, R.layout.layout_vg_dividing_line_card_item);
        }

        @Override
        public int getType() {
            return VBCardType.DIVIDING_LINE.getTypeId();
        }

        @Override
        protected void initView(final View itemView) {
        }

        @Override
        public void bindData(VBIconCardModel data, int position) {
        }
    }

    public abstract class IconFontViewHolder extends AbsViewHolder<VBIconCardModel> {
        RelativeLayout iconFontCardContainer;
        ImageView icon;

        IconFontViewHolder(@NonNull ViewGroup parent) {
            super(parent, R.layout.layout_vg_iconfont_card_item);
        }

        @Override
        protected void initView(final View itemView) {
            icon = itemView.findViewById(R.id.vg_card_center_iconfont);
            iconFontCardContainer = itemView.findViewById(R.id.iconfont_card_container);
            iconFontCardContainer.setOnClickListener(v -> {
                BaseVBItem item = (BaseVBItem) itemView.getTag();
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(item);
                }
            });
        }

        @Override
        public void bindData(VBIconCardModel data, int position) {
            iconFontCardContainer.setSelected(data.selected);
            itemView.setTag(data);
            itemView.setTag(R.id.vb_card_pos_tag_key, position);
            icon.setImageResource(data.iconResId);
        }
    }

    public static abstract class AbsViewHolder<T> extends RecyclerView.ViewHolder {
        AbsViewHolder(@NonNull ViewGroup parent, int layoutId) {
            super(LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false));
            initView(itemView);
        }

        public abstract int getType();

        protected abstract void initView(View itemView);

        public abstract void bindData(T data, int position);
    }

    public interface OnItemClickListener {
        void onItemClick(BaseVBItem item);
    }

    public interface OnCardRemoveListener {
        void onRemove(int pos, VBUserCustomCardModel item);
    }
}
