package com.fenqile.licai.base.mvp.view;

import android.os.Bundle;

import com.fenqile.licai.base.BaseTintActivity;
import com.fenqile.licai.base.mvp.model.EBaseModel;
import com.fenqile.licai.base.mvp.mvputils.TUtil;
import com.fenqile.licai.base.mvp.presenter.EBasePresenter;

/**
 * mvpBase
 * Created by：Xing.wan
 * Created Time：16/7/4 18:00
 */
public abstract class EMvpBaseActivity<P extends EBasePresenter, M extends EBaseModel> extends BaseTintActivity {
    private static final String TAG = "EMvpBaseActivity";
    //p 层实例
    private EBasePresenter mPresenter;
    //model 层实例
    private EBaseModel mModel;

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (mModel == null) {
            //实例化 Model
            mModel = getModelInstance();
        }

        if (mPresenter == null) {
            mPresenter = getPresenterInstance();
        }
        if (mPresenter != null) {
            mPresenter.setVM(EMvpBaseActivity.this, mModel);
        }

        super.onCreate(savedInstanceState);

        if (mPresenter != null) {
            mPresenter.onStart();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPresenter != null) {
            mPresenter.onDestroy();
        }
    }

    //获取 Presenter 实例
    private P getPresenterInstance() {
        return TUtil.getT(EMvpBaseActivity.this, 0); //泛型首个参数
    }

    //获取 Model 实例
    private M getModelInstance() {
        return TUtil.getT(EMvpBaseActivity.this, 1); //泛型第二个参数
    }
    @SuppressWarnings("unchecked")
    protected P getPresenter() {
        if (mPresenter == null) return null;
        return (P) mPresenter;
    }
    @SuppressWarnings("unchecked")
    protected M getModel() {
        if (mModel == null) return null;
        return (M) mModel;
    }
}
