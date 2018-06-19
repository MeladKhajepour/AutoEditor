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

import com.example.android.autoeditor.filters.Editor;
import com.example.android.autoeditor.utils.Utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import static com.example.android.autoeditor.filters.Editor.getImageUri;
import static com.example.android.autoeditor.filters.Editor.setImageUri;
import static com.example.android.autoeditor.utils.Utils.readSharedSetting;
import static com.example.android.autoeditor.utils.Utils.setViewDimens;

public class MainActivity extends AppCompatActivity {
    public static final String SELECTED_IMAGE_URI = "image_uri";
    public static final int CAMERA_REQUEST_CODE = (int) Math.floor(7*Math.random()*100);
    public static final int MEDIA_REQUEST_CODE = (int) Math.floor(11*Math.random()*100);
    public static final String PREF_USER_FIRST_TIME = "user_first_time";

    boolean isUserFirstTime;
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
        calculateImageViewSize();
        initOnClickListeners();
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

    private void checkOnboarding() {
        isUserFirstTime = Boolean.valueOf(readSharedSetting(MainActivity.this, PREF_USER_FIRST_TIME, "true"));

        if (isUserFirstTime) {
            startActivity(new Intent(mainActivity, Onboarding.class));
        }
    }

    private void calculateImageViewSize() {
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

    private void initOnClickListeners() {
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.add_from_camera:
                        handleImageSelection(Manifest.permission.CAMERA, CAMERA_REQUEST_CODE);
                        break;

                    case R.id.add_from_gallery:
                        handleImageSelection(Manifest.permission.WRITE_EXTERNAL_STORAGE, MEDIA_REQUEST_CODE);
                        break;
                }
            }
        };

        cameraBtn = findViewById(R.id.add_from_camera);
        galleryBtn = findViewById(R.id.add_from_gallery);

        cameraBtn.setOnClickListener(listener);
        galleryBtn.setOnClickListener(listener);
    }

    private void handleImageSelection(String permission, int requestCode) {

        if(hasPermission(permission)) {
            if(requestCode == CAMERA_REQUEST_CODE) {
                selectFromCamera();
            } else {
                selectFromGallery();
            }
        } else {
            getPermission(permission, requestCode);
        }
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(mainActivity, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void getPermission(String permission, int code) {
        ActivityCompat.requestPermissions(mainActivity, new String[] {permission}, code);
    }

    private void selectFromCamera() {
        Intent launchCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Intent openWithIntent;

        if (launchCameraIntent.resolveActivity(getPackageManager()) != null) {

            if (createDesinationFileUri()) {
                launchCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, getImageUri());
                openWithIntent = Intent.createChooser(launchCameraIntent, "Take Picture Using");

                startActivityForResult(openWithIntent, CAMERA_REQUEST_CODE);
            } else {
                //todo make alert signaling of no image directory and prompt to maybe make one
            }
        }
    }

    private boolean createDesinationFileUri() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CANADA).format(new Date());
        String imageFileSuffix = ".jpg";
        String imageFileName = "AutoEdit_" + timeStamp + imageFileSuffix;
        File imagesDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if(imagesDirectory != null) {
            imagesDirectory = new File(imagesDirectory + "/AutoEdits");

            if(!imagesDirectory.isDirectory() && !imagesDirectory.exists()) {
                imagesDirectory.mkdirs();
            }

            File imageFile = new File(imagesDirectory, imageFileName);
            Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID, imageFile);

            setImageUri(uri);
            return true;
        }

        return false;
    }

    private void selectFromGallery() {
        Intent fileBrowserIntent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");//file browser
        Intent openWithIntent;

        if (fileBrowserIntent.resolveActivity(getPackageManager()) != null) {
            openWithIntent = Intent.createChooser(fileBrowserIntent, "Select Image From");
            startActivityForResult(openWithIntent, MEDIA_REQUEST_CODE);

        } else if(fileBrowserIntent.resolveActivity(getPackageManager()) == null) {// user has no file browser so try gallery
            Intent imageBrowserIntent = new Intent(Intent.ACTION_PICK).setType("image/*");//image browser
            openWithIntent = Intent.createChooser(imageBrowserIntent, "Select Image From");
            startActivityForResult(openWithIntent, MEDIA_REQUEST_CODE);

        } else {
            //todo make dialog signaling to user no file picker or gallery app is available
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        // Once the image URI has been saved from taking or selecting a picture, edit picture activity starts
        Intent editImageActivity = new Intent(this, EditPicture.class);

        if (requestCode == CAMERA_REQUEST_CODE &&  resultCode == Activity.RESULT_OK) {
            startActivity(editImageActivity);

        } else if (requestCode == MEDIA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri imageUri = intent.getData();

            if(imageUri != null) {
                setImageUri(imageUri);
                startActivity(editImageActivity);

            } else {
                //todo alert user something isnt right and couldnt get selected image
                Toast.makeText(this, "Hmm, something didn't quite go right...", Toast.LENGTH_LONG).show();
            }
        }
    }//todo see how file handling is now on the edit picture activity

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {

        for (int i = 0, len = permissions.length; i < len; i++) {
            final String permission = permissions[i];

            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {

                // user rejected the permission
                boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(mainActivity, permission);

                if (! showRationale) {
                    // user also CHECKED "never ask again"
                    // you can either enable some fall back,
                    // disable features of your app
                    // or open another dialog explaining
                    // again the permission and directing to
                    // the app setting
                    if(Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission)) {
                        Snackbar.make(galleryBtn, "File access required", Snackbar.LENGTH_LONG)
                                .show();
                    } else if (Manifest.permission.CAMERA.equals(permission)) {
                        Snackbar.make(cameraBtn, "Camera access required", Snackbar.LENGTH_LONG)
                                .show();
                    }
                } else if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission)) { // make an alert to explain what permission is for

                    new AlertDialog.Builder(this).setTitle("File Access Denied")
                            .setMessage("File access is required for loading images to the app and saving the edited images")
                            .setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Utils.requestPermissionList(mainActivity, Collections.singletonList(permission));
                                }
                            })
                            .setNegativeButton("Later", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Snackbar.make(galleryBtn, "File access required", Snackbar.LENGTH_LONG)
                                            .show();
                                }
                            })
                            .create().show();

                } else if (Manifest.permission.CAMERA.equals(permission)) {
                    new AlertDialog.Builder(this).setTitle("Camera Access Denied")
                            .setMessage("Camera access is required for taking pictures")
                            .setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Utils.requestPermissionList(mainActivity, Collections.singletonList(permission));
                                }
                            })
                            .setNegativeButton("Later", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Snackbar.make(cameraBtn, "Camera access required", Snackbar.LENGTH_LONG)
                                            .show();
                                }
                            })
                            .create()
                            .show();
                }
            }
        }
    }
}
