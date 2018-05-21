package com.example.android.autoeditor;

import android.app.ActionBar;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.util.Objects;

public class EditPicture extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_picture);
        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        ImageView result = findViewById(R.id.selected_picture_image_view);
        Bundle extras = getIntent().getExtras();
        Bitmap image = (Bitmap) Objects.requireNonNull(extras).get("image");

        if(image == null) {
            try {
                String filename = getIntent().getStringExtra("galleryImage");
                FileInputStream is = this.openFileInput(filename);
                Bitmap bmp = BitmapFactory.decodeStream(is);
                result.setImageBitmap(bmp);
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else{
            result.setImageBitmap(image);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish(); //goes back to main activity
        }
        return super.onOptionsItemSelected(item);
    }
}
