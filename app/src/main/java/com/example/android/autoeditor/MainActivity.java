package com.example.android.autoeditor;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    public static final String TAKE_A_PICTURE = "Take a Picture";
    public static final String SELECT_FROM_GALLERY = "Select from Gallery";
    public static final String IMAGE = "image";
    public static final String GALLERY_IMAGE = "galleryImage";
    public static final String ALERT_DIALOG_TITLE = "Select a Photo";
    public static final int CAMERA_REQUEST_CODE = 1;
    public static final int MEDIA_REQUEST_CODE = 2;
    private String userSelectedTask;
    private TextView textSelect;
    private Intent startEditPictureActivity;
    String mCurrentPhotoPath;
    File photoFile;
    Uri photoURI;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
                    userSelectedTask = TAKE_A_PICTURE;
                    if (checkPermission(Manifest.permission.CAMERA, CAMERA_REQUEST_CODE)) {
                        Log.d("Test", "Permissions granted");
                        cameraIntent();
                        Log.d("Test", "In");
                    }
                } else if (alertDialogSelections[selection].equals(SELECT_FROM_GALLERY)) {
                    userSelectedTask = SELECT_FROM_GALLERY;
                    if (checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, MEDIA_REQUEST_CODE)) {
                        galleryIntent();
                    }
                }
            }
        });

        builder.show();

    }

    private void cameraIntent() {
        Intent pictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File path = new File(this.getFilesDir(), ".");
        boolean wasSuccessfull = true;
        if(!path.exists()) {
            wasSuccessfull = path.mkdirs();
        }

        if (!wasSuccessfull){
            // Directory already exist
            String text = "Directory already exists";
            Toast.makeText(this, text, Toast.LENGTH_LONG).show();
        }
        File image = new File(path, "image.jpg");
        Uri imageUri = FileProvider.getUriForFile(this, getApplicationContext().getApplicationContext().getPackageName() + ".provider"
                , image);
        pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        Log.d("TAG", imageUri.toString());
        startActivityForResult(pictureIntent, CAMERA_REQUEST_CODE);

    }

    private void galleryIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select File"), MEDIA_REQUEST_CODE);
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
                File path = new File(getFilesDir(), ".");
                boolean wasSuccessfull = true;
                if(!path.exists()) {
                    wasSuccessfull = path.mkdirs();
                }

                if (!wasSuccessfull){
                    // Directory already exist
                    String text = "Directory already exists";
                    Toast.makeText(this, text, Toast.LENGTH_LONG).show();
                }
                File imageFile = new File(path, "image.jpg");
                startEditPictureActivity.putExtra(IMAGE, imageFile);
                startActivity(startEditPictureActivity);
            }
        }
        super.onActivityResult(requestCode, resultCode, intent);

    }

    //grabs image from gallery and sents the intent into another activity
    @SuppressWarnings("deprecation")
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

    /* Storing the file url as it'll be null after returning from camera app */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save file url in bundle as it will be null on scren orientation changes
        outState.putParcelable("file_uri", photoURI);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)         {
        super.onRestoreInstanceState(savedInstanceState);
// get the file url
        photoURI = savedInstanceState.getParcelable("file_uri");
    }
}
