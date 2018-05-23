package com.example.android.autoeditor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    public static final String TAKE_A_PICTURE = "Take a Picture";
    public static final String SELECT_FROM_GALLERY = "Select from Gallery";
    public static final String IMAGE = "image";
    public static final String GALLERY_IMAGE = "galleryImage";
    private String userSelectedTask;
    private int REQUEST_CAMERA = 0, SELECT_FILE = 1;
    private TextView textSelect;
    private Intent startEditPictureActivity;

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
                editPicker();
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

    private void editPicker() {

        //selectable items inside an array
        final CharSequence[] alertDialogSelections = {TAKE_A_PICTURE, SELECT_FROM_GALLERY};

        //opens the alertbox
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Add a Photo!");
        builder.setItems(alertDialogSelections, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int selection) {
                boolean hasPermission = CameraPermissionUtility.checkPermission(MainActivity.this); // todo add camera permission
                if (alertDialogSelections[selection].equals(TAKE_A_PICTURE)) {
                    userSelectedTask = TAKE_A_PICTURE;
                    if (hasPermission) {
                        cameraIntent();
                    }
                } else if (alertDialogSelections[selection].equals(SELECT_FROM_GALLERY)) {
                    userSelectedTask = SELECT_FROM_GALLERY;
                    if (hasPermission)
                        galleryIntent();
                }
            }
        });
        builder.show();

    }

    private void cameraIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    private void galleryIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);//
        startActivityForResult(Intent.createChooser(intent, "Select File"), SELECT_FILE);
    }

    //responsible for receiving results for permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CameraPermissionUtility.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (userSelectedTask.equals(TAKE_A_PICTURE))
                        cameraIntent();
                    else if (userSelectedTask.equals(SELECT_FROM_GALLERY))
                        galleryIntent();
                } else {
                    Toast.makeText(getApplicationContext(), "Media Permissions have been denied!",
                            Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    //results on dialog pick user
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_FILE)
                onSelectFromGalleryResult(data);
            else if (requestCode == REQUEST_CAMERA)
                onCaptureImageResult(data);
        }
    }

    //responsible to getting the image taken by user and then sending it to second activity
    private void onCaptureImageResult(Intent data) {
        Bundle extras = data.getExtras();
        Bitmap imageBitmap = (Bitmap) Objects.requireNonNull(extras).get("data");

        startEditPictureActivity.putExtra(IMAGE, imageBitmap);
        startActivity(startEditPictureActivity);
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

}
