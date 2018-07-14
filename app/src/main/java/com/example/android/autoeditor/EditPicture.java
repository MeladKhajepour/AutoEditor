package com.example.android.autoeditor;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.autoeditor.filters.Editor;
import com.example.android.autoeditor.utils.Utils;

import java.io.File;
import java.io.IOException;

/*
*
* This class is for handling the edit pic activity. It is responsible for:
*   Setting the image bitmap
*   Providing the UI for editing the bitmap
*   Saving the final image
* ******should only have to worry about initializing the image, setting it and updating it. no logic
 */
public class EditPicture extends AppCompatActivity implements Editor.OnSaveListener, Editor.OnEditListener {
    private final int ANIMATION_DURATION = 200;
    private Editor imageEditor;
    private ImageView mImageView;
    private TextView saveStatus;
    private Button viewButton;
    private Button saveButton;
    private Button saveMode;
    private File imgFile; // gets set in onSave
    private boolean isSaved = false;

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
        saveStatus = findViewById(R.id.save_confirmation);
        viewButton = findViewById(R.id.view_button);
        viewButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                viewImage();
            }
        });

        saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    imageEditor.saveImage();
                } catch (IOException e) {
                    Toast.makeText(getBaseContext(), "Something went wrong with saving your image ...", Toast.LENGTH_LONG).show();
                    saveStatus.setText(R.string.error);
                    e.printStackTrace();
                }
            }
        });

        initSaveMode();
        updatePreview();
    }

    public void updatePreview() { //todo make background task
        mImageView.setImageBitmap(imageEditor.getPreviewBitmap());
    }

    @Override
    public void onSave(File imgFile) {
        /* todo, make the save button become flat and say "isSaved!", add another button to view the picture,
         * and if they edit the pic again switch it back to save, and on save ask if they want to overwrite
         * their last one or if they wanna keep both */
        //todo - make save button flat
        onImgSave();

        //todo - make save button ask for overwrite
        if(!isSaved) {
            setSaved(true);
            setImgFile(imgFile);
        }
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

    private void viewImage() {
        // Get URI and MIME type of file
        Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID, imgFile);
        String mime = getContentResolver().getType(uri);

        // Open file with user selected app
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mime);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    private void onImgSave() {
        viewButton.setVisibility(View.VISIBLE);
        saveButton.animate().alpha(0).setDuration(ANIMATION_DURATION).start();
        showSaveMode(false);
        saveStatus.setText(R.string.saved);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                saveButton.setVisibility(View.GONE);
            }
        }, ANIMATION_DURATION);
    }

    private void setSaved(boolean saved) {
        this.isSaved = saved;
    }

    private void setImgFile(File imgFile) {
        this.imgFile = imgFile;
    }

    @Override
    public void onEdit() {
        if(isSaved) {
            saveButton.setVisibility(View.VISIBLE);
            saveButton.animate().alpha(1).setDuration(ANIMATION_DURATION).start();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    viewButton.setVisibility(View.INVISIBLE);
                }
            }, ANIMATION_DURATION);
            showSaveMode(true);
        }

        setSaved(false);
    }

    private void showSaveMode(boolean show) {
        if(show) {
            saveStatus.animate().alpha(0).setDuration(ANIMATION_DURATION).start();
            saveMode.animate().alpha(1).setDuration(ANIMATION_DURATION).start();
        } else {
            saveStatus.animate().alpha(1).setDuration(ANIMATION_DURATION).start();
            saveMode.animate().alpha(0).setDuration(ANIMATION_DURATION).start();
        }
    }

    private void initSaveMode() {
        boolean overwriteMode = Utils.readSharedSetting(this, "overwrite_mode", false);

        saveMode = findViewById(R.id.save_mode);
        saveMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSaveMode();
            }
        });

        if(overwriteMode) {
            setOverwriteMode();
        } else {
            setCopyMode();
        }
    }

    private void toggleSaveMode() {
        boolean overwriteMode = Utils.readSharedSetting(this, "overwrite_mode", false);

        if(!overwriteMode) {
            setOverwriteMode();
        } else {
            setCopyMode();
        }

        Utils.saveSharedSetting(this, "overwrite_mode", !overwriteMode);
    }

    private void setOverwriteMode() {
        saveMode.setText(R.string.overwrite);
        saveMode.setTextColor(getResources().getColor(R.color.overwriteColor));
    }

    private void setCopyMode() {
        saveMode.setText(R.string.copy);
        saveMode.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
    }
}
