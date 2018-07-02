package com.example.android.autoeditor;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.example.android.autoeditor.filters.Editor;
import com.example.android.autoeditor.imageManipulation.GetAndAddMasks;
import com.example.android.autoeditor.tensorFlow.Classifier;
import com.example.android.autoeditor.utils.Cluster;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.example.android.autoeditor.filters.Editor.getTempFile;
import static com.example.android.autoeditor.utils.Utils.getTargetWidth;

/*
*
* This class is for handling the edit pic activity. It is responsible for:
*   Setting the image bitmap
*   Providing the UI for editing the bitmap
*   Saving the final image
*
 */
public class EditPicture extends AppCompatActivity implements Cluster.OnFilterAdjustment {
    Button saveButton;
    ImageView mImageView;
    Cluster exposure, contrast, sharpness, saturation;
    Bitmap editedBitmap;
    Bitmap image;
    Context context;
    private Editor imageEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_picture);

        imageEditor = new Editor(this, getTargetWidth(), getTargetWidth());//Todo
        context = EditPicture.this;

        initUi();
    }

    private void initUi() {

        mImageView = findViewById(R.id.selected_picture_image_view);
        saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                saveImage();
            }
        });

        initClusters();
        updatePreview();
    }

    private void initClusters() {
        exposure = new Cluster(this, R.id.exposure_seekbar);
        contrast = new Cluster(this, R.id.contrast_seekbar);
        sharpness = new Cluster(this, R.id.sharpen_seekbar);
        saturation = new Cluster(this, R.id.saturation_seekbar);
        new LoadDataForActivity(this).execute();
    }

    @Override
    public void updatePreview() {

    }

    private static class LoadDataForActivity extends AsyncTask<Void, Void, Void> {
        ProgressDialog pd;

        //need to do this as Async task was not being static was causing memory leak
        private WeakReference<EditPicture> activityReference;

        // only retain a weak reference to the activity
        LoadDataForActivity(EditPicture context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            EditPicture activity = activityReference.get();

            activity.image = activity.imageEditor.getPreviewBitmap(); //todo do stuff with bitmaputils class
            pd = new ProgressDialog(activity);
            pd.setTitle("Can't rush perfection!");
            pd.setMessage("Identifying your image...");
            pd.setCancelable(false);
            pd.show();
        }
        @Override
        protected Void doInBackground(Void... params) {
            EditPicture activity = activityReference.get();
            //How to use GetAndAddMasks class
            //Initialize the class
            GetAndAddMasks process = new GetAndAddMasks();
            //Get the tensorflow results
            List<Classifier.Recognition> tfResults = process.getTFResults(activity, activity.image);
            //get the mask in a list of bitmaps
            ArrayList<Bitmap> masks = process.getMask(tfResults, activity.image);
            /*Useful if you want to tell user the object identified*/
            // ArrayList<String> identifiedObjects = process.getObjects(tfResults);
            //add all the edited bitmaps back
            activity.editedBitmap = process.addBitmapBackToOriginal(tfResults, masks, activity.image);
            return null;
        }

        @SuppressWarnings("InfiniteRecursion")
        @Override
        protected void onProgressUpdate(Void... values) {
            onProgressUpdate(values);
        }


        @Override
        protected void onPostExecute(Void result) {
            EditPicture activity = activityReference.get();
            activity.mImageView.setImageBitmap(activity.editedBitmap);
            pd.dismiss();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.overflow_menu, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i); //goes back to main activity
    }

    private void saveImage(){
        File imageToSaveFile = getTempFile();

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(Objects.requireNonNull(imageToSaveFile));
            image.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        addToGallery(imageToSaveFile);
    }

    void addToGallery(File imageFile) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(imageFile));//todo cant it just be imageUri?
        this.sendBroadcast(mediaScanIntent);
    }
}
