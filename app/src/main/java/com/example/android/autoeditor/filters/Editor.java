package com.example.android.autoeditor.filters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.view.View;

import com.example.android.autoeditor.EditPicture;
import com.example.android.autoeditor.R;
import com.example.android.autoeditor.utils.Cluster;
import com.example.android.autoeditor.utils.ClusterParams;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.example.android.autoeditor.utils.BitmapUtils.calculateInSampleSize;
import static com.example.android.autoeditor.utils.BitmapUtils.rotateImageIfRequired;
import static com.example.android.autoeditor.utils.Utils.CONVOLUTION_SHARPEN;

public class Editor {
    private static boolean shouldOverwrite;
    private static int imageScaleX, imageScaleY;
    private static Uri contentUri;
    private static File pictureDir;
    private static String imgFileName = null;
    private static String timeStamp = null;
    private static int copyCount = 1;
    private EditPicture activity;
    private Cluster.ActiveFilter activeFilter;
    private Bitmap previewBitmap;

    public Editor(EditPicture activity) throws Exception {
        this.activity = activity;
        createPreviewBitmap();
        initClusters();
    }

    private void createPreviewBitmap() throws IOException, NullPointerException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        options.inJustDecodeBounds = true;

        InputStream is = activity.getContentResolver().openInputStream(contentUri);
        if(is == null) {
            throw new NullPointerException("Image stream null");
        }
        BitmapFactory.decodeStream(is, null, options);
        is.close();

        options.inSampleSize = calculateInSampleSize(options, imageScaleX, imageScaleY);
        options.inJustDecodeBounds = false;

        is = activity.getContentResolver().openInputStream(contentUri);
        if(is == null) {
            throw new NullPointerException("Image stream null");
        }
        previewBitmap = BitmapFactory.decodeStream(is, null, options);
        is.close();

        previewBitmap = rotateImageIfRequired(activity, previewBitmap, contentUri);
        Filters.initFilter(previewBitmap);
    }

    private void initClusters() {
        new Cluster(new ClusterParams(this, R.id.exposure_seekbar));
        new Cluster(new ClusterParams(this, R.id.contrast_seekbar));
        new Cluster(new ClusterParams(this, R.id.sharpen_seekbar));
        new Cluster(new ClusterParams(this, R.id.saturation_seekbar));
    }

    public void onSeekBarTouch(Cluster.ActiveFilter activeFilter) {
        this.activeFilter = activeFilter;

        if(activeFilter != null && activeFilter.filterType == CONVOLUTION_SHARPEN) {
            Filters.initRs(activity);
        }
        activity.onSeekBarTouch();
    }

    public void onSeekBarEdit() {
        previewBitmap = Filters.applyFilter(activeFilter);
        activity.updatePreview();
    }

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

    public static File saveImg(EditPicture activity) throws Exception {
        File imgFile = createSaveFile();
        encodeFile(activity, imgFile);
        return imgFile;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static File createSaveFile() {
        if(imgFileName == null) {
            pictureDir = Environment.getExternalStorageDirectory();
            pictureDir = new File(pictureDir.getAbsolutePath() + File.separator + "AutoEdits");
            pictureDir.mkdirs();

            timeStamp = new SimpleDateFormat("yyMMdd_HHmmss", Locale.CANADA).format(new Date());
            imgFileName = "AE_" + timeStamp + ".jpeg";

        } else if(!shouldOverwrite) {
            imgFileName = "AE_COPY_" + copyCount++ + "_" + timeStamp + ".jpeg";
        }

        return new File(pictureDir, imgFileName);
    }

    private static void encodeFile(EditPicture activity, File imgFile) throws IOException, NullPointerException {
        FileOutputStream outStream = new FileOutputStream(imgFile, false);
        Bitmap originalImg = openOriginalImage(activity);

        Filters.applyEditsToBitmap(activity, originalImg);
        originalImg.compress(Bitmap.CompressFormat.JPEG, 100, outStream);

        outStream.flush();
        outStream.close();
        originalImg.recycle();
        originalImg = null;
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
}
