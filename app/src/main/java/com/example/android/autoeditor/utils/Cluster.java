package com.example.android.autoeditor.utils;

import android.os.Build;
import android.os.Handler;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.android.autoeditor.EditPicture;
import com.example.android.autoeditor.filters.Editor;

import java.util.Locale;

import static com.example.android.autoeditor.utils.Utils.CONVOLUTION_SHARPEN;

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
        activeFilter = new ActiveFilter();

        EditPicture activity = editor.getActivity();
        seekBar = activity.findViewById(params.seekBarId);
        textView = activity.findViewById(params.textViewId);
        prefix = activity.getResources().getString(params.prefixId);

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
                editor.setActiveFilter(activeFilter);
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                activeFilter.strength = progress - 100;

                editor.applyFilter();
                updateLabel(prefix, activeFilter.strength);
                updatePreview();
            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
                if(inDoubleTapWindow && Math.abs(initialProgress - seekBar.getProgress()) <= 25) {
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

                if(filterType == CONVOLUTION_SHARPEN) {
                    editor.destroyRs();
                }

                editor.setActiveFilter(null);
            }

            private void reset() {
                initialProgress = 0;
                inDoubleTapWindow = false;
                activeFilter.strength = 0;

                resetCluster();
                updateLabel(prefix, activeFilter.strength);
            }
        });
    }

    private void updateLabel(String prefix, float strength) {
        textView.setText(String.format(Locale.getDefault(), prefix + " " + "%d", (int) strength));
    }

    private void updatePreview() {
        editor.updatePreview();
    }

    private void resetCluster() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            seekBar.setProgress(100, true);
        } else {
            seekBar.setProgress(100);
        }

        updatePreview();
    }

    public class ActiveFilter {
        public final Cluster cluster;
        public final int filterType;
        public float strength;

        private ActiveFilter() {
            cluster = _this;
            filterType = _this.filterType;
        }
    }
}
