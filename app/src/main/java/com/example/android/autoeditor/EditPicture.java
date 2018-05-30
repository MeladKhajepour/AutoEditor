package com.example.android.autoeditor;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.core.ImagePipeline;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static com.example.android.autoeditor.MainActivity.GALLERY_IMAGE;
import static com.example.android.autoeditor.MainActivity.IMAGE;

public class EditPicture extends AppCompatActivity {
    String TITLE = "Auto Editor";
    String DESCRIPTION = "Editing picture";
    Button saveButton;
    SimpleDraweeView result;
    Uri myUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_picture);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        //tries the receive the intent on photo taken
        result = findViewById(R.id.selected_picture_image_view);
        result.getHierarchy().setPlaceholderImage(null);

        Intent intent = getIntent();
        Bundle extras = getIntent().getExtras();

        if (intent.hasExtra(IMAGE)) {
            myUri = Uri.parse((extras != null) ? Objects.requireNonNull(extras).getString(IMAGE) : null);
            Log.d("TEST", myUri.toString());
            result.setImageURI(myUri);
        }

        if (intent.hasExtra(GALLERY_IMAGE)) {
            myUri = Uri.parse(Objects.requireNonNull(extras).getString(GALLERY_IMAGE));
            result.setImageURI(myUri);
        }

        saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    addToGallery(myUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        });
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish(); //goes back to main activity
        }
        return super.onOptionsItemSelected(item);
    }

    void addToGallery(Uri imageUri) throws IOException {
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
        MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, TITLE , DESCRIPTION);

    }

}
