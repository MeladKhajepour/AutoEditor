package com.example.android.autoeditor.utils;

import com.example.android.autoeditor.R;
import com.example.android.autoeditor.filters.Editor;

import static com.example.android.autoeditor.utils.Utils.CONTRAST_FILTER;
import static com.example.android.autoeditor.utils.Utils.CONVOLUTION_SHARPEN;
import static com.example.android.autoeditor.utils.Utils.EXPOSURE_FILTER;
import static com.example.android.autoeditor.utils.Utils.SATURATION_FILTER;

public class ClusterParams {
    public Editor editor;
    public int seekBarId;
    public int textViewId;
    public int prefixId;
    public int filterType;

    public ClusterParams(Editor editor, int seekBarId) {
        this.editor = editor;
        this.seekBarId = seekBarId;

        switch (seekBarId) {
            case R.id.exposure_seekbar:
                textViewId = R.id.exposure_label;
                prefixId = R.string.exposure;
                filterType = EXPOSURE_FILTER;
                break;

            case R.id.contrast_seekbar:
                textViewId = R.id.contrast_label;
                prefixId = R.string.contrast;
                filterType = CONTRAST_FILTER;
                break;

            case R.id.sharpen_seekbar:
                textViewId = R.id.sharpen_label;
                prefixId = R.string.sharpness;
                filterType = CONVOLUTION_SHARPEN;
                break;

            case R.id.saturation_seekbar:
                textViewId = R.id.saturation_label;
                prefixId = R.string.saturation;
                filterType = SATURATION_FILTER;
                break;
        }
    }
}
