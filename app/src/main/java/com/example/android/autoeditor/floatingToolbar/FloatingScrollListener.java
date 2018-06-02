package com.example.android.autoeditor.floatingToolbar;

import android.support.v7.widget.RecyclerView;

class FloatingScrollListener extends RecyclerView.OnScrollListener {

    private FloatBar mToolbar;

    FloatingScrollListener(FloatBar toolbar) {
        mToolbar = toolbar;
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        if (newState == RecyclerView.SCROLL_STATE_DRAGGING && mToolbar.isShowing()) {
            mToolbar.hide();
        }
    }

}
