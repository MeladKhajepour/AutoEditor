package com.example.android.autoeditor.utils;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;

import java.util.ArrayList;
import java.util.List;

public class Utils {

    private static final String PREFERENCES_FILE = "PREFS";
    public static final int PERMISSIONS_REQUEST_ID = 111;

    public static void requestMissingPermissions(Activity ctx) {
        List<String> permissionsList = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {//granted perm int = 0
            permissionsList.add(Manifest.permission.CAMERA);
        }

        if (!permissionsList.isEmpty()) {
            requestPermission(ctx, permissionsList);
        }
    }

    public static void requestPermission(Activity ctx, List<String> permission) {
        ActivityCompat.requestPermissions(ctx, permission.toArray(new String[permission.size()]), PERMISSIONS_REQUEST_ID);
    }

    public static boolean allPermissionsGranted(Activity ctx) {
        if(ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            return false;
        }

        return true;
    }

    public static Drawable tintMyDrawable(Drawable drawable, int color) {
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, color);
        DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN);
        return drawable;
    }

    public static String readSharedSetting(Context ctx, String settingName, String defaultValue) {
        SharedPreferences sharedPref = ctx.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        return sharedPref.getString(settingName, defaultValue);
    }

    public static void saveSharedSetting(Context ctx, String settingName, String settingValue) {
        SharedPreferences sharedPref = ctx.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(settingName, settingValue);
        editor.apply();
    }

    public static void darkenStatusBar(Activity activity, int baseColour) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            float[] hsv = new float[3];
            Color.colorToHSV(baseColour, hsv);
            hsv[2] *= 0.8f;
            activity.getWindow().setStatusBarColor(Color.HSVToColor(hsv));
        }

    }

    public static Bitmap setContrast(Bitmap bmp, float value){

        value = value < 0 ? value / 2 : value;
        float contrast = (float) Math.pow((100 + value) / 100, 2);
        float brightness = 127.5f * (1 - contrast);

        ColorMatrix cm = new ColorMatrix(new float[]
                {
                        contrast, 0, 0, 0, brightness,
                        0, contrast, 0, 0, brightness,
                        0, 0, contrast, 0, brightness,
                        0, 0, 0, 1, 0
                });

        Bitmap ret = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());

        Canvas canvas = new Canvas(ret);

        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bmp, 0, 0, paint);

        return ret;
    }
}