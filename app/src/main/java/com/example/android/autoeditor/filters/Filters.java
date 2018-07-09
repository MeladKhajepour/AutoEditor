package com.example.android.autoeditor.filters;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.ScriptIntrinsicConvolve3x3;

import com.example.android.autoeditor.EditPicture;
import com.example.android.autoeditor.utils.Cluster;

import static com.example.android.autoeditor.utils.Utils.CONTRAST_FILTER;
import static com.example.android.autoeditor.utils.Utils.CONVOLUTION_SHARPEN;
import static com.example.android.autoeditor.utils.Utils.EXPOSURE_FILTER;
import static com.example.android.autoeditor.utils.Utils.SATURATION_FILTER;

public class Filters {
    private static Bitmap originalImg;
    private static Bitmap sharpenedImg;
    private static Bitmap finalImg;
    private static Canvas canvas;
    private static float[] brightnessStrength;
    private static float[] contrastStrength;
    private static float[] sharpnessStrength;
    private static float[] saturationStrength;
    private static float[] colorMatrixVals;

    // RenderScript fields
    private static RenderScript rs;
    private static Allocation allocIn;
    private static Allocation allocOut;
    private static ScriptIntrinsicConvolve3x3 convolutionScript;
    private static ScriptIntrinsicBlur blurScript;

    public static void init(Bitmap img) {
        originalImg = img;
        sharpenedImg = img.copy(img.getConfig(), true);
        finalImg = Bitmap.createBitmap(img.getWidth(), img.getHeight(), img.getConfig());
        canvas = new Canvas(finalImg);
        initMatrices();
    }

    private static void initMatrices() {
        brightnessStrength = contrastStrength = saturationStrength = new float[] {
                1, 0, 0, 0, 0,
                0, 1, 0, 0, 0,
                0, 0, 1, 0, 0,
                0, 0, 0, 1, 0
        };
    }

    public static void initRs(EditPicture activity) {
        rs = RenderScript.create(activity);
        allocIn = Allocation.createFromBitmap(rs, originalImg);
        allocOut = Allocation.createFromBitmap(rs, finalImg);

        convolutionScript = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs));
        blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));

        convolutionScript.setInput(allocIn);
        blurScript.setInput(allocIn);
    }

    public static void destroyRs() {
        allocIn.destroy();
        allocOut.destroy();
        blurScript.destroy();
        rs.destroy();
    }

    public static Bitmap applyFilter(Cluster.ActiveFilter filter) {

        switch (filter.filterType) {
            case EXPOSURE_FILTER:
                brightnessFilter(filter.strength);
                break;

            case CONTRAST_FILTER:
                contrastFilter(filter.strength);
                break;

            case CONVOLUTION_SHARPEN:
                sharpnessFilter(filter.strength);
                break;

            case SATURATION_FILTER:
                saturationFilter(filter.strength);
                break;
        }

        concatMatrices();
        createFinalImg();

        return finalImg;
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
        if(strength < 0) {
            blurBitmap(strength/-4);
            return;
        }

        float d = strength/100f;
        float o = d *  (float) Math.sqrt(2);
        float c = (o * 4) + (d * 4) + 1;

        sharpnessStrength = new float[] {
                -d, -o, -d,
                -o, c, -o,
                -d, -o, -d
        };

        convolutionScript.setCoefficients(sharpnessStrength);
        convolutionScript.forEach(allocOut);
        allocOut.copyTo(sharpenedImg);
    }

    private static void saturationFilter(float strength) {
        strength = strength > 0 ? (float) Math.pow(1.01, strength) : (strength + 100)/100;
        ColorMatrix saturationMatrix = new ColorMatrix();
        saturationMatrix.setSaturation(strength);
        saturationStrength = saturationMatrix.getArray();
    }

    private static void blurBitmap(float radius) {
        //Set blur radius (maximum 25.0)
        blurScript.setRadius(radius);
        blurScript.forEach(allocOut);
        allocOut.copyTo(sharpenedImg);
    }

    private static void concatMatrices() {
        ColorMatrix matrix = new ColorMatrix();
        matrix.postConcat(new ColorMatrix(brightnessStrength));
        matrix.postConcat(new ColorMatrix(contrastStrength));
        matrix.postConcat(new ColorMatrix(saturationStrength));
        colorMatrixVals = matrix.getArray();
    }

    private static void createFinalImg() {
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrixVals));

        canvas.drawBitmap(sharpenedImg, 0, 0, paint);
    }
}
