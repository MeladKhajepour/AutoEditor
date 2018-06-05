package com.example.android.autoeditor.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;

import com.example.android.autoeditor.EditPicture;

import java.util.ArrayList;
import java.util.List;

public class Utils {

    private static final String PREFERENCES_FILE = "PREFS";
    public static final int CONTRAST_FILTER = 0;
    public static final int EXPOSURE_FILTER = 1;
    public static final int PERMISSIONS_REQUEST_ID = 111;

    private static ColorMatrix contrastCm = new ColorMatrix();
    private static ColorMatrix exposureCm = new ColorMatrix();

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

    public static Bitmap setFilter(Bitmap bmp, float value, int filter){

        ColorMatrix cm = new ColorMatrix();

        switch (filter) {
            case CONTRAST_FILTER:
                value /= 3f;
                value = value < 0 ? value / 2 : value;
                float contrast = (float) Math.pow((100 + value) / 100, 2);
                float brightness = 127.5f * (1 - contrast);

                contrastCm.set(new float[]
                        {
                                contrast, 0, 0, 0, brightness,
                                0, contrast, 0, 0, brightness,
                                0, 0, contrast, 0, brightness,
                                0, 0, 0, 1, 0
                        });
                break;

            case EXPOSURE_FILTER:

                value = (float) Math.pow(1/2f, -1*value/100*3);
                exposureCm.set(new float[]
                        {
                                value, 0, 0, 0, 0,
                                0, value, 0, 0, 0,
                                0, 0, value, 0, 0,
                                0, 0, 0, 1, 0
                        });
                break;

        }

        Bitmap ret = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());

        Canvas canvas = new Canvas(ret);

        cm.setConcat(contrastCm, exposureCm);

        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bmp, 0, 0, paint);

        return ret;
    }

    public Bitmap changeBitmapContrast(Bitmap bmp, float contrast)
    {
        float scale = contrast + 1.f;
        float translate = (-.5f * scale + .5f) * 255.f;
        ColorMatrix cm = new ColorMatrix(new float[]
                {
                        scale, 0, 0, 0, translate,
                        0, scale, 0, 0, translate,
                        0, 0, scale, 0, translate,
                        0, 0, 0, 1, 0
                });

        Bitmap ret = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());

        Canvas canvas = new Canvas(ret);

        RectF drawRect=new RectF();
        drawRect.set(0,0,bmp.getWidth(),bmp.getHeight());

        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bmp, null, drawRect, paint);

        return ret;
    }

    public Bitmap changeBitmapBrightness(Bitmap bmp, float value)
    {
        ColorMatrix cm = new ColorMatrix(new float[]
                {
                        1, 0, 0, 0, value,
                        0, 1, 0, 0, value,
                        0, 0, 1, 0, value,
                        0, 0, 0, 1, 0,
                        0, 0, 0, 0, 1
                });

        Bitmap ret = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());

        Canvas canvas = new Canvas(ret);

        RectF drawRect=new RectF();
        drawRect.set(0,0,bmp.getWidth(),bmp.getHeight());

        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bmp, null, drawRect, paint);

        return ret;
    }

    public Bitmap changeBitmapHue(Bitmap bmp, float value)
    {
        float cosVal = (float) Math.cos(value);
        float sinVal = (float) Math.sin(value);
        float lumR = 0.213f;
        float lumG = 0.715f;
        float lumB = 0.072f;
        ColorMatrix cm = new ColorMatrix(new float[]
                {
                        lumR + cosVal * (1 - lumR) + sinVal * (-lumR), lumG + cosVal * (-lumG) + sinVal * (-lumG), lumB + cosVal * (-lumB) + sinVal * (1 - lumB), 0, 0,
                        lumR + cosVal * (-lumR) + sinVal * (0.143f), lumG + cosVal * (1 - lumG) + sinVal * (0.140f), lumB + cosVal * (-lumB) + sinVal * (-0.283f), 0, 0,
                        lumR + cosVal * (-lumR) + sinVal * (-(1 - lumR)), lumG + cosVal * (-lumG) + sinVal * (lumG), lumB + cosVal * (1 - lumB) + sinVal * (lumB), 0, 0,
                        0f, 0f, 0f, 1f, 0f,
                        0f, 0f, 0f, 0f, 1f
                });

        Bitmap ret = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());

        Canvas canvas = new Canvas(ret);

        RectF drawRect=new RectF();
        drawRect.set(0,0,bmp.getWidth(),bmp.getHeight());

        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bmp, null, drawRect, paint);

        return ret;
    }

    public Bitmap changeBitmapSaturation(Bitmap bmp, float value)
    {
        float x = 1 + ((value > 0) ? 3 * value / 100 : value / 100);
        float lumR = 0.3086f;
        float lumG = 0.6094f;
        float lumB = 0.0820f;
        ColorMatrix cm = new ColorMatrix(new float[]
                {
                        lumR * (1 - x) + x, lumG * (1 - x), lumB * (1 - x), 0, 0,
                        lumR * (1 - x), lumG * (1 - x) + x, lumB * (1 - x), 0, 0,
                        lumR * (1 - x), lumG * (1 - x), lumB * (1 - x) + x, 0, 0,
                        0, 0, 0, 1, 0,
                        0, 0, 0, 0, 1
                });

        Bitmap ret = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());

        Canvas canvas = new Canvas(ret);

        RectF drawRect=new RectF();
        drawRect.set(0,0,bmp.getWidth(),bmp.getHeight());

        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bmp, null, drawRect, paint);

        return ret;
    }

    public Bitmap changeBitmapTemperature(Bitmap bmp, int r, int g, int b)
    {
        ColorMatrix cm = new ColorMatrix(new float[]
                {
                        r / 255.0f, 0, 0, 0, 0,
                        0, g / 255.0f, 0, 0, 0,
                        0, 0, b / 255.0f, 0, 0,
                        0, 0, 0, 1, 0
                });

        Bitmap ret = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());

        Canvas canvas = new Canvas(ret);

        RectF drawRect=new RectF();
        drawRect.set(0,0,bmp.getWidth(),bmp.getHeight());

        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bmp, null, drawRect, paint);

        return ret;
    }

    public Bitmap changeBitmapExposure(Bitmap bmp, float value)
    {
        ColorMatrix cm = new ColorMatrix(new float[]
                {
                        value, 0, 0, 0, 0,
                        0, value, 0, 0, 0,
                        0, 0, value, 0, 0,
                        0, 0, 0, 1, 0,
                });

        Bitmap ret = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());

        Canvas canvas = new Canvas(ret);

        RectF drawRect=new RectF();
        drawRect.set(0,0,bmp.getWidth(),bmp.getHeight());

        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bmp, null, drawRect, paint);

        return ret;
    }

    public static void applyFilters() {

    }
}