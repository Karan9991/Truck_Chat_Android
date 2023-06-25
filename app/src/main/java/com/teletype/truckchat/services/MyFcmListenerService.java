package com.teletype.truckchat.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;

import com.google.android.gms.gcm.GcmListenerService;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.teletype.truckchat.ui.conversations.ConversationsActivity;
import com.teletype.truckchat.MyApplication;
import com.teletype.truckchat.R;
import com.teletype.truckchat.util.Constants;
import com.teletype.truckchat.util.Utils;

import java.util.Map;
import java.util.concurrent.TimeUnit;


public class MyFcmListenerService extends FirebaseMessagingService {

    private static final String EXTRA_NOTIFICATION_TYPE = "notification_type" ;
    private static final int NOTIFICATION_TYPE_RECEIVE = 1;
    private static final int NOTIFICATION_TYPE_REPLY = 2;

    @Override
    public void onMessageReceived(RemoteMessage message) {
        String from = message.getFrom();
        Map<String, String> data = message.getData();

        if (data.containsKey(EXTRA_NOTIFICATION_TYPE)) {
            try {
                boolean isMessageHandled = false;

                switch (Integer.parseInt(data.get(EXTRA_NOTIFICATION_TYPE))) {
                    case NOTIFICATION_TYPE_RECEIVE:
                        isMessageHandled = ChatServerAPI.processNewConversation(getContentResolver(), data);
                        break;

                    case NOTIFICATION_TYPE_REPLY:
                        isMessageHandled = ChatServerAPI.processConversationReply(getContentResolver(), data);
                        break;
                }

                if (isMessageHandled && MyApplication.isInBackground()) {
                    SharedPreferences sharedPreferences = Utils.getSharedPreferences(this);
                    if (sharedPreferences.getBoolean(Constants.PREFS_NOTIFICATION, true)) {
                        long now = System.nanoTime();
                        long before = sharedPreferences.getLong(Constants.PREFS_LAST_NOTIFICATION, 0);
                        boolean allowSoundOrVibrate = (now < before) || (now - before > TimeUnit.SECONDS.toNanos(2));

                        Intent intent = new Intent(this, ConversationsActivity.class);
                        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                        Notification.Builder builder =
                                new Notification.Builder(this)
                                        .setSmallIcon(R.drawable.ic_stat_notification)
                                        .setContentTitle(getString(R.string.pending_messages_title))
                                        .setContentText(getString(R.string.pending_messages_text))
                                        .setContentIntent(pendingIntent)
                                        .setAutoCancel(true);

                        if (allowSoundOrVibrate) {
                            String ringtone = sharedPreferences.getString(Constants.PREFS_NOTIFICATION_RINGTONE, Settings.System.DEFAULT_NOTIFICATION_URI.toString());
                            if (!TextUtils.isEmpty(ringtone)) {
                                builder.setSound(Uri.parse(ringtone));
                            }
                        }

                        Notification notification;
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                            //noinspection deprecation
                            notification = builder.getNotification();
                        } else {
                            notification = builder.build();
                        }

                        if (allowSoundOrVibrate) {
                            if (sharedPreferences.getBoolean(Constants.PREFS_NOTIFICATION_VIBRATE, true)) {
                                notification.defaults |= Notification.DEFAULT_VIBRATE;
                            }
                        }

                        sharedPreferences.edit().putLong(Constants.PREFS_LAST_NOTIFICATION, now).apply();

                        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        mNotificationManager.notify(Constants.EVENT_NEW_MESSAGE, notification);
                    }
                }

            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }
}
