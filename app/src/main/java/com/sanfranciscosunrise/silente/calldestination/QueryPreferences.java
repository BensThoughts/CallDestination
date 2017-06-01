package com.sanfranciscosunrise.silente.calldestination;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Created by Blu-J on 5/31/17.
 */

public class QueryPreferences {
    private static final String PREF_IS_SERVICE_SEARCH = "isServiceSearch";
    private static final String PREF_IS_SERVICE_ON = "isServiceOn";
    private static final String PREF_LAST_KNOWN_PHONE_NUMBER = "lastKnownPhoneNumber";

    public static boolean isServiceOn(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_IS_SERVICE_ON, false);
    }

    public static void setServiceOn(Context context, boolean isOn) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(PREF_IS_SERVICE_ON, isOn)
                .apply();
    }

    public static boolean isServiceSearch(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_IS_SERVICE_SEARCH, true);
    }

    public static void setServiceSearch(Context context, boolean isOn) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(PREF_IS_SERVICE_SEARCH, isOn)
                .apply();
    }

    public static String getPrefLastKnownPhoneNumber(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_LAST_KNOWN_PHONE_NUMBER, null);
    }

    public static void setPrefLastKnownPhoneNumber(Context context, String lastKnownPhoneNumber) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_LAST_KNOWN_PHONE_NUMBER, lastKnownPhoneNumber)
                .apply();
    }
}
