package com.example.android.autoeditor;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.example.android.autoeditor.floatingToolbar.FloatBar;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    public static final String IMAGE = "image";
    public static final String GALLERY_IMAGE = "galleryImage";
    public static final int CAMERA_REQUEST_CODE = 0;
    public static final int MEDIA_REQUEST_CODE = 1;

    private FloatBar floatBar;
    private FloatingActionButton fab;
    private Intent startEditPictureActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initFloatBar();

        startEditPictureActivity = new Intent(this, EditPicture.class);
    }

    @Override
    public void onBackPressed() {
        floatBar.hide();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.overflow_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {

            case R.id.how_app_works:
                //Todo: interesting page about how app works? maybe Medium article to get more exposure?
                break;

            case R.id.settings:
                //Todo: settings activity
                break;
        }


        return super.onOptionsItemSelected(item);
    }

    private void cameraIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA_REQUEST_CODE);
    }

    private void galleryIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);//
        startActivityForResult(Intent.createChooser(intent, "Select File"), MEDIA_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        String text;
        Snackbar snackbar;


        switch (requestCode) {

            case CAMERA_REQUEST_CODE:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cameraIntent();
                } else {
                    text = "Camera permissions have been denied";

                    snackbar = Snackbar.make(fab, text, Snackbar.LENGTH_SHORT);
                    floatBar.showSnackBar(snackbar);
                }
                break;

            case MEDIA_REQUEST_CODE:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    galleryIntent();
                } else {
                    text = "Media permissions have been denied";

                    snackbar = Snackbar.make(fab, text, Snackbar.LENGTH_SHORT);
                    floatBar.showSnackBar(snackbar);
                }
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (resultCode == Activity.RESULT_OK) {

            switch (requestCode) {

                case CAMERA_REQUEST_CODE:
                    onCaptureImageResult(intent);
                    break;

                case MEDIA_REQUEST_CODE:
                    onSelectFromGalleryResult(intent);
                    break;
            }
        }
    }

    private void onCaptureImageResult(Intent intent) {
        Bundle extras = intent.getExtras();

        Bitmap imageBitmap = (Bitmap) Objects.requireNonNull(extras).get("data");

        startEditPictureActivity.putExtra(IMAGE, imageBitmap);
        startActivity(startEditPictureActivity);
    }

    private void onSelectFromGalleryResult(Intent data) {

        try {
            final Uri imageUri = data.getData();
            final InputStream imageStream = getContentResolver().openInputStream(Objects.requireNonNull(imageUri));
            final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);

            //Write file
            String filename = "bitmap.png";
            FileOutputStream stream = this.openFileOutput(filename, Context.MODE_PRIVATE);
            selectedImage.compress(Bitmap.CompressFormat.PNG, 100, stream);

            //Cleanup
            stream.close();
            selectedImage.recycle();

            //Pop intent
            startEditPictureActivity.putExtra(GALLERY_IMAGE, filename);
            startActivity(startEditPictureActivity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private boolean checkPermission(String permission, int requestCode) {
        int currentAPIVersion = Build.VERSION.SDK_INT;

        if(currentAPIVersion >= Build.VERSION_CODES.M && checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[] {permission}, requestCode);
            return false;
        }

        return true;
    }

    private void initFloatBar() {

        floatBar  = findViewById(R.id.fabtoolbar);
        fab = findViewById(R.id.fab);
        floatBar.attachFab(fab);

        floatBar.setClickListener(new FloatBar.ItemClickListener() {
            @Override
            public void onItemClick(MenuItem item) {

                switch (item.getItemId()) {

                    case R.id.add_from_camera:
                        if (checkPermission(Manifest.permission.CAMERA, CAMERA_REQUEST_CODE)) {
                            cameraIntent();
                        }
                        break;

                    case R.id.add_from_gallery:
                        if (checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, MEDIA_REQUEST_CODE)) {
                            galleryIntent();
                        }
                        break;

                }
            }

            @Override
            public void onItemLongClick(MenuItem item) {

            }
        });
    }
}
