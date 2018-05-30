package com.example.android.autoeditor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    public static final String TAKE_A_PICTURE = "Take a Picture";
    public static final String SELECT_FROM_GALLERY = "Select from Gallery";
    public static final String IMAGE = "image";
    public static final String GALLERY_IMAGE = "galleryImage";
    public static final String ALERT_DIALOG_TITLE = "Select a Photo";
    public static final int CAMERA_REQUEST_CODE = 1;
    public static final int MEDIA_REQUEST_CODE = 2;
    private TextView textSelect;
    private Intent startEditPictureActivity;
    Uri photoURI;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Fresco.initialize(this);
        //button select for the plus button
        textSelect = findViewById(R.id.select_option_TextView);
        textSelect.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                imageSelectionDialog();
            }
        });
        startEditPictureActivity = new Intent(this, EditPicture.class);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void imageSelectionDialog() {

        //opens the alertbox
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(ALERT_DIALOG_TITLE);

        //selectable items inside an array
        final CharSequence[] alertDialogSelections = {TAKE_A_PICTURE, SELECT_FROM_GALLERY};
        builder.setItems(alertDialogSelections, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int selection) {
                if (alertDialogSelections[selection].equals(TAKE_A_PICTURE)) {
                    if (checkPermission(Manifest.permission.CAMERA, CAMERA_REQUEST_CODE)) {
                        cameraIntent();
                    }
                } else if (alertDialogSelections[selection].equals(SELECT_FROM_GALLERY)) {
                    if (checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, MEDIA_REQUEST_CODE)) {
                        galleryIntent();
                    }
                }
            }
        });

        builder.show();

    }

    private void cameraIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.autoeditor",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            }
        }
    }

    private void galleryIntent() {
        Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getIntent.setType("image/*");

        Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");

        Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});
        startActivityForResult(Intent.createChooser(chooserIntent, "Select File"), MEDIA_REQUEST_CODE);
    }

    //responsible for receiving results for permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        String text;


        switch (requestCode) {

            case CAMERA_REQUEST_CODE:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cameraIntent();
                } else {
                    text = "Camera permissions have been denied";
                    Toast.makeText(this, text, Toast.LENGTH_LONG).show();
                }
                break;

            case MEDIA_REQUEST_CODE:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    galleryIntent();
                } else {
                    text = "Media permissions have been denied";
                    Toast.makeText(this, text, Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    //results on dialog pick user
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    startEditPictureActivity.putExtra(IMAGE, Objects.requireNonNull(photoURI).toString());
                    startActivity(startEditPictureActivity);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        else if (requestCode == MEDIA_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    final Uri imageUri = intent.getData();
                    startEditPictureActivity.putExtra(GALLERY_IMAGE, Objects.requireNonNull(imageUri).toString());
                    startActivity(startEditPictureActivity);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, intent);

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

    private File createImageFile() throws IOException {
        // Create an image file name
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }


}
