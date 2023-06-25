package com.teletype.truckchat.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.teletype.truckchat.services.ChatServerAPI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


final class ConversationDb extends SQLiteOpenHelper {

    //private static final String TAG = "TAG_" + ConversationDb.class.getSimpleName();

    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = ConversationsContract.Conversation.DIR + ".db";

    private static final String _ID = ConversationsContract.Conversation._ID;
    private static final String TABLE_CONVERSATION = ConversationsContract.Conversation.DIR;
    private static final String COLUMN_ID = ConversationsContract.Conversation._ID;
    private static final String CONVERSATION_ID = ConversationsContract.Conversation._CID;
    private static final String MESSAGE = ConversationsContract.Conversation._MESSAGE;
    private static final String REPLY_ID = ConversationsContract.Conversation._RID;
    private static final String USER_ID = ConversationsContract.Conversation._UID;
    private static final String REPLY_COUNT = ConversationsContract.Conversation._RCOUNT;
    private static final String FLAG = ConversationsContract.Conversation._FLAG;
    private static final String TIMESTAMP = ConversationsContract.Conversation._TIMESTAMP;
    private static final String LAST_UPDATED = ConversationsContract.Conversation._LUPDATED;
    private static final String UNREAD = ConversationsContract.Conversation._UNREAD;
    private static final String EMOJI_ID = ConversationsContract.Conversation._EMOJI_ID;

    private static final String SELECTION_CONVERSATION =
            String.format("(`%s` = ?) AND (`%s` IS NOT NULL)",
                    CONVERSATION_ID,
                    REPLY_COUNT
            );

    private static final String SELECTION_CONVERSATIONS =
            String.format("(`%s` IS NOT NULL) AND ((`%s` IS NULL) OR NOT (`%s` & 16))",
                    REPLY_COUNT,
                    FLAG,
                    FLAG
                    );

    private static final String SELECTION_CONVERSATIONS_ALL =
            String.format("%s IS NOT NULL",
                    REPLY_COUNT
            );

    private static final String SELECTION_CONVERSATIONS_STARRED =
            String.format("(`%s` IS NOT NULL) AND (`%s` IS NOT NULL) AND (`%s` & 1) AND NOT (`%s` & 16)",
                    REPLY_COUNT,
                    FLAG,
                    FLAG,
                    FLAG
            );

    private static final String SELECTION_CONVERSATIONS_UNSTARRED =
            String.format("(`%s` IS NOT NULL) AND ((`%s` IS NULL) OR NOT (`%s` & 1))",
                    REPLY_COUNT,
                    FLAG,
                    FLAG
            );

    private static final String SELECTION_REPLIES =
            String.format("(`%s` = ?) AND (`%s` IS NULL)",
                    CONVERSATION_ID,
                    REPLY_COUNT
            );

