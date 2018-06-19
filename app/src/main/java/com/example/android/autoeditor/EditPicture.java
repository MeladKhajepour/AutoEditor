package com.example.android.autoeditor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.media.ExifInterface;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.android.autoeditor.filters.Editor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import static com.example.android.autoeditor.filters.Editor.getImageUri;
import static com.example.android.autoeditor.utils.Utils.getTargetHeight;
import static com.example.android.autoeditor.utils.Utils.getTargetWidth;

public class EditPicture extends AppCompatActivity {
    Button saveButton;
    ImageView mImageView;
    SeekBar contrastSeekbar;
    SeekBar exposureSeekbar;
    SeekBar sharpenSeekbar;
    SeekBar saturationSeekbar;
    TextView contrastTextView;
    TextView exposureTextView;
    TextView sharpenTextView;
    TextView saturationTextView;
    Bitmap mBitmap;
    private Editor imageEditor;
    private TextView seekbarLabel;
    private SeekBar.OnSeekBarChangeListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_picture);

        mImageView = findViewById(R.id.selected_picture_image_view);
        imageEditor = new Editor(this);

        initUi();
    }

    private void initUi() {
        //Start of test sliders etc
        contrastTextView = findViewById(R.id.contrast_label);
        contrastSeekbar = findViewById(R.id.contrast_seekbar);
        sharpenTextView = findViewById(R.id.sharpen_label);
        sharpenSeekbar = findViewById(R.id.sharpen_seekbar);
        exposureTextView = findViewById(R.id.exposure_label);
        exposureSeekbar = findViewById(R.id.exposure_seekbar);
        saturationTextView = findViewById(R.id.saturation_label);
        saturationSeekbar = findViewById(R.id.saturation_seekbar);

        contrastSeekbar.setMax(200);
        exposureSeekbar.setMax(200);
        sharpenSeekbar.setMax(200);
        saturationSeekbar.setMax(200);
        contrastSeekbar.setProgress(100);
        exposureSeekbar.setProgress(100);
        sharpenSeekbar.setProgress(100);
        saturationSeekbar.setProgress(100);

        saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                saveImage();
            }
        });

        listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                progress -= 100;
                imageEditor.setFilterStrength(progress, seekBar.getId());

                mImageView.setImageBitmap(imageEditor.getActivityBitmap());
                seekbarLabel.setText(imageEditor.getSeekbarLabel(progress, seekBar.getId()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seekbarLabel = getSeekbarTextView(seekBar.getId());
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };

        contrastSeekbar.setOnSeekBarChangeListener(listener);
        exposureSeekbar.setOnSeekBarChangeListener(listener);
        sharpenSeekbar.setOnSeekBarChangeListener(listener);
        saturationSeekbar.setOnSeekBarChangeListener(listener);

        setImageViewPic();
    }

    private void setImageViewPic() {
        Bitmap image = null;
        try {
            image = resizeBitmapToPreview(getTargetWidth(), getTargetHeight());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(image != null) {
            mImageView.setImageBitmap(image);
        } else {
            //todo do something if cant set pic
        }
    }

    private Bitmap resizeBitmapToPreview(int reqWidth, int reqHeight) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap img = null;

        try {
            InputStream imageStream = getContentResolver().openInputStream(getImageUri());
            BitmapFactory.decodeStream(imageStream, null, options);
            Objects.requireNonNull(imageStream).close();

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            imageStream = getContentResolver().openInputStream(getImageUri());
            img = BitmapFactory.decodeStream(imageStream, null, options);
            options.inJustDecodeBounds = true;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return rotateImageIfRequired(img, getImageUri());
    }

    private int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private Bitmap rotateImageIfRequired(Bitmap img, Uri selectedImage) throws IOException {

        InputStream input = getContentResolver().openInputStream(selectedImage);
        ExifInterface ei;
        if (Build.VERSION.SDK_INT > 23)
            ei = new ExifInterface(Objects.requireNonNull(input));
        else
            ei = new ExifInterface(selectedImage.getPath());

        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.overflow_menu, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i); //goes back to main activity
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        if (!storageDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            storageDir.mkdir();
        }

        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }
    private void saveImage(){
        File imageToSaveFile = null;

        try {
            imageToSaveFile = createImageFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(Objects.requireNonNull(imageToSaveFile));
            mBitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Uri finalUri = Uri.fromFile(imageToSaveFile);
        addToGallery(finalUri);
    }

    void addToGallery(Uri imageUri) {

        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(imageUri.getPath());
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);//todo cant it just be imageUri?
        this.sendBroadcast(mediaScanIntent);
    }

    private TextView getSeekbarTextView(int activeSeekbar) {
        TextView seekbarLabel = null;

        switch (activeSeekbar) {
            case R.id.contrast_seekbar:
                seekbarLabel = findViewById(R.id.contrast_label);
                break;

            case R.id.exposure_seekbar:
                seekbarLabel = findViewById(R.id.exposure_label);
                break;

            case R.id.sharpen_seekbar:
                seekbarLabel = findViewById(R.id.sharpen_label);
                break;

            case R.id.saturation_seekbar:
                seekbarLabel = findViewById(R.id.saturation_label);
                break;
        }

        return seekbarLabel;
    }
}
