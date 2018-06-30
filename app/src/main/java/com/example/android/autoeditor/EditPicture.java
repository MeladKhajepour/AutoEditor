package com.example.android.autoeditor;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.example.android.autoeditor.filters.Editor;
import com.example.android.autoeditor.utils.Cluster;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

import static com.example.android.autoeditor.filters.Editor.getTempFile;

public class EditPicture extends AppCompatActivity implements Cluster.OnFilterAdjustment {
    Button saveButton;
    ImageView mImageView;
    Cluster exposure, contrast, sharpness, saturation;
    Bitmap mBitmap;
    private Editor imageEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_picture);

        imageEditor = new Editor(this);//Todo

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
    }

    @Override
    public void updatePreview() {
        Bitmap image = imageEditor.getPreviewBitmap(); //todo do stuff with bitmaputils class

        if(image != null) {
            mImageView.setImageBitmap(image);
        } else {
            //todo do something if cant set pic
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
            mBitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
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
