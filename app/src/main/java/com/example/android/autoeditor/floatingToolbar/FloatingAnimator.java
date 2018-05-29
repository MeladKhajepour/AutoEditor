package com.example.android.autoeditor.floatingToolbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Build;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;

abstract class FloatingAnimator implements AppBarLayout.OnOffsetChangedListener {

    public static final int DELAY_MIN_WIDTH = 300;
    public static final int DELAY_MAX_WIDTH = 600;
    public static final int DELAY_MAX = 100;
    public static final int FAB_MORPH_DURATION = 120;
    public static final int FAB_UNMORPH_DURATION = 200;
    public static final int FAB_UNMORPH_DELAY = 220;
    public static final int CIRCULAR_REVEAL_DURATION = 230; //expanding colour ring
    public static final int CIRCULAR_UNREVEAL_DURATION = 230; //opposite^
    public static final int CIRCULAR_REVEAL_DELAY = 0;
    public static final int CIRCULAR_UNREVEAL_DELAY = 0;
    public static final int TOOLBAR_UNREVEAL_DELAY = 175; //toolbar sliding off screen delay
    public static final int MENU_ANIMATION_DELAY = 100;
    public static final int MENU_ANIMATION_DURATION = 250;

    private float mAppbarOffset;
    private AppBarLayout mAppBar;
    private FloatingActionButton mFab;
    private FloatBar mToolbar;
    private View mRootView;
    private View mContentView;
    private long mDelay;
    private boolean mMoveFabX;
    private FloatingAnimatorListener mAnimationListener;

    public FloatingAnimator(FloatBar toolbar) {
        mToolbar = toolbar;
        mRootView = mToolbar.getRootView();
    }

    public void setFab(FloatingActionButton fab) {
        mFab = fab;
        mFab.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (mToolbar.getWidth() != 0) {
                    mMoveFabX = mFab.getRight() > mToolbar.getWidth() * 0.75
                            || mFab.getLeft() < mToolbar.getHeight() * 0.25;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        mFab.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    } else {
                        //noinspection deprecation
                        mFab.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }
                }
            }
        });
    }

    public void setAppBarLayout(AppBarLayout appBarLayout) {
        mAppBar = appBarLayout;
    }

    public AppBarLayout getAppBar() {
        return mAppBar;
    }

    public FloatingActionButton getFab() {
        return mFab;
    }

    public FloatBar getFloatingToolbar() {
        return mToolbar;
    }

    public void setFloatingAnimatorListener(FloatingAnimatorListener listener) {
        mAnimationListener = listener;
    }

    public FloatingAnimatorListener getAnimationListener() {
        return mAnimationListener;
    }

    public void setContentView(View contentView) {
        mContentView = contentView;
    }

    public float getAppBarOffset() {
        return mAppbarOffset;
    }

    public long getDelay() {
        return mDelay;
    }

    public boolean shouldMoveFabX() {
        return mMoveFabX;
    }

    public View getRootView() {
        return mRootView;
    }

    public void show() {
        if (mMoveFabX) {
            float fabEndX = mFab.getLeft() > mRootView.getWidth() / 2f ?
                    mFab.getLeft() - mFab.getWidth() : mFab.getLeft() + mFab.getWidth();

            // Place view a bit closer to the fab
            mToolbar.setX(fabEndX - mToolbar.getWidth() / 2f + mFab.getWidth());

            // Move FloatingToolbar to the original position
            mToolbar.animate().x(mToolbar.getLeft()).setStartDelay(CIRCULAR_REVEAL_DELAY + mDelay)
                    .setDuration((long) (CIRCULAR_REVEAL_DURATION) + mDelay)
                    .setInterpolator(new AccelerateDecelerateInterpolator());
        }

        // Start showing content view
        if (mContentView != null) {
            mContentView.setAlpha(0f);
            mContentView.setScaleX(0.7f);
            mContentView.animate().alpha(1).scaleX(1f)
                    .setDuration(MENU_ANIMATION_DURATION + mDelay)
                    .setStartDelay(MENU_ANIMATION_DELAY + mDelay)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mAnimationListener.onAnimationFinished();
                            mContentView.animate().setListener(null);
                        }
                    });
        }
    }

    public void hide() {
        if (mMoveFabX) {
            mToolbar.animate().x(mFab.getLeft() - mToolbar.getWidth() / 2f)
                    .setDuration(CIRCULAR_UNREVEAL_DURATION + mDelay)
                    .setStartDelay(TOOLBAR_UNREVEAL_DELAY + mDelay)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .setListener(null);
        }
        if (mContentView != null) {
            mContentView.animate().alpha(0f).scaleX(0.7f)
                    .setStartDelay(CIRCULAR_UNREVEAL_DELAY + mDelay)
                    .setDuration((MENU_ANIMATION_DURATION / 2) + mDelay)
                    .setListener(null);
        }
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        // Fab can be a bit higher than the AppBar when this last covers the whole screen.
        mAppbarOffset = verticalOffset;
    }

    /**
     * Calculate a delay that depends on the screen width so that animations don't happen too quick
     * on larger phones or tablets
     * <p>
     * Base is 300dp.
     * <p>
     * A root view with 300dp as width has 0 delay
     * <p>
     * The max width is 900dp, with a max delay of 150 ms
     */
    public void updateDelay() {
        float minWidth = FloatBar.dpToPixels(mToolbar.getContext(), DELAY_MIN_WIDTH);
        float maxWidth = FloatBar.dpToPixels(mToolbar.getContext(), DELAY_MAX_WIDTH);
        float diff = maxWidth - minWidth;

        int width = mToolbar.getWidth();

        if (width == 0 || width < minWidth) {
            mDelay = 0;
            return;
        }

        if (width > maxWidth) {
            mDelay = DELAY_MAX;
            return;
        }

        mDelay = (long) (DELAY_MAX / diff * (width - minWidth));
    }

    interface FloatingAnimatorListener {
        void onAnimationFinished();
    }
}
