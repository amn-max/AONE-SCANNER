package com.aonescan.scanner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.aonescan.scanner.Adapter.IntroViewPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class IntroActivity extends AppCompatActivity {

    private ViewPager2 screenPager;
    private TabLayout tabIndicator;
    private Button btnNext;
    private int position = 0;
    private Button btnGetStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        try {
            Objects.requireNonNull(getSupportActionBar()).hide();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        if (restorePrefData()) {
            Intent mainIntent = new Intent(IntroActivity.this, MainActivity.class);
            startActivity(mainIntent);
            finish();
        }

//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        tabIndicator = findViewById(R.id.tabLayout);
        btnNext = findViewById(R.id.btn_next);
        btnGetStarted = findViewById(R.id.btn_getStarted);
        List<ScreenItem> mList = new ArrayList<>();

        mList.add(new ScreenItem("Fast Camera Right Away", "A custom camera that's build right inside the app for ease of use. Lets you Capture images and add to your Project.", R.drawable.camera_costum));
        mList.add(new ScreenItem("Auto Detects Edges", "A algorithm that auto detects edges a paper and lets you edit the Image.", R.drawable.autodetectedges));
        mList.add(new ScreenItem("Set Your Desired Name", "You can also set desired name for your PDF before saving.", R.drawable.pdf_name));
        mList.add(new ScreenItem("Rearrange Using Drag", "You can rearrange photos based on your preference and then convert them to PDF for correct page order.", R.drawable.rearrange_photos));
        mList.add(new ScreenItem("Explore PDF", "Seamlessly access files right inside Apps and share them to desired Social Media Apps", R.drawable.view_pdf));


        screenPager = findViewById(R.id.screen_view_pager);
        IntroViewPagerAdapter introViewPagerAdapter = new IntroViewPagerAdapter(this, mList);
        screenPager.setAdapter(introViewPagerAdapter);

        new TabLayoutMediator(tabIndicator, screenPager, (tab, position) -> {
        }).attach();

        btnNext.setOnClickListener(v -> {
            position = screenPager.getCurrentItem();
            if (position < mList.size()) {
                position++;
                screenPager.setCurrentItem(position);
            }
            if (position == mList.size() - 1) {
                loadLastScreen();
            }
        });

        tabIndicator.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == mList.size() - 1) {
                    loadLastScreen();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        btnGetStarted.setOnClickListener(v -> {
            Intent mainIntent = new Intent(IntroActivity.this, MainActivity.class);
            startActivity(mainIntent);
            savePrefsData();
            finish();
        });
    }

    private boolean restorePrefData() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences(getResources().getString(R.string.app_name), MODE_PRIVATE);
        return pref.getBoolean("isIntroOpened", false);
    }

    private void savePrefsData() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences(getResources().getString(R.string.app_name), MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("isIntroOpened", true);
        editor.putInt("timesOpened", 1);
        editor.putInt("initialCount", 1);
        editor.apply();
    }

    private void loadLastScreen() {
        btnNext.setVisibility(View.INVISIBLE);
        btnGetStarted.setVisibility(View.VISIBLE);
        tabIndicator.setVisibility(View.VISIBLE);
    }
}