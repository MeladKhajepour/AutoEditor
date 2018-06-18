package com.example.android.autoeditor.filters;

import android.graphics.Bitmap;

abstract class Filter {
    int contrastStrength, exposureStrength, sharpnessStrength, saturationStrength;
    Bitmap originalBitmap, originalPreviewBitmap;


    Bitmap getActivityBitmap() {
        Bitmap bitmapToEdit = originalPreviewBitmap.copy(originalPreviewBitmap.getConfig(), true);
        return bitmapToEdit;//todo
    }
}
