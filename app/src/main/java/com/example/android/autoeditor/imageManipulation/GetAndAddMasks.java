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
    private static final int TF_OD_API_INPUT_SIZE = 224; //todo- Q: what does the size do?
    private static final String TF_OD_API_MODEL_FILE = "frozen_inference_graph.pb";
    private static final String TF_OD_API_LABELS_FILE = "coco_labels_list.txt";
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.9f;

    //To get all identified object's mask
    public  List<Classifier.Entity> getImgEntities(Context context, Bitmap img){
        Classifier detector = initializeDetector(context);
        Bitmap resizedImg = null;

        if (img != null) {
            resizedImg = imgResizer(cloneBitmap(img), TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE);
        } else {
            //todo something if bitmap is null
        }

        List<Classifier.Entity> entities = detector.recognizeEntities(resizedImg);

        return mapEntityLocationsToOriginalImg(entities, img);
    }

    public ArrayList<Bitmap> getMasks(List<Classifier.Entity> entities, Bitmap resizedImg){
        ArrayList<Bitmap> entityMask = new ArrayList<>();

        for (final Classifier.Entity entity : entities) {
            RectF location = entity.getLocation();
            Rect locationRect = new Rect();
            location.round(locationRect);
            Bitmap croppedBitmap = Bitmap.createBitmap(resizedImg, locationRect.left,
                    locationRect.top,
                    locationRect.right - locationRect.left ,
                    locationRect.bottom - locationRect.top);
            /*The next line fills the bitmap with color black. I used it to test that edited bitmap was getting added to final bitmap*/
            //croppedBitmap.eraseColor(Color.BLACK);
            entityMask.add(croppedBitmap);
        }
        return entityMask;
    }

    public ArrayList<String> getObjects(List<Classifier.Entity> results){
        ArrayList<String> listOfIdentifiedObjects = new ArrayList<>();
        for (final Classifier.Entity result : results) {
            String identifiedObjects = result.getTitle();
            listOfIdentifiedObjects.add(identifiedObjects);
        }
        return listOfIdentifiedObjects;
    }

    public Bitmap addBitmapBackToOriginal(List<Classifier.Entity> entities, ArrayList<Bitmap> editedMasks, Bitmap scaledImg){
        int i = 0;
        Bitmap editedBitmap = cloneBitmap(scaledImg);
        for (final Classifier.Entity entity : entities) {
            Bitmap editedMask = editedMasks.get(i);
            RectF location = entity.getLocation();
            Rect locationRect = new Rect();
            location.round(locationRect);
            final Canvas canvas = new Canvas(editedBitmap);
            canvas.drawBitmap(editedMask,null, locationRect,null);
            i++;

        }
        return editedBitmap;
    }

    private Classifier initializeDetector(Context ctx){
        Classifier detector = null;
        try {
            detector = TensorFlowObjectDetectionAPIModel.create(
                    ctx.getAssets(), TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, TF_OD_API_INPUT_SIZE);
        } catch (IOException e) {
            e.printStackTrace();
            Toast toast =
                    Toast.makeText(
                            ctx, "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
        }
        return detector;
    }

    private List<Classifier.Entity> mapEntityLocationsToOriginalImg(List<Classifier.Entity> entities, Bitmap resizedBitmap){
        List<Classifier.Entity> mappedEntities = new LinkedList<>();
        Matrix cropToFrameTransform = new Matrix();

        for (Classifier.Entity entity : entities) {
            RectF location = entity.getLocation();
            if (location != null && entity.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {
                location.left /=  TF_OD_API_INPUT_SIZE/ (float) resizedBitmap.getWidth();
                location.right /= TF_OD_API_INPUT_SIZE/ (float) resizedBitmap.getWidth();
                location.top /= TF_OD_API_INPUT_SIZE/ (float) resizedBitmap.getHeight();
                location.bottom /= TF_OD_API_INPUT_SIZE/ (float) resizedBitmap.getHeight();

                cropToFrameTransform.mapRect(location);
                entity.setLocation(location);
                mappedEntities.add(entity);
            }
        }
        return mappedEntities;
    }

    private Bitmap cloneBitmap(Bitmap bitmap){
        return bitmap.copy(bitmap.getConfig(), true);
    }

    private Bitmap imgResizer(Bitmap originalImg, int newWidth, int newHeight) {// todo see if the pic has to be square otherwise keep ratios
        Bitmap scaledImg = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
        float scaleX;
        float scaleY;

        scaleX = newWidth / (float) originalImg.getWidth();
        scaleY = newHeight / (float) originalImg.getHeight();
        float pivotX = 0;
        float pivotY = 0;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(scaleX, scaleY, pivotX, pivotY);

        Canvas canvas = new Canvas(scaledImg);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(originalImg, 0, 0, new Paint(Paint.FILTER_BITMAP_FLAG));

        return scaledImg;
    }
}
