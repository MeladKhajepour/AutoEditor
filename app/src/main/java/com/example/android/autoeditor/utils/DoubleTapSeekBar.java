package com.example.android.autoeditor.utils;

import android.content.Context;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.util.Timer;
import java.util.TimerTask;

public class DoubleTapSeekBar extends android.support.v7.widget.AppCompatSeekBar {

    private GestureDetectorCompat _detector;
    private DoubleTapSeekBarEvent _doubleTapEvent;
    private Timer _timer = new Timer();
    private boolean _gesturesEnabled = true;
    private int _debounceTimeMs = 200;

    public DoubleTapSeekBar(Context context) {
        super(context);
        _detector = new GestureDetectorCompat(context, new DoubleTapGestureListener(this));
    }

    public DoubleTapSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        _detector = new GestureDetectorCompat(context, new DoubleTapGestureListener(this));
    }

    public DoubleTapSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        _detector = new GestureDetectorCompat(context, new DoubleTapGestureListener(this));
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (_gesturesEnabled) {
            _detector.onTouchEvent(e);
            return super.onTouchEvent(e);
        } else {
            return false;
        }
    }

    public void setDoubleTapEvent(DoubleTapSeekBarEvent event) {
        _doubleTapEvent = event;
    }

    private class ReenableTouchEventsTask extends TimerTask {
        @Override
        public void run() {
            _gesturesEnabled = true;
        }
    }

    private class DoubleTapGestureListener extends
            GestureDetector.SimpleOnGestureListener {

        private DoubleTapSeekBar _seekBar;

        DoubleTapGestureListener(DoubleTapSeekBar seekBar) {
            _seekBar = seekBar;
        }

        @Override
        public boolean onDoubleTap(MotionEvent event) {
            _doubleTapEvent.onDoubleTap(_seekBar);
            _gesturesEnabled = false;
            _timer.schedule(new ReenableTouchEventsTask(), _debounceTimeMs);
            return true;
        }
    }
}
