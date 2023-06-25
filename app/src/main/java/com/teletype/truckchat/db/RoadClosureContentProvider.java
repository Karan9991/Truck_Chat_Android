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

public class RoadClosureContentProvider extends ContentProvider {

    private static final int ROAD_CLOSURE_DIR = 1;
    private static final int ROAD_CLOSURE_DIR_ALL = 2;
    private static final int ROAD_CLOSURE_DIR_NO_EXPIRED = 3;
    private static final int ROAD_CLOSURE_ITEM = 4;

    private static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(RoadClosuresContract.AUTHORITY, RoadClosuresContract.RoadClosures.DIR, ROAD_CLOSURE_DIR);
        uriMatcher.addURI(RoadClosuresContract.AUTHORITY, RoadClosuresContract.RoadClosures.DIR + "/ALL", ROAD_CLOSURE_DIR_ALL);
        uriMatcher.addURI(RoadClosuresContract.AUTHORITY, RoadClosuresContract.RoadClosures.DIR + "/NOEXPIRED", ROAD_CLOSURE_DIR_NO_EXPIRED);
        uriMatcher.addURI(RoadClosuresContract.AUTHORITY, RoadClosuresContract.RoadClosures.DIR + "/*", ROAD_CLOSURE_ITEM);
    }

    private RoadClosureDb db;

    @Override
    public boolean onCreate() {
        db = new RoadClosureDb(getContext());

        return true;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        if (uriMatcher.match(uri) != ROAD_CLOSURE_DIR) {
            throw new UnsupportedOperationException("Individual record deletion is not supported.");
        }

        int rows_affected = db.delete();

        if (rows_affected > 0) {
            Context context = getContext();
            if (context != null) {
                context.getContentResolver().notifyChange(RoadClosuresContract.RoadClosures.CONTENT_URI, null);
            }
        }

        return rows_affected;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            case ROAD_CLOSURE_DIR:
                return RoadClosuresContract.RoadClosures.CONTENT_TYPE;

            case ROAD_CLOSURE_ITEM:
                return RoadClosuresContract.RoadClosures.CONTENT_ITEM_TYPE;
        }

        return null;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        if (uriMatcher.match(uri) != ROAD_CLOSURE_DIR) {
            return null;
        }

        Long poiId = values.getAsLong(RoadClosuresContract.RoadClosures._POIID);
        if (poiId == null) {
            return null;
        }

        String name = values.getAsString(RoadClosuresContract.RoadClosures._HIGHWAY_NAME);
        if (TextUtils.isEmpty(name)) {
            return null;
        }

        Long lat = values.getAsLong(RoadClosuresContract.RoadClosures._LAT);
        if (lat == null) {
            return null;
        }

        Long lon = values.getAsLong(RoadClosuresContract.RoadClosures._LON);
        if (lon == null) {
            return null;
        }

        String label = values.getAsString(RoadClosuresContract.RoadClosures._DESC);
        String state = values.getAsString(RoadClosuresContract.RoadClosures._STATE);
        String country = values.getAsString(RoadClosuresContract.RoadClosures._COUNTRY);
        String expiration = values.getAsString(RoadClosuresContract.RoadClosures._EXPIRATION);

        long _id = db.insert(poiId, name, lat, lon, label, state, country, expiration);

        if (_id == -1) {
            return null;
        }

        Context context = getContext();
        if (context != null) {
            context.getContentResolver().notifyChange(RoadClosuresContract.RoadClosures.CONTENT_URI, null);
        }

        return ContentUris.withAppendedId(uri, _id);
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor;

        switch (uriMatcher.match(uri)) {
            case ROAD_CLOSURE_DIR_ALL:
                cursor = db.queryAlerts(true);
                break;

            case ROAD_CLOSURE_DIR_NO_EXPIRED:
                cursor = db.queryAlerts(false);
                break;

            case ROAD_CLOSURE_DIR:
            case ROAD_CLOSURE_ITEM:
//                throw new UnsupportedOperationException("Not yet implemented.");

            default:
                return new MatrixCursor(new String[0], 0);
        }

        Context context = getContext();
        if (context != null) {
            cursor.setNotificationUri(context.getContentResolver(), RoadClosuresContract.RoadClosures.CONTENT_URI);
        }

        return cursor;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Updating records is not supported.");
    }
}
