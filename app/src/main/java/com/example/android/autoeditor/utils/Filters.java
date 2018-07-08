package com.example.android.autoeditor.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorMatrix;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import com.example.android.autoeditor.EditPicture;

import static com.example.android.autoeditor.utils.Utils.CONTRAST_FILTER;
import static com.example.android.autoeditor.utils.Utils.CONVOLUTION_SHARPEN;
import static com.example.android.autoeditor.utils.Utils.EXPOSURE_FILTER;
import static com.example.android.autoeditor.utils.Utils.SATURATION_FILTER;

public class Filters {
    private static Bitmap originalImg;
    private static Bitmap finalImg;
    private static float[] brightnessStrength;
    private static float[] contrastStrength;
    private static float[] sharpnessStrength;
    private static float[] saturationStrength;
    private static float[] colorMatrixVals;

    // RenderScript fields
    private static RenderScript rs;
    private static Allocation allocation;
    private static Allocation outAlloc;
    private static ScriptIntrinsicBlur blurScript;

    public static void setBitmap(Bitmap img) {
        originalImg = img;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;

        finalImg = Bitmap.createBitmap(img.getWidth(), img.getHeight(), img.getConfig());
    }

    public static void initRs(EditPicture activity) {
        rs = RenderScript.create(activity);
        allocation = Allocation.createFromBitmap(rs, originalImg);
        outAlloc = Allocation.createFromBitmap(rs, finalImg);
        blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        blurScript.setInput(allocation);
    }

    public static void destroyRs() {
        allocation.destroy();
        outAlloc.destroy();
        blurScript.destroy();
        rs.destroy();
    }

    public static void applyFilter(Cluster.ActiveFilter filter) {

        switch (filter.filterType) {
            case EXPOSURE_FILTER:
                brightnessFilter(filter.strength);
                break;

            case CONTRAST_FILTER:
                contrastFilter(filter.strength);
                break;

            case CONVOLUTION_SHARPEN:
                sharpnessFilter(filter.strength);

            case SATURATION_FILTER:
                saturationFilter(filter.strength);
                break;
        }
    }

    private static void brightnessFilter(float strength) {
        strength = (float) Math.pow(2f, strength/100*3);

        brightnessStrength = new float[] {
                        strength, 0, 0, 0, 0,
                        0, strength, 0, 0, 0,
                        0, 0, strength, 0, 0,
                        0, 0, 0, 1, 0
        };
    }

    private static void contrastFilter(float strength) {
        strength /= 3f;
        strength = strength < 0 ? strength / 2 : strength;
        float contrast = (float) Math.pow((100 + strength) / 100, 2);
        float brightness = 127.5f * (1 - contrast);

        contrastStrength = new float[] {
                        contrast, 0, 0, 0, brightness,
                        0, contrast, 0, 0, brightness,
                        0, 0, contrast, 0, brightness,
                        0, 0, 0, 1, 0
        };
    }

    private static void sharpnessFilter(float strength) {

    }

    private static void saturationFilter(float strength) {
        strength = strength > 0 ? (float) Math.pow(1.01, strength) : (strength + 100)/100;
        ColorMatrix saturationMatrix = new ColorMatrix();
        saturationMatrix.setSaturation(strength);
        saturationStrength = saturationMatrix.getArray();
    }

    private static void blurBitmap(float radius, Context context) {
        if (radius == 0) {
            return;
        }

        //Set blur radius (maximum 25.0)
        blurScript.setRadius(radius);
        blurScript.forEach(outAlloc);
        outAlloc.copyTo(finalImg);
    }
}
