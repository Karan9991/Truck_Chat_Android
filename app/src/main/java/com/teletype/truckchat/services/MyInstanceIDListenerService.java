package com.teletype.truckchat.services;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.google.android.gms.iid.InstanceIDListenerService;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.teletype.truckchat.util.Constants;
import com.teletype.truckchat.util.Utils;

import java.io.IOException;


public class MyInstanceIDListenerService extends FirebaseInstanceIdService {

    private static final String TAG = "TAG_" + MyInstanceIDListenerService.class.getSimpleName();

    @Override
    public void onTokenRefresh() {
        Log.i(TAG, "Refreshing token...");

        startService(new Intent(this, RegistrationIntentService.class));
    }
}
