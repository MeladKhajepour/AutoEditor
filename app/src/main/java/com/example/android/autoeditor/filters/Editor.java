package com.example.android.autoeditor.filters;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.example.android.autoeditor.R;

public class Editor extends Filter {

    public Editor(Context context) {
        super(context);
    }

    public Bitmap getActivityBitmap() {

        return originalPreviewBitmap;
    }

    public void setFilterStrength(int sliderProgress, int activeSeekbar) {
        switch (activeSeekbar) {
            case R.id.contrast_seekbar:
                contrastStrength = sliderProgress;
                break;

            case R.id.exposure_seekbar:
                exposureStrength = sliderProgress;
                break;

            case R.id.sharpen_seekbar:
                sharpnessStrength = sliderProgress;
                break;

            case R.id.saturation_seekbar:
                saturationStrength = sliderProgress;
                break;
        }
    }

    public String getSeekbarLabel(int sliderProgress, int activeSeekbar) {
        String labelPrefix = "";
        String labelValue = String.valueOf(sliderProgress);

        switch (activeSeekbar) {
            case R.id.contrast_seekbar:
                labelPrefix = "Contrast: ";
                break;

            case R.id.exposure_seekbar:
                labelPrefix = "Exposure: ";
                labelValue = String.valueOf(sliderProgress/100f*3f);
                break;

            case R.id.sharpen_seekbar:
                labelPrefix = "Sharpness: ";
                break;

            case R.id.saturation_seekbar:
                labelPrefix = "Saturation: ";
                break;
        }

        return labelPrefix + labelValue;
    }

    public static void setImageUri(Uri uri) {
        selectedImageUri = uri;
    }

    public static Uri getImageUri() {
        return selectedImageUri;
    }
}
