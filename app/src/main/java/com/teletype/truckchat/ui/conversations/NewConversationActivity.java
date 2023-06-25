package com.teletype.truckchat.ui.conversations;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.teletype.truckchat.R;
import com.teletype.truckchat.services.ChatServerAPI;
import com.teletype.truckchat.services.RegistrationIntentService;
import com.teletype.truckchat.ui.common.InterWebActivity;
import com.teletype.truckchat.ui.settings.SettingsPreferenceActivity;
import com.teletype.truckchat.util.Constants;
import com.teletype.truckchat.util.Utils;

import java.util.ArrayList;
import java.util.Locale;


public class NewConversationActivity extends Activity {

    private static final String TAG = "TAG_" + NewConversationActivity.class.getSimpleName();

    private static final int REQ_CODE_SPEECH_INPUT = 1;

    private TextView mTextViewMessage;
    private ImageView mImageViewSend;
    private Location mLocation;
    private String mUserHandle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_new_conversation);

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mRegistrationReceiver, new IntentFilter(TAG));

        setResult(Activity.RESULT_CANCELED);

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(Constants.BROADCAST_EXTRA_LOCATION)) {
                mLocation = intent.getParcelableExtra(Constants.BROADCAST_EXTRA_LOCATION);
            } else {
                finish();
            }
        }

        initWidgets();

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(Constants.BROADCAST_EXTRA_MESSAGE)) {
                mTextViewMessage.setText(savedInstanceState.getString(Constants.BROADCAST_EXTRA_MESSAGE));
            }
        }
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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (mTextViewMessage != null) {
            outState.putString(Constants.BROADCAST_EXTRA_MESSAGE, mTextViewMessage.getText().toString());
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationReceiver);

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new_conversation, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (result != null && result.size() > 0) {
                    String text = result.get(0);
                    if (!TextUtils.isEmpty(text)) {
                        if (mTextViewMessage.length() > 0) {
                            mTextViewMessage.append(" ");
                        }

                        mTextViewMessage.append(text);
                    }
                }
            }
        }
    }

    private void initWidgets() {
        setProgressBarIndeterminateVisibility(false);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mTextViewMessage = (TextView) findViewById(R.id.newConversationActivity_editText_message);
        mImageViewSend = (ImageView) findViewById(R.id.newConversationActivity_imageView_send);
        mImageViewSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send();
            }
        });

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            findViewById(R.id.newConversationActivity_imageView_nospeak).setVisibility(View.GONE);
            View view = findViewById(R.id.newConversationActivity_imageView_speak);
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
                        Toast toast = Toast.makeText(NewConversationActivity.this, R.string.error_speech_error, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                }
            });
        }
    }

    private void send() {
        String msgBody = mTextViewMessage.getText().toString().trim();
        if (TextUtils.isEmpty(msgBody)) {
            return;
        }

        final String messageBody = mUserHandle + msgBody;

        mImageViewSend.setEnabled(false);
        setProgressBarIndeterminateVisibility(true);
        new ChatServerAPI.DeviceMessageAsyncTask(
                Utils.getSerialNumber(this),
                messageBody,
                mLocation.getLatitude(),
                mLocation.getLongitude(),
                Utils.getAvatar(this)) {

            @Override
            public void onDeviceMessageResult(boolean success, int status_code, String status_message,
                                              String conversation_id, int emoji_id) {
                setProgressBarIndeterminateVisibility(false);
                if (success && status_code == 200) {
                    Intent intent = new Intent()
                            .putExtra(Constants.BROADCAST_EXTRA_CONVERSATION_ID, conversation_id)
                            .putExtra(Constants.EMOJI_ID, emoji_id)
                            .putExtra(Constants.BROADCAST_EXTRA_MESSAGE, messageBody);
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                } else {
                    if (status_code == 401) {
                        if ("MismatchSenderId".equals(status_message)) {
                            startService(new Intent(NewConversationActivity.this, RegistrationIntentService.class)
                                    .putExtra(Constants.BROADCAST_ACTION_REGISTRATION, TAG));
                            return;
                        }
                    }

                    Toast toast = Toast.makeText(NewConversationActivity.this, R.string.error_send_error, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    mImageViewSend.setEnabled(true);
                }
            }
        }.run();
    }

    private final BroadcastReceiver mRegistrationReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int status_code = intent.getIntExtra(Constants.BROADCAST_EXTRA_STATUS_CODE, 0);
                    Toast toast;

                    switch (status_code) {
                        case 200:
                            send();
                            return;

                        case 0:
                            toast = Toast.makeText(NewConversationActivity.this, R.string.error_chatservices_connecting, Toast.LENGTH_SHORT);
                            break;

                        case -1:
                            toast = Toast.makeText(NewConversationActivity.this, R.string.error_googleplayservices_connecting, Toast.LENGTH_SHORT);
                            break;

                        default:
                            toast = Toast.makeText(NewConversationActivity.this, R.string.error_send_error, Toast.LENGTH_SHORT);
                    }

                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    mImageViewSend.setEnabled(true);
                }
            };
}
