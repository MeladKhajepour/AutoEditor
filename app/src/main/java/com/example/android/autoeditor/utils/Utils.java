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
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlend;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.ScriptIntrinsicConvolve3x3;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;

import java.util.ArrayList;
import java.util.List;

public class Utils {

    private static final String PREFERENCES_FILE = "PREFS";
    public static final int CONTRAST_FILTER = 0;
    public static final int EXPOSURE_FILTER = 1;
    public static final int SATURATION_FILTER = 2;
    public static final int SHARPNESS_FILTER = 3;
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

    public static Bitmap setFilter(Bitmap bmp, float value, int filter, Context ctx){

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

                value = (float) Math.pow(2f, value/100*3);
                exposureCm.set(new float[]
                        {
                                value, 0, 0, 0, 0,
                                0, value, 0, 0, 0,
                                0, 0, value, 0, 0,
                                0, 0, 0, 1, 0
                        });
                break;

            case SATURATION_FILTER:

                value = value > 0 ?
                        (float) Math.pow(1.01, value) : (value + 100)/100;

                exposureCm.setSaturation(value);
                break;

            case SHARPNESS_FILTER:

                float radius = Math.abs(value)/4f;
                Bitmap blurred = blurBitmap(bmp, radius, ctx);

                if(value < 0) {
                    return blurred;
                }

                Bitmap sub = subtractBitmaps(bmp, blurred, ctx);

                return addBitmaps(bmp, sub, ctx);

            case 9:

                Bitmap bm = bmp.copy(bmp.getConfig(), true);
                Bitmap bm2;

                for(int i = 0; i <= value/4; i++) {
                    bm2 = sharpen(bm, value, ctx);
                    bm = bm2.copy(bm2.getConfig(), true);
                }
                return bm;


        }

        Bitmap ret = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());

        Canvas canvas = new Canvas(ret);

        cm.setConcat(contrastCm, exposureCm);

        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bmp, 0, 0, paint);

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

    public static Bitmap sharpen(Bitmap original, float progress, Context ctx) {
        float[] radius = new float[] {
                -1,-1,-1,
                -1,8,-1,
                -1,-1,-1
        };

        Bitmap bitmap = Bitmap.createBitmap(
                original.getWidth(), original.getHeight(),
                Bitmap.Config.ARGB_8888);

        RenderScript rs = RenderScript.create(ctx);

        Allocation allocIn = Allocation.createFromBitmap(rs, original);
        Allocation allocOut = Allocation.createFromBitmap(rs, bitmap);

        ScriptIntrinsicConvolve3x3 convolution
                = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs));
        convolution.setInput(allocIn);
        convolution.setCoefficients(radius);

        convolution.forEach(allocOut);
        allocOut.copyTo(bitmap);
        rs.destroy();

        return bitmap;

    }

    private static Bitmap addBitmaps(Bitmap orig, Bitmap blurred, Context context) {
        Bitmap out = orig.copy(orig.getConfig(), true);
        Bitmap blur = blurred.copy(blurred.getConfig(), true);
        //Create renderscript
        RenderScript rs = RenderScript.create(context);

        //Create allocation from Bitmap
        Allocation origAlloc = Allocation.createFromBitmap(rs, out);
        Allocation blurAlloc = Allocation.createFromBitmap(rs, blur);

        //Create script
        ScriptIntrinsicBlend blendScript = ScriptIntrinsicBlend.create(rs, Element.U8_4(rs));
        blendScript.forEachAdd(blurAlloc, origAlloc);
        //now add to orig

        //Copy script result into bitmap
        origAlloc.copyTo(out);

        //Destroy everything to free memory
        origAlloc.destroy();
        blurAlloc.destroy();
        blendScript.destroy();
        rs.destroy();
        return out;
    }

    private static Bitmap subtractBitmaps(Bitmap orig, Bitmap blurred, Context context) {
        Bitmap out = orig.copy(orig.getConfig(), true);
        Bitmap blur = blurred.copy(blurred.getConfig(), true);
        //Create renderscript
        RenderScript rs = RenderScript.create(context);

        //Create allocation from Bitmap
        Allocation origAlloc = Allocation.createFromBitmap(rs, out);
        Allocation blurAlloc = Allocation.createFromBitmap(rs, blur);

        //Create script
        ScriptIntrinsicBlend blendScript = ScriptIntrinsicBlend.create(rs, Element.U8_4(rs));
        blendScript.forEachSubtract(blurAlloc, origAlloc);
        //now add to orig

        //Copy script result into bitmap
        origAlloc.copyTo(out);

        //Destroy everything to free memory
        origAlloc.destroy();
        blurAlloc.destroy();
        blendScript.destroy();
        rs.destroy();
        return out;
    }

    private static Bitmap blurBitmap(Bitmap bitmap, float radius, Context context) {
        if (radius == 0) {
            return bitmap;
        }

        Bitmap out = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        //Create renderscript
        RenderScript rs = RenderScript.create(context);

        //Create allocation from Bitmap
        Allocation allocation = Allocation.createFromBitmap(rs, bitmap);

        Allocation outAlloc = Allocation.createFromBitmap(rs, out);

        //Create script
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        //Set blur radius (maximum 25.0)
        blurScript.setRadius(radius);
        //Set input for script
        blurScript.setInput(allocation);
        //Call script for output allocation
        blurScript.forEach(outAlloc);

        //Copy script result into bitmap
        outAlloc.copyTo(out);

        //Destroy everything to free memory
        allocation.destroy();
        outAlloc.destroy();
        blurScript.destroy();
        rs.destroy();
        return out;
    }
}