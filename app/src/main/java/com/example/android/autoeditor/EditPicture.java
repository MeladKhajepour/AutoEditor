package com.example.android.autoeditor;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.media.ExifInterface;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
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

import com.example.android.autoeditor.env.BorderedText;
import com.example.android.autoeditor.env.ImageUtils;
import com.example.android.autoeditor.env.Logger;
import com.example.android.autoeditor.OverlayView.DrawCallback;
import com.example.android.autoeditor.tracking.MultiBoxTracker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

import static com.example.android.autoeditor.MainActivity.GALLERY_IMAGE;
import static com.example.android.autoeditor.MainActivity.IMAGE;
import static com.example.android.autoeditor.utils.Utils.CONTRAST_FILTER;
import static com.example.android.autoeditor.utils.Utils.CONVOLUTION_SHARPEN;
import static com.example.android.autoeditor.utils.Utils.EXPOSURE_FILTER;
import static com.example.android.autoeditor.utils.Utils.UNSHARP_MASK_SHARPEN;
import static com.example.android.autoeditor.utils.Utils.setFilter;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class EditPicture extends AppCompatActivity {
    private static final Logger LOGGER = new Logger();
    private MultiBoxTracker tracker;
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final String TF_OD_API_MODEL_FILE =
            "mobilenet_ssd.tflite";
    private static final String TF_OD_API_LABELS_FILE = "coco_labels_list.txt";
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;
    private static final boolean MAINTAIN_ASPECT = false;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    private static final int[] COLORS = {
            Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.WHITE,
            Color.parseColor("#55FF55"), Color.parseColor("#FFA500"), Color.parseColor("#FF8888"),
            Color.parseColor("#AAAAFF"), Color.parseColor("#FFFFAA"), Color.parseColor("#55AAAA"),
            Color.parseColor("#AA33AA"), Color.parseColor("#0D0068")
    };
    private final Paint boxPaint = new Paint();
    private Integer sensorOrientation;
    private Classifier detector;
    private int previewWidth;
    private int previewHeight;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private Bitmap cropCopyBitmap;
    private BorderedText borderedText;
    private long lastProcessingTimeMs;
    private OverlayView detectionOverlay;
    private List<Classifier.Recognition> mappedRecognitions =
            new LinkedList<>();
    private boolean debug = false;
    private Handler handler;
    private HandlerThread handlerThread;
    protected Runnable postInferenceCallback;
    protected byte[][] yuvBytes = new byte[3][];
    private long timestamp = 0;

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


        try {
            mBitmap = handleSamplingAndRotationBitmap(this, myUri);
        } catch (IOException e) {
            e.printStackTrace();
        }

        recognize();
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
    public synchronized void onPause() {
        LOGGER.d("onPause " + this);

        if (!isFinishing()) {
            LOGGER.d("Requesting finish");
            finish();
        }

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }

        super.onPause();
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
     * This method is responsible for solving the rotation issue if exist. Also scale the images to
     * 1024x1024 resolution
     *
     * @param context       The current context
     * @param selectedImage The Image URI
     * @return Bitmap image results
     */
    public Bitmap handleSamplingAndRotationBitmap(Context context, Uri selectedImage)
            throws IOException {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream imageStream = context.getContentResolver().openInputStream(selectedImage);
        BitmapFactory.decodeStream(imageStream, null, options);
        Objects.requireNonNull(imageStream).close();
        previewHeight = options.outHeight;
        previewWidth  = options.outWidth;

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        imageStream = context.getContentResolver().openInputStream(selectedImage);
        Bitmap img = BitmapFactory.decodeStream(imageStream, null, options);

        img = rotateImageIfRequired(context, img, selectedImage);
        return img;
    }


    /**
     * Rotate an image if required.
     *
     * @param img           The image bitmap
     * @param selectedImage Image URI
     * @return The resulted Bitmap after manipulation
     */
    private static Bitmap rotateImageIfRequired(Context context, Bitmap img, Uri selectedImage) throws IOException {

        InputStream input = context.getContentResolver().openInputStream(selectedImage);
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
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    public void recognize() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        yuvBytes[0] = byteArray;

        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        try {
            detector = TFLiteObjectDetectionAPIModel.create(
                    getApplicationContext().getAssets(), TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, TF_OD_API_INPUT_SIZE);
        } catch (final IOException e) {
            LOGGER.e("Exception initializing classifier!", e);
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        final Display display = getWindowManager().getDefaultDisplay();

        sensorOrientation = display.getRotation();

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        detectionOverlay = findViewById(R.id.detection_overlay);
        detectionOverlay.addCallback(
                new DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        tracker.draw(canvas);
                        if (isDebug()) {
                            tracker.drawDebug(canvas);
                        }
                    }
                });
    }

    protected void processImageRGBbytes() {
        detectionOverlay.postInvalidate();
        mBitmap = Bitmap.createScaledBitmap(mBitmap,TF_OD_API_INPUT_SIZE,TF_OD_API_INPUT_SIZE,true);
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

                        final long startTime = SystemClock.uptimeMillis();
                        final List<Classifier.Recognition> results = detector.recognizeImage(mBitmap);
                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
                        LOGGER.i("Detect: %s", results);
                        croppedBitmap = Bitmap.createBitmap(mBitmap);
                        Bitmap cropCopyBitmap = croppedBitmap.copy(Bitmap.Config.ARGB_8888, true);
                        final Canvas canvas = new Canvas(cropCopyBitmap);
                        canvas.drawBitmap(cropCopyBitmap, 0, 0, new Paint());
                        final Paint paint = new Paint();
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
                        detectionOverlay.postInvalidate();
                        scaledBitmap = getResizedBitmap(cropCopyBitmap,previewHeight, previewWidth);
                        result.setImageBitmap(scaledBitmap);

                    }
                });
            }
        }).start();
    }

    public boolean isDebug() {
        return debug;
   }

    public Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth)
    {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // create a matrix for the manipulation
        Matrix matrix = new Matrix();
        // resize the bit map
        matrix.postScale(scaleWidth, scaleHeight);
        // recreate the new Bitmap
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
    }


}
