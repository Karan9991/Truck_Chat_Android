package com.teletype.truckchat.db;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public final class RoadClosuresContract extends BaseContract {

    public static final String AUTHORITY = SUPER_AUTHORITY + ".road_closures";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    public static final class RoadClosures implements BaseColumns {

        public static final String DIR = "road_closures";

        public static final String _POIID = "_data1";
        public static final String _HIGHWAY_NAME = "_data2";
        public static final String _LAT = "_data3";
        public static final String _LON = "_data4";
        public static final String _DESC = "_data5";
        public static final String _STATE = "_data6";
        public static final String _COUNTRY = "_data7";
        public static final String _EXPIRATION = "_data8";

        public static final Uri CONTENT_URI = Uri.withAppendedPath(RoadClosuresContract.CONTENT_URI, DIR);
        public static final Uri CONTENT_URI_ALL = Uri.withAppendedPath(Uri.withAppendedPath(RoadClosuresContract.CONTENT_URI, DIR), "ALL");
        public static final Uri CONTENT_URI_NO_EXPIRED = Uri.withAppendedPath(Uri.withAppendedPath(RoadClosuresContract.CONTENT_URI, DIR), "NOEXPIRED");
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + "_" + DIR;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + AUTHORITY + "_" + DIR;
    }
}
