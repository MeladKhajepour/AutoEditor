package com.example.android.autoeditor.filters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.example.android.autoeditor.R;
import com.example.android.autoeditor.utils.Cluster;

import java.io.IOException;
import java.io.InputStream;

import static com.example.android.autoeditor.utils.BitmapUtils.calculateInSampleSize;
import static com.example.android.autoeditor.utils.BitmapUtils.rotateImageIfRequired;
import static com.example.android.autoeditor.utils.Utils.getPreviewHeight;
import static com.example.android.autoeditor.utils.Utils.getPreviewWidth;

public class Editor {
    private static Uri contentUri;
    private Context ctx;
    private Cluster exposure, contrast, sharpness, saturation;
    private int contrastStrength, exposureStrength, sharpnessStrength, saturationStrength;
    private Bitmap originalImg, mutablePreviewImg;//Original picture should never be laoded in memory till save

    public Editor(Context ctx) throws IOException {
        this.ctx = ctx;
        createPreviewBitmap();
    }

    public Bitmap getOriginalBitmap() {
        return originalImg;
    }

    public Bitmap getPreviewBitmap() {
        return mutablePreviewImg;
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

    private void createPreviewBitmap() throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        options.inJustDecodeBounds = true;

        InputStream is = ctx.getContentResolver().openInputStream(contentUri);
        BitmapFactory.decodeStream(is, null, options);
        is.close();

        options.inSampleSize = calculateInSampleSize(options, getPreviewWidth(), getPreviewHeight());
        options.inJustDecodeBounds = false;

        is = ctx.getContentResolver().openInputStream(contentUri);
        mutablePreviewImg = BitmapFactory.decodeStream(is, null, options);
        is.close();

        mutablePreviewImg = rotateImageIfRequired(ctx, mutablePreviewImg, contentUri);
    }

    public static void setContentUri(Uri uri) {
        contentUri = uri;
    }
}
