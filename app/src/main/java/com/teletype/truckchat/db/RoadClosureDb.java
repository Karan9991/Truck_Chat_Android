package com.teletype.truckchat.db;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RoadClosureDb extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = RoadClosuresContract.RoadClosures.DIR + ".db";
    private static final String TABLE_NAME = RoadClosuresContract.RoadClosures.DIR;

    private static final String COLUMN_ID = RoadClosuresContract.RoadClosures._ID;
    private static final String COLUMN_POIID = RoadClosuresContract.RoadClosures._POIID;
    private static final String COLUMN_HIGHWAY_NAME = RoadClosuresContract.RoadClosures._HIGHWAY_NAME;
    private static final String COLUMN_LAT = RoadClosuresContract.RoadClosures._LAT;
    private static final String COLUMN_LON = RoadClosuresContract.RoadClosures._LON;
    private static final String COLUMN_DESC = RoadClosuresContract.RoadClosures._DESC;
    private static final String COLUMN_STATE = RoadClosuresContract.RoadClosures._STATE;
    private static final String COLUMN_COUNTRY = RoadClosuresContract.RoadClosures._COUNTRY;
    private static final String COLUMN_EXPIRATION = RoadClosuresContract.RoadClosures._EXPIRATION;

    private static final SimpleDateFormat SDF_ISO8601 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private static final String INSERT = String.format("INSERT INTO `%s` VALUES (NULL, ?, ?, ?, ?, ?, ?, ?, ?);", TABLE_NAME);
    private static final String[] ALL_COLUMNS = new String[]{COLUMN_ID, COLUMN_POIID, COLUMN_HIGHWAY_NAME, COLUMN_LAT, COLUMN_LON, COLUMN_DESC, COLUMN_STATE, COLUMN_COUNTRY, COLUMN_EXPIRATION};

    public RoadClosureDb(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(String.format("CREATE TABLE IF NOT EXISTS `%s` (" + // TABLE_NAME
                        "`%s` INTEGER PRIMARY KEY, " + // COLUMN_ID
                        "`%s` INTEGER UNIQUE NOT NULL, " + // COLUMN_POIID (id)
                        "`%s` TEXT NOT NULL, " + // COLUMN_HIGHWAY_NAME (hwy)
                        "`%s` INTEGER NOT NULL, " + // COLUMN_LAT (lat)
                        "`%s` INTEGER NOT NULL, " + // COLUMN_LON (lon)
                        "`%s` TEXT DEFAULT NULL, " + // COLUMN_DESC (desc)
                        "`%s` TEXT DEFAULT NULL, " + // COLUMN_STATE
                        "`%s` TEXT DEFAULT NULL, " + // COLUMN_COUNTRY
                        "`%s` TEXT DEFAULT NULL)", // COLUMN_EXPIRATION
                TABLE_NAME, COLUMN_ID, COLUMN_POIID, COLUMN_HIGHWAY_NAME, COLUMN_LAT, COLUMN_LON, COLUMN_DESC, COLUMN_STATE, COLUMN_COUNTRY, COLUMN_EXPIRATION));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(String.format("DROP TABLE IF EXISTS `%s`", TABLE_NAME));
        onCreate(db);
    }

    int delete() {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_NAME, "1", null);
    }

    long insert(long poi_id, @NonNull String name, long latitudeE6, long longitudeE6, String label, String state, String country, String expiration) {
        SQLiteDatabase db = getWritableDatabase();

        SQLiteStatement sqLiteStatement = db.compileStatement(INSERT);
        sqLiteStatement.bindLong(1, poi_id); // COLUMN_POIID
        sqLiteStatement.bindString(2, name); // COLUMN_HIGHWAY_NAME (hwy)
        sqLiteStatement.bindLong(3, latitudeE6); // COLUMN_LAT (lat)
        sqLiteStatement.bindLong(4, longitudeE6); // COLUMN_LON (lon)

        if (label == null) {
            sqLiteStatement.bindNull(5); // COLUMN_DESC (desc)
        } else {
            sqLiteStatement.bindString(5, label); // COLUMN_DESC (desc)
        }

        if (state == null) {
            sqLiteStatement.bindNull(6); // COLUMN_STATE
        } else {
            sqLiteStatement.bindString(6, state); // COLUMN_STATE
        }

        if (country == null) {
            sqLiteStatement.bindNull(7); // COLUMN_COUNTRY
        } else {
            sqLiteStatement.bindString(7, country); // COLUMN_COUNTRY
        }

        if (expiration == null) {
            sqLiteStatement.bindNull(8); // COLUMN_EXPIRATION
        } else {
            sqLiteStatement.bindString(8, expiration); // COLUMN_EXPIRATION
        }

        return sqLiteStatement.executeInsert();
    }

    Cursor queryAlerts(boolean includeExpired) {
        Cursor c = null;
        MatrixCursor result = new MatrixCursor(ALL_COLUMNS);

        try {
            c = getReadableDatabase().query(TABLE_NAME, ALL_COLUMNS, null, null, null, null, COLUMN_COUNTRY + " COLLATE NOCASE, " + COLUMN_STATE + " COLLATE NOCASE, " + COLUMN_HIGHWAY_NAME + " COLLATE NOCASE");
            Date now = new Date();

            while (c.moveToNext()) {
                try {
                    String expiry = c.getString(8); // COLUMN_EXPIRATION
                    if (includeExpired || expiry == null || expiry.contains("0000-00-00") || now.before(SDF_ISO8601.parse(expiry))) {
                        result.addRow(
                                new Object[]{
                                        c.getString(0), // COLUMN_ID
                                        c.getString(1), // COLUMN_POIID
                                        c.getString(2), // COLUMN_HIGHWAY_NAME
                                        c.getString(3), // COLUMN_LAT
                                        c.getString(4), // COLUMN_LON
                                        c.getString(5), // COLUMN_DESC
                                        c.getString(6), // COLUMN_STATE
                                        c.getString(7), // COLUMN_COUNTRY
                                        expiry
                                }
                        );
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return result;
    }
}
