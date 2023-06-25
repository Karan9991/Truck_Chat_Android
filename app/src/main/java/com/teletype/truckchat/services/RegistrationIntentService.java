package com.teletype.truckchat.services;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.teletype.truckchat.util.Constants;
import com.teletype.truckchat.util.Utils;

public class RegistrationIntentService extends IntentService {

    private static final String TAG = "TAG_" + RegistrationIntentService.class.getSimpleName();

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Intent broadcastIntent = new Intent(
                intent.hasExtra(Constants.BROADCAST_ACTION_REGISTRATION) ? intent.getStringExtra(Constants.BROADCAST_ACTION_REGISTRATION) : Constants.BROADCAST_ACTION_REGISTRATION);

        synchronized (TAG) {
            String token = FirebaseInstanceId.getInstance().getToken();
            ChatServerAPI.RegistrationAsyncTask registrationAsyncTask =
                    new ChatServerAPI.RegistrationAsyncTask(Utils.getSerialNumber(this), token);

            if (registrationAsyncTask.runNow()) {
                if (registrationAsyncTask.status_code == 200) {
                    Utils.storeRegistrationId(this, token, registrationAsyncTask.user_id);
                }
            }

            broadcastIntent.putExtra(Constants.BROADCAST_EXTRA_STATUS_CODE, registrationAsyncTask.status_code)
                    .putExtra(Constants.BROADCAST_EXTRA_STATUS_STRING, registrationAsyncTask.status_message);
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }
}
