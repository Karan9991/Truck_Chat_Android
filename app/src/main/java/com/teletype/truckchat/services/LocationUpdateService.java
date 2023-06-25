package com.teletype.truckchat.services;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.teletype.truckchat.ui.conversations.ConversationsActivity;
import com.teletype.truckchat.R;
import com.teletype.truckchat.util.Constants;
import com.teletype.truckchat.util.Utils;

import java.util.concurrent.TimeUnit;

public final class LocationUpdateService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "TAG_" + LocationUpdateService.class.getSimpleName();

    private static final long UPDATE_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(15);
    private static final long UPDATE_FASTEST_INTERVAL_MILLIS = UPDATE_INTERVAL_MILLIS / 2;

    private GoogleApiClient mGoogleApiClient;
    private boolean mIsInResolution;
    private boolean mBroadcastErrors;
    private AsyncTask mUpdateDeviceAsyncTask;
    private Location mLastLocation;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mBroadcastErrors = intent != null;

        if (Utils.isLocationDisabled(this)) {
            if (mBroadcastErrors) {
                LocalBroadcastManager.getInstance(this).sendBroadcast(
                        new Intent(Constants.BROADCAST_ACTION_LOCATION_UPDATE)
                                .putExtra(Constants.BROADCAST_EXTRA_EVENT, Constants.EVENT_LOCATION_SERVICES_UNAVAILABLE)
                                .putExtra(Constants.BROADCAST_EXTRA_STATUS_CODE, -1)
                                .putExtra(Constants.BROADCAST_EXTRA_STATUS_STRING, getString(R.string.error_location_services_unavailable)));
            } else {
                //TODO
            }

            stopSelfResult(startId);
            return START_NOT_STICKY;
        }

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        if (mGoogleApiClient.isConnected()) {
            requestLocationUpdates();
        } else {
            retryConnecting();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        cancelUpdateDeviceLocation();

        if (mGoogleApiClient != null) {
            if (mGoogleApiClient.isConnected()) {
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            }
            mGoogleApiClient.disconnect();
            mGoogleApiClient = null;
        }

        mIsInResolution = false;
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        requestLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        retryConnecting();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (mIsInResolution) {
            return;
        }

        int error_code = result.getErrorCode();
        String error_string = GoogleApiAvailability.getInstance().getErrorString(error_code);

        Intent intent = (mBroadcastErrors ? new Intent(Constants.BROADCAST_ACTION_LOCATION_UPDATE) : new Intent(this, ConversationsActivity.class))
                .putExtra(Constants.BROADCAST_EXTRA_EVENT, Constants.EVENT_GOOGLE_PLAY_ERROR)
                .putExtra(Constants.BROADCAST_EXTRA_STATUS_CODE, error_code)
                .putExtra(Constants.BROADCAST_EXTRA_STATUS_STRING, error_string);

        if (result.hasResolution()) {
            intent.putExtra(Constants.BROADCAST_EXTRA_RESOLVABLE_PENDINGINTENT, result.getResolution());
            mIsInResolution = true;
        }

        if (mBroadcastErrors) {
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        } else {
            PendingIntent notifyPendingIntent =
                    PendingIntent.getActivity(this, Constants.EVENT_GOOGLE_PLAY_ERROR, intent, PendingIntent.FLAG_CANCEL_CURRENT);

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle(getString(R.string.app_name))
                            .setContentText(getString(R.string.error_googleplayservices_connecting))
                            .setContentIntent(notifyPendingIntent)
                            .setAutoCancel(true);

            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(Constants.EVENT_GOOGLE_PLAY_ERROR, builder.build());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            if (mBroadcastErrors) {
                Intent intent = new Intent(Constants.BROADCAST_ACTION_LOCATION_UPDATE)
                        .putExtra(Constants.BROADCAST_EXTRA_EVENT, Constants.EVENT_NEW_LOCATION)
                        .putExtra(Constants.BROADCAST_EXTRA_LOCATION, location);

                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            }

            if (mLastLocation == null ||
                    mLastLocation.getLatitude() != location.getLatitude() ||
                    mLastLocation.getLongitude() != location.getLongitude()) {
                updateDeviceLocation(location);
            }
            mLastLocation = location;

        }
    }

    private void retryConnecting() {
        mIsInResolution = false;

        if (!mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }
    }

    private void requestLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            if (lastLocation != null) {
                onLocationChanged(lastLocation);
            }

            LocationRequest locationRequest = new LocationRequest()
                    .setInterval(UPDATE_INTERVAL_MILLIS)
                    .setFastestInterval(UPDATE_FASTEST_INTERVAL_MILLIS)
                    .setPriority(LocationRequest.PRIORITY_LOW_POWER);

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this)
                    .setResultCallback(
                            new ResultCallback<Status>() {
                                @Override
                                public void onResult(Status status) {
                                    if (!status.isSuccess()) {
                                        if (mBroadcastErrors) {
                                            LocalBroadcastManager.getInstance(LocationUpdateService.this).sendBroadcast(
                                                    new Intent(Constants.BROADCAST_ACTION_LOCATION_UPDATE)
                                                            .putExtra(Constants.BROADCAST_EXTRA_EVENT, Constants.EVENT_REQUEST_LOCATION)
                                                            .putExtra(Constants.BROADCAST_EXTRA_STATUS_CODE, status.getStatusCode())
                                                            .putExtra(Constants.BROADCAST_EXTRA_STATUS_STRING, status.getStatusMessage()));
                                        } else {
                                            //TODO
                                        }
                                    }
                                }
                            }
                    );
        } else {
            LocalBroadcastManager.getInstance(LocationUpdateService.this).sendBroadcast(
                    new Intent(Constants.BROADCAST_ACTION_LOCATION_UPDATE)
                            .putExtra(Constants.BROADCAST_EXTRA_EVENT, Constants.EVENT_REQUEST_PERMISSION)
                            .putExtra(Constants.BROADCAST_EXTRA_STATUS_CODE, PackageManager.PERMISSION_DENIED)
                            .putExtra(Constants.BROADCAST_EXTRA_STATUS_STRING, Manifest.permission.ACCESS_COARSE_LOCATION));
        }
    }

    private void updateDeviceLocation(Location location) {
        cancelUpdateDeviceLocation();

        mUpdateDeviceAsyncTask = new DeviceUpdateAsyncTask(this, location).run();
    }

    private void cancelUpdateDeviceLocation() {
        if (mUpdateDeviceAsyncTask != null) {
            mUpdateDeviceAsyncTask.cancel(true);
            mUpdateDeviceAsyncTask = null;
        }
    }

    private class DeviceUpdateAsyncTask extends ChatServerAPI.DeviceUpdateAsyncTask {

        private final Context context;
        //private final Location location;

        public DeviceUpdateAsyncTask(Context context, Location location) {
            super(Utils.getSerialNumber(context), location.getLatitude(), location.getLongitude());

            this.context = context;
            //this.location = new Location(location);
        }

        @Override
        public void onDeviceUpdateResult(boolean success, int status, String message) {
            mUpdateDeviceAsyncTask = null;

            if (success) {
                if (mBroadcastErrors) {
                    Intent intent = new Intent(Constants.BROADCAST_ACTION_LOCATION_UPDATE)
                            .putExtra(Constants.BROADCAST_EXTRA_EVENT, Constants.EVENT_DEVICE_UPDATE)
                            .putExtra(Constants.BROADCAST_EXTRA_STATUS_CODE, status)
                            .putExtra(Constants.BROADCAST_EXTRA_STATUS_STRING, message);

                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                }
            } else {
                Log.e(TAG, message);
            }
        }
    }

}
