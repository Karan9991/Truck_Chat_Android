package com.teletype.truckchat.db;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public final class ConversationsContract extends BaseContract {

    public static final String AUTHORITY = SUPER_AUTHORITY + ".conversations";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    public static final class Conversation implements BaseColumns {

        public static final String DIR = "conversation";

        public static final String _CID = "_data1";
        public static final String _MESSAGE = "_data2";
        public static final String _RID = "_data3";
        public static final String _UID = "_data4";
        public static final String _RCOUNT = "_data5";
        public static final String _FLAG = "_data6";
        public static final String _TIMESTAMP = "_data7";
        public static final String _LUPDATED = "_data8";
        public static final String _UNREAD = "_data9";
        public static final String _EMOJI_ID = "_data10";

        public static final Uri CONTENT_URI = Uri.withAppendedPath(ConversationsContract.CONTENT_URI, DIR);
        public static final Uri CONTENT_URI_ALL = Uri.withAppendedPath(Uri.withAppendedPath(ConversationsContract.CONTENT_URI, DIR), "ALL");
        public static final Uri CONTENT_URI_STARRED = Uri.withAppendedPath(Uri.withAppendedPath(ConversationsContract.CONTENT_URI, DIR), "STARRED");
        public static final Uri CONTENT_URI_UNSTARRED = Uri.withAppendedPath(Uri.withAppendedPath(ConversationsContract.CONTENT_URI, DIR), "UNSTARRED");
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + "_" + DIR;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + AUTHORITY + "_" + DIR;
    }
}
