package com.example.android.autoeditor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.support.media.ExifInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;

import static com.example.android.autoeditor.MainActivity.GALLERY_IMAGE;
import static com.example.android.autoeditor.MainActivity.IMAGE;

public class EditPicture extends AppCompatActivity {
    ImageView result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_picture);
        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }



        //tries the receive the intent on photo taken
        result = findViewById(R.id.selected_picture_image_view);
        Bundle extras = getIntent().getExtras();
        File pictureFile = (File) Objects.requireNonNull(extras).get(IMAGE);
        String photoPath = Objects.requireNonNull(pictureFile).getAbsolutePath();
        Bitmap myBitmap = BitmapFactory.decodeFile(pictureFile.getAbsolutePath());


        //if photo taken is not there, get the gallery image
        if(myBitmap == null) {
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
            try {
                setImageTaken(myBitmap, photoPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
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

   public void setImageTaken(Bitmap bitmap, String photoPath) throws IOException {
       ExifInterface ei = new ExifInterface(photoPath);
       int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
               ExifInterface.ORIENTATION_UNDEFINED);

       Bitmap rotatedBitmap;
       switch(orientation) {

           case ExifInterface.ORIENTATION_ROTATE_90:
               rotatedBitmap = rotateImage(bitmap, 90);
               break;

           case ExifInterface.ORIENTATION_ROTATE_180:
               rotatedBitmap = rotateImage(bitmap, 180);
               break;

           case ExifInterface.ORIENTATION_ROTATE_270:
               rotatedBitmap = rotateImage(bitmap, 270);
               break;

           case ExifInterface.ORIENTATION_NORMAL:
           default:
               rotatedBitmap = bitmap;
       }
       result.setImageBitmap(rotatedBitmap);
   }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }
}
