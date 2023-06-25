package com.teletype.truckchat.ui.conversations;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.teletype.truckchat.ui.common.OnFragmentInteractionListener;
import com.teletype.truckchat.R;
import com.teletype.truckchat.ui.news.NewsFragment;
import com.teletype.truckchat.ui.road.RoadClosuresAdapter;
import com.teletype.truckchat.db.ConversationsContract;
import com.teletype.truckchat.services.ChatServerAPI;
import com.teletype.truckchat.services.LocationUpdateService;
import com.teletype.truckchat.services.RegistrationIntentService;
import com.teletype.truckchat.ui.common.InterWebActivity;
import com.teletype.truckchat.ui.replies.RepliesActivity;
import com.teletype.truckchat.ui.settings.SettingsPreferenceActivity;
import com.teletype.truckchat.ui.common.SlidingTabsFragment;
import com.teletype.truckchat.util.Constants;
import com.teletype.truckchat.util.Utils;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


public class ConversationsActivity
        extends FragmentActivity
        implements OnFragmentInteractionListener, ConversationsFragment.InterWebFragment.OnFragmentInterWebListener,
        ActivityCompat.OnRequestPermissionsResultCallback, NewsFragment.NewsResults {
    private static final int REQUEST_CODE = 0x0001;
    private static final int REQUEST_CODE2 = 0x0002;private static final int REQUEST_CODE3 = 0x0003;
    private static final int REQUEST_CODE_PERMISSION = 0x0004;private boolean mIsBusy = false;
    private boolean mIsReadyToFinish = false;private boolean mSkipGooglePlayServices = false;
    private Boolean mWantPreviousMessages;private boolean mIsRequestingPermission = false;
    private Intent mBroadcastIntent;private View mViewStatus;
    private TextView mTextViewStatus;
    private Location mLocation;private MenuItem mMenuNewConversation;private AdView mAdView;
    private AsyncTask mGetPreviousMessagesAsyncTask;
    private InterstitialAd mInterstitialAd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_conversations);
        setProgressBarIndeterminateVisibility(false);

        PreferenceManager.setDefaultValues(this, R.xml.preference_settings_messages, false);
        PreferenceManager.setDefaultValues(this, R.xml.preference_settings_notifications, false);
        Utils.getSharedPreferences(this).edit().putString(Constants.PREFS_MSG_AUTOHIDE, "1").apply();
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(Constants.BROADCAST_EXTRA_LOCATION)) {
                mLocation = savedInstanceState.getParcelable(Constants.BROADCAST_EXTRA_LOCATION);
            }
        }
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                showFullscreenAd();
            }
        }, 60000);
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocationUpdateReceiver, new IntentFilter(Constants.BROADCAST_ACTION_LOCATION_UPDATE));
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationReceiver, new IntentFilter(Constants.BROADCAST_ACTION_REGISTRATION));
        initWidgets();
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(Constants.BROADCAST_EXTRA_EVENT)) {
                if (intent.getIntExtra(Constants.BROADCAST_EXTRA_EVENT, -1) == Constants.EVENT_GOOGLE_PLAY_ERROR) {
                    mBroadcastIntent = new Intent(Constants.BROADCAST_ACTION_LOCATION_UPDATE).putExtra(Constants.BROADCAST_EXTRA_EVENT, Constants.EVENT_GOOGLE_PLAY_ERROR).putExtra(Constants.BROADCAST_EXTRA_STATUS_CODE, intent.getIntExtra(Constants.BROADCAST_EXTRA_STATUS_CODE, -1)).putExtra(Constants.BROADCAST_EXTRA_STATUS_STRING, intent.getStringExtra(Constants.BROADCAST_EXTRA_STATUS_STRING));
                    if (intent.hasExtra(Constants.BROADCAST_EXTRA_RESOLVABLE_PENDINGINTENT)) {
                        PendingIntent pendingIntent = intent.getParcelableExtra(Constants.BROADCAST_EXTRA_RESOLVABLE_PENDINGINTENT);
                        if (pendingIntent != null) {
                            mBroadcastIntent.putExtra(Constants.BROADCAST_EXTRA_RESOLVABLE_PENDINGINTENT, pendingIntent);
                        }
                    }
                }
            }
        }
        AdRequest.Builder builder = new AdRequest.Builder();
    }
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (mLocation != null) {
            outState.putParcelable(Constants.BROADCAST_EXTRA_LOCATION, mLocation);
        }
        super.onSaveInstanceState(outState);
    }
    @Override
    protected void onResume() {
        super.onResume();
        boolean isIAgreed = Utils.getSharedPreferences(this).getBoolean(Constants.PREFS_I_AGREE, false);
        if (isIAgreed) {
            resume();
        } else {
            new AlertDialog.Builder(this).setTitle(R.string.about_tos).setMessage(String.format("%s\n\n%s", getString(R.string.about_tos_body), getString(R.string.about_tos_agree))).setCancelable(false).setPositiveButton(R.string.about_agree, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Utils.getSharedPreferences(ConversationsActivity.this).edit().putBoolean(Constants.PREFS_I_AGREE, true).apply();
                            resume();
                        }
                    })
                    .setNegativeButton(R.string.about_disagree, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .show();
        }
    }
    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocationUpdateReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationReceiver);
        if (mGetPreviousMessagesAsyncTask != null) {
            mGetPreviousMessagesAsyncTask.cancel(false);
            mGetPreviousMessagesAsyncTask = null;
        }
        super.onDestroy();
    }
    @Override
    public void onBackPressed() {
        Fragment fragment_ = getSupportFragmentManager().findFragmentById(R.id.conversationsActivity_frameLayout_content);
        if (fragment_ != null) {
            if (!((SlidingTabsFragment) fragment_).onBackPressed()) {
                return;
            }
        }
        super.onBackPressed();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE:
                mIsReadyToFinish = resultCode != Activity.RESULT_OK;
                break;
            case REQUEST_CODE2:
                mSkipGooglePlayServices = resultCode != Activity.RESULT_OK;
                break;
            case REQUEST_CODE3:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String cid = data.getStringExtra(Constants.BROADCAST_EXTRA_CONVERSATION_ID);
                    String topic = data.getStringExtra(Constants.BROADCAST_EXTRA_MESSAGE);
                    int emoji_id = data.getIntExtra(Constants.EMOJI_ID, 0);
                    if (!TextUtils.isEmpty(cid) && !TextUtils.isEmpty(topic) && mLocation != null) {
                        onFragmentInteractionItemClick(new ConversationsAdapter.Conversation(cid, topic, 0, System.currentTimeMillis(), false, false, 0, 0, String.valueOf(emoji_id)), false);
                    }
                }
                break;
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_conversations, menu);
        mMenuNewConversation = menu.findItem(R.id.action_new_conversation);
        return true;
    }
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        setupNewConversationAction();
        return super.onPrepareOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_new_conversation:
                if (mLocation == null) {
                    if (Utils.isLocationDisabled(this)) {
                        showEnableLocationServicesDlg();
                    } else {
                        Toast toast = Toast.makeText(this, R.string.error_location_unavailable, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                } else {
                    startActivityForResult(
                            new Intent(this, NewConversationActivity.class).putExtra(Constants.BROADCAST_EXTRA_LOCATION, mLocation),
                            REQUEST_CODE3);
                }
                break;
            case R.id.action_select_all:
                Fragment fragment_ = getSupportFragmentManager().findFragmentById(R.id.conversationsActivity_frameLayout_content);
                if (fragment_ != null) {
                    ((SlidingTabsFragment) fragment_).actionSelectAll();
                }
                break;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsPreferenceActivity.class));
                break;
            case R.id.action_tell_a_friend:
                Utils.composeTellAFriendEmail(this);
                break;
            case R.id.action_help:
                startActivity(new Intent(this, InterWebActivity.class).putExtra(Constants.EXTRA_URI, Constants.WEBPAGE_HELP));
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }
    @Override
    public void onFragmentInteractionItemClick(ConversationsAdapter.Conversation conversation, boolean ignoreAutoHide) {
        if (mLocation == null) {
            if (Utils.isLocationDisabled(this)) {
                showEnableLocationServicesDlg();} else {
                Toast toast = Toast.makeText(this, R.string.error_location_unavailable, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();}} else {
            startActivity(new Intent(this, RepliesActivity.class).putExtra(Constants.BROADCAST_EXTRA_LOCATION, mLocation).putExtra(Constants.BROADCAST_EXTRA_CONVERSATION_ID, conversation.cid).putExtra(Constants.BROADCAST_EXTRA_MESSAGE, conversation.message).putExtra(Constants.BROADCAST_EXTRA_IGNORE_AUTOHIDE, ignoreAutoHide).putExtra(Constants.EMOJI_ID, conversation.emoji_id)
            );}}
    @Override
    public void onFragmentInteractionItemLongClick(ConversationsAdapter.Conversation conversation) {
        final ConversationsAdapter.Conversation saved = conversation;int itemsid;
        if (saved.isStarred) {
            if (saved.unread == 0) {itemsid = R.array.action_unstar_unread;
            } else {itemsid = R.array.action_unstar_read;}} else {
            if (saved.unread == 0) {itemsid = R.array.action_star_unread;
            } else {itemsid = R.array.action_star_read;}}
        new AlertDialog.Builder(this).setItems(itemsid, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Uri uri = Uri.withAppendedPath(ConversationsContract.Conversation.CONTENT_URI, saved.cid).buildUpon().build();
                        ContentValues values = new ContentValues(1);
                        switch (which) {
                            case 0:
                                values.put(ConversationsContract.Conversation._FLAG, saved.isStarred ? (saved.flag & ~0x1) : (saved.flag | 0x1));
                                break;
                            case 1:
                                if (saved.unread == 0) {values.putNull(ConversationsContract.Conversation._UNREAD);} else {values.put(ConversationsContract.Conversation._UNREAD, 0);}
                                break;
                            default:
                                return;
                        }
                        getContentResolver().update(uri, values, null, null);
                    }}).setNegativeButton(android.R.string.cancel, null).show();}
    @Override
    public void onFragmentInteractionItemClick(RoadClosuresAdapter.RoadClosure roadClosure) {
        try {String url = String.format(Locale.US, "geo:q=loc:%s@%.6f,%.6f", roadClosure.highwayName, roadClosure.latitude, roadClosure.longitude);
            startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url)));} catch (ActivityNotFoundException e) {e.printStackTrace();}}
    @Override
    public void onEmptyData() {
        if (mWantPreviousMessages == null) {if (Utils.isFirstRun(this)) {mWantPreviousMessages = false;} else {if (mLocation == null) {mWantPreviousMessages = true;} else {mWantPreviousMessages = false;getPreviousMessages();}}}
    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {showProgressBarIndeterminate(true);}
    @Override
    public void onPageFinished(WebView view, String url) {showProgressBarIndeterminate(false);}
    private void resume() {String userHandle = Utils.getSharedPreferences(this).getString(Constants.PREFS_MSG_HANDLE, "");if (TextUtils.isEmpty(userHandle)) {setTitle(R.string.app_name);} else {
            setTitle(getString(R.string.app_name_with_handle, userHandle));}
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(Constants.EVENT_NEW_MESSAGE);
        if (mBroadcastIntent == null) {checkGooglePlayServices();} else {mLocationUpdateReceiver.onReceive(this, mBroadcastIntent);
            mBroadcastIntent = null;}
        if (mIsReadyToFinish) {showRequireGooglePlayServicesAndExit();}}
    private void initWidgets() {if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {ActionBar actionBar = getActionBar();
            if (actionBar != null) {actionBar.setHomeAsUpIndicator(R.drawable.empty);}} else {View view = findViewById(android.R.id.home);
            if (view != null) {ViewGroup home = (ViewGroup) view.getParent();ImageView up = (ImageView) home.getChildAt(0);
                if (up != null) {up.setImageResource(R.drawable.empty);}}}
        ImageView view = (ImageView) findViewById(android.R.id.home);
        if (view != null) {view.setPadding(getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin_half), 0, 0, 0);}
        mViewStatus = findViewById(R.id.conversationsActivity_relativeLayout_status);
        mTextViewStatus = (TextView) findViewById(R.id.conversationsActivity_textView_status);
        findViewById(R.id.conversationsActivity_button_retry).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mViewStatus.setVisibility(View.GONE);checkGooglePlayServices();}});
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.conversationsActivity_frameLayout_content);
        if (fragment == null) {Bundle bundle = new Bundle();bundle.putParcelable(Constants.BROADCAST_EXTRA_LOCATION, mLocation);fragment = SlidingTabsFragment.newInstance(bundle);fm.beginTransaction().add(R.id.conversationsActivity_frameLayout_content, fragment).commit();}}
        private int mShowProgressBarIndeterminateCount = 0;
    private void showProgressBarIndeterminate(boolean show) {if (show) {
            mShowProgressBarIndeterminateCount++;} else if (mShowProgressBarIndeterminateCount > 0) {mShowProgressBarIndeterminateCount--;}
        if (mShowProgressBarIndeterminateCount == 0) {setProgressBarIndeterminateVisibility(false);} else {setProgressBarIndeterminateVisibility(true);}}
    private void setIsBusy(boolean isBusy) {mIsBusy = isBusy;setupNewConversationAction();showProgressBarIndeterminate(mIsBusy);}
    private void setupNewConversationAction() {
        if (mMenuNewConversation != null) {if (mIsBusy) {mMenuNewConversation.setEnabled(false);mMenuNewConversation.setVisible(false);} else {mMenuNewConversation.setEnabled(true);mMenuNewConversation.setVisible(true);}}
    private void startServices() {
        if (mLocation == null) {setIsBusy(true);}
        String days = Utils.getSharedPreferences(this).getString(Constants.PREFS_MSG_AUTOHIDE, "1");
        if (!"0".equals(days)) {long daysInMillis = TimeUnit.DAYS.toMillis(Integer.parseInt(days));long expired = System.currentTimeMillis() - daysInMillis;
            String selection = String.format("%s < ?", ConversationsContract.Conversation._LUPDATED);
            String[] selectionArgs = new String[]{Long.toString(expired)};
            Uri uri = ConversationsContract.Conversation.CONTENT_URI_UNSTARRED.buildUpon().build();
            getContentResolver().delete(uri, selection, selectionArgs);}startService(new Intent(getApplicationContext(), LocationUpdateService.class));}
    private void checkGooglePlayServices() {
        if (mSkipGooglePlayServices) {mSkipGooglePlayServices = false;return;}
        if (!mIsReadyToFinish) {try {if (Utils.isGooglePlayServicesAvailable(this, REQUEST_CODE)) {String registrationId = Utils.getCurrentRegistrationId(this);
                    if (TextUtils.isEmpty(registrationId)) {registerInBackground();
                    } else {sendBroadcast(new Intent("com.google.android.intent.action.GTALK_HEARTBEAT"));
                        sendBroadcast(new Intent("com.google.android.intent.action.MCS_HEARTBEAT"));
                        startServices();}}} catch (UnsupportedOperationException e) {mIsReadyToFinish = true;}}}
    private void showRequireGooglePlayServicesAndExit() {new AlertDialog.Builder(this).setTitle(R.string.app_name).setMessage(R.string.error_googleplayservices_requires).setCancelable(false).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }}).show();}
    private void registerInBackground() {startService(new Intent(this, RegistrationIntentService.class));}
    private void showEnableLocationServicesDlg() {
        AlertDialog dialog = new AlertDialog.Builder(ConversationsActivity.this).setTitle(R.string.enable_location_services).setMessage(R.string.enable_location_services_desc).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));}}).setNegativeButton(android.R.string.no, null).create();dialog.setCanceledOnTouchOutside(false);dialog.show();}
    private final BroadcastReceiver mLocationUpdateReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {if (mLocation == null) {setIsBusy(false);}
                    mTextViewStatus.setText(intent.getStringExtra(Constants.BROADCAST_EXTRA_STATUS_STRING));
                    switch (intent.getIntExtra(Constants.BROADCAST_EXTRA_EVENT, -1)) {
                        case Constants.EVENT_REQUEST_LOCATION:
                        case Constants.EVENT_LOCATION_SERVICES_UNAVAILABLE:
                            showEnableLocationServicesDlg();
                        case Constants.EVENT_INVALID_REGISTRATION_ID:
                            mViewStatus.setVisibility(View.VISIBLE);
                            break;
                        case Constants.EVENT_GOOGLE_PLAY_ERROR:
                            mTextViewStatus.setText(R.string.error_googleplayservices_connecting);
                            mViewStatus.setVisibility(View.VISIBLE);
                            if (intent.hasExtra(Constants.BROADCAST_EXTRA_RESOLVABLE_PENDINGINTENT)) {PendingIntent pendingIntent = intent.getParcelableExtra(Constants.BROADCAST_EXTRA_RESOLVABLE_PENDINGINTENT);
                                try {startIntentSenderForResult(pendingIntent.getIntentSender(), REQUEST_CODE2, null, 0, 0, 0);
                                } catch (IntentSender.SendIntentException e) {e.printStackTrace();}}
                            break;
                        case Constants.EVENT_DEVICE_UPDATE:
                            if (intent.getIntExtra(Constants.BROADCAST_EXTRA_STATUS_CODE, -1) == 200) {mViewStatus.setVisibility(View.GONE);
                            } else {mViewStatus.setVisibility(View.VISIBLE);}
                            break;
                        case Constants.EVENT_NEW_LOCATION:
                            mLocation = intent.getParcelableExtra(Constants.BROADCAST_EXTRA_LOCATION);
                            if (mAdView == null) {
                                mAdView = (AdView) findViewById(R.id.conversationsActivity_adView);
                                mAdView.setAdListener(new AdListener() {
                                    @Override
                                    public void onAdLoaded() {
                                        mAdView.setVisibility(View.VISIBLE);}
                                });
                                Bundle extras = new Bundle();
                                extras.putBoolean("is_designed_for_families", false);
                                AdRequest adRequest = new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR).addTestDevice("CFEB1227FEDD32469041AC295FD25F25") // Man's tablet.setLocation(mLocation).addNetworkExtrasBundle(AdMobAdapter.class, extras).addKeyword("truck").tagForChildDirectedTreatment(false).build();
                                mAdView.loadAd(adRequest);}
                            if (Utils.isFirstRun(context) || Boolean.TRUE.equals(mWantPreviousMessages)) {mWantPreviousMessages = false;getPreviousMessages();}
                            break;
                        case Constants.EVENT_REQUEST_PERMISSION:
                            String status_msg = intent.getStringExtra(Constants.BROADCAST_EXTRA_STATUS_STRING);
                            int status_code = intent.getIntExtra(Constants.BROADCAST_EXTRA_STATUS_CODE, PackageManager.PERMISSION_DENIED);
                            if (Manifest.permission.ACCESS_COARSE_LOCATION.equals(status_msg) && PackageManager.PERMISSION_DENIED == status_code) {if (ActivityCompat.shouldShowRequestPermissionRationale(ConversationsActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION)) {if (!mIsRequestingPermission) {mIsRequestingPermission = true;
                                        AlertDialog alert = new AlertDialog.Builder(ConversationsActivity.this).setCancelable(false).setMessage(R.string.permissions_location).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {mIsRequestingPermission = false;
                                                        requestPermission();}}).create();
                                        alert.setCanceledOnTouchOutside(false);
                                        alert.show();}} else {requestPermission();}}
                            break;}}
            };
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION: {
                if (grantResults.length > 0) {
                    for (int grantResult : grantResults) {
                        if (PackageManager.PERMISSION_GRANTED != grantResult) {
                            noPermissionGranted();
                            return;
                        }
                    }

                    mIsRequestingPermission = false;
                    startServices();
                } else {
                    noPermissionGranted();
                }
                break;
            }
        }
    }

    private void noPermissionGranted() {
        mIsRequestingPermission = true;
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setNegativeButton(R.string.action_quit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            builder.setMessage(R.string.permissions_location)
                    .setPositiveButton(R.string.action_retry, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mIsRequestingPermission = false;
                            requestPermission();
                        }
                    });
        } else {
            builder.setMessage(R.string.permissions_location_manual)
                    .setPositiveButton(R.string.action_retry, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mIsRequestingPermission = true;
                            startActivity(new Intent()
                                    .setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .setData(Uri.fromParts("package", getPackageName(), null)));
                        }
                    });
        }

        AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(false);
        alert.show();
    }

    private void requestPermission() {
        if (mIsRequestingPermission) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            mIsRequestingPermission = true;
            ActivityCompat.requestPermissions(
                    ConversationsActivity.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_CODE_PERMISSION);
        }
    }

    private void getPreviousMessages() {
        if (mGetPreviousMessagesAsyncTask == null) {
            setIsBusy(true);
            mGetPreviousMessagesAsyncTask = new ChatServerAPI.GetPreviousMessagesAsyncTask(
                    Utils.getCurrentUserId(this),
                    mLocation.getLatitude(), mLocation.getLongitude(),
                    null, null) {

                @Override
                protected void onCancelled() {
                    setIsBusy(false);
                    mGetPreviousMessagesAsyncTask = null;
                }

                @Override
                public void onGetPreviousMessagesResult(boolean success, int status_code, String status_message, List<String> server_msg_id) {
                    mGetPreviousMessagesAsyncTask = new UpdatePreviousMessagesAsyncTask(ConversationsActivity.this) {
                        @Override
                        protected void onCancelled() {
                            setIsBusy(false);
                            mGetPreviousMessagesAsyncTask = null;
                        }

                        @Override
                        protected void onPostExecute(Void result) {
                            setIsBusy(false);
                            mGetPreviousMessagesAsyncTask = null;
                        }
                    }.execute(server_msg_id.toArray(new String[server_msg_id.size()]));
                }
            }.run();
        }
    }

    @Override
    public void onNewsResults(ChatServerAPI.GetNewsAsyncTask.NewsResult news) {
        try {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.slidingTabsFragment_viewpager);
            if (fragment instanceof NewsFragment) {
                ((NewsFragment) fragment).initAdapter(news);
            }
        } catch (Exception e) {e.printStackTrace();}
    }

    private class UpdatePreviousMessagesAsyncTask extends AsyncTask<String[], Void, Void>  {
        private final Context context;

        UpdatePreviousMessagesAsyncTask(Context context) {
            this.context = context.getApplicationContext();
        }

        @Override
        protected Void doInBackground(String[]... params) {
            Uri uri = Uri.withAppendedPath(ConversationsContract.Conversation.CONTENT_URI, "0")
                    .buildUpon()
                    .build();
            getContentResolver().update(uri, null, null, params[0]);

            Utils.setFirstRun(context);
            return null;
        }
    }

    private final BroadcastReceiver mRegistrationReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int status_code = intent.getIntExtra(Constants.BROADCAST_EXTRA_STATUS_CODE, 0);
                    switch (status_code) {
                        case 200:
                            startServices();
                            break;

                        case 0:
                            mTextViewStatus.setText(R.string.error_chatservices_connecting);
                            mViewStatus.setVisibility(View.VISIBLE);
                            break;

                        case -1:
                            mTextViewStatus.setText(R.string.error_googleplayservices_connecting);
                            mViewStatus.setVisibility(View.VISIBLE);
                            break;

                        default:
                            mTextViewStatus.setText(intent.getStringExtra(Constants.BROADCAST_EXTRA_STATUS_STRING));
                            mViewStatus.setVisibility(View.VISIBLE);
                    }
                }
            };

    private void showFullscreenAd() {
                MobileAds.initialize(this,
                        "ca-pub-7181343877669077~7476370948");

                AdRequest adRequest2 = new AdRequest.Builder().build();

                mInterstitialAd = new InterstitialAd(this);

                mInterstitialAd.setAdUnitId(getString(R.string.admob_id_full_screen));
                mInterstitialAd.loadAd(adRequest2);
                mInterstitialAd.setAdListener(new AdListener() {
                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        mInterstitialAd.show();
                    }

                    @Override
                    public void onAdFailedToLoad(int i) {
                        super.onAdFailedToLoad(i);
                    }
                });
    }
}
}