//package com.aonescan.scanner;
//
//import android.app.Application;
//import android.content.Intent;
//
//import com.aonescan.scanner.CostumClass.AppOpenManager;
//import com.google.android.gms.ads.MobileAds;
//import com.google.android.gms.ads.RequestConfiguration;
//import com.google.android.gms.ads.initialization.InitializationStatus;
//import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
//
//import java.util.Arrays;
//import java.util.List;
//
//public class MyApplication extends Application {
//
//    private static AppOpenManager appOpenManager;
//
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        MobileAds.initialize(
//                this,
//                new OnInitializationCompleteListener() {
//                    @Override
//                    public void onInitializationComplete(InitializationStatus initializationStatus) {
//
//                    }
//                });
//
//        List<String> testDeviceIds = Arrays.asList("7948DE963ECC155EF8A346ED9E86531B","30B542D714F718DA7E924CEE0E364374");
//        RequestConfiguration configuration = new RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds)
//                .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
//                .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
//                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_PG).build();
//        MobileAds.setRequestConfiguration(configuration);
//
//        appOpenManager = new AppOpenManager(this);
//    }
//}
