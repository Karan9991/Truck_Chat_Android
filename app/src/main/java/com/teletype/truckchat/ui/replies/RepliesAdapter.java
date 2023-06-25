package com.teletype.truckchat.ui.replies;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.teletype.truckchat.R;
import com.teletype.truckchat.db.ConversationsContract;
import com.teletype.truckchat.util.Constants;
import com.teletype.truckchat.util.Utils;

import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;


public class RepliesAdapter extends ResourceCursorAdapter {

    private static final int LOADER_ID = 2;

    private static final String[] PROJECTION = {
            ConversationsContract.Conversation._ID,
            ConversationsContract.Conversation._MESSAGE,
            ConversationsContract.Conversation._UID,
            ConversationsContract.Conversation._TIMESTAMP,
            ConversationsContract.Conversation._EMOJI_ID
    };

    private final Context mContext;
    private final DateFormat mDateFormat;
    private final String mConversationId;
    private final String mUserId;
    private final boolean mIgnoreAutoHide;
    private final String emoji_id;

    private Integer mMessageCount;

    public RepliesAdapter(Context context, String conversationId, String userId, boolean ignoreAutoHide, String emoji_id) {
        super(context, R.layout.adapter_replies, null, 0);

        this.mContext = context;
        this.mDateFormat = DateFormat.getDateTimeInstance();
        this.mConversationId = conversationId;
        this.mUserId = userId;
        this.mIgnoreAutoHide = ignoreAutoHide;
        this.emoji_id = emoji_id;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = super.newView(context, cursor, parent);

        ViewHolder viewHolder = new ViewHolder();
        viewHolder.layout_them = view.findViewById(R.id.repliesAdapter_linearLayout_them);
        viewHolder.message_them = view.findViewById(R.id.repliesAdapter_textView_message_them);
        viewHolder.timestamp_them = view.findViewById(R.id.repliesAdapter_textView_timestamp_them);
        viewHolder.layout_me = view.findViewById(R.id.repliesAdapter_linearLayout_me);
        viewHolder.message_me = view.findViewById(R.id.repliesAdapter_textView_message_me);
        viewHolder.timestamp_me = view.findViewById(R.id.repliesAdapter_textView_timestamp_me);
        viewHolder.ivAvatar = view.findViewById(R.id.ivAvatar);
        viewHolder.ivMessage = view.findViewById(R.id.ivMessage);
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        mDateFormat.setTimeZone(TimeZone.getDefault());

        if (mUserId.equals(cursor.getString(2))) {
            viewHolder.layout_them.setVisibility(View.GONE);
            viewHolder.message_them.setText(null);
            viewHolder.timestamp_them.setText(null);

            viewHolder.layout_me.setVisibility(View.VISIBLE);
            viewHolder.message_me.setText(cursor.getString(1).trim());
            viewHolder.timestamp_me.setText(mDateFormat.format(new Date(cursor.getLong(3))));
            int avatarId = Utils.getAvatar(context);
            viewHolder.ivAvatar.setImageResource(avatarId);

        } else {
            int emoji = cursor.getInt(4);
            viewHolder.ivMessage.setImageResource(isResource(context, emoji)? emoji :
                    R.drawable.ic_menu_start_conversation_flipped);

            viewHolder.layout_me.setVisibility(View.GONE);
            viewHolder.message_me.setText(null);
            viewHolder.timestamp_me.setText(null);

            viewHolder.layout_them.setVisibility(View.VISIBLE);
            viewHolder.message_them.setText(cursor.getString(1).trim());
            viewHolder.timestamp_them.setText(mDateFormat.format(new Date(cursor.getLong(3))));
        }
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

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    public void loadData() {
        ((Activity) mContext).getLoaderManager()
                .initLoader(LOADER_ID, null,
                        new LoaderManager.LoaderCallbacks<Cursor>() {
                            @Override
                            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                                Uri uri = Uri.withAppendedPath(ConversationsContract.Conversation.CONTENT_URI, mConversationId)
                                        .buildUpon()
                                        .build();

                                return new CursorLoader(mContext, uri, PROJECTION, null, null, null);

/*                              String selection = null;
                                String[] selectionArgs = null;

                                if (!mIgnoreAutoHide) {
                                    String days = Utils.getSharedPreferences(mContext).getString(Constants.PREFS_MSG_AUTOHIDE, "1");
                                    if (!"0".equals(days)) {
                                        long daysInMillis = TimeUnit.DAYS.toMillis(Integer.parseInt(days));
                                        long expired = System.currentTimeMillis() - daysInMillis;

                                        selection = String.format("%s >= ?", ConversationsContract.Conversation._TIMESTAMP);
                                        selectionArgs = new String[]{Long.toString(expired)};
                                    }
                                }

                                return new CursorLoader(mContext, uri, PROJECTION, selection, selectionArgs, null);
*/
                            }

                            @Override
                            public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                                swapCursor(data);

                                final int count = data.getCount();
                                if (mMessageCount != null && count > mMessageCount) {
                                    SharedPreferences sharedPreferences = Utils.getSharedPreferences(mContext);
                                    if (sharedPreferences.getBoolean(Constants.PREFS_CHAT_TONE, true)) {
                                        ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
                                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);
                                    }
                                }

                                mMessageCount = count;
                            }

                            @Override
                            public void onLoaderReset(Loader<Cursor> loader) {
                                swapCursor(null);
                            }
                        });
    }

    public void unloadData() {
        ((Activity) mContext).getLoaderManager().destroyLoader(LOADER_ID);
    }

    private class ViewHolder {
        View layout_them;
        TextView message_them;
        TextView timestamp_them;
        View layout_me;
        TextView message_me;
        TextView timestamp_me;
        ImageView ivAvatar;
        ImageView ivMessage;
    }

}
