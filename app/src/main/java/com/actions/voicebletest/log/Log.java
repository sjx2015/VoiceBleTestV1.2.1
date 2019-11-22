package com.actions.voicebletest.log;

public class Log {
    private static final boolean DEBUG = true;
    public static void d(String tag, String msg){
        if(DEBUG) android.util.Log.d(tag, msg);
    }

    public static void e(String tag, String msg){
        if(DEBUG) android.util.Log.e(tag, msg);
    }

    public static void i(String tag, String msg){
        if(DEBUG) android.util.Log.i(tag, msg);
    }
}
