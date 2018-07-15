package com.example.android.autoeditor.utils;

import android.os.Build;
import android.os.Handler;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.android.autoeditor.filters.Editor;

import java.util.Locale;

/*
*
* This class is responsible for containing and handling the UI elements for
* filtering the image in EditPicture editor. It is responsible for:
*   Initializing the seekbar
*   Setting the label and strength on seekbar change
*   Setting the filter type
*
 */
public class Cluster {
    private final Cluster _this = this;
    private final Editor editor;
    private final int filterType;
    private final SeekBar seekBar;
    private final TextView textView;
    private final String prefix;
    private final ActiveFilter activeFilter;

    public Cluster(ClusterParams params) {
        editor = params.editor;
        filterType = params.filterType;
        seekBar = params.seekBar;
        textView = params.textView;
        prefix = params.prefix;
        activeFilter = new ActiveFilter();

        initSeekbar();
        updateLabel(prefix, activeFilter.strength);
    }

    private void initSeekbar() {
        seekBar.setMax(200);
        seekBar.setProgress(100);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean inDoubleTapWindow = false;
            int initialProgress = 0;
            Handler handler = new Handler();

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                editor.onSeekBarTouch(activeFilter);
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                activeFilter.strength = progress - 100;
                updateLabel(prefix, activeFilter.strength);

                editor.onSeekBarEdit();
            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
                if(inDoubleTapWindow && Math.abs(initialProgress - seekBar.getProgress()) <= 40) {
                    reset();
                    return;
                }

                inDoubleTapWindow = true;

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        inDoubleTapWindow = false;
                        initialProgress = seekBar.getProgress();
                    }
                }, 250);

                editor.onSeekBarRelease();
            }

            private void reset() {
                initialProgress = 0;
                inDoubleTapWindow = false;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    seekBar.setProgress(100, true);
                } else {
                    seekBar.setProgress(100);
                }
            }
        });
    }

    private void updateLabel(String prefix, float strength) {
        textView.setText(String.format(Locale.getDefault(), prefix + " " + "%d", (int) strength));
    }

    public class ActiveFilter {
        public final int filterType;
        public float strength;

        private ActiveFilter() {
            filterType = _this.filterType;
        }
    }
}
