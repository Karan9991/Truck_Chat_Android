package com.teletype.truckchat.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.teletype.truckchat.R;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;


public final class Utils {

    private static final String PROPERTY_REG_ID = "PROPERTY_REG_ID";
    private static final String PROPERTY_USER_ID = "PROPERTY_USER_ID";
    private static final String PROPERTY_APP_VERSION = "PROPERTY_APP_VERSION";
    private static final String PROPERTY_FIRST_RUN = "PROPERTY_FIRST_RUN";
    private static final String PROPERTY_AVATAR_ID = "PROPERTY_AVATAR_ID";

    public static boolean isGooglePlayServicesAvailable(Activity activity, int requestCode) {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int statusCode = googleApiAvailability.isGooglePlayServicesAvailable(activity);
        if (statusCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(statusCode)) {
                googleApiAvailability.getErrorDialog(activity, statusCode, requestCode).show();
                return false;
            }

            throw new UnsupportedOperationException(googleApiAvailability.getErrorString(statusCode));
        }

        return true;
    }

    public static String getSerialNumber(Context context) {
        String id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        try {
            id += "ttt";
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(id.getBytes(), 0, id.length());
            BigInteger bi = new BigInteger(1, md.digest());

            return bi.toString(16);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return "";
    }

    public static String getPaddedSerialNumber(Context context) {
        String serial = getSerialNumber(context);

        final int padding = 4;
        if ((serial.length() % padding) == 0) {
            StringBuilder stringBuilder = new StringBuilder();

            for (int i = 0; i < serial.length(); i += padding) {
                if (stringBuilder.length() > 0) {
                    stringBuilder.append("-");
                }

                stringBuilder.append(serial.substring(i, i + padding));
            }

            return stringBuilder.toString();
        }

        return serial;
    }

    public static String getCurrentRegistrationId(Context context) {
        final SharedPreferences prefs = getSharedPreferences(context);

        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (TextUtils.isEmpty(registrationId)) {
            return "";
        }

        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);

        if (registeredVersion != currentVersion) {
            return "";
        }

        return registrationId;
    }

    public static String getCurrentUserId(Context context) {
        final SharedPreferences prefs = getSharedPreferences(context);

        return prefs.getString(PROPERTY_USER_ID, "");
    }

    public static void storeRegistrationId(Context context, String regId, String userId) {
        final SharedPreferences prefs = getSharedPreferences(context);
        int appVersion = getAppVersion(context);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putString(PROPERTY_USER_ID, userId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.apply();
    }

    public static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    public static String getAppVersionName(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    public static boolean isLocationDisabled(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            //noinspection deprecation
            String locationProvidersAllowed = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return TextUtils.isEmpty(locationProvidersAllowed) || !locationProvidersAllowed.contains("network");
        } else {
            try {
                switch (Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE)) {
                    case Settings.Secure.LOCATION_MODE_OFF:
                    case Settings.Secure.LOCATION_MODE_SENSORS_ONLY:
                        break;

                    default:
                        return false;
                }

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    public static boolean isNetworkEnabled(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public static boolean isFirstRun(Context context) {
        final SharedPreferences prefs = getSharedPreferences(context);

        return prefs.getBoolean(PROPERTY_FIRST_RUN, true);
    }

    public static void setFirstRun(Context context) {
        final SharedPreferences prefs = getSharedPreferences(context);

        prefs.edit().putBoolean(PROPERTY_FIRST_RUN, false).apply();
    }

    public static int getAvatar(Context context) {
        final SharedPreferences prefs = getSharedPreferences(context);

        return prefs.getInt(PROPERTY_AVATAR_ID, -1);
    }

    public static void setAvatar(Context context, int drawableId) {
        final SharedPreferences prefs = getSharedPreferences(context);
        prefs.edit().putInt(PROPERTY_AVATAR_ID, drawableId).apply();
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static void composeTellAFriendEmail(Context context) {
        String to = "";
        String subject = context.getString(R.string.about_tell_a_friend_mail_subject);
        String body = "\n" + context.getString(R.string.about_tell_a_friend_mail_body);

        Intent email = new Intent(Intent.ACTION_SENDTO);
        email.setData(Uri.fromParts("mailto", to, null));
        email.putExtra(android.content.Intent.EXTRA_SUBJECT,subject);
        email.putExtra(android.content.Intent.EXTRA_TEXT, body);

        try {
            context.startActivity(Intent.createChooser(email, context.getString(R.string.about_tell_a_friend_mail_choose)));
        } catch (ActivityNotFoundException anfe) {
            anfe.printStackTrace();
            Toast toast = Toast.makeText(context, R.string.error_no_email, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }

    public static String getLocalizedLongDate(Context context, String iso_8601) {
        final SimpleDateFormat SDF_ISO8601 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        String longDate = "";

        if (!TextUtils.isEmpty(iso_8601)) {
            try {
                if (!iso_8601.contains("0000-00-00")) {
                    DateFormat df = android.text.format.DateFormat.getLongDateFormat(context);
                    longDate = df.format(SDF_ISO8601.parse(iso_8601));
                }
            } catch (ParseException e) {
                e.printStackTrace();
                return null;
            }
        }

        return longDate;
    }

}
