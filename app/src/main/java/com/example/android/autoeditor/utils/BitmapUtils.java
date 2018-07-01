package com.example.android.autoeditor.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.support.media.ExifInterface;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static com.example.android.autoeditor.filters.Editor.getImageUri;

public class BitmapUtils {

    public static Bitmap resizeBitmapToPreview(Context ctx, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap img = null;

        try {
            InputStream imageStream = ctx.getContentResolver().openInputStream(getImageUri());
            BitmapFactory.decodeStream(imageStream, null, options);
            Objects.requireNonNull(imageStream).close();

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            imageStream = ctx.getContentResolver().openInputStream(getImageUri());
            img = BitmapFactory.decodeStream(imageStream, null, options);
            options.inJustDecodeBounds = true;

        } catch (Exception e) {
            e.printStackTrace();
        }

        Bitmap i = null;

        try {
            i = rotateImageIfRequired(ctx, img, getImageUri());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return i;
    }

    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private static Bitmap rotateImageIfRequired(Context ctx, Bitmap img, Uri selectedImage) throws IOException {

        InputStream input = ctx.getContentResolver().openInputStream(selectedImage);
        ExifInterface ei;
        if (Build.VERSION.SDK_INT > 23)
            ei = new ExifInterface(Objects.requireNonNull(input));
        else
            ei = new ExifInterface(selectedImage.getPath());

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

    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }
}
