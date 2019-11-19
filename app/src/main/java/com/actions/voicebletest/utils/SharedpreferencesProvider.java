package com.actions.voicebletest.utils;

import android.content.SharedPreferences;

import com.actions.voicebletest.VoiceBleTestApplication;

/**
 * Created by zhongchangwen on 2017/11/6.
 */

public class SharedpreferencesProvider {

    public static String OTA_PATH = "OTA_PATH";
    public static String BLE_NAME_FILTER = "BLE_NAME_FILTER";


    public static void saveSharePerferences(String key, String value){
        SharedPreferences settings = VoiceBleTestApplication.getContext().getSharedPreferences("setting", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static String getSharePerferences(String key){
        SharedPreferences settings = VoiceBleTestApplication.getContext().getSharedPreferences("setting", 0);
        String value = settings.getString(key,"");
        return value;
    }

    public static void saveSharePerferences(String key, int value){
        SharedPreferences settings = VoiceBleTestApplication.getContext().getSharedPreferences("setting", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public static int getSharePerferences(String key, int val){
        SharedPreferences settings = VoiceBleTestApplication.getContext().getSharedPreferences("setting", 0);
        int value = settings.getInt(key, val);
        return value;
    }
}
