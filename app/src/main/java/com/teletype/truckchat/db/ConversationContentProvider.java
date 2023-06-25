package com.teletype.truckchat.db;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;


public class ConversationContentProvider extends ContentProvider {

    //private static final String TAG = "TAG_" + ConversationContentProvider.class.getSimpleName();

    private static final int CONVERSATION_DIR = 1;
    private static final int CONVERSATION_DIR_ALL = 2;
    private static final int CONVERSATION_DIR_STARRED = 3;
    private static final int CONVERSATION_DIR_UNSTARRED = 4;
    private static final int CONVERSATION_ITEM = 5;

    private static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(ConversationsContract.AUTHORITY, ConversationsContract.Conversation.DIR, CONVERSATION_DIR);
        uriMatcher.addURI(ConversationsContract.AUTHORITY, ConversationsContract.Conversation.DIR + "/ALL", CONVERSATION_DIR_ALL);
        uriMatcher.addURI(ConversationsContract.AUTHORITY, ConversationsContract.Conversation.DIR + "/STARRED", CONVERSATION_DIR_STARRED);
        uriMatcher.addURI(ConversationsContract.AUTHORITY, ConversationsContract.Conversation.DIR + "/UNSTARRED", CONVERSATION_DIR_UNSTARRED);
        uriMatcher.addURI(ConversationsContract.AUTHORITY, ConversationsContract.Conversation.DIR + "/*", CONVERSATION_ITEM);
    }

    private ConversationDb db;

    @Override
    public boolean onCreate() {
        db = new ConversationDb(getContext());

        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor;

        switch (uriMatcher.match(uri)) {
            case CONVERSATION_DIR:
                cursor = db.queryConversations(projection, selection, selectionArgs, sortOrder);
                break;

            case CONVERSATION_DIR_ALL:
                cursor = db.queryConversationsAll(projection, selection, selectionArgs, sortOrder);
                break;

            case CONVERSATION_DIR_STARRED:
                cursor = db.queryConversationsStarred(projection, selection, selectionArgs, sortOrder);
                break;

            case CONVERSATION_DIR_UNSTARRED:
                cursor = db.queryConversationsUnstarred(projection, selection, selectionArgs, sortOrder);
                break;

            case CONVERSATION_ITEM:
                if ("flags".equals(selection)) {
                    cursor = db.queryConversation(uri.getLastPathSegment(), projection);
                } else {
                    cursor = db.queryConversationReplies(uri.getLastPathSegment(), projection, selection, selectionArgs, sortOrder);
                }
                break;

            default:
                return new MatrixCursor(new String[0], 0);
        }

        Context context = getContext();
        if (context != null) {
            cursor.setNotificationUri(context.getContentResolver(), ConversationsContract.Conversation.CONTENT_URI);
        }

        return cursor;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            case CONVERSATION_DIR:
                return ConversationsContract.Conversation.CONTENT_TYPE;

            case CONVERSATION_ITEM:
                return ConversationsContract.Conversation.CONTENT_ITEM_TYPE;
        }

        return null;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        if (uriMatcher.match(uri) != CONVERSATION_DIR) {
            return null;
        }

        String cid = values.getAsString(ConversationsContract.Conversation._CID);
        if (TextUtils.isEmpty(cid)) {
            return null;
        }

        String message = values.getAsString(ConversationsContract.Conversation._MESSAGE);
        if (TextUtils.isEmpty(message)) {
            return null;
        }

        String rid = values.getAsString(ConversationsContract.Conversation._RID);
        String uid = values.getAsString(ConversationsContract.Conversation._UID);
        String emoji_id = values.getAsString(ConversationsContract.Conversation._EMOJI_ID);
        Long timestamp = values.getAsLong(ConversationsContract.Conversation._TIMESTAMP);

        long _id = db.insertMessage(cid, rid, uid, message, timestamp, emoji_id);

        if (_id == -1) {
            return null;
        }

        Context context = getContext();
        if (context != null) {
            context.getContentResolver().notifyChange(ConversationsContract.Conversation.CONTENT_URI, null);
        }

        return ContentUris.withAppendedId(uri, _id);
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        if (uriMatcher.match(uri) != CONVERSATION_DIR_UNSTARRED) {
            return 0;
        }

        int rows_affected = 0;
        Cursor cursor = null;
        try {
            cursor = query(uri, new String[] { ConversationsContract.Conversation._CID }, selection, selectionArgs, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        rows_affected += db.deleteConversation(cursor.getString(0));
                    } while (cursor.moveToNext());
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return rows_affected;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int rows_affected = 0;

        if (uriMatcher.match(uri) != CONVERSATION_ITEM) {
            return rows_affected;
        }

        if (values == null) {
            if (selectionArgs != null) {
                for (String cid : selectionArgs) {
                    rows_affected += db.insertPreviousMessages(cid);
                }
            }
        } else {
            String message = values.getAsString(ConversationsContract.Conversation._MESSAGE);
            Integer flags = values.getAsInteger(ConversationsContract.Conversation._FLAG);
            Long timestamp = values.getAsLong(ConversationsContract.Conversation._TIMESTAMP);
            Integer unread = null;
            if (values.containsKey(ConversationsContract.Conversation._UNREAD)) {
                unread = values.getAsInteger(ConversationsContract.Conversation._UNREAD);
                if (unread == null) {
                    unread = Integer.MAX_VALUE;
                }
            }

            rows_affected = db.updateConversation(uri.getLastPathSegment(), message, flags, timestamp, unread);
        }

        if (rows_affected > 0) {
            Context context = getContext();
            if (context != null) {
                context.getContentResolver().notifyChange(ConversationsContract.Conversation.CONTENT_URI, null);
            }
        }

        return rows_affected;
    }
}
