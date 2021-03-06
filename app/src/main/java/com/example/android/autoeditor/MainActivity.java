package com.example.android.autoeditor;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.example.android.autoeditor.filters.Editor.setContentUri;
import static com.example.android.autoeditor.filters.Editor.setViewDimens;
import static com.example.android.autoeditor.utils.Utils.readSharedSetting;
import static com.example.android.autoeditor.utils.Utils.requestPermission;

public class MainActivity extends AppCompatActivity {
    public static final int CAMERA_REQUEST_CODE = (int) Math.floor(7*Math.random()*100);
    public static final int MEDIA_REQUEST_CODE = (int) Math.floor(11*Math.random()*100);
    public static final String PREF_USER_FIRST_TIME = "user_first_time";

    private Activity mainActivity;
    private ImageButton cameraBtn, galleryBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mainActivity = this;

        checkOnboarding();
        precalculateImageViewSize();
        initImageSelectionBtns();
    }

    private void checkOnboarding() {
        boolean isUserFirstTime = readSharedSetting(MainActivity.this, PREF_USER_FIRST_TIME, true);

        if (isUserFirstTime) {
            startActivity(new Intent(mainActivity, Onboarding.class));
        }
    }

    private void precalculateImageViewSize() {
        final View view = findViewById(R.id.ghost_view);
        ViewTreeObserver vto = view.getViewTreeObserver();
        vto.addOnGlobalLayoutListener (new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                setViewDimens(view);
            }
        });
    }

    private void initImageSelectionBtns() {
        cameraBtn = findViewById(R.id.add_from_camera);
        galleryBtn = findViewById(R.id.add_from_gallery);
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.add_from_camera:
                        selectFromCamera();
                        break;

                    case R.id.add_from_gallery:
                        selectFromGallery();
                        break;
                }
            }
        };

        for(ImageButton btn : new ImageButton[] {cameraBtn, galleryBtn}) {
            btn.setOnClickListener(listener);
        }
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

    private void selectFromCamera() {
        String permission = Manifest.permission.CAMERA;
        if(hasPermission(permission)) {
            Intent launchCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            try {
                if (launchCameraIntent.resolveActivity(getPackageManager()) != null) {

                    Uri imageUri = createDesinationFileUri();

                    if(imageUri != null) {
                        launchCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                        startActivityForResult(Intent.createChooser(launchCameraIntent, "Take Picture Using"), CAMERA_REQUEST_CODE);
                    } else {
                        //todo something if fails
                    }
                }
            } catch (IOException e) {
                Snackbar.make(cameraBtn, "Could not create image file... ", Toast.LENGTH_SHORT);
                e.printStackTrace();
            }
        } else {
            askPermission(permission, CAMERA_REQUEST_CODE);
        }
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(mainActivity, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void askPermission(String permission, int code) {//todo check for file perm on saving
        ActivityCompat.requestPermissions(mainActivity, new String[] {permission}, code);
    }

    private Uri createDesinationFileUri() throws IOException {
        String timeStamp = new SimpleDateFormat("yyMMdd_HHmmss", Locale.CANADA).format(new Date());
        String imageFileSuffix = ".jpg";
        String imageFileName = "AE_" + timeStamp;
        File picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if(picturesDir != null) {
            File folder = new File(picturesDir.getAbsolutePath() +
                    File.separator + "AutoEdits");

            folder.mkdirs();

            File tempFile = File.createTempFile(imageFileName, imageFileSuffix, folder);
            Uri imageUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID, tempFile);

            setContentUri(imageUri);
            return imageUri;
        }

        return null;
    }

    private void selectFromGallery() {
        String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        if(hasPermission(permission)) {
            Intent fileBrowserIntent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");//file browser

            if (fileBrowserIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(Intent.createChooser(fileBrowserIntent, "Select Image From"), MEDIA_REQUEST_CODE);
            }
        } else {
            askPermission(permission, MEDIA_REQUEST_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Intent editPictureActivity = new Intent(this, EditPicture.class);

        if (requestCode == CAMERA_REQUEST_CODE &&  resultCode == Activity.RESULT_OK) {
            startActivity(editPictureActivity);

        } else if (requestCode == MEDIA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri imageUri = intent.getData();

            if(imageUri != null) {
                setContentUri(imageUri);
                startActivity(editPictureActivity);

            } else {
                //todo alert user something isnt right and couldnt get selected image
                Toast.makeText(this, "Hmm, something didn't quite go right...", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        final String permission = permissions[0];

        if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (permission) {
                case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                    selectFromGallery();
                    break;

                case Manifest.permission.CAMERA:
                    selectFromCamera();
                    break;
            }

        } else if (grantResults[0] == PackageManager.PERMISSION_DENIED ) {

            if (! ActivityCompat.shouldShowRequestPermissionRationale(mainActivity, permission)) { // Never show again is selected

                switch (permission) {
                    case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                        makePermissionSnackbar(galleryBtn, "File access is denied");
                        break;

                    case Manifest.permission.CAMERA:
                        makePermissionSnackbar(cameraBtn, "Camera access is denied");
                        break;
                }

                return;
            }

            switch (permission) {
                case Manifest.permission.WRITE_EXTERNAL_STORAGE:

                    makePermissionAlert(
                            permission,
                            "File Access is Denied",
                            "File access is required for loading images to the app and saving the edited images");
                    break;

                case Manifest.permission.CAMERA:

                    makePermissionAlert(
                            permission,
                            "Camera Access is Denied",
                            "Camera access is required for taking pictures");
                    break;
            }
        }
    }

    private void makePermissionAlert(final String permission, String title, String message) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(title);
        alert.setMessage(message);

        alert.setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestPermission(mainActivity, permission);
                    }
                })
                .setNegativeButton("Later", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

        alert.create().show();
    }

    private void makePermissionSnackbar(View v, String message) {
        Snackbar.make(v, message, Snackbar.LENGTH_LONG)
                .setAction("Grant", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                })
                .show();
    }
}
