package com.example.android.autoeditor;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.android.autoeditor.imageManipulation.GetAndAddMasks;
import com.example.android.autoeditor.tensorFlow.Classifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.example.android.autoeditor.MainActivity.GALLERY_IMAGE;
import static com.example.android.autoeditor.MainActivity.IMAGE;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class EditPicture extends AppCompatActivity {
    Button saveButton;
    ImageView result;
    Uri myUri;
    SeekBar contrastSeekbar;
    SeekBar exposureSeekbar;
    SeekBar sharpenSeekbar;
    TextView contrastTextView;
    TextView exposureTextView;
    TextView sharpenTextView;

    Context ctx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_picture);
        ctx = getApplicationContext();

        //tries the receive the intent on photo taken
        result = findViewById(R.id.selected_picture_image_view);

        //Start of test sliders etc
     /*   contrastTextView = findViewById(R.id.contrast_label);
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
        sharpenSeekbar.setProgress(100);*/

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

        //How to use GetAndAddMasks class
        //Initialiaze the class
        GetAndAddMasks process = new GetAndAddMasks();
        //Get the tensorflow results
        List<Classifier.Recognition> tfResults = process.getTFResults(ctx, myUri);
        //need the scaled bitmap if you want to get masks
        Bitmap scaledBitmap = process.getScaledBitmap(ctx, myUri);
        //get the mask in a list of bitmaps
        ArrayList<Bitmap> masks = process.getMask(tfResults, scaledBitmap);
        /*Useful if you want to tell user the object identified*/
       // ArrayList<String> identifiedObjects = process.getObjects(tfResults);
        //add all the edited bitmaps back
        Bitmap editedBitmap = process.addBitmapBackToOriginal(tfResults, masks, scaledBitmap);
        //see the final product!
       result.setImageBitmap(editedBitmap);
    }

    @Override
    protected void onResume() {
        super.onResume();

      /*  contrastSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
        */

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
}
