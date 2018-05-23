package com.example.android.autoeditor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.ImageView;

import java.io.FileInputStream;
import java.util.Objects;

import static com.example.android.autoeditor.MainActivity.GALLERY_IMAGE;
import static com.example.android.autoeditor.MainActivity.IMAGE;

public class EditPicture extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_picture);
        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        //tries the receive the intent on photo taken
        ImageView result = findViewById(R.id.selected_picture_image_view);
        Bundle extras = getIntent().getExtras();
        Bitmap image = (Bitmap) Objects.requireNonNull(extras).get(IMAGE);

        //if photo taken is not there, get the gallery image
        if(image == null) {
            try {
                String filename = getIntent().getStringExtra(GALLERY_IMAGE);
                FileInputStream is = this.openFileInput(filename);
                Bitmap bmp = BitmapFactory.decodeStream(is);
                result.setImageBitmap(bmp);
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else{ //else add the photo taken to image view of activity
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
