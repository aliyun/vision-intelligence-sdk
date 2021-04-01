package com.aliyun.ai.viapi.ui.model.base;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

/**
 * @author: created by hanbing
 * @date: 2021/3/9
 * @Description:
 */
public class BaseViewModel<T extends BaseDataRepo> extends ViewModel {
    protected T mDataRepo;
    private static final String TAG = "BaseViewModel";
    private CompositeDisposable mCompositeDisposable;
    public final MutableLiveData<Integer> mLoadingLiveData = new MutableLiveData<>();

    protected CompositeDisposable getCompositeDisposable() {
        if (mCompositeDisposable == null) {
            mCompositeDisposable = new CompositeDisposable();
        }
        return mCompositeDisposable;
    }

    protected void addDisposable(@NonNull final Disposable disposable) {
        getCompositeDisposable().add(disposable);
    }

    protected void disposeDisposable() {
        if (mCompositeDisposable != null && !mCompositeDisposable.isDisposed()) {
            mCompositeDisposable.dispose();
            mCompositeDisposable = null;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposeDisposable();
    }

}
