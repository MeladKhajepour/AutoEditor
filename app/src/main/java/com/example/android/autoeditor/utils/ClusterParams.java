package com.example.android.autoeditor.utils;

import android.widget.SeekBar;
import android.widget.TextView;

import com.example.android.autoeditor.EditPicture;
import com.example.android.autoeditor.R;
import com.example.android.autoeditor.filters.Editor;

import static com.example.android.autoeditor.utils.Utils.CONTRAST_FILTER;
import static com.example.android.autoeditor.utils.Utils.CONVOLUTION_SHARPEN;
import static com.example.android.autoeditor.utils.Utils.EXPOSURE_FILTER;
import static com.example.android.autoeditor.utils.Utils.SATURATION_FILTER;

public class ClusterParams {
    public Editor editor;
    public SeekBar seekBar;
    public TextView textView;
    public String prefix;
    public int filterType;

    public ClusterParams(Editor editor, int seekBarId) {
        this.editor = editor;
        EditPicture activity = editor.getActivity();

        seekBar = activity.findViewById(seekBarId);

        switch (seekBarId) {
            case R.id.exposure_seekbar:
                textView = activity.findViewById(R.id.exposure_label);
                prefix = activity.getResources().getString(R.string.exposure);
                filterType = EXPOSURE_FILTER;
                break;

            case R.id.contrast_seekbar:
                textView = activity.findViewById(R.id.contrast_label);
                prefix = activity.getResources().getString(R.string.contrast);
                filterType = CONTRAST_FILTER;
                break;

            case R.id.sharpen_seekbar:
                textView = activity.findViewById(R.id.sharpen_label);
                prefix = activity.getResources().getString(R.string.sharpness);
                filterType = CONVOLUTION_SHARPEN;
                break;

            case R.id.saturation_seekbar:
                textView = activity.findViewById(R.id.saturation_label);
                prefix = activity.getResources().getString(R.string.saturation);
                filterType = SATURATION_FILTER;
                break;
        }
    }
}
