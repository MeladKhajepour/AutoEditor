package com.example.android.autoeditor.filters;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import com.example.android.autoeditor.EditPicture;
import com.example.android.autoeditor.R;
import com.example.android.autoeditor.utils.Cluster;
import com.example.android.autoeditor.utils.ClusterParams;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.example.android.autoeditor.utils.BitmapUtils.calculateInSampleSize;
import static com.example.android.autoeditor.utils.BitmapUtils.rotateImageIfRequired;
import static com.example.android.autoeditor.utils.Utils.CONVOLUTION_SHARPEN;

public class Editor implements Cluster.OnFilterAdjusted {
    private EditPicture activity;
    private static boolean shouldOverwrite;
    private static int imageScaleX, imageScaleY;
    private static Uri contentUri;
    private static File savedImgFile;
    private static File pictureDir;
    private static String imgFileName = null;
    private static String timeStamp = null;
    private static int copyCount = 1;
    private Cluster.ActiveFilter activeFilter;
    private Bitmap previewBitmap;

    public Editor(EditPicture activity) throws Exception {
        this.activity = activity;
        createPreviewBitmap();
        initClusters();
        Filters.initFilter(previewBitmap);
    }

    private void createPreviewBitmap() throws IOException, NullPointerException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        options.inJustDecodeBounds = true;

        InputStream is = activity.getContentResolver().openInputStream(contentUri);

        if(is != null) {
            BitmapFactory.decodeStream(is, null, options);
            is.close();

            options.inSampleSize = calculateInSampleSize(options, imageScaleX, imageScaleY);
            options.inJustDecodeBounds = false;
        } else {
            throw new NullPointerException("Image stream null");
        }

        is = activity.getContentResolver().openInputStream(contentUri);

        if(is != null) {
            previewBitmap = BitmapFactory.decodeStream(is, null, options);
            is.close();
        } else {
            throw new NullPointerException("Image stream null");
        }

        previewBitmap = rotateImageIfRequired(activity, previewBitmap, contentUri);
    }

    private void initClusters() {
        new Cluster(new ClusterParams(this, R.id.exposure_seekbar));
        new Cluster(new ClusterParams(this, R.id.contrast_seekbar));
        new Cluster(new ClusterParams(this, R.id.sharpen_seekbar));
        new Cluster(new ClusterParams(this, R.id.saturation_seekbar));
    }

    @Override
    public void onSeekBarTouch(Cluster.ActiveFilter activeFilter) {
        this.activeFilter = activeFilter;

        if(activeFilter != null && activeFilter.filterType == CONVOLUTION_SHARPEN) {
            Filters.initRs(activity);
        }
        activity.onSeekBarTouch();
    }

    @Override
    public void onEdit() {
        previewBitmap = Filters.applyFilter(activeFilter);
        activity.updatePreview(previewBitmap);
    }

    @Override
    public void onSeekBarRelease() {
        if(activeFilter.filterType == CONVOLUTION_SHARPEN) {
            Filters.destroyRs();
        }

        activeFilter = null;
    }

    public Bitmap getPreviewBitmap() {
        return previewBitmap;
    }

    public EditPicture getActivity() {
        return activity;
    }

    public void overWriteEnabled(boolean enabled) {
        shouldOverwrite = enabled;
    }

    public void saveImg() {
        new SaveInBackground(activity).execute();
    }

    private static class SaveInBackground extends AsyncTask<Void, Void, Void> {
        ProgressDialog pd;

        WeakReference<EditPicture> activityReference;

        SaveInBackground(EditPicture activity) {
            activityReference = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            pd = new ProgressDialog(activityReference.get());
            pd.setTitle("Hang tight");
            pd.setMessage("Your image is being saved");
            pd.setCancelable(false);
            pd.show();
        }
        @Override
        protected Void doInBackground(Void... params) {
            try {
                savedImgFile = createSaveFile();//starts null
                encodeFile(activityReference.get(), savedImgFile);

                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(Uri.fromFile(savedImgFile));
                activityReference.get().sendBroadcast(intent);
            } catch (Exception e) {
                Toast.makeText(activityReference.get(), "Something went wrong with saving your image ...", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            pd.dismiss();
            activityReference.get().onSaveComplete(savedImgFile);
            activityReference.clear();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static File createSaveFile() {
        if(imgFileName == null) {
            pictureDir = Environment.getExternalStorageDirectory();
            pictureDir = new File(pictureDir.getAbsolutePath() + File.separator + "AutoEdits");
            pictureDir.mkdirs();

            timeStamp = new SimpleDateFormat("yyMMdd_HHmmss", Locale.CANADA).format(new Date());
            imgFileName = "AE_" + timeStamp + ".jpeg";

        } else if(shouldOverwrite) {
            return savedImgFile;

        } else {
            imgFileName = "AE_COPY_" + copyCount++ + "_" + timeStamp + ".jpeg";
        }

        savedImgFile = new File(pictureDir, imgFileName);
        return savedImgFile;
    }

    private static void encodeFile(EditPicture activity, File savedImgFile) throws IOException {
        Bitmap originalImg = openOriginalImage(activity);
        FileOutputStream outStream = new FileOutputStream(savedImgFile, !shouldOverwrite);
        originalImg = Filters.applyFinalEdits(activity, originalImg);
        originalImg.compress(Bitmap.CompressFormat.JPEG, 100, outStream);

        originalImg.recycle();
        outStream.flush();
        outStream.close();
    }

    private static Bitmap openOriginalImage(EditPicture activity) throws IOException, NullPointerException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        InputStream is = activity.getContentResolver().openInputStream(contentUri);
        Bitmap originalImg;

        if(is != null) {
            originalImg = BitmapFactory.decodeStream(is, null, options);
            is.close();
        } else {
            throw new NullPointerException("Image stream null");
        }

        return rotateImageIfRequired(activity, originalImg, contentUri);
    }

    public static void setViewDimens(View view) {
        imageScaleX = view.getWidth();
        imageScaleY = view.getHeight();
    }

    public static void setContentUri(Uri uri) {
        contentUri = uri;
    }

    public interface OnSaveListener {
        void onSaveComplete(File imgFile);
    }

    public interface OnEditListener {
        void onSeekBarTouch();
    }
}
