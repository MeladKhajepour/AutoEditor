package com.example.android.autoeditor.imageManipulation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.widget.Toast;

import com.example.android.autoeditor.tensorFlow.Classifier;
import com.example.android.autoeditor.tensorFlow.TensorFlowObjectDetectionAPIModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class GetAndAddMasks {
    private static final int TF_OD_API_INPUT_SIZE = 224;
    private static final String TF_OD_API_MODEL_FILE =
            "frozen_inference_graph.pb";
    private static final String TF_OD_API_LABELS_FILE = "coco_labels_list.txt";
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;

    //To get all identified object's mask
    public  List<Classifier.Recognition> getTFResults(Context context, Bitmap bitmapForTF){
        Classifier detector = initializeDetector(context);

        Bitmap scaledBitmap = null;
        if (bitmapForTF != null) {
            scaledBitmap = cloneBitmap(bitmapForTF);
        }
        if (bitmapForTF != null) {
            bitmapForTF = bitmapResizer(bitmapForTF,TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE);
        }
        List<Classifier.Recognition> tfResults = detectWithTensorFlow(detector, bitmapForTF);
        return applyDetectedObjectToImage(tfResults, scaledBitmap);
    }

    public ArrayList<Bitmap> getMask(List<Classifier.Recognition> results,Bitmap scaledBitmap){
        ArrayList<Bitmap> masks = new ArrayList<>();
        for (final Classifier.Recognition result : results) {
            RectF location = result.getLocation();
            Rect locationRect = new Rect();
            location.round(locationRect);
            Bitmap croppedBitmap = Bitmap.createBitmap(scaledBitmap, locationRect.left,
                    locationRect.top,
                    locationRect.right - locationRect.left ,
                    locationRect.bottom - locationRect.top);
            /*The next line fills the bitmap with color black. I used it to test that edited bitmap was getting added to final bitmap*/
           // croppedBitmap.eraseColor(Color.BLACK);
            masks.add(croppedBitmap);
        }
        return masks;
    }

    public ArrayList<String> getObjects(List<Classifier.Recognition> results){
        ArrayList<String> listOfIdentifiedObjects = new ArrayList<>();
        for (final Classifier.Recognition result : results) {
            String identifiedObjects = result.getTitle();
            listOfIdentifiedObjects.add(identifiedObjects);
        }
        return listOfIdentifiedObjects;
    }

    public Bitmap addBitmapBackToOriginal(List<Classifier.Recognition> results, ArrayList<Bitmap> editedMasks, Bitmap scaledBitmap){
        int i = 0;
        Bitmap editedBitmap = cloneBitmap(scaledBitmap);
        for (final Classifier.Recognition result : results) {
            Bitmap editedMask = editedMasks.get(i);
            RectF location = result.getLocation();
            Rect locationRect = new Rect();
            location.round(locationRect);
            final Canvas canvas = new Canvas(editedBitmap);
            canvas.drawBitmap(editedMask,null,locationRect,null);
            i++;

        }
        return editedBitmap;
    }

    private Classifier initializeDetector(Context ctx){
        Classifier detector = null;
        try {
            detector = TensorFlowObjectDetectionAPIModel.create(
                    ctx.getAssets(), TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, TF_OD_API_INPUT_SIZE);
        } catch (final IOException e) {
            Toast toast =
                    Toast.makeText(
                            ctx, "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
        }
        return detector;
    }

    private List<Classifier.Recognition> detectWithTensorFlow(Classifier detector, Bitmap croppedBitmap){
        return detector.recognizeImage(croppedBitmap);
    }

    private List<Classifier.Recognition> applyDetectedObjectToImage(List<Classifier.Recognition> results, Bitmap scaledBitmap){
        List<Classifier.Recognition> mappedRecognitions = new LinkedList<>();
        Matrix cropToFrameTransform = new Matrix();

        mappedRecognitions.clear();
        for (final Classifier.Recognition result : results) {
            RectF location = result.getLocation();
            if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {
                location.left /=  TF_OD_API_INPUT_SIZE/ (float) scaledBitmap.getWidth();
                location.right /= TF_OD_API_INPUT_SIZE/ (float) scaledBitmap.getWidth();
                location.top /= TF_OD_API_INPUT_SIZE/ (float) scaledBitmap.getHeight();
                location.bottom /= TF_OD_API_INPUT_SIZE/ (float) scaledBitmap.getHeight();
                cropToFrameTransform.mapRect(location);
                result.setLocation(location);
                mappedRecognitions.add(result);
            }
        }
        return mappedRecognitions;
    }

    private Bitmap cloneBitmap(Bitmap bitmap){
        return bitmap.copy(bitmap.getConfig(), true);
    }

    private Bitmap bitmapResizer(Bitmap bitmap,int newWidth,int newHeight) {
        Bitmap scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
        float scaleX;
        float scaleY;

        scaleX = newWidth / (float) bitmap.getWidth();
        scaleY = newHeight / (float) bitmap.getHeight();
        float pivotX = 0;
        float pivotY = 0;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(scaleX, scaleY, pivotX, pivotY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bitmap, 0, 0, new Paint(Paint.FILTER_BITMAP_FLAG));

        return scaledBitmap;

    }
}
