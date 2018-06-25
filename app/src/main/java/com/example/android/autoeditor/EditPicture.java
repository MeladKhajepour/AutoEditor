package com.example.android.autoeditor;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.support.media.ExifInterface;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.util.TypedValue;
import android.view.Display;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static com.example.android.autoeditor.MainActivity.GALLERY_IMAGE;
import static com.example.android.autoeditor.MainActivity.IMAGE;
import static com.example.android.autoeditor.utils.Utils.CONTRAST_FILTER;
import static com.example.android.autoeditor.utils.Utils.CONVOLUTION_SHARPEN;
import static com.example.android.autoeditor.utils.Utils.UNSHARP_MASK_SHARPEN;
import static com.example.android.autoeditor.utils.Utils.setFilter;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class EditPicture extends AppCompatActivity {
    private static final int TF_OD_API_INPUT_SIZE = 224;
    private static final String TF_OD_API_MODEL_FILE =
            "frozen_inference_graph.pb";
    private static final String TF_OD_API_LABELS_FILE = "coco_labels_list.txt";
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;
    private Classifier detector;
    private int previewWidth;
    private int previewHeight;
    private int scaledWidth;
    private int scaledHeight;
    private Matrix cropToFrameTransform;
    private List<Classifier.Recognition> mappedRecognitions =
            new LinkedList<>();
    private Handler handler;

    Button saveButton;
    ImageView result;
    Uri myUri;
    SeekBar contrastSeekbar;
    SeekBar exposureSeekbar;
    SeekBar sharpenSeekbar;
    TextView contrastTextView;
    TextView exposureTextView;
    TextView sharpenTextView;
    Bitmap mBitmap;
    Bitmap scaledBitmap;

    Context ctx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_picture);
        ctx = getApplicationContext();

        //tries the receive the intent on photo taken
        result = findViewById(R.id.selected_picture_image_view);

        //Start of test sliders etc
        contrastTextView = findViewById(R.id.contrast_label);
        contrastSeekbar = findViewById(R.id.contrast_seekbar);
        sharpenSeekbar = findViewById(R.id.sharpen_seekbar);
        exposureSeekbar = findViewById(R.id.exposure_seekbar);
        exposureTextView = findViewById(R.id.exposure_label);
        sharpenTextView = findViewById(R.id.sharpen_label);

        contrastSeekbar.setMax(200);
        exposureSeekbar.setMax(200);
        sharpenSeekbar.setMax(200);
        contrastSeekbar.setProgress(100);
        exposureSeekbar.setProgress(100);
        sharpenSeekbar.setProgress(100);

        saveButton = findViewById(R.id.save_button);

    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = getIntent();
        Bundle extras = getIntent().getExtras();

        if (intent.hasExtra(IMAGE)) {
            myUri = Uri.parse(Objects.requireNonNull(extras).getString(IMAGE));
        } else {
            myUri = Uri.parse(Objects.requireNonNull(extras).getString(GALLERY_IMAGE));
        }

        processImageRGBbytes();
    }

    @Override
    protected void onResume() {
        super.onResume();

        contrastSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            Bitmap res;

            @SuppressLint("SetTextI18n")
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                res = setFilter(scaledBitmap, progress - 100, CONTRAST_FILTER, ctx);
                result.setImageBitmap(res);
                contrastTextView.setText("contrast: " + (progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        exposureSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            Bitmap res;

            @SuppressLint("SetTextI18n")
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                res = setFilter(scaledBitmap, progress - 100, UNSHARP_MASK_SHARPEN, ctx);
                result.setImageBitmap(res);
                exposureTextView.setText("exposure: " + (progress/100f*3f));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        sharpenSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            Bitmap res;

            @SuppressLint("SetTextI18n")
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                res= setFilter(scaledBitmap, progress - 100, CONVOLUTION_SHARPEN, ctx);
                result.setImageBitmap(res);
                exposureTextView.setText("Sharpness: " + (progress - 100));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                saveImage();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.overflow_menu, menu);
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save selective extras from original Intent...

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i); //goes back to main activity
    }

    /**
     * Rotate an image if required.
     *
     * @param img           The image bitmap
     * @param selectedImage Image URI
     * @return The resulted Bitmap after manipulation
     */
    private Bitmap rotateImageIfRequired(Context context, Bitmap img, Uri selectedImage) throws IOException {
        int tempWidth = previewWidth;

        InputStream input = context.getContentResolver().openInputStream(selectedImage);
        ExifInterface ei;
        if (Build.VERSION.SDK_INT > 23)
            ei = new ExifInterface(Objects.requireNonNull(input));
        else
            ei = new ExifInterface(selectedImage.getPath());

        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                previewWidth = previewHeight;
                previewHeight = tempWidth;
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                previewWidth = previewHeight;
                previewHeight = tempWidth;
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
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
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
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    protected void processImageRGBbytes() {
        try {
            detector = TensorFlowObjectDetectionAPIModel.create(
                    getApplicationContext().getAssets(), TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, TF_OD_API_INPUT_SIZE);
        } catch (final IOException e) {
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }
        cropToFrameTransform = new Matrix();

        try {
            mBitmap =  decodeSampledBitmapFromResource(this, myUri, TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE);
            scaledHeight = mBitmap.getHeight();
            scaledWidth = mBitmap.getWidth();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        mBitmap = BITMAP_RESIZER(mBitmap,TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE);
        handler = new Handler();


        new Thread(new Runnable() {
            @Override
            public void run () {
                // Perform long-running task here
                // (like audio buffering).
                // you may want to update some progress
                // bar every second, so use handler:
                handler.post(new Runnable() {
                    @Override
                    public void run () {
                        // make operation on UI - on example
                        // on progress bar.

                        final List<Classifier.Recognition> results = detector.recognizeImage(mBitmap);
                        final Canvas canvas = new Canvas(mBitmap);
                        canvas.drawBitmap(mBitmap, 0, 0, new Paint());
                        final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG |
                                Paint.DITHER_FLAG |
                                Paint.ANTI_ALIAS_FLAG);
                        paint.setColor(Color.RED);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(2.0f);

                        mappedRecognitions.clear();
                        for (final Classifier.Recognition result : results) {
                            final RectF location = result.getLocation();
                            if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {
                                canvas.drawRect(location, paint);
                                cropToFrameTransform.mapRect(location);
                                result.setLocation(location);
                                mappedRecognitions.add(result);
                            }
                        }
                       scaledBitmap = BITMAP_RESIZER(mBitmap, scaledWidth, scaledHeight);

                        result.setImageBitmap(scaledBitmap);

                    }
                });
            }
        }).start();
    }

    public Bitmap BITMAP_RESIZER(Bitmap bitmap,int newWidth,int newHeight) {
        Bitmap scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);

        float scaleX = newWidth / (float) bitmap.getWidth();
        float scaleY = newHeight / (float) bitmap.getHeight();
        float pivotX = 0;
        float pivotY = 0;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(scaleX, scaleY, pivotX, pivotY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bitmap, 0, 0, new Paint(Paint.FILTER_BITMAP_FLAG));

        return scaledBitmap;

    }

    public Bitmap decodeSampledBitmapFromResource(Context context, Uri uri,
                                                         int reqWidth, int reqHeight)
            throws FileNotFoundException {
        ContentResolver contentResolver = context.getContentResolver();
        InputStream inputStream = contentResolver.openInputStream(uri);
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, options);
        previewHeight = options.outHeight;
        previewWidth = options.outWidth;

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        inputStream = contentResolver.openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
        try {
            bitmap = rotateImageIfRequired(this, bitmap, myUri);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    public static int calculateInSampleSize(
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
}
