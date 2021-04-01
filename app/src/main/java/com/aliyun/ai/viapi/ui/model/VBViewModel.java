package com.aliyun.ai.viapi.ui.model;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.aliyun.ai.viapi.ui.model.base.BaseViewModel;
import com.aliyun.ai.viapi.util.Logs;

import java.io.File;
import java.util.List;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * @author: created by hanbing
 * @date: 2021/3/11
 * @Description:
 */
public class VBViewModel extends BaseViewModel<VBBaseDataRepo> {
    private static final String TAG = "VBViewModel";
    public final MutableLiveData<List<BaseVBItem>> baseCardItemLiveData = new MutableLiveData<>();

    public VBViewModel() {
        mDataRepo = new VBBaseDataRepo(getCompositeDisposable());
    }


    public MutableLiveData<List<BaseVBItem>> getBaseCardItemLiveData() {
        return baseCardItemLiveData;
    }

    public void loadCardItems(Context context) {
        Disposable d = mDataRepo
                .getAllVBCardItems(context)
                .subscribeOn(Schedulers.io())
                .subscribe(new Consumer<List<BaseVBItem>>() {
                    @Override
                    public void accept(List<BaseVBItem> models) {
                        if (models == null || models.size() == 0) {
                            return;
                        }
                        baseCardItemLiveData.postValue(models);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        Logs.e(TAG, "loadSkinCardItems failed" + throwable.getMessage());
                    }
                });
        addDisposable(d);
    }

    public BaseVBItem createUserCardItem(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            return mDataRepo.createUserCardItem(file);
        }
        return null;
    }

    public BaseVBItem createDivideLineCardItem() {
        return mDataRepo.createDivideLineCardItem();
    }

}
