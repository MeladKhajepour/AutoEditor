package com.example.android.autoeditor;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
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
import java.lang.ref.WeakReference;

import static com.example.android.autoeditor.utils.Utils.OVERWRITE_FLAG;

/*
*
* This class is for handling the edit pic activity. It is responsible for:
*   Setting the image bitmap
*   Providing the UI for editing the bitmap
*   Saving the final image
* ******should only have to worry about initializing the image, setting it and updating it. no logic
 */
public class EditPicture extends AppCompatActivity {
    private final int ANIMATION_DURATION = 200; //Fade out duration of buttons
    private boolean savedOnce = false; // Has user saved the file once already
    private boolean isSaved = false; // Is the file currently saved
    private boolean shouldOverwrite; // Should the new image overwrite old one
    private Editor editor; // handles image editing
    private ImageView mImageView;
    private TextView saveStatus;
    private Button viewButton;
    private Button saveButton;
    private Button overwriteToggle;
    private static File imgFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_picture);

        mImageView = findViewById(R.id.preview);
        saveStatus = findViewById(R.id.save_status);
        saveStatus.setAlpha(0);
        saveButton = findViewById(R.id.save_button);
        viewButton = findViewById(R.id.view_button);
        viewButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                viewImage();
            }
        });

        try {
            editor = new Editor(this);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Something went wrong with opening your image ...", Toast.LENGTH_LONG).show();
            finish();
        }

        initSaveMode();
        updatePreview();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.overflow_menu, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i); //goes back to main activity
        finish();
    }

    private void viewImage() {
        Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID, imgFile);
        String mime = getContentResolver().getType(uri);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mime);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    private void initSaveMode() {
        shouldOverwrite = Utils.readSharedSetting(this, OVERWRITE_FLAG, false);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(shouldOverwrite && savedOnce) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(EditPicture.this);
                    builder.setTitle(R.string.overwrite_confirmation);
                    builder.setMessage(R.string.overwrite_description);
                    builder.setPositiveButton(R.string.overwrite, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new SaveInBackground(EditPicture.this).execute();
                        }
                    });
                    builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    builder.show();
                } else {
                    new SaveInBackground(EditPicture.this).execute();
                    savedOnce = true;
                }
            }
        });

        overwriteToggle = findViewById(R.id.overwrite_toggle);
        overwriteToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSaveMode(shouldOverwrite);
            }
        });

        if(shouldOverwrite) {
            setOverwriteMode();
        } else {
            setCopyMode();
        }

        editor.overWriteEnabled(shouldOverwrite);
    }

    private static class SaveInBackground extends AsyncTask<Void, Void, Void> {
        ProgressDialog pd;
        WeakReference<EditPicture> activityReference;

        private SaveInBackground(EditPicture activity) {
            activityReference = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            pd = new ProgressDialog(activityReference.get());
            pd.setTitle("Hang tight");
            pd.setMessage("Your image is being saved");
            pd.setCancelable(false);
            pd.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                imgFile = Editor.saveImg(activityReference.get()); // returns the file reference
            } catch (Exception e) {
                Toast.makeText(activityReference.get(), "Something went wrong with saving your image ...", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            pd.dismiss();
            activityReference.get().onSaveComplete();
            activityReference.clear();
        }
    }

    private void toggleSaveMode(boolean shouldOverwrite) {
        if(shouldOverwrite) {
            setCopyMode();
        } else {
            setOverwriteMode();
        }

        this.shouldOverwrite = !shouldOverwrite;
        Utils.saveSharedSetting(this, OVERWRITE_FLAG, this.shouldOverwrite);
        editor.overWriteEnabled(this.shouldOverwrite);
    }

    public void updatePreview() { //todo make background task
        mImageView.setImageBitmap(editor.getPreviewBitmap());
    }

    private void setOverwriteMode() {
        overwriteToggle.setText(R.string.overwrite);
        overwriteToggle.setTextColor(getResources().getColor(R.color.overwriteColor));
    }

    private void setCopyMode() {
        overwriteToggle.setText(R.string.copy);
        overwriteToggle.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
    }

    public void onSeekBarTouch() {
        if(isSaved) {
            showSaveMode();
        }
    }

    private void onSaveComplete() {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(imgFile));
        sendBroadcast(intent);

        showViewMode();
    }

    private void showSaveMode() {
        isSaved = false;
        saveButton.setVisibility(View.VISIBLE);
        saveButton.animate().alpha(1).setDuration(ANIMATION_DURATION).start();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                viewButton.setVisibility(View.INVISIBLE);
            }
        }, ANIMATION_DURATION);

        saveStatus.animate().alpha(0).setDuration(ANIMATION_DURATION).start();
        overwriteToggle.animate().alpha(1).setDuration(ANIMATION_DURATION).start();
    }

    private void showViewMode() {
        isSaved = true;
        viewButton.setVisibility(View.VISIBLE);
        saveButton.animate().alpha(0).setDuration(ANIMATION_DURATION).start();
        saveStatus.animate().alpha(1).setDuration(ANIMATION_DURATION).start();
        overwriteToggle.animate().alpha(0).setDuration(ANIMATION_DURATION).start();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                saveButton.setVisibility(View.GONE);
            }
        }, ANIMATION_DURATION);
    }
}
