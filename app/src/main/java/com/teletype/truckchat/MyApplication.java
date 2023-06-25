package com.teletype.truckchat;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.provider.Settings;

import com.google.firebase.FirebaseApp;


public class MyApplication extends Application {

    private static MyApplication singleton;

    private static int resumed;
    private static int paused;
    private static int started;
    private static int stopped;

    public MyApplication getInstance() {
        return singleton;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;
        FirebaseApp.initializeApp(MyApplication.this);

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(Activity activity) {
                ++started;
            }

            @Override
            public void onActivityResumed(Activity activity) {
                ++resumed;
            }

            @Override
            public void onActivityPaused(Activity activity) {
                ++paused;
            }

            @Override
            public void onActivityStopped(Activity activity) {
                ++stopped;
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });

        String androidId =  Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        deviceId = MD5(androidId).toUpperCase();
    }

    public static String getDeviceId() {
        return deviceId;
    }

    public String MD5(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
    }

    private static String deviceId;

    public static boolean isVisible() {
        return started > stopped;
    }

    public static boolean isInForeground() {
        return resumed > paused;
    }

    public static boolean isInBackground() {
        return !isVisible() && !isInForeground();
    }

}
