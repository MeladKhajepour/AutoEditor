package com.example.android.autoeditor.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.support.media.ExifInterface;

import java.io.IOException;
import java.io.InputStream;

public class BitmapUtils {//original bitmap should never be loaded in memory until save

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    //bitmap is mutable
    public static Bitmap rotateImageIfRequired(Context ctx, Bitmap img, Uri imageUri) {

        ExifInterface ei;

        try {
            InputStream input = ctx.getContentResolver().openInputStream(imageUri);
            if(input != null) {
                ei = new ExifInterface(input);
            } else {
                throw new Exception("InputStream failed to open");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return img;
        }

        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }

    //bitmap is mutable
    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        img = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);

        return img;
    }
}
