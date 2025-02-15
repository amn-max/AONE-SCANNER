package com.aonescan.scanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.aonescan.scanner.CostumClass.CustomDialog;
import com.aonescan.scanner.Fragments.PdfFragment;
import com.aonescan.scanner.Fragments.ProjectHistoryFragment;
import com.aonescan.scanner.update.Constants;
import com.aonescan.scanner.update.InAppUpdateManager;
import com.aonescan.scanner.update.InAppUpdateStatus;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.navigation.NavigationView;

import org.jetbrains.annotations.NotNull;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.InstallCallbackInterface;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.shreyaspatil.MaterialDialog.AbstractDialog;
import dev.shreyaspatil.MaterialDialog.interfaces.DialogInterface;

public class MainActivity extends AppCompatActivity implements InAppUpdateManager.InAppUpdateHandler {

    private static final int PERMISSION_REQUEST_CODE = 1240;
    private static final int UPDATE_REQUEST_CODE = 1001;
    private static final String BACK_STACK_ROOT_TAG = "root_fragment";
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.INTERNET,
    Manifest.permission.ACCESS_NETWORK_STATE};
    private final BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i("MainActivity", "loaded successfully");
            } else {
                super.onManagerConnected(status);
                CustomDialog customDialog = new CustomDialog(MainActivity.this);
                customDialog.showMyDialog("Oops!, this is awkward", "You Device is Not Supported", false, "Ok", R.drawable.ic_done, new AbstractDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        dialogInterface.dismiss();
                    }
                }, null, 0, null);
            }
        }

        @Override
        public void onPackageInstall(int operation, InstallCallbackInterface callback) {

        }
    };
    private MaterialToolbar toolbar;
    private NavigationView navigationView;
    private SharedPreferences pref;
    private BottomAppBar bottomAppBar;
    private BottomSheetBehavior<NavigationView> bottomSheetBehavior;
    private FrameLayout background_transparent_frame;
    private CustomDialog customDialog;

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        try {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//        if (hasFocus) {
//            hideSystemUI();
//        }
//    }
//
//    private void hideSystemUI() {
//        // Enables regular immersive mode.
//        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
//        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//        View decorView = getWindow().getDecorView();
//        decorView.setSystemUiVisibility(
//                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
//    }
//
//    private void showSystemUI() {
//        View decorView = getWindow().getDecorView();
//        decorView.setSystemUiVisibility(
//                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
//    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("ImagesScanActivity", "Internal library not found. Using Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d("ImagesScanActivity", "library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        pref = getApplicationContext().getSharedPreferences(getResources().getString(R.string.app_name), MODE_PRIVATE);
//        if (isNightModeOn) {
//            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
//        } else {
//            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
//        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(getResources().getString(R.string.app_name));
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(ContextCompat.getColor(MainActivity.this, R.color.dark));

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, getApplicationContext(), mLoaderCallback);
        if (Build.VERSION.SDK_INT < 21) {
            customDialog = new CustomDialog(this);
            customDialog.showMyDialog("It seems your have a old device", "Your device is too old, functions provided by this app may not work. Recommended Android version is Lollipop or Above.", true, "Ok", R.drawable.ic_done, new AbstractDialog.OnClickListener() {
                @Override
                public void onClick(dev.shreyaspatil.MaterialDialog.interfaces.DialogInterface dialogInterface, int which) {
                    dialogInterface.dismiss();
                    finish();
                }
            }, null, 0, null);
        }
        toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        try {
            Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setTitle(getString(R.string.app_name));
        } catch (Exception e) {
            e.printStackTrace();
        }
        navigationView = findViewById(R.id.navigation_view);
        bottomAppBar = findViewById(R.id.bottomAppBar);
        background_transparent_frame = findViewById(R.id.background_transparent_frame);
        bottomSheetBehavior = BottomSheetBehavior.from(navigationView);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        InAppUpdateManager inAppUpdateManager = InAppUpdateManager.Builder(this,UPDATE_REQUEST_CODE)
                .resumeUpdates(true)
                .mode(Constants.UpdateMode.FLEXIBLE)
                .useCustomNotification(false)
                .snackBarMessage("An update has just been downloaded.")
                .snackBarAction("RESTART")
                .handler(this);

        inAppUpdateManager.checkForAppUpdate();
        bottomAppBar.setNavigationOnClickListener(v -> {
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        background_transparent_frame.setOnClickListener(v -> bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN));

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull @NotNull View bottomSheet, int newState) {

            }

            @Override
            public void onSlide(@NonNull @NotNull View bottomSheet, float slideOffset) {
                if (slideOffset > -0.5) {
                    background_transparent_frame.setBackgroundColor(getResources().getColor(R.color.transparent_400));
                    background_transparent_frame.setTranslationZ(10);
                } else {
                    background_transparent_frame.setBackgroundColor(0x00000000);
                    background_transparent_frame.setTranslationZ(-10);
                }
            }
        });

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onNavigationItemSelected(@NonNull @NotNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.prev_pdf:
                        replaceFragment(PdfFragment.newInstance(), "PDF_FRAGMENT");
                        item.setChecked(true);
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                        return true;

                    case R.id.appInfo:
                        Intent infoIntent = new Intent(MainActivity.this, AppInfoActivity.class);
                        startActivity(infoIntent);
                        item.setChecked(true);
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                        return true;

                    case R.id.privacyPoli:
                        Intent privacyIntent = new Intent(MainActivity.this, PrivacyActivity.class);
                        startActivity(privacyIntent);
                        item.setChecked(true);
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                        return true;

                    case R.id.rateApp:
                        askForReview();
                        item.setChecked(true);
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                        return true;

                }
                return false;
            }
        });
        if (allPermissionsGranted()) {
            initApp();
        }
    }


    private void getAppCountAndIncrement() {
        int appCount = pref.getInt("timesOpened", 1);
        appCount++;
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("timesOpened", appCount);
        editor.apply();
    }

    private void askForReview() {
        final String appPackageName = getPackageName();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName + "reviewId=0")));
        }
    }

    private boolean allPermissionsGranted() {
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String perm : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(perm);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            HashMap<String, Integer> permissionResult = new HashMap<>();
            int deniedCount = 0;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    permissionResult.put(permissions[i], grantResults[i]);
                    deniedCount++;
                }
            }

            //check if all permissions are granted
            if (deniedCount == 0) {
                initApp();
            } else {//Atleast one or all permissions are denied
                for (Map.Entry<String, Integer> entry : permissionResult.entrySet()) {
                    String permName = entry.getKey();
                    int permResult = entry.getValue();
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permName)) {
                        showDialog("", "This app needs Camera and Storage permissions to work without any problems.", "Yes, Grant Permissions", new AbstractDialog.OnClickListener() {
                            @Override
                            public void onClick(dev.shreyaspatil.MaterialDialog.interfaces.DialogInterface dialogInterface, int which) {
                                dialogInterface.dismiss();
                                allPermissionsGranted();
                            }
                        }, "No, Exit App", new AbstractDialog.OnClickListener() {

                            @Override
                            public void onClick(dev.shreyaspatil.MaterialDialog.interfaces.DialogInterface dialogInterface, int which) {
                                dialogInterface.dismiss();
                                finish();
                            }
                        }, false);
                    } else {
                        showDialog("", "You have denied some permissions. Allow all permissions at [Setting] > [Permissions]", "Go to Settings", new AbstractDialog.OnClickListener() {

                            @Override
                            public void onClick(dev.shreyaspatil.MaterialDialog.interfaces.DialogInterface dialogInterface, int which) {
                                dialogInterface.dismiss();
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            }
                        }, "No, Exit App", new AbstractDialog.OnClickListener() {

                            @Override
                            public void onClick(dev.shreyaspatil.MaterialDialog.interfaces.DialogInterface dialogInterface, int which) {
                                dialogInterface.dismiss();
                                finish();
                            }
                        }, false);
                        break;
                    }
                }
            }
        }
    }

    private void initApp() {
        getAppCountAndIncrement();
//        toc = findViewById(R.id.txt_Toc);
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in,
                        R.anim.fade_out,
                        R.anim.fade_in,
                        R.anim.slide_out)
                .addToBackStack(BACK_STACK_ROOT_TAG)
                .replace(R.id.main_frame_layout, ProjectHistoryFragment.newInstance(), BACK_STACK_ROOT_TAG)
                .commit();

        MainViewModel viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        viewModel._title.observe(this, s -> Objects.requireNonNull(getSupportActionBar()).setTitle(s));


        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
//        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//        Boolean switchPref = sharedPreferences.getBoolean(SettingsActivity.KEY_PREF_AUTO,false);
//        Toast.makeText(this,switchPref.toString(),Toast.LENGTH_SHORT).show();
    }


    public void replaceFragment(Fragment fragment, String tag) {
        //Get current fragment placed in container
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.main_frame_layout);

        //Prevent adding same fragment on top
        if (Objects.requireNonNull(currentFragment).getClass() == fragment.getClass()) {
            return;
        }

        //If fragment is already on stack, we can pop back stack to prevent stack infinite growth
        if (getSupportFragmentManager().findFragmentByTag(tag) != null) {
            getSupportFragmentManager().popBackStack(tag, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        //Otherwise, just replace fragment
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in,
                        R.anim.fade_out,
                        R.anim.fade_in,
                        R.anim.slide_out)
                .addToBackStack(tag)
                .replace(R.id.main_frame_layout, fragment, tag)
                .commit();
    }


    public void showDialog(String title, String msg, String positiveLabel, AbstractDialog.OnClickListener positiveOnClick, String negativeLabel, AbstractDialog.OnClickListener negativeOnClick, boolean isCancelLabel) {
        customDialog = new CustomDialog(this);
        customDialog.showMyDialog(title, msg, isCancelLabel, positiveLabel, R.drawable.ic_done, positiveOnClick, negativeLabel, R.drawable.ic_close, negativeOnClick);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == UPDATE_REQUEST_CODE){
            if(resultCode != RESULT_OK){
                Log.d("MainActivity", "Update flow failed! Result code: " + resultCode);
            }
        }
    }

    @Override
    public void onBackPressed() {

        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        } else {
            int fragmentInStack = getSupportFragmentManager().getBackStackEntryCount();
            if (fragmentInStack > 1) {
                getSupportFragmentManager().popBackStack();
            } else if (fragmentInStack == 1) {
                finish();
            } else {
                super.onBackPressed();
                finish();
            }
        }
    }

    @Override
    public void onInAppUpdateError(int code, Throwable error) {
        Log.d("MainActivity", "code: " + code, error);
    }

    @Override
    public void onInAppUpdateStatus(InAppUpdateStatus status) {

    }
}