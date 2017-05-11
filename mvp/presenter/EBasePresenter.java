package com.fenqile.licai.base.mvp.presenter;

import android.content.Context;

/**
 * Created by：Xing.wan
 * Created Time：16/7/4 09:56
 */
public abstract class EBasePresenter<M, V>  {
    public Context context;
    //model
    protected M mModel;
    //view
    protected V mView;

    public void setVM(V v, M m) {
        this.mView = v;
        this.mModel = m;
    }

    /**
     * onPresenterStart
     */
    public abstract void onStart();

    /**
     * onDestory
     */
    public void onDestroy() {

    }

    /**
     * 获取 View
     * @return
     */
    protected V getView(){
        return mView;
    }

    /**
     * 获取 Model
     * @return
     */
    protected M getModel(){
        return mModel;
    }
}
