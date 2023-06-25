package com.teletype.truckchat.ui.conversations.starred;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import com.teletype.truckchat.db.ConversationsContract;
import com.teletype.truckchat.ui.conversations.ConversationsAdapter;


public class StarredConversationsAdapter extends ConversationsAdapter {

    private static final int LOADER_ID = 5;

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

    public StarredConversationsAdapter(Context context) {
        super(context);

        this.mContext = context;
    }

    public void loadData() {
        ((FragmentActivity) mContext).getSupportLoaderManager()
                .initLoader(LOADER_ID, null,
                        new LoaderManager.LoaderCallbacks<Cursor>() {
                            @Override
                            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                                Uri uri = ConversationsContract.Conversation.CONTENT_URI_STARRED
                                        .buildUpon()
                                        .build();

                                return new CursorLoader(mContext, uri, PROJECTION, null, null, null);
                            }

                            @Override
                            public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                                swapCursor(data);
                            }

                            @Override
                            public void onLoaderReset(Loader<Cursor> loader) {
                                swapCursor(null);
                            }
                        });
    }

    public void unloadData() {
        ((FragmentActivity) mContext).getSupportLoaderManager().destroyLoader(LOADER_ID);
    }

}
