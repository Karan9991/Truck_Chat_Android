package com.teletype.truckchat.ui.road;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.teletype.truckchat.R;
import com.teletype.truckchat.db.RoadClosuresContract;
import com.teletype.truckchat.util.Utils;

public class RoadClosuresAdapter extends ResourceCursorAdapter {

    private static final int LOADER_ID = 3;

    private final Context mContext;

    public RoadClosuresAdapter(Context context) {
        super(context, R.layout.adapter_road_closures, null, 0);

        this.mContext = context;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag(R.id.roadClosuresAdapter_linearLayout_topic);

        String highwayName = cursor.getString(2);
        String description = cursor.getString(5);
        String state = cursor.getString(6);
        String country = cursor.getString(7);
        String expiration = cursor.getString(8);

        viewHolder.highwayName.setText(highwayName);
        viewHolder.description.setText(description);

        if (!TextUtils.isEmpty(state)) {
            if (!TextUtils.isEmpty(country)) {
                viewHolder.locale.setText(context.getString(R.string.locale, state, country));
            } else {
                viewHolder.locale.setText(state);
            }
        } else if (!TextUtils.isEmpty(country)) {
            viewHolder.locale.setText(country);
        } else {
            viewHolder.locale.setText(null);
        }

        String until = Utils.getLocalizedLongDate(context, expiration);
        if (until == null) {
            viewHolder.expiration.setText(null);
        } else {
            if (until.length() == 0) {
                viewHolder.expiration.setText(R.string.indefinitely);
            } else {
                viewHolder.expiration.setText(context.getString(R.string.until, until));
            }
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = super.newView(context, cursor, parent);

        ViewHolder viewHolder = new ViewHolder();
        viewHolder.highwayName = (TextView) view.findViewById(R.id.roadClosuresAdapter_textView_highwayName);
        viewHolder.description = (TextView) view.findViewById(R.id.roadClosuresAdapter_textView_description);
        viewHolder.locale = (TextView) view.findViewById(R.id.roadClosuresAdapter_textView_locale);
        viewHolder.expiration = (TextView) view.findViewById(R.id.roadClosuresAdapter_textView_expiration);
        view.setTag(R.id.roadClosuresAdapter_linearLayout_topic, viewHolder);

        return view;
    }

    public void loadData() {
        ((FragmentActivity) mContext).getSupportLoaderManager()
                .initLoader(LOADER_ID, null,
                        new LoaderManager.LoaderCallbacks<Cursor>() {
                            @Override
                            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                                Uri uri = RoadClosuresContract.RoadClosures.CONTENT_URI_NO_EXPIRED
                                        .buildUpon()
                                        .build();

                                return new CursorLoader(mContext, uri, null, null, null, null);
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

    public RoadClosure getRoadClosure(int position) {
        Cursor cursor = getCursor();
        if (cursor.moveToPosition(position)) {
            return new RoadClosure(
                    cursor.getLong(1),
                    cursor.getString(2),
                    cursor.getLong(3) / 1E6,
                    cursor.getLong(4) / 1E6,
                    cursor.getString(5),
                    cursor.getString(6),
                    cursor.getString(7),
                    cursor.getString(8));
        }

        return null;
    }

    public final static class RoadClosure {
        public final long poiId;
        public final String highwayName;
        public final double latitude;
        public final double longitude;
        public final String description;
        public final String locality;
        public final String country;
        public final String expiration;

        private RoadClosure(long poiId, String highwayName, double latitude, double longitude, String description, String locality, String country, String expiration) {
            this.poiId = poiId;
            this.highwayName = highwayName;
            this.latitude = latitude;
            this.longitude = longitude;
            this.description = description;
            this.locality = locality;
            this.country = country;
            this.expiration = expiration;
        }
    }

    private class ViewHolder {
        TextView highwayName;
        TextView description;
        TextView locale;
        TextView expiration;
    }
}
