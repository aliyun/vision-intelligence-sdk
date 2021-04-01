package com.aliyun.ai.viapi.ui.model.base;

import io.reactivex.disposables.CompositeDisposable;

/**
 * @author: created by hanbing
 * @date: 2021/3/9
 * @Description:
 */
public abstract class BaseDataRepo {
    protected CompositeDisposable compositeDisposable;

    public BaseDataRepo(CompositeDisposable compositeDisposable) {
        this.compositeDisposable = compositeDisposable;
    }
}
