package com.example.android.autoeditor.floatingToolbar;

import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.view.View;

class FloatingSnackBarManager implements FloatBar.MorphListener {


    private BaseTransientBottomBar.BaseCallback<Snackbar> mShowCallback
            = new BaseTransientBottomBar.BaseCallback<Snackbar>() {
        @Override
        public void onDismissed(Snackbar transientBottomBar, int event) {
            super.onDismissed(transientBottomBar, event);
            mFloatBar.dispatchShow();
            transientBottomBar.removeCallback(this);
        }
    };

    private BaseTransientBottomBar.BaseCallback<Snackbar> mHideCallback
            = new BaseTransientBottomBar.BaseCallback<Snackbar>() {
        @Override
        public void onDismissed(Snackbar transientBottomBar, int event) {
            super.onDismissed(transientBottomBar, event);
            mFloatBar.dispatchHide();
            transientBottomBar.removeCallback(this);
        }
    };

    private BaseTransientBottomBar.BaseCallback<Snackbar> mDismissCallback
            = new BaseTransientBottomBar.BaseCallback<Snackbar>() {
        @Override
        public void onDismissed(Snackbar transientBottomBar, int event) {
            super.onDismissed(transientBottomBar, event);
            mSnackBar = null;
            transientBottomBar.removeCallback(this);
        }
    };

    Snackbar mSnackBar;
    FloatBar mFloatBar;

    public FloatingSnackBarManager(FloatBar toolbar) {
        mFloatBar = toolbar;
        mFloatBar.addMorphListener(this);
    }

    public boolean hasSnackBar() {
        return mSnackBar != null && mSnackBar.isShownOrQueued();
    }

    public void dismissAndShow() {
        mSnackBar.addCallback(mShowCallback);
        mSnackBar.dismiss();
    }

    public void dismissAndHide() {
        mSnackBar.addCallback(mHideCallback);
        mSnackBar.dismiss();
    }

    public void showSnackBar(final Snackbar snackbar) {
        // If we're currently morphing, show the snackbar after
        if (mFloatBar.mAnimating) {
            // If we're showing a snackbar,
            // remove the callbacks since we'll have a new one
            if (mSnackBar != null && mSnackBar.isShownOrQueued()) {
                mSnackBar.removeCallback(mHideCallback);
                mSnackBar.removeCallback(mShowCallback);
                mSnackBar.removeCallback(mDismissCallback);
            }
            mSnackBar = snackbar;
        } else {
            showSnackBarInternal(snackbar);
        }

    }

    private void showSnackBarInternal(Snackbar snackbar) {

        // If we're showing a snackbar,
        // remove the callbacks since we'll have a new one
        if (mSnackBar != null && mSnackBar.isShownOrQueued()) {
            mSnackBar.removeCallback(mDismissCallback);
            mSnackBar.removeCallback(mHideCallback);
            mSnackBar.removeCallback(mShowCallback);
            mSnackBar.dismiss();
        }

        mSnackBar = snackbar;
        mSnackBar.addCallback(mDismissCallback);

        // We can show the snackbar now if the toolbar isn't showing
        if (!mFloatBar.isShowing()) {
            mSnackBar.show();
            return;
        }

        // If the toolbar is showing, we show the snackbar
        // on top by applying a bottom margin
        View view = snackbar.getView();
        CoordinatorLayout.LayoutParams params
                = (CoordinatorLayout.LayoutParams) view.getLayoutParams();
        params.bottomMargin = mFloatBar.getHeight();
        view.setLayoutParams(params);
        mSnackBar.show();
    }

    @Override
    public void onMorphEnd() {
        if (mSnackBar != null && !mSnackBar.isShownOrQueued()) {
            showSnackBarInternal(mSnackBar);
        }
    }

    @Override
    public void onMorphStart() {

    }

    @Override
    public void onUnmorphStart() {

    }

    @Override
    public void onUnmorphEnd() {
        if (mSnackBar != null && !mSnackBar.isShownOrQueued()) {
            showSnackBarInternal(mSnackBar);
        }
    }
}