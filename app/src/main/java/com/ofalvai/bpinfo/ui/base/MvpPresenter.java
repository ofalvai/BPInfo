package com.ofalvai.bpinfo.ui.base;

public interface MvpPresenter<V extends MvpView> {

    void attachView(V view);

    void detachView();
}
