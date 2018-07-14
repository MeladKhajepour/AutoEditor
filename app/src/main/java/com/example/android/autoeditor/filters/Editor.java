package com.example.android.autoeditor.filters;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;

import com.example.android.autoeditor.EditPicture;
import com.example.android.autoeditor.R;
import com.example.android.autoeditor.utils.Cluster;
import com.example.android.autoeditor.utils.ClusterParams;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.example.android.autoeditor.utils.BitmapUtils.calculateInSampleSize;
import static com.example.android.autoeditor.utils.BitmapUtils.rotateImageIfRequired;
import static com.example.android.autoeditor.utils.Utils.CONVOLUTION_SHARPEN;
import static com.example.android.autoeditor.utils.Utils.getPreviewHeight;
import static com.example.android.autoeditor.utils.Utils.getPreviewWidth;

public class Editor implements Cluster.OnFilterAdjusted {
    private static Uri contentUri;
    private EditPicture activity;
    private List<Cluster> clusters = new ArrayList<>();
    private Cluster.ActiveFilter activeFilter;
    private Bitmap originalImg, mutablePreviewImg;//Original picture should never be laoded in memory till save
    private File savedImgFile;

    public Editor(EditPicture activity) throws IOException {
        this.activity = activity;
        createPreviewBitmap();
        initClusters();
        Filters.initFilter(mutablePreviewImg);
    }

    private void createPreviewBitmap() throws IOException, NullPointerException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        options.inJustDecodeBounds = true;

        InputStream is = activity.getContentResolver().openInputStream(contentUri);
        BitmapFactory.decodeStream(is, null, options);
        is.close();

        options.inSampleSize = calculateInSampleSize(options, getPreviewWidth(), getPreviewHeight());
        options.inJustDecodeBounds = false;

        is = activity.getContentResolver().openInputStream(contentUri);
        mutablePreviewImg = BitmapFactory.decodeStream(is, null, options);
        is.close();

        mutablePreviewImg = rotateImageIfRequired(activity, mutablePreviewImg, contentUri);
    }

    public void saveImage() throws IOException, NullPointerException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        InputStream is = activity.getContentResolver().openInputStream(contentUri);
        originalImg = BitmapFactory.decodeStream(is, null, options);
        originalImg = rotateImageIfRequired(activity, originalImg, contentUri);
        is.close();

        File pictureDir = Environment.getExternalStorageDirectory();
        pictureDir = new File(pictureDir.getAbsolutePath() + File.separator + "AutoEdits");
        pictureDir.mkdirs();

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CANADA).format(new Date());
        String imageFileName = "AE_" + timeStamp + ".jpeg";

        savedImgFile = new File(pictureDir, imageFileName);
        FileOutputStream outStream = new FileOutputStream(savedImgFile);
        originalImg = Filters.applyFinalEdits(activity, originalImg);
        originalImg.compress(Bitmap.CompressFormat.JPEG, 100, outStream);

        outStream.flush();
        outStream.close();

        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(savedImgFile));
        activity.sendBroadcast(intent);

        activity.onSave(savedImgFile);
    }

    private void initClusters() {
        clusters.addAll(
                Arrays.asList(
                        new Cluster(new ClusterParams(this, R.id.exposure_seekbar)),
                        new Cluster(new ClusterParams(this, R.id.contrast_seekbar)),
                        new Cluster(new ClusterParams(this, R.id.sharpen_seekbar)),
                        new Cluster(new ClusterParams(this, R.id.saturation_seekbar))
                )
        );
    }

    @Override
    public void setActiveFilter(Cluster.ActiveFilter activeFilter) {
        this.activeFilter = activeFilter;

        if(activeFilter != null && activeFilter.filterType == CONVOLUTION_SHARPEN) {
            Filters.initRs(activity);
        }
    }

    public void destroyRs() {
        Filters.destroyRs();
    }

    public Bitmap getPreviewBitmap() {
        return mutablePreviewImg;
    }

    public EditPicture getActivity() {
        return activity;
    }

    public void updatePreview() {
        activity.updatePreview();
    }

    public void applyFilter() {
        mutablePreviewImg = Filters.applyFilter(activeFilter);
    }

    public interface OnSaveListener {
        void onSave(File imgFile);
    }

    public interface OnEditListener {
        void onEdit();
    }

    public void onEdit() {
        activity.onEdit();
    }

    public static void setContentUri(Uri uri) {
        contentUri = uri;
    }
}
