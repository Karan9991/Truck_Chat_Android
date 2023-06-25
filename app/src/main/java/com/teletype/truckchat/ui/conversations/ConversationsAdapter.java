package com.teletype.truckchat.ui.conversations;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.teletype.truckchat.MyApplication;
import com.teletype.truckchat.R;
import com.teletype.truckchat.db.ConversationsContract;
import com.teletype.truckchat.ui.common.SlidingTabsFragment;
import com.teletype.truckchat.util.Constants;
import com.teletype.truckchat.util.Utils;

import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


public class ConversationsAdapter extends ResourceCursorAdapter {

    //private static final String TAG = "TAG_" + ConversationsAdapter.class.getSimpleName();

    private static final int LOADER_ID = 1;

    private static final String[] PROJECTION = {
            ConversationsContract.Conversation._ID,
            ConversationsContract.Conversation._CID,
            ConversationsContract.Conversation._MESSAGE,
            ConversationsContract.Conversation._RCOUNT,
            ConversationsContract.Conversation._LUPDATED,
            ConversationsContract.Conversation._FLAG,
            ConversationsContract.Conversation._UNREAD,
            ConversationsContract.Conversation._EMOJI_ID
    };

    private final Context mContext;
    private final DateFormat mDateFormat;
    private final StyleSpan[] mStyles = { new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC) };

    private OnEmptyDataListener mListener;

    public boolean mIsAfterInitialLoad = false;

    public ConversationsAdapter(Context context) {
        super(context, R.layout.adapter_conversations, null, 0);

        this.mContext = context;
        this.mDateFormat = DateFormat.getDateTimeInstance();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = super.newView(context, cursor, parent);

        ViewHolder viewHolder = new ViewHolder();
        viewHolder.topic = (TextView) view.findViewById(R.id.conversationsAdapter_textView_topic);
        viewHolder.replies = (TextView) view.findViewById(R.id.conversationsAdapter_textView_replies);
        viewHolder.timestamp = (TextView) view.findViewById(R.id.conversationsAdapter_textView_timestamp);
        viewHolder.starred = (ImageView) view.findViewById(R.id.conversationsAdapter_imageView_starred);
        viewHolder.unread = (ImageView) view.findViewById(R.id.conversationsAdapter_imageView_unread);
        viewHolder.conversationsAdapter_linearLayout_topic = (LinearLayout) view.findViewById(R.id.conversationsAdapter_linearLayout_topic);
        viewHolder.adViewTopic = (AdView) view.findViewById(R.id.adViewTopic);
        view.setTag(R.id.llRowNewsTopic, viewHolder);

        return view;
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag(R.id.llRowNewsTopic);

        final int pos = cursor.getPosition();
        final AdView mAdView = viewHolder.adViewTopic;

        if (pos != 0 && pos % 3 == 0) {
            mAdView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    mAdView.setVisibility(View.VISIBLE);
                }
            });
            Bundle extras = new Bundle();
            extras.putBoolean("is_designed_for_families", false);

            AdRequest adRequest = new AdRequest.Builder()
                    /*.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                    .addTestDevice(MyApplication.getDeviceId())*/
                    .addNetworkExtrasBundle(AdMobAdapter.class, extras)
                    .addKeyword("truck")
                    .tagForChildDirectedTreatment(false)
                    .build();
            mAdView.loadAd(adRequest);
            view.findViewById(R.id.conversationsAdapter_linearLayout_topic).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SlidingTabsFragment fragment = (SlidingTabsFragment)
                            ((ConversationsActivity) context).getSupportFragmentManager()
                                    .findFragmentById(R.id.conversationsActivity_frameLayout_content);
                    fragment.getConversationsFragment(pos);
                    Log.e("context", String.valueOf(context));
                }
            });
        } else {
            mAdView.setTag(false);
            mAdView.setVisibility(View.GONE);
        }


        String topic = cursor.getString(2).trim();
        String replies = cursor.getString(3);
        replies = context.getString(R.string.num_replies, TextUtils.isEmpty(replies) ? "0" : replies);

        if (cursor.isNull(6)) {
            SpannableString spanString = new SpannableString(topic);
            spanString.setSpan(mStyles[0], 0, spanString.length(), 0);
            spanString.setSpan(mStyles[1], 0, spanString.length(), 0);
            viewHolder.topic.setText(spanString);
            viewHolder.replies.setText(replies);
            viewHolder.unread.setVisibility(View.VISIBLE);
        } else {
            viewHolder.topic.setText(topic);
            if (cursor.getInt(6) > 0) {
                SpannableString spanString = new SpannableString(replies);
                spanString.setSpan(mStyles[0], 0, spanString.length(), 0);
                spanString.setSpan(mStyles[1], 0, spanString.length(), 0);
                viewHolder.replies.setText(spanString);
                viewHolder.unread.setVisibility(View.VISIBLE);
            } else {
                viewHolder.replies.setText(replies);
                viewHolder.unread.setVisibility(View.GONE);
            }
        }

        mDateFormat.setTimeZone(TimeZone.getDefault());
        viewHolder.timestamp.setText(context.getString(R.string.last_active, mDateFormat.format(new Date(cursor.getLong(4)))));

        int emoji = cursor.getInt(7);
        viewHolder.starred.setImageResource(isResource(context, emoji)? emoji : R.drawable.ic_action_chat);
    }

    private boolean isResource(Context context, int resId){
        if (context != null){
            try {
                return context.getResources().getResourceName(resId) != null;
            } catch (Resources.NotFoundException ignore) {
            }
        }
        return false;
    }

    public void loadData(OnEmptyDataListener l) {
        this.mListener = l;
        ((FragmentActivity) mContext).getSupportLoaderManager()
                .initLoader(LOADER_ID, null,
                        new LoaderManager.LoaderCallbacks<Cursor>() {
                            @Override
                            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                                Uri uri = ConversationsContract.Conversation.CONTENT_URI_ALL
                                        .buildUpon()
                                        .build();

                                String selection = null;
                                String[] selectionArgs = null;

                                String days = Utils.getSharedPreferences(mContext).getString(Constants.PREFS_MSG_AUTOHIDE, "1");
                                if (!"0".equals(days)) {
                                    long daysInMillis = TimeUnit.DAYS.toMillis(Integer.parseInt(days));
                                    long expired = System.currentTimeMillis() - daysInMillis;

                                    selection = String.format("%s IS NOT NULL AND %s >= ?", ConversationsContract.Conversation._LUPDATED, ConversationsContract.Conversation._LUPDATED);
                                    selectionArgs = new String[]{Long.toString(expired)};
                                }

                                return new CursorLoader(mContext, uri, PROJECTION, selection, selectionArgs, null);
                            }

                            @Override
                            public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                                swapCursor(data);

                                if (mIsAfterInitialLoad) {
                                    SharedPreferences sharedPreferences = Utils.getSharedPreferences(mContext);
                                    if (sharedPreferences.getBoolean(Constants.PREFS_CHAT_TONE, true)) {
                                        ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
                                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);
                                    }
                                } else {
                                    mIsAfterInitialLoad = true;

                                    if (data.getCount() == 0) {
                                        if (mListener != null) {
                                            mListener.onEmptyData();
                                        }
                                    }
                                }
                            }

                            @Override
                            public void onLoaderReset(Loader<Cursor> loader) {
                                swapCursor(null);
                            }
                        });
    }

    public void unloadData() {
        mListener = null;
        ((FragmentActivity) mContext).getSupportLoaderManager().destroyLoader(LOADER_ID);
    }

    public static class Conversation {
        public final String cid;
        public final String message;
        public final int replies;
        public final long lastUpdated;
        public final boolean isStarred;
        public final boolean isHidden;
        public final int flag;
        public final int unread;
        public final String emoji_id;

        public Conversation(String cid, String message, int replies, long lastUpdated, boolean isStarred, boolean isHidden, int flag, int unread, String emoji_id) {
            this.cid = cid;
            this.message = message;
            this.replies = replies;
            this.lastUpdated = lastUpdated;
            this.isStarred = isStarred;
            this.isHidden = isHidden;
            this.flag = flag;
            this.unread = unread;
            this.emoji_id = emoji_id;
        }
    }

    public Conversation getConversation(int position) {
        Cursor cursor = getCursor();
        if (cursor.moveToPosition(position)) {
            return new Conversation(
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getInt(3),
                    cursor.getLong(4),
                    (cursor.getInt(5) & 0x1) > 0,
                    (cursor.getInt(5) & 0x10) > 0,
                    cursor.getInt(5),
                    cursor.isNull(6) ? Integer.MAX_VALUE : cursor.getInt(6),
                    cursor.getString(7)
            );
        }

        return null;
    }

    public void markAll(boolean asRead) {
        Cursor cursor = getCursor();
        if (cursor.moveToFirst()) {
            ContentValues values = new ContentValues(1);
            do {
                Uri uri = Uri.withAppendedPath(ConversationsContract.Conversation.CONTENT_URI, cursor.getString(1))
                        .buildUpon()
                        .build();
                if (asRead) {
                    values.put(ConversationsContract.Conversation._UNREAD, 0);
                } else {
                    values.putNull(ConversationsContract.Conversation._UNREAD);
                }
                mContext.getContentResolver().update(uri, values, null, null);
            } while (cursor.moveToNext());
        }
    }

    public interface OnEmptyDataListener {
        void onEmptyData();
    }

    private class ViewHolder {
        LinearLayout conversationsAdapter_linearLayout_topic;
        TextView topic;
        TextView replies;
        TextView timestamp;
        ImageView starred;
        ImageView unread;
        AdView adViewTopic;
    }

}