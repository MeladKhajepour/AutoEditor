package com.example.android.autoeditor;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.support.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.facebook.drawee.view.SimpleDraweeView;

import java.io.ByteArrayOutputStream;
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
            myUri = Uri.parse(Objects.requireNonNull(extras).getString(IMAGE));
            Bitmap mBitmap = null;
            try {
                mBitmap = getRotatedBitmap(myUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            Objects.requireNonNull(mBitmap).compress(Bitmap.CompressFormat.JPEG, 100, bytes);
            String path = MediaStore.Images.Media.insertImage(this.getContentResolver(), mBitmap,
                    TITLE, DESCRIPTION);
            Uri.parse(path);
        }
        else{
            myUri = Uri.parse(Objects.requireNonNull(extras).getString(GALLERY_IMAGE));
        }

        result.setImageURI(myUri);

        saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                addToGallery(myUri);
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

    void addToGallery(Uri imageUri) {

        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(imageUri.getPath());
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);

    }

    Bitmap getRotatedBitmap(Uri myUri) throws IOException {

        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), myUri);
        ExifInterface ei = new ExifInterface(myUri.getPath());
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
        return rotatedBitmap;
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }
}
