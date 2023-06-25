package com.teletype.truckchat.ui.replies;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.teletype.truckchat.R;
import com.teletype.truckchat.db.ConversationsContract;
import com.teletype.truckchat.services.ChatServerAPI;
import com.teletype.truckchat.services.RegistrationIntentService;
import com.teletype.truckchat.ui.common.InterWebActivity;
import com.teletype.truckchat.ui.settings.SettingsPreferenceActivity;
import com.teletype.truckchat.util.Constants;
import com.teletype.truckchat.util.Utils;

import java.util.ArrayList;
import java.util.Locale;

public class RepliesActivity extends Activity {

    private static final String TAG = "TAG_" + RepliesActivity.class.getSimpleName();
    private static final int LOADER_ID = 4;

    private static final int REQ_CODE_SPEECH_INPUT = 1;

    private static final String[] PROJECTION = {
            ConversationsContract.Conversation._ID,
            ConversationsContract.Conversation._FLAG,
            ConversationsContract.Conversation._EMOJI_ID
    };

    private AdView mAdView;
    private MenuItem mMenuStar;
    private MenuItem mMenuUnstar;
    private ImageView mImageViewStarred;
    private TextView mTextViewCompose;
    private ImageView mImageViewSend;
    private ProgressBar mProgressBarSending;
    private Location mLocation;
    private String mConversationId;
    private String mConversationTopic;
    private String mUserId;
    private String mUserHandle;
    private Boolean mIsStarred = null;
    private boolean mIgnoreAutoHide;
    private Integer mFlags;
    private String emoji_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_replies);
        setResult(Activity.RESULT_CANCELED);

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mRegistrationReceiver, new IntentFilter(TAG));

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(Constants.BROADCAST_EXTRA_LOCATION)) {
                mLocation = intent.getParcelableExtra(Constants.BROADCAST_EXTRA_LOCATION);
            } else {
                finish();
            }

            mConversationId = intent.getStringExtra(Constants.BROADCAST_EXTRA_CONVERSATION_ID);
            if (TextUtils.isEmpty(mConversationId)) {
                finish();
            }

            mConversationTopic = intent.getStringExtra(Constants.BROADCAST_EXTRA_MESSAGE);

            mIgnoreAutoHide = intent.getBooleanExtra(Constants.BROADCAST_EXTRA_IGNORE_AUTOHIDE, false);

            emoji_id = intent.getStringExtra(Constants.EMOJI_ID);
        }

        mUserId = Utils.getCurrentUserId(this);
        if (TextUtils.isEmpty(mUserId)) {
            finish();
        }

        initWidgets();

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(Constants.BROADCAST_EXTRA_MESSAGE)) {
                mTextViewCompose.setText(savedInstanceState.getString(Constants.BROADCAST_EXTRA_MESSAGE));
            }
        }

        loadData();

        mAdView = (AdView) findViewById(R.id.repliesActivity_adView);
        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                mAdView.setVisibility(View.VISIBLE);
            }
        });

        Bundle extras = new Bundle();
        extras.putBoolean("is_designed_for_families", false);

        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("5F18997E57B09D90875E5BFFF902E13D") // Man's tablet
                .setLocation(mLocation)
                .addNetworkExtrasBundle(AdMobAdapter.class, extras)
                .addKeyword("truck")
                .tagForChildDirectedTreatment(false)
                .build();

        mAdView.loadAd(adRequest);
    }

    @Override
    protected void onDestroy() {
        unloadData();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationReceiver);

        getContentResolver().notifyChange(ConversationsContract.Conversation.CONTENT_URI, null);

        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (mTextViewCompose != null) {
            outState.putString(Constants.BROADCAST_EXTRA_MESSAGE, mTextViewCompose.getText().toString());
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_replies, menu);
        mMenuStar = menu.findItem(R.id.action_star);
        mMenuUnstar = menu.findItem(R.id.action_unstar);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        setupStarred();
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_star:
                if (mFlags != null) {
                    Uri uri = Uri.withAppendedPath(ConversationsContract.Conversation.CONTENT_URI, mConversationId)
                            .buildUpon()
                            .build();
                    ContentValues values = new ContentValues(1);
                    values.put(ConversationsContract.Conversation._FLAG, (mFlags | 0x1));
                    getContentResolver().update(uri, values, null, null);
                }
                break;

            case R.id.action_unstar:
                if (mFlags != null) {
                    Uri uri = Uri.withAppendedPath(ConversationsContract.Conversation.CONTENT_URI, mConversationId)
                            .buildUpon()
                            .build();
                    ContentValues values = new ContentValues(1);
                    values.put(ConversationsContract.Conversation._FLAG, (mFlags & ~0x1));
                    getContentResolver().update(uri, values, null, null);
                }
                break;

            case R.id.action_settings:
                startActivity(new Intent(this, SettingsPreferenceActivity.class));
                break;

            case R.id.action_tell_a_friend:
                Utils.composeTellAFriendEmail(this);
                break;

            case R.id.action_help:
                startActivity(new Intent(this, InterWebActivity.class)
                        .putExtra(Constants.EXTRA_URI, Constants.WEBPAGE_HELP));
                break;

            case android.R.id.home:
                finish();
                break;

            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences sharedPref = Utils.getSharedPreferences(this);
        mUserHandle = sharedPref.getString(Constants.PREFS_MSG_HANDLE, "");
        if (TextUtils.isEmpty(mUserHandle)) {
            setTitle(R.string.app_name);
        } else {
            setTitle(getString(R.string.app_name_with_handle, mUserHandle));
            mUserHandle = getString(R.string.pref_format_handle, mUserHandle);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (result != null && result.size() > 0) {
                    String text = result.get(0);
                    if (!TextUtils.isEmpty(text)) {
                        if (mTextViewCompose.length() > 0) {
                            mTextViewCompose.append(" ");
                        }

                        mTextViewCompose.append(text);
                    }
                }
            }
        }
    }

    private void initWidgets() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mImageViewStarred = findViewById(R.id.repliesActivity_imageView_icon);

        ((TextView) findViewById(R.id.repliesActivity_textView_topic)).setText(mConversationTopic);

        mTextViewCompose = findViewById(R.id.repliesActivity_editText_compose);

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            findViewById(R.id.repliesActivity_imageView_nospeak).setVisibility(View.GONE);
            View view = findViewById(R.id.repliesActivity_imageView_speak);
            view.setVisibility(View.VISIBLE);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                    //intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.));

                    try {
                        startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
                    } catch (ActivityNotFoundException a) {
                        Toast toast = Toast.makeText(RepliesActivity.this, R.string.error_speech_error, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                }
            });
        }

        mImageViewSend =  findViewById(R.id.repliesActivity_imageView_send);
        mImageViewSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send();
            }
        });

        mProgressBarSending = findViewById(R.id.repliesActivity_progressBar_sending);

        FragmentManager fm = getFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.repliesActivity_frameLayout_replies);
        if (fragment == null) {
            Bundle bundle = new Bundle();
            bundle.putString(Constants.BROADCAST_EXTRA_CONVERSATION_ID, mConversationId);
            bundle.putString(Constants.BROADCAST_EXTRA_USER_ID, mUserId);
            bundle.putBoolean(Constants.BROADCAST_EXTRA_IGNORE_AUTOHIDE, mIgnoreAutoHide);
            bundle.putString(Constants.EMOJI_ID, String.valueOf(emoji_id));
            fragment = RepliesFragment.newInstance(bundle);
            fm.beginTransaction().add(R.id.repliesActivity_frameLayout_replies, fragment).commit();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void send() {
        String messageBody = mTextViewCompose.getText().toString().trim();
        if (TextUtils.isEmpty(messageBody)) {
            return;
        }

        messageBody = mUserHandle + messageBody;

        mTextViewCompose.setEnabled(false);
        mImageViewSend.setEnabled(false);
        mImageViewSend.setVisibility(View.GONE);
        mProgressBarSending.setVisibility(View.VISIBLE);
        int emoji_id = Utils.getAvatar(this);
        new ChatServerAPI.ReplyMessageAsyncTask(
                messageBody,
                mConversationId,
                mUserId,
                mLocation.getLatitude(),
                mLocation.getLongitude(),
                String.valueOf(emoji_id)) {

            @Override
            public void onReplyMessageResult(boolean success, int status_code, String status_message) {
                if (success && status_code == 200) {
                    mTextViewCompose.setText(null);

                    //InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    //inputMethodManager.hideSoftInputFromWindow(mTextViewCompose.getWindowToken(), 0);
                } else {
                    if (status_code == 401) {
                        if ("MismatchSenderId".equals(status_message)) {
                            startService(new Intent(RepliesActivity.this, RegistrationIntentService.class)
                                    .putExtra(Constants.BROADCAST_ACTION_REGISTRATION, TAG));
                            return;
                        }
                    }

                    Toast toast = Toast.makeText(RepliesActivity.this, R.string.error_send_error, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }

                mProgressBarSending.setVisibility(View.GONE);
                mImageViewSend.setVisibility(View.VISIBLE);
                mImageViewSend.setEnabled(true);
                mTextViewCompose.setEnabled(true);
            }
        }.run();
    }

    private void setupStarred() {
        if (mMenuStar != null) {
            if (mIsStarred == null) {
                mMenuStar.setEnabled(false);
                mMenuStar.setVisible(false);
            } else {
                mMenuStar.setEnabled(!mIsStarred);
                mMenuStar.setVisible(!mIsStarred);
            }
        }

        if (mMenuUnstar != null) {
            if (mIsStarred == null) {
                mMenuUnstar.setEnabled(false);
                mMenuUnstar.setVisible(false);
            } else {
                mMenuUnstar.setEnabled(mIsStarred);
                mMenuUnstar.setVisible(mIsStarred);
            }
        }

        if (mImageViewStarred != null) {
            if (emoji_id != null) {
                mImageViewStarred.setImageResource(Integer.parseInt(emoji_id));
            } else {
                mImageViewStarred.setImageResource(mIsStarred != null && mIsStarred ? R.drawable.ic_action_important : R.drawable.ic_action_chat);
            }
        }
    }

    private void setIsStarred(boolean isStarred) {
        mIsStarred = isStarred;
        setupStarred();
    }

    private void loadData() {
        getLoaderManager().initLoader(LOADER_ID, null,
                new LoaderManager.LoaderCallbacks<Cursor>() {
                    @Override
                    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                        Uri uri = Uri.withAppendedPath(ConversationsContract.Conversation.CONTENT_URI, mConversationId)
                                .buildUpon()
                                .build();

                        return new CursorLoader(RepliesActivity.this, uri, PROJECTION, "flags", null, null);
                    }

                    @Override
                    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                        if (data.moveToFirst()) {
                            mFlags = data.getInt(1);
                            setIsStarred((mFlags & 0x1) > 0);
                        }
                    }

                    @Override
                    public void onLoaderReset(Loader<Cursor> loader) {
                        mFlags = null;
                    }
                });
    }

    private void unloadData() {
        getLoaderManager().destroyLoader(LOADER_ID);
    }

    private final BroadcastReceiver mRegistrationReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    mProgressBarSending.setVisibility(View.GONE);
                    mImageViewSend.setVisibility(View.VISIBLE);
                    mImageViewSend.setEnabled(true);
                    mTextViewCompose.setEnabled(true);

                    int status_code = intent.getIntExtra(Constants.BROADCAST_EXTRA_STATUS_CODE, 0);
                    Toast toast;

                    switch (status_code) {
                        case 200:
                            send();
                            return;

                        case 0:
                            toast = Toast.makeText(RepliesActivity.this, R.string.error_chatservices_connecting, Toast.LENGTH_SHORT);
                            break;

                        case -1:
                            toast = Toast.makeText(RepliesActivity.this, R.string.error_googleplayservices_connecting, Toast.LENGTH_SHORT);
                            break;

                        default:
                            toast = Toast.makeText(RepliesActivity.this, R.string.error_send_error, Toast.LENGTH_SHORT);
                    }

                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            };
}
