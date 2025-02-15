//package com.aonescan.scanner.CostumClass;
//
//import android.app.Activity;
//import android.app.Application;
//import android.os.Bundle;
//import android.util.Log;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.lifecycle.Lifecycle;
//import androidx.lifecycle.LifecycleObserver;
//import androidx.lifecycle.OnLifecycleEvent;
//import androidx.lifecycle.ProcessLifecycleOwner;
//
//import com.aonescan.scanner.MyApplication;
//import com.google.android.gms.ads.AdError;
//import com.google.android.gms.ads.AdRequest;
//import com.google.android.gms.ads.FullScreenContentCallback;
//import com.google.android.gms.ads.LoadAdError;
//import com.google.android.gms.ads.appopen.AppOpenAd;
//
//import org.jetbrains.annotations.NotNull;
//
//import java.util.Date;
//
//import static androidx.lifecycle.Lifecycle.Event.ON_CREATE;
//import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
//import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;
//import static androidx.lifecycle.Lifecycle.Event.ON_START;
//
//public class AppOpenManager implements LifecycleObserver,Application.ActivityLifecycleCallbacks {
//    private static final String LOG_TAG = "AppOpenManager";
//    private static final String AD_UNIT_ID = "ca-app-pub-7148598053427607/2061808863";
//    private AppOpenAd appOpenAd = null;
//    private static boolean isShowingAd = false;
//    private AppOpenAd.AppOpenAdLoadCallback loadCallback;
//    private Activity currentActivity;
//    private long loadTime = 0;
//    private final MyApplication myApplication;
//
//    /** Constructor */
//    public AppOpenManager(MyApplication myApplication) {
//        this.myApplication = myApplication;
//        this.myApplication.registerActivityLifecycleCallbacks(this);
//        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
//    }
//
//    @OnLifecycleEvent(ON_START)
//    public void onStart() {
//        showAdIfAvailable();
//        Log.d(LOG_TAG, "onStart");
//    }
//
//    public void showAdIfAvailable() {
//        // Only show ad if there is not already an app open ad currently showing
//        // and an ad is available.
//        if (!isShowingAd && isAdAvailable()) {
//            Log.d(LOG_TAG, "Will show ad.");
//
//            FullScreenContentCallback fullScreenContentCallback =
//                    new FullScreenContentCallback() {
//                        @Override
//                        public void onAdDismissedFullScreenContent() {
//                            // Set the reference to null so isAdAvailable() returns false.
//                            AppOpenManager.this.appOpenAd = null;
//                            isShowingAd = false;
//                            fetchAd();
//                        }
//
//                        @Override
//                        public void onAdFailedToShowFullScreenContent(AdError adError) {}
//
//                        @Override
//                        public void onAdShowedFullScreenContent() {
//                            isShowingAd = true;
//                        }
//                    };
//
//            appOpenAd.setFullScreenContentCallback(fullScreenContentCallback);
//            appOpenAd.show(currentActivity);
//
//        } else {
//            Log.d(LOG_TAG, "Can not show ad.");
//            fetchAd();
//        }
//    }
//
//    /** Request an ad */
//    public void fetchAd() {
//        // We will implement this below.
//        if(isAdAvailable()){
//            return;
//        }
//        loadCallback = new AppOpenAd.AppOpenAdLoadCallback() {
//            @Override
//            public void onAdLoaded(@NonNull @NotNull AppOpenAd appOpenAd) {
//                AppOpenManager.this.appOpenAd = appOpenAd;
//                AppOpenManager.this.loadTime = (new Date()).getTime();
//            }
//
//            @Override
//            public void onAdFailedToLoad(@NonNull @NotNull LoadAdError loadAdError) {
//
//            }
//        };
//        AdRequest request = getAdRequest();
//        AppOpenAd.load(
//                myApplication,AD_UNIT_ID,request,AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,loadCallback
//        );
//    }
//
//    /** Creates and returns ad request. */
//    private AdRequest getAdRequest() {
//        return new AdRequest.Builder().build();
//    }
//
//    private boolean wasLoadTimeLessThanNHoursAgo(long numHours) {
//        long dateDifference = (new Date()).getTime() - this.loadTime;
//        long numMilliSecondsPerHour = 3600000;
//        return (dateDifference < (numMilliSecondsPerHour * numHours));
//    }
//
//    /** Utility method that checks if ad exists and can be shown. */
//    public boolean isAdAvailable() {
//        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4);
//    }
//
//    @Override
//    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
//
//    }
//
//    @Override
//    public void onActivityStarted(@NonNull Activity activity) {
//        currentActivity = activity;
//    }
//
//    @Override
//    public void onActivityResumed(@NonNull Activity activity) {
//        currentActivity = activity;
//    }
//
//    @Override
//    public void onActivityPaused(@NonNull Activity activity) {
//
//    }
//
//    @Override
//    public void onActivityStopped(@NonNull Activity activity) {
//
//    }
//
//    @Override
//    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
//
//    }
//
//    @Override
//    public void onActivityDestroyed(@NonNull Activity activity) {
//        currentActivity = null;
//    }
//}