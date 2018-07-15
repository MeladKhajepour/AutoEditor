package com.example.android.autoeditor;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
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

import static com.example.android.autoeditor.utils.Utils.OVERWRITE_FLAG;

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
    private boolean savedOnce = false;
    private boolean isSaved = false;
    private boolean shouldOverwrite;
    private Editor imageEditor;
    private ImageView mImageView;
    private TextView saveStatus;
    private Button viewButton;
    private Button saveButton;
    private Button saveMode;
    private File imgFile;

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

    private void initUi() {
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

        initSaveMode();
        updatePreview(imageEditor.getPreviewBitmap());
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
                            imageEditor.saveImg();//check for overwrite status first here
                        }
                    });
                    builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    builder.show();
                } else {
                    imageEditor.saveImg();
                    savedOnce = true;
                }
            }
        });

        saveMode = findViewById(R.id.save_mode);
        saveMode.setOnClickListener(new View.OnClickListener() {
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
    }

    private void toggleSaveMode(boolean shouldOverwrite) {
        if(shouldOverwrite) {
            setCopyMode();
        } else {
            setOverwriteMode();
        }

        this.shouldOverwrite = !shouldOverwrite;
        Utils.saveSharedSetting(this, OVERWRITE_FLAG, this.shouldOverwrite);
        imageEditor.overWriteEnabled(this.shouldOverwrite);
    }

    public void updatePreview(Bitmap previewImg) { //todo make background task
        mImageView.setImageBitmap(previewImg);
    }

    private void setOverwriteMode() {
        saveMode.setText(R.string.overwrite);
        saveMode.setTextColor(getResources().getColor(R.color.overwriteColor));
    }

    private void setCopyMode() {
        saveMode.setText(R.string.copy);
        saveMode.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
    }

    @Override
    public void onSeekBarTouch() {
        if(isSaved) {
            showSaveMode();
        }

        setSaveStatus(false);
    }

    @Override
    public void onSaveComplete(File imgFile) {
        showViewMode();

        if(!isSaved) {
            setSaveStatus(true);
            setImgFile(imgFile);
        }
    }

    private void showSaveMode() {
        saveButton.setVisibility(View.VISIBLE);
        saveButton.animate().alpha(1).setDuration(ANIMATION_DURATION).start();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                viewButton.setVisibility(View.INVISIBLE);
            }
        }, ANIMATION_DURATION);

        saveStatus.animate().alpha(0).setDuration(ANIMATION_DURATION).start();
        saveMode.animate().alpha(1).setDuration(ANIMATION_DURATION).start();
    }

    private void showViewMode() {
        viewButton.setVisibility(View.VISIBLE);
        saveButton.animate().alpha(0).setDuration(ANIMATION_DURATION).start();
        saveStatus.animate().alpha(1).setDuration(ANIMATION_DURATION).start();
        saveMode.animate().alpha(0).setDuration(ANIMATION_DURATION).start();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                saveButton.setVisibility(View.GONE);
            }
        }, ANIMATION_DURATION);
    }

    private void setSaveStatus(boolean saved) {
        this.isSaved = saved;
    }

    private void setImgFile(File imgFile) {
        this.imgFile = imgFile;
    }
}
