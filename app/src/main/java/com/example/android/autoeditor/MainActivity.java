package com.example.android.autoeditor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.android.autoeditor.floatingToolbar.FloatBar;
import com.example.android.autoeditor.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.example.android.autoeditor.utils.Utils.PERMISSIONS_REQUEST_ID;

public class MainActivity extends AppCompatActivity {
    public static final String IMAGE = "image";
    public static final String GALLERY_IMAGE = "galleryImage";
    public static final int CAMERA_REQUEST_CODE = 1;
    public static final int MEDIA_REQUEST_CODE = 2;
    public static final String PREF_USER_FIRST_TIME = "user_first_time";

    private static final String TAG = MainActivity.class.getSimpleName();

    boolean isUserFirstTime;
    private FloatBar floatBar;
    private FloatingActionButton fab;
    private Intent startEditPictureActivity;
    private Uri photoURI;
    private Activity that;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isUserFirstTime = Boolean.valueOf(Utils.readSharedSetting(MainActivity.this, PREF_USER_FIRST_TIME, "true"));
        that = this;

        if (isUserFirstTime) {
            Intent introIntent = new Intent(MainActivity.this, Onboarding.class);
            introIntent.putExtra(PREF_USER_FIRST_TIME, isUserFirstTime);

            startActivity(introIntent);
        }

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

    private void cameraIntent(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        photoURI = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID, Objects.requireNonNull(photoFile));
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);

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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d("in", "It is: " + Activity.RESULT_OK);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    // Create the File where the photo should go
                    startEditPictureActivity.putExtra(IMAGE, photoURI.toString());
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {

        for (int i = 0, len = permissions.length; i < len; i++) {
            final String permission = permissions[i];
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                // user rejected the permission
                boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(that, permission);
                if (! showRationale) {
                    // user also CHECKED "never ask again"
                    // you can either enable some fall back,
                    // disable features of your app
                    // or open another dialog explaining
                    // again the permission and directing to
                    // the app setting
                    if(Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission)) {
                        Snackbar.make(fab, "File access required", Snackbar.LENGTH_LONG)
                                .show();
                    } else if (Manifest.permission.CAMERA.equals(permission)) {
                        Snackbar.make(fab, "Camera access required", Snackbar.LENGTH_LONG)
                                .show();
                    }
                } else if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission)) {

                    new AlertDialog.Builder(this).setTitle("File Access Denied")
                            .setMessage("File access is required for loading images to the app and saving the edited images")
                            .setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Utils.requestPermission(that, Arrays.asList(permission));
                                }
                            })
                            .setNegativeButton("Later", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Snackbar.make(fab, "File access required", Snackbar.LENGTH_LONG)
                                            .show();
                                }
                            })
                            .create()
                            .show();
                } else if (Manifest.permission.CAMERA.equals(permission)) {
                    new AlertDialog.Builder(this).setTitle("Camera Access Denied")
                            .setMessage("Camera access is required for taking pictures")
                            .setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Utils.requestPermission(that, Arrays.asList(permission));
                                }
                            })
                            .setNegativeButton("Later", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Snackbar.make(fab, "Camera access required", Snackbar.LENGTH_LONG)
                                            .show();
                                }
                            })
                            .create()
                            .show();
                }
            }
        }
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

    private void initFloatBar() {

        floatBar  = findViewById(R.id.fabtoolbar);
        fab = findViewById(R.id.fab);
        floatBar.attachFab(fab);

        floatBar.setClickListener(new FloatBar.ItemClickListener() {
            @Override
            public void onItemClick(MenuItem item) {

                switch (item.getItemId()) {

                    case R.id.add_from_camera:
                        if (ContextCompat.checkSelfPermission(that, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            cameraIntent();
                        } else {
                            ActivityCompat.requestPermissions(that, new String[] {Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_ID);
                        }
                        break;

                    case R.id.add_from_gallery:
                        if (ContextCompat.checkSelfPermission(that, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                            galleryIntent();
                        } else {
                            ActivityCompat.requestPermissions(that, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_ID);
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
