package com.example.android.autoeditor;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.autoeditor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import static com.example.android.autoeditor.utils.Utils.saveSharedSetting;

public class Onboarding extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} onboardingActivity will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} onboardingActivity will host the section contents.
     */
    private ViewPager mViewPager;
    private ImageButton mNextBtn;
    private Button mSkipBtn, mFinishBtn;

    private ImageView zero, one, two;
    private ImageView[] indicators;

    private Activity onboardingActivity;

    int page = 0;   //  to track page position

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_onboarding);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            Utils.darkenStatusBar(this, R.color.cyan);
        }

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        onboardingActivity = this;

        mNextBtn = findViewById(R.id.intro_btn_next);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP)
            mNextBtn.setImageDrawable(
                    Utils.tintMyDrawable(ContextCompat.getDrawable(this, R.drawable.ic_chevron_right_24dp), Color.WHITE)
            );

        mSkipBtn = findViewById(R.id.intro_btn_skip);
        mFinishBtn = findViewById(R.id.intro_btn_finish);

        zero = findViewById(R.id.intro_indicator_0);
        one = findViewById(R.id.intro_indicator_1);
        two = findViewById(R.id.intro_indicator_2);

        indicators = new ImageView[]{zero, one, two};

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mViewPager.setCurrentItem(page);
        updateIndicators(page);

        final int color1 = ContextCompat.getColor(this, R.color.cyan);
        final int color2 = ContextCompat.getColor(this, R.color.orange);
        final int color3 = ContextCompat.getColor(this, R.color.green);

        final int[] colorList = new int[]{color1, color2, color3};

        final ArgbEvaluator evaluator = new ArgbEvaluator();

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                /*
                color update
                 */
                int colorUpdate = (Integer) evaluator.evaluate(positionOffset, colorList[position], colorList[position == 2 ? position : position + 1]);
                mViewPager.setBackgroundColor(colorUpdate);
                Utils.darkenStatusBar(onboardingActivity, colorUpdate);
            }

            @Override
            public void onPageSelected(int position) {

                page = position;

                updateIndicators(page);

                switch (position) {
                    case 0:
                        mViewPager.setBackgroundColor(color1);
                        break;
                    case 1:
                        mViewPager.setBackgroundColor(color2);
                        break;
                    case 2:
                        mViewPager.setBackgroundColor(color3);
                        break;
                }


                mNextBtn.setVisibility(position == 2 ? View.GONE : View.VISIBLE);
                mFinishBtn.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
                mSkipBtn.setVisibility(position == 2 ? View.GONE : View.VISIBLE);

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                page += 1;
                mViewPager.setCurrentItem(page, true);
            }
        });

        mSkipBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mViewPager.setCurrentItem(2, true);
            }
        });

        mFinishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSharedSetting(Onboarding.this, MainActivity.PREF_USER_FIRST_TIME, "false");

                if(Utils.allPermissionsGranted(onboardingActivity)) {
                    finish();
                } else {
                    Utils.requestMissingPermissions(onboardingActivity);
                }
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(permissionsDenied(grantResults)) {
            String alertBodyText = getAlertBodyText(permissions, grantResults);

            new AlertDialog.Builder(this).setTitle(R.string.onboarding_alert_title)
                    .setMessage(alertBodyText)
                    .setPositiveButton(R.string.onboarding_alert_positive_btn, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Utils.requestMissingPermissions(onboardingActivity);
                        }
                    })
                    .setNegativeButton(R.string.onboarding_alert_negative_btn, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .create().show();
        } else {
            finish();
        }
    }

    private boolean permissionsDenied(int[] permissionResults) { // Checks if any permissions were denied

        for(int i : permissionResults) {
            if(i == PackageManager.PERMISSION_DENIED) {
                return true;
            }
        }

        return false;
    }

    private String getAlertBodyText(String[] permissions, int[] grantResults) { // Gets alert body text based on denied permission
        int numDeniedPerms = 0;
        List<String> deniedPerms = new ArrayList<>();

        for(int i = 0; i < grantResults.length; i++) {
            if(grantResults[i] == PackageManager.PERMISSION_DENIED) {
                numDeniedPerms++;
                deniedPerms.add(permissions[i]);
            }
        }

        if(numDeniedPerms > 1) {
            return getResources().getString(R.string.onboarding_alert_body_both);

        } else if(deniedPerms.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            return getResources().getString(R.string.onboarding_alert_body_gallery);

        } else if(deniedPerms.contains(Manifest.permission.CAMERA)) {
            return getResources().getString(R.string.onboarding_alert_body_camera);

        }

        return "";
    }

    void updateIndicators(int position) {
        for (int i = 0; i < indicators.length; i++) {
            indicators[i].setBackgroundResource(
                    i == position ? R.drawable.indicator_selected : R.drawable.indicator_unselected
            );
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class OnboardingFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        TextView headerTv;
        TextView descTv;
        ImageView img;

        int[] header = new int[] {R.string.page_1_header, R.string.page_2_header, R.string.page_3_header};
        int[] desc = new int[] {R.string.page_1_desc, R.string.page_2_desc, R.string.page_3_desc};
        int[] bgs = new int[] {R.drawable.ic_flight_24dp, R.drawable.ic_mail_24dp, R.drawable.ic_explore_24dp};

        public OnboardingFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static OnboardingFragment newInstance(int sectionNumber) {
            OnboardingFragment fragment = new OnboardingFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_onboarding, container, false);
            headerTv = rootView.findViewById(R.id.section_label);
            descTv = rootView.findViewById(R.id.section_desc);

            assert getArguments() != null;
            headerTv.setText(header[getArguments().getInt(ARG_SECTION_NUMBER) - 1]);
            descTv.setText(desc[getArguments().getInt(ARG_SECTION_NUMBER) - 1]);

            img = rootView.findViewById(R.id.section_img);
            img.setBackgroundResource(bgs[getArguments().getInt(ARG_SECTION_NUMBER) - 1]);


            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} onboardingActivity returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            return OnboardingFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }
    }
}
