package com.example.android.autoeditor.filters;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

abstract class Filter {
    Context context;
    static Uri selectedImageUri;
    int contrastStrength, exposureStrength, sharpnessStrength, saturationStrength;
    Bitmap originalBitmap, originalPreviewBitmap;

    Filter(Context context) {
        this.context = context;
    }
}
