package com.example.android.autoeditor.filters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.example.android.autoeditor.EditPicture;
import com.example.android.autoeditor.R;
import com.example.android.autoeditor.utils.Cluster;
import com.example.android.autoeditor.utils.ClusterParams;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.example.android.autoeditor.utils.BitmapUtils.calculateInSampleSize;
import static com.example.android.autoeditor.utils.BitmapUtils.rotateImageIfRequired;
import static com.example.android.autoeditor.utils.Utils.CONVOLUTION_SHARPEN;
import static com.example.android.autoeditor.utils.Utils.getPreviewHeight;
import static com.example.android.autoeditor.utils.Utils.getPreviewWidth;

public class Editor {
    private static Uri contentUri;
    private EditPicture activity;
    private List<Cluster> clusters = new ArrayList<>();
    private Cluster.ActiveFilter activeFilter;
    private Bitmap originalImg, mutablePreviewImg;//Original picture should never be laoded in memory till save

    public Editor(EditPicture activity) throws IOException {
        this.activity = activity;
        createPreviewBitmap();
        initClusters();
        Filters.init(mutablePreviewImg);
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

    public void setActiveFilter(Cluster.ActiveFilter activeFilter) {
        this.activeFilter = activeFilter;

        if(activeFilter != null && activeFilter.filterType == CONVOLUTION_SHARPEN) {
            Filters.initRs(activity);
        }
    }

    public void destroyRs() {
        Filters.destroyRs();
    }

    public Bitmap getOriginalBitmap() {
        if(originalImg == null) {
            createOriginalImg();
        }

        return originalImg;
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

    private void createOriginalImg() {

    }

    public static void setContentUri(Uri uri) {
        contentUri = uri;
    }
}