    public ConversationDb(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion > oldVersion) {
            db.execSQL(String.format("ALTER TABLE `%s` ADD COLUMN `%s` INTEGER",
                    TABLE_CONVERSATION,
                    UNREAD
            ));
        } else if (oldVersion == 2 && newVersion > oldVersion) {
            db.execSQL("DROP TABLE IF EXISTS conversation");
            createTable(db);

        }
    }
    public Cursor queryConversations(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (TextUtils.isEmpty(sortOrder)) {
            sortOrder = String.format("`%s` DESC", LAST_UPDATED); // latest conversation on top
        }

        if (TextUtils.isEmpty(selection)) {
            selection = SELECTION_CONVERSATIONS;
            selectionArgs = null;
        } else {
            selection = String.format("%s AND (%s)", selection, SELECTION_CONVERSATIONS);
        }

        return getReadableDatabase().query(TABLE_CONVERSATION, projection, selection, selectionArgs, null, null, sortOrder);
    }

    public Cursor queryConversationsAll(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (TextUtils.isEmpty(sortOrder)) {
            sortOrder = String.format("`%s` DESC", LAST_UPDATED); // latest conversation on top
        }

        if (TextUtils.isEmpty(selection)) {
            selection = SELECTION_CONVERSATIONS_ALL;
            selectionArgs = null;
        } else {
            selection = String.format("%s AND (%s)", selection, SELECTION_CONVERSATIONS_ALL);
        }

        return getReadableDatabase().query(TABLE_CONVERSATION, projection, selection, selectionArgs, null, null, sortOrder);
    }

    public Cursor queryConversationsStarred(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (TextUtils.isEmpty(sortOrder)) {
            sortOrder = String.format("`%s` DESC", LAST_UPDATED); // latest conversation on top
        }

        if (TextUtils.isEmpty(selection)) {
            selection = SELECTION_CONVERSATIONS_STARRED;
            selectionArgs = null;
        } else {
            selection = String.format("%s AND (%s)", selection, SELECTION_CONVERSATIONS_STARRED);
        }

        return getReadableDatabase().query(TABLE_CONVERSATION, projection, selection, selectionArgs, null, null, sortOrder);
    }

    public Cursor queryConversationsUnstarred(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (TextUtils.isEmpty(sortOrder)) {
            sortOrder = String.format("`%s` DESC", LAST_UPDATED); // latest conversation on top
        }

        if (TextUtils.isEmpty(selection)) {
            selection = SELECTION_CONVERSATIONS_UNSTARRED;
            selectionArgs = null;
        } else {
            selection = String.format("%s AND (%s)", selection, SELECTION_CONVERSATIONS_UNSTARRED);
        }

        return getReadableDatabase().query(TABLE_CONVERSATION, projection, selection, selectionArgs, null, null, sortOrder);
    }

    public Cursor queryConversationReplies(String cid, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        if (TextUtils.isEmpty(sortOrder)) {
            sortOrder = String.format("`%s` ASC", TIMESTAMP); // latest chat on bottom
        }

        if (TextUtils.isEmpty(selection)) {
            selection = SELECTION_REPLIES;
            selectionArgs = new String[] {cid};
        } else {
            selection = String.format("%s AND (%s)", selection, SELECTION_REPLIES);

            if (selectionArgs == null) {
                selectionArgs = new String[] {cid};
            } else {
                List<String> args = new ArrayList<>(selectionArgs.length + 1);
                Collections.addAll(args, selectionArgs);
                args.add(cid);

                selectionArgs = args.toArray(new String[args.size()]);
            }
        }

        Cursor cursor = getReadableDatabase().query(TABLE_CONVERSATION, projection, selection, selectionArgs, null, null, sortOrder);

        long id = queryConversationId(cid, null, null);
        if (id != -1) {
            ContentValues values = new ContentValues();
            values.put(UNREAD, 0);

            getWritableDatabase().update(TABLE_CONVERSATION, values, COLUMN_ID + " = ?", new String[]{Long.toString(id)});
        }

        return cursor;
    }

    public Cursor queryConversation(String cid, String[] projection) {
        String[] selectionArgs = { cid };

        return getReadableDatabase().query(TABLE_CONVERSATION, projection, SELECTION_CONVERSATION, selectionArgs, null, null, null);
    }

    public int updateConversation(String cid, String message, Integer flags, Long timestamp, Integer unread) {
        int result = 0;

        if (TextUtils.isEmpty(cid) || (TextUtils.isEmpty(message) && flags == null && timestamp == null && unread == null)) {
            return result;
        }

        final SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        try {
            long id = queryConversationId(cid, null, null);
            if (id != -1) {
                ContentValues values = new ContentValues();
                if (!TextUtils.isEmpty(message)) {
                    values.put(MESSAGE, message);
                }

                if (flags != null) {
                    values.put(FLAG, flags);
                }

                if (timestamp != null) {
                    values.put(TIMESTAMP, timestamp);
                }

                if (unread != null) {
                    if (unread == Integer.MAX_VALUE) {
                        values.putNull(UNREAD);
                    } else {
                        values.put(UNREAD, unread);
                    }
                }

                result = db.update(TABLE_CONVERSATION, values, String.format("`%s` = ?", COLUMN_ID), new String[]{Long.toString(id)});
                db.setTransactionSuccessful();
            }
        } finally {
            db.endTransaction();
        }

        return result;
    }

    public long insertPreviousMessages(String cid) {
        final SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        int count = 0;

        try {
            long _id = queryConversationId(cid, null, null);
            if (_id == -1) {
                ChatServerAPI.GetAllMessagesAsyncTask getAllMessagesAsyncTask = new ChatServerAPI.GetAllMessagesAsyncTask(cid);
                if (getAllMessagesAsyncTask.runNow()) {
                    long _id2 = insertConversation(cid, getAllMessagesAsyncTask.conversation_topic, getAllMessagesAsyncTask.conversation_timestamp, getAllMessagesAsyncTask.emotion_id);

                    for (ChatServerAPI.GetAllMessagesAsyncTask.ReplyMsg replyMsg : getAllMessagesAsyncTask.reply_msgs) {
                        if (insertReply(cid, replyMsg.replyId, replyMsg.userId, replyMsg.message, _id2, replyMsg.timestamp, replyMsg.emotion_id) != -1) {
                            count++;
                        }
                    }
                }
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return count;
    }

    public long insertMessage(String cid, String rid, String uid, String message, long timestamp, final String emoji_id) {
        long _id;

        final SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        try {
            if (TextUtils.isEmpty(rid)) {
                _id = queryConversationId(cid, message, new OnSameIdWithDifferentMessageListener() {
                    @Override
                    public int onSameIdWithDifferentMessage(long id, String message) {
                        // duplicate CID with different msg
                        ContentValues values = new ContentValues(2);
                        values.put(MESSAGE, message);
                        values.put(EMOJI_ID, emoji_id);
                        return db.update(TABLE_CONVERSATION, values, String.format("`%s` = ?", COLUMN_ID), new String[] {Long.toString(id)});
                    }
                });

                if (_id == -1) {
                    _id = insertConversation(cid, message, timestamp, emoji_id);
                } else {
                    // conversation exists, do nothing
                    _id = -1;
                }
            } else {
                _id = queryConversationId(cid, null, null);
                if (_id == -1) {
                    ChatServerAPI.GetAllMessagesAsyncTask getAllMessagesAsyncTask = new ChatServerAPI.GetAllMessagesAsyncTask(cid);
                    if (getAllMessagesAsyncTask.runNow()) {
                        long _id2 = insertConversation(cid, getAllMessagesAsyncTask.conversation_topic, getAllMessagesAsyncTask.conversation_timestamp, getAllMessagesAsyncTask.emotion_id);

                        for (ChatServerAPI.GetAllMessagesAsyncTask.ReplyMsg replyMsg : getAllMessagesAsyncTask.reply_msgs) {
                            _id = insertReply(cid, replyMsg.replyId, replyMsg.userId, replyMsg.message, _id2, replyMsg.timestamp, replyMsg.emotion_id);
                        }
                    }
                } else {
                    ContentValues values = new ContentValues();
                    values.put(UNREAD, queryConversationUnread(cid) + 1);
                    db.update(TABLE_CONVERSATION, values, COLUMN_ID + " = ?", new String[]{Long.toString(_id)});

                    _id = insertReply(cid, rid, uid, message, _id, timestamp, emoji_id);

                    Cursor cursor = null;
                    try {
                        cursor = queryConversation(cid, new String[] { FLAG });
                        if (cursor.moveToFirst()) {
                            if ((cursor.getInt(0) & 0x10) > 0) {
                                _id = -1; // prevents ContentProvider from notifying an update for hidden conversations
                            }
                        }
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                }
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return _id;
    }

    public int deleteConversation(String cid) {
        SQLiteDatabase db = getWritableDatabase();

        return db.delete(TABLE_CONVERSATION, String.format("%s = ?", CONVERSATION_ID), new String[] { cid });
    }

    private interface OnSameIdWithDifferentMessageListener {
        int onSameIdWithDifferentMessage(long id, String message);
    }

    private int queryConversationUnread(String cid) {
        Cursor cursor = null;

        try {
            String[] columns = { UNREAD };
            String[] selectionArgs = { cid };

            cursor = getReadableDatabase().query(TABLE_CONVERSATION, columns, SELECTION_CONVERSATION, selectionArgs, null, null, null);

            if (cursor.moveToFirst()) {
                if (!cursor.isNull(0)) {
                    return cursor.getInt(0);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return 0;
    }

    private long queryConversationId(String cid, String message, OnSameIdWithDifferentMessageListener listener) {
        Cursor cursor = null;

        try {
            String[] columns = { COLUMN_ID, MESSAGE };
            String[] selectionArgs = { cid };

            cursor = getReadableDatabase().query(TABLE_CONVERSATION, columns, SELECTION_CONVERSATION, selectionArgs, null, null, null);

            if (cursor.moveToFirst()) {
                long id = cursor.getLong(0);

                if (message != null && !message.equals(cursor.getString(1))) {
                    if (listener != null) {
                        listener.onSameIdWithDifferentMessage(id, message);
                    }
                }

                return id;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // no existing conversation
        return -1;
    }

    private long insertConversation(String cid, String message, long timestamp, String emoji_id) {
        ContentValues values = new ContentValues(6);
        values.put(CONVERSATION_ID, cid);
        values.put(MESSAGE, message);
        values.put(REPLY_COUNT, 0);
        values.put(TIMESTAMP, timestamp);
        values.put(LAST_UPDATED, timestamp);
        values.put(EMOJI_ID, emoji_id);

        return getWritableDatabase().insert(TABLE_CONVERSATION, null, values);
    }

    private long insertReply(String cid, String rid, String uid, String message, long existing_cid, long timestamp, String emoji_id) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues(existing_cid == -1 ? 6 : 5);
        values.put(CONVERSATION_ID, cid);
        values.put(REPLY_ID, rid);
        values.put(USER_ID, uid);
        values.put(MESSAGE, message);
        values.put(EMOJI_ID, emoji_id);
        if (existing_cid != -1) {
            String[] bindArgs = { Long.toString(existing_cid) };
            db.execSQL(
                    String.format("UPDATE `%s` " +
                                    "SET `%s` = `%s` + %d, " +
                                    "`%s` = %d " +
                                    "WHERE `%s` = ?",
                            TABLE_CONVERSATION,
                            REPLY_COUNT, REPLY_COUNT, 1,
                            LAST_UPDATED, timestamp,
                            _ID, EMOJI_ID),
                    bindArgs);
        }
        values.put(TIMESTAMP, timestamp);

        return db.insert(TABLE_CONVERSATION, null, values);
    }

    private void createTable(SQLiteDatabase db) {
        db.execSQL(
                String.format("CREATE TABLE IF NOT EXISTS `%s` (" + // TABLE_CONVERSATION
                                "`%s` INTEGER PRIMARY KEY, " + // COLUMN_ID
                                "`%s` TEXT, " + // CONVERSATION_ID
                                "`%s` TEXT, " + // MESSAGE
                                "`%s` TEXT, " + // REPLY_ID
                                "`%s` TEXT, " + // USER_ID
                                "`%s` INTEGER, " + // REPLY_COUNT
                                "`%s` INTEGER, " + // FLAG
                                "`%s` INTEGER, " + // TIMESTAMP
                                "`%s` INTEGER, " + // LAST_UPDATED
                                "`%s` INTEGER, " + // UNREAD
                                "`%s` TEXT)", // EMOJI_ID
                        TABLE_CONVERSATION,
                        COLUMN_ID,
                        CONVERSATION_ID, MESSAGE, REPLY_ID, USER_ID,
                        REPLY_COUNT, FLAG, TIMESTAMP, LAST_UPDATED, UNREAD, EMOJI_ID
                )
        );
    }

//    private void dropTable(SQLiteDatabase db) {
//       db.execSQL(String.format("DROP TABLE IF EXISTS `%s`", TABLE_CONVERSATION));
//    }
}
