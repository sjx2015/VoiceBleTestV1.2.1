package com.actions.voicebletest.utils;

import android.util.Log;

/**
 * Created by chenxiangjie on 2016/12/13.
 */

public class SpeedUtil {
    private static final String TAG = "SpeedUtil";

    public static String calculateSpeed(long startTime, long endTime, long size) {
        Log.d(TAG, "time: " + ((endTime - startTime) / 1000) + " secs, size: " + size + " bytes");
        double rate = ((size  / ((endTime - startTime) / 1000f)) * 8);
        rate = Math.round(rate * 100.0) / 100.0;
        String rateValue;

        if (rate > 1000_000) {
            rateValue = String.format("%.2f", rate / 1024).concat(" Mbps");
        } else if (rate > 1000) {
            rateValue = String.format("%.2f", rate / 1024).concat(" Kbps");
        } else {
            rateValue = String.format("%.2f", rate).concat(" bps");
        }
        return rateValue;
    }

}
