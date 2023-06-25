package com.teletype.truckchat.services;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

import com.teletype.truckchat.db.RoadClosuresContract;
import com.teletype.truckchat.util.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RoadClosuresIntentService extends IntentService {

    private static final long CUSTOM_START_ID = 1500000000l;

    private static final String ACTION_GET_ROAD_CLOSURES = "com.teletype.truckchat.services.action.GET_ROAD_CLOSURES";

    public RoadClosuresIntentService() {
        super("RoadClosuresIntentService");
    }

    public static void startActionGetRoadClosures(Context context) {
        Intent intent = new Intent(context, RoadClosuresIntentService.class);
        intent.setAction(ACTION_GET_ROAD_CLOSURES);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_GET_ROAD_CLOSURES.equals(action)) {
                handleActionGetRoadClosures();
            }
        }
    }

    private void handleActionGetRoadClosures() {
        TTHttpConnection connection = new TTHttpConnection();
        connection.addURL("http://www.teletype.com/truckroutes/getTravelAlertsGooglePlay.php");
        connection.addQuery("userid", Utils.getSerialNumber(this));
        connection.setTimeouts(500, 1000);
        connection.setIsResultWanted(true);
        if (connection.execute()) {
            try {
                JSONObject response = new JSONObject(connection.getResult());
                JSONObject status = response.getJSONObject("status");
                if (status.getInt("code") != 200) {
                    //throw new Exception(status.getString("message"));
                    return;
                }

                ContentValues values = new ContentValues(8);
                ContentResolver contentResolver = getContentResolver();
                contentResolver.delete(RoadClosuresContract.RoadClosures.CONTENT_URI, null, null);

                JSONArray travel_alerts = response.getJSONArray("travel_alerts");
                int count = travel_alerts.length();
                for (int i = 0; i < count; ++i) {
                    JSONObject alert = travel_alerts.getJSONObject(i);
                    values.clear();
                    values.put(RoadClosuresContract.RoadClosures._POIID, CUSTOM_START_ID + alert.getLong("id"));
                    values.put(RoadClosuresContract.RoadClosures._HIGHWAY_NAME, alert.getString("hwy"));
                    values.put(RoadClosuresContract.RoadClosures._LAT, alert.getLong("lat"));
                    values.put(RoadClosuresContract.RoadClosures._LON, alert.getLong("lon"));
                    values.put(RoadClosuresContract.RoadClosures._DESC, alert.getString("desc"));
                    values.put(RoadClosuresContract.RoadClosures._STATE, alert.getString("state"));
                    values.put(RoadClosuresContract.RoadClosures._COUNTRY, alert.getString("country"));
                    values.put(RoadClosuresContract.RoadClosures._EXPIRATION, alert.getString("expiration"));

                    contentResolver.insert(RoadClosuresContract.RoadClosures.CONTENT_URI, values);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }
}
