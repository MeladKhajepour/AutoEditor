package com.example.android.autoeditor;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.android.autoeditor.filters.Editor;
import com.example.android.autoeditor.imageManipulation.GetAndAddMasks;
import com.example.android.autoeditor.tensorFlow.Classifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/*
*
* This class is for handling the edit pic activity. It is responsible for:
*   Setting the image bitmap
*   Providing the UI for editing the bitmap
*   Saving the final image
* ******should only have to worry about initializing the image, setting it and updating it. no logic
 */
public class EditPicture extends AppCompatActivity implements Editor.OnSaveListener {
    private Editor imageEditor;
    private ImageView mImageView;
//    private Bitmap editedBitmap;
//    private Bitmap previewImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_picture);

        try {
            imageEditor = new Editor(this);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Something went wrong with opening your image ...", Toast.LENGTH_LONG).show();
            finish();
        }

        initUi();
    }

    private void initUi() {

        mImageView = findViewById(R.id.selected_picture_image_view);

        Button saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    imageEditor.saveImage();
                } catch (IOException e) {
                    Toast.makeText(getBaseContext(), "Something went wrong with saving your image ...", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        });

        updatePreview();
    }

    public void updatePreview() { //todo make background task
        mImageView.setImageBitmap(imageEditor.getPreviewBitmap());
    }

    @Override
    public void onSave(File savedImg) {
        Snackbar.make(mImageView, "Image saved", Snackbar.LENGTH_LONG).setAction("View", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //todo action to view
            }
        }).show();
    }

//    private static class LoadDataForActivity extends AsyncTask<Void, Void, Void> {
//        ProgressDialog pd;
//
//        //need to do this as Async task was not being static was causing memory leak
//        private WeakReference<EditPicture> activityReference;
//
//        // only retain a weak reference to the activity
//        LoadDataForActivity(Context context) {
//            activityReference = new WeakReference<>((EditPicture) context);
//        }
//
//        @Override
//        protected void onPreExecute() {
//            EditPicture _this = activityReference.get();
//
//            _this.previewImg = _this.imageEditor.getPreviewBitmap(); //todo do stuff with bitmaputils class
//            //todo also maybe null checking on image
//            pd = new ProgressDialog(_this);
//            pd.setTitle("Can't rush perfection!");
//            pd.setMessage("Identifying your image...");
//            pd.setCancelable(false);
//            pd.show();
//        }
//        @Override
//        protected Void doInBackground(Void... params) {
//            EditPicture _this = activityReference.get();
//            //How to use GetAndAddMasks class
//            //Initialize the class
//            GetAndAddMasks process = new GetAndAddMasks();
//            //Get the tensorflow results
//            List<Classifier.Entity> entities = process.getImgEntities(_this, _this.previewImg);
//            //get the mask in a list of bitmaps
//            ArrayList<Bitmap> masks = process.getMasks(entities, _this.previewImg);
//            /*Useful if you want to tell user the object identified*/
//            // ArrayList<String> identifiedObjects = process.getObjects(entities);
//            //add all the edited bitmaps back
//            _this.editedBitmap = process.addBitmapBackToOriginal(entities, masks, _this.previewImg);
//            return null;
//        }
//
//        @SuppressWarnings("InfiniteRecursion")
//        @Override
//        protected void onProgressUpdate(Void... values) {
//            onProgressUpdate(values);
//        }
//
//
//        @Override
//        protected void onPostExecute(Void result) {
//            EditPicture activity = activityReference.get();
//            activity.mImageView.setImageBitmap(activity.editedBitmap);
//            pd.dismiss();
//        }
//
//    }

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

//    private void saveImage(){     // this only works for the file from camera not content uri
//        File imageToSaveFile = getTempFile();
//
//        FileOutputStream out = null;
//        try {
//            out = new FileOutputStream(Objects.requireNonNull(imageToSaveFile));
//            previewImg.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
//            // PNG is a lossless format, the compression factor (100) is ignored
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                if (out != null) {
//                    out.close();
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        addToGallery(imageToSaveFile);
//    }
//
//    void addToGallery(File imageFile) {
//        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//        mediaScanIntent.setData(Uri.fromFile(imageFile));//todo cant it just be imageUri?
//        this.sendBroadcast(mediaScanIntent);
//    }
}
