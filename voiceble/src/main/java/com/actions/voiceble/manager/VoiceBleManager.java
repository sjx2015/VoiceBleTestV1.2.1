package com.actions.voiceble.manager;

import android.content.Context;

/**
 * Created by chang on 2018/3/26.
 */

public class VoiceBleManager {
    public static final String TAG = VoiceBleManager.class.getSimpleName();

    //private static RxBleClient rxBleClient;
    private static VoiceBleManager voiceBleManager = null;

    public static VoiceBleManager getInstance(Context context){
        if (voiceBleManager == null){
            voiceBleManager = new VoiceBleManager(context);
        }
        return voiceBleManager;
    }

    private VoiceBleManager(Context context){
        create(context);
    }

    public void create(Context context){
        //rxBleClient = RxBleClient.create(context);
        //RxBleClient.setLogLevel(RxBleLog.VERBOSE);
    }

    //public RxBleClient getRxBleClient(){
        //return rxBleClient;
    //}
}
