package com.aliyun.ai.viapi.ui.model;

import android.content.Context;

import androidx.annotation.NonNull;

import com.aliyun.ai.viapi.R;
import com.aliyun.ai.viapi.ui.model.base.BaseDataRepo;
import com.aliyun.ai.viapi.ui.vb.VBConfRecord;
import com.aliyun.ai.viapi.util.FileUtil;
import com.aliyun.ai.viapi.util.PictureUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.CompositeDisposable;

/**
 * @author: created by hanbing
 * @date: 2021/3/11
 * @Description:
 */
public class VBBaseDataRepo extends BaseDataRepo {

    public VBBaseDataRepo(CompositeDisposable compositeDisposable) {
        super(compositeDisposable);
    }

    @NonNull
    private List<BaseVBItem> getInnerCardItems(boolean isAddDivideLine, String selectedBgName) {
        List<BaseVBItem> cardItemList = new ArrayList<>();

        BaseVBItem model;
        // 分割线
        if (isAddDivideLine) {
            model = new VBDivideCardModel();
            cardItemList.add(model);
        }
        model = new VBInnerCardModel(R.drawable.pic_virtual_bg_01, false, "pic_virtual_bg_01", "pic_virtual_bg_01".equals(selectedBgName));
        cardItemList.add(model);

        model = new VBInnerCardModel(R.drawable.pic_virtual_bg_02, false, "pic_virtual_bg_02", "pic_virtual_bg_02".equals(selectedBgName));
        cardItemList.add(model);

        model = new VBInnerCardModel(R.drawable.pic_virtual_bg_03, false, "pic_virtual_bg_03", "pic_virtual_bg_03".equals(selectedBgName));
        cardItemList.add(model);

        model = new VBInnerCardModel(R.drawable.pic_virtual_bg_04, false, "pic_virtual_bg_04", "pic_virtual_bg_04".equals(selectedBgName));
        cardItemList.add(model);

        model = new VBInnerCardModel(R.drawable.pic_virtual_bg_05, false, "pic_virtual_bg_05", "pic_virtual_bg_05".equals(selectedBgName));
        cardItemList.add(model);

        model = new VBInnerCardModel(R.drawable.pic_virtual_bg_06, false, "pic_virtual_bg_06", "pic_virtual_bg_06".equals(selectedBgName));
        cardItemList.add(model);

        model = new VBInnerCardModel(R.drawable.pic_virtual_bg_07, false, "pic_virtual_bg_07", "pic_virtual_bg_07".equals(selectedBgName));
        cardItemList.add(model);

        model = new VBInnerCardModel(R.drawable.pic_virtual_bg_08, false, "pic_virtual_bg_08", "pic_virtual_bg_08".equals(selectedBgName));
        cardItemList.add(model);

        model = new VBInnerCardModel(R.drawable.pic_virtual_bg_09, false, "pic_virtual_bg_09", "pic_virtual_bg_09".equals(selectedBgName));
        cardItemList.add(model);

        model = new VBInnerCardModel(R.drawable.pic_virtual_bg_10, false, "pic_virtual_bg_10", "pic_virtual_bg_10".equals(selectedBgName));
        cardItemList.add(model);

        model = new VBInnerCardModel(R.drawable.pic_virtual_bg_11, false, "pic_virtual_bg_11", "pic_virtual_bg_11".equals(selectedBgName));

        cardItemList.add(model);
        return cardItemList;
    }

    private List<BaseVBItem> getFixedCardItems(String selectedBgName) {
        List<BaseVBItem> cardItemList = new ArrayList<>();
        BaseVBItem model;
        model = new VBNoneCardModel(R.drawable.ic_none, VBCardType.NONE.getCardName().equals(selectedBgName));
        cardItemList.add(model);
        model = new VBAddCardModel(R.drawable.ic_add, false);
        cardItemList.add(model);
        return cardItemList;
    }

    private List<VBUserCustomCardModel> getUserSetCardItems(Context context, String selectedBgName) {
        List<VBUserCustomCardModel> cardItemList = new ArrayList<>();
        String path = PictureUtil.getVBImageSavePath(context);
        File file = new File(path);
        File[] listFiles = file.listFiles();
        String bgFilePath;
        if (listFiles.length > 0) {
            VBUserCustomCardModel model;
            for (File filePath : listFiles) {
                if (PictureUtil.IsSupportImageFile(filePath.getPath())) {
                    bgFilePath = filePath.getAbsolutePath();
                    String bgImageName = FileUtil.getFileNameNoExt(bgFilePath);
                    model = new VBUserCustomCardModel(0,
                            true, bgFilePath, bgImageName, selectedBgName.equals(bgImageName));
                    cardItemList.add(model);

                }
            }
        }
        Collections.sort(cardItemList, (o1, o2) -> {
            int result;
//                if (o1.timeStamp - o2.timeStamp < 0) {
//                    result = 1;
//                }
            result = o2.bgImageName.compareTo(o1.bgImageName);
            return result;
        });
        return cardItemList;
    }

    public Observable<List<BaseVBItem>> getAllVBCardItems(final Context context) {
        return Observable.create(e -> {
            String selectedBgName = VBConfRecord.getUserSelectVbImage(context);
            if (selectedBgName == null) {
                selectedBgName = "";
            } else {
                if (VBConfRecord.isLocalImagePath(selectedBgName)) {
                    selectedBgName = FileUtil.getFileNameNoExt(selectedBgName);
                }
            }
            List<BaseVBItem> cardItemList = new ArrayList<>(getFixedCardItems(selectedBgName));
            List<VBUserCustomCardModel> userSetCardItems = getUserSetCardItems(context, selectedBgName);
            cardItemList.addAll(userSetCardItems);
            cardItemList.addAll(getInnerCardItems(userSetCardItems.size() > 0, selectedBgName));
            e.onNext(cardItemList);
        });
    }

    public BaseVBItem createUserCardItem(File filePath) {
        String bgFilePath = filePath.getAbsolutePath();
        String bgImageName = FileUtil.getFileNameNoExt(bgFilePath);
        return new VBUserCustomCardModel(0, true, bgFilePath, bgImageName, true);
    }

    public BaseVBItem createDivideLineCardItem() {
        return new VBDivideCardModel();
    }

}
