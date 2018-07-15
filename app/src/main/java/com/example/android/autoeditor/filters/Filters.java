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
    private static Paint paint;
    private static Canvas canvas;
    private static ColorMatrix contrastMatrix;
    private static ColorMatrix brightnessMatrix;
    private static ColorMatrix saturationMatrix;
    private static ColorMatrix finalColorMatrix;
    private static float[] sharpnessStrength;
    private static float blurRadius;
    private static boolean blurred;

    // RenderScript fields
    private static RenderScript rs;
    private static Allocation allocIn;
    private static Allocation allocOut;
    private static ScriptIntrinsicConvolve3x3 convolutionScript;
    private static ScriptIntrinsicBlur blurScript;

    public static void initFilter(Bitmap img) {
        originalImg = img;
        sharpenedImg = img.copy(img.getConfig(), true);
        finalImg = Bitmap.createBitmap(img.getWidth(), img.getHeight(), img.getConfig());
        paint = new Paint();
        canvas = new Canvas(finalImg);
        contrastMatrix = new ColorMatrix();
        brightnessMatrix = new ColorMatrix();
        sharpnessStrength = createSharpenMatrix(0);
        saturationMatrix = new ColorMatrix();
        finalColorMatrix = new ColorMatrix();
        blurred = false;
    }

    /*
     * Called in onStartTrackingTouch in Cluster
     */
    public static void initRs(EditPicture activity) {
        rs = RenderScript.create(activity);
        allocIn = Allocation.createFromBitmap(rs, originalImg);
        allocOut = Allocation.createFromBitmap(rs, sharpenedImg);

        convolutionScript = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs));
        blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));

        convolutionScript.setInput(allocIn);
        blurScript.setInput(allocIn);
    }

    public static Bitmap applyFilter(Cluster.ActiveFilter filter) {

        switch (filter.filterType) {
            case EXPOSURE_FILTER:
                setBrightnessStrength(filter.strength);
                break;

            case CONTRAST_FILTER:
                setContrastStrength(filter.strength);
                break;

            case CONVOLUTION_SHARPEN:
                sharpnessFilter(filter.strength);
                break;

            case SATURATION_FILTER:
                setSaturationStrength(filter.strength);
                break;
        }

        concatMatrices();
        createFinalImg();

        return finalImg;
    }

    /*
     * Called in onStopTrackingTouch in Cluster
     */
    public static void destroyRs() {
        allocIn.destroy();
        allocOut.destroy();
        blurScript.destroy();
        rs.destroy();
    }

    private static void setBrightnessStrength(float strength) {
        strength = (float) Math.pow(2f, strength/100*1.25f);

        brightnessMatrix.set(new float[] {
                        strength, 0, 0, 0, 0,
                        0, strength, 0, 0, 0,
                        0, 0, strength, 0, 0,
                        0, 0, 0, 1, 0
        });
    }

    private static void setContrastStrength(float strength) {
        strength /= 4f;
        strength = strength < 0 ? strength / 2 : strength;
        float contrast = (float) Math.pow((100 + strength) / 100, 2);
        float brightness = 127.5f * (1 - contrast);

        contrastMatrix.set(new float[] {
                        contrast, 0, 0, 0, brightness,
                        0, contrast, 0, 0, brightness,
                        0, 0, contrast, 0, brightness,
                        0, 0, 0, 1, 0
        });
    }

    private static void sharpnessFilter(float strength) {
        if(strength < 0) {
            blurBitmap(strength/-4);
            blurred = true;
            return;
        }

        blurred = false;
        convolutionScript.setCoefficients(createSharpenMatrix(strength));
        convolutionScript.forEach(allocOut);
        allocOut.copyTo(sharpenedImg);
    }

    private static void blurBitmap(float radius) {
        //Set blur radius (maximum 25.0)
        blurRadius = radius;
        blurScript.setRadius(radius);
        blurScript.forEach(allocOut);
        allocOut.copyTo(sharpenedImg);
    }

    private static float[] createSharpenMatrix(float strength) {
        float d = strength/1000f;
        float o = d * 12;
        float c = (o * 4) + (d * 4) + 1;

        sharpnessStrength = new float[] {
                -d, -o, -d,
                -o, c, -o,
                -d, -o, -d
        };

        return sharpnessStrength;
    }

    private static void setSaturationStrength(float strength) {
        strength = strength > 0 ? (float) Math.pow(1.008, strength) : (strength + 100)/100;
        saturationMatrix.setSaturation(strength);
    }

    private static void concatMatrices() {
        finalColorMatrix.reset();
        finalColorMatrix.postConcat(brightnessMatrix);
        finalColorMatrix.postConcat(contrastMatrix);
        finalColorMatrix.postConcat(saturationMatrix);
    }

    private static void createFinalImg() {
        paint.setColorFilter(new ColorMatrixColorFilter(finalColorMatrix));
        canvas.drawBitmap(sharpenedImg, 0, 0, paint);
    }

    /*
     * Called when save button pressed
     *
     * Paint is already set
     */
    public static Bitmap applyFinalEdits(EditPicture activity, Bitmap img) {
        rs = RenderScript.create(activity);
        allocIn = Allocation.createFromBitmap(rs, img);
        allocOut = Allocation.createFromBitmap(rs, img);

        if(blurred) {
            blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            blurScript.setInput(allocIn);
            blurScript.setRadius(blurRadius);
            blurScript.forEach(allocOut);
            allocOut.copyTo(img);
        } else {
            convolutionScript = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs));
            convolutionScript.setInput(allocIn);
            convolutionScript.setCoefficients(sharpnessStrength);
            convolutionScript.forEach(allocOut);
            allocOut.copyTo(img);
        }

        Canvas canvas = new Canvas(img);
        canvas.drawBitmap(img, 0, 0, paint);

        return img;
    }
}
