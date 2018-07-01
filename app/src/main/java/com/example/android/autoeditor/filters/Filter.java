package com.example.android.autoeditor.filters;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import java.io.File;

abstract class Filter {
    private Context context;
    static Uri selectedImageUri;
    static File tempFile;
    int contrastStrength, exposureStrength, sharpnessStrength, saturationStrength;
    Bitmap originalBitmap, originalPreviewBitmap;

    Filter(Context context) {
        this.context = context;
    }
}
