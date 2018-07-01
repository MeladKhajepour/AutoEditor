package com.example.android.autoeditor.filters;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.example.android.autoeditor.R;
import com.example.android.autoeditor.utils.BitmapUtils;

import java.io.File;

public class Editor extends Filter {

    public Editor(Context context, int width, int height) {
        super(context);

        originalPreviewBitmap = BitmapUtils.resizeBitmapToPreview(context, width, height);
    }

    public Bitmap getPreviewBitmap() {

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

    public static void setImageUri(Uri uri) {
        selectedImageUri = uri;
    }

    public static Uri getImageUri() {
        return selectedImageUri;
    }

    public static void setTempFile(File file) {
        tempFile = file;
    }

    public static File getTempFile() {
        return tempFile;
    }
}
