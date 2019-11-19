package com.actions.voicebletest.bean;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by chang on 2018/4/20.
 */

public class KeyMapping {
    Map mKeyMap = new HashMap<String,String>();
    public KeyMapping(){
        mKeyMap.put("300000", "REMOTE_KEY_POWER");
        mKeyMap.put("400000", "REMOTE_KEY_MENU");
        mKeyMap.put("410000", "REMOTE_KEY_OK");
        mKeyMap.put("420000", "REMOTE_KEY_UP");
        mKeyMap.put("430000", "REMOTE_KEY_DOWN");
        mKeyMap.put("440000", "REMOTE_KEY_LEFT");
        mKeyMap.put("450000", "REMOTE_KEY_RIGHT ");
        mKeyMap.put("950000", "REMOTE_KEY_HELP");
        mKeyMap.put("e20000", "REMOTE_KEY_MUTE");
        mKeyMap.put("e90000", "REMOTE_KEY_VOL_INC");
        mKeyMap.put("ea0000", "REMOTE_KEY_VOL_DEC");
        mKeyMap.put("0c0200", "REMOTE_KEY_VOICE_COMMAND");
        mKeyMap.put("0d0200", "REMOTE_KEY_VOICE_COMMAND");
        mKeyMap.put("0e0200", "REMOTE_KEY_VOICE_COMMAND");
        mKeyMap.put("0f0200", "REMOTE_KEY_VOICE_COMMAND");
        mKeyMap.put("210200", "REMOTE_KEY_SEARCH");
        mKeyMap.put("230200", "REMOTE_KEY_HOME");
        mKeyMap.put("240200", "REMOTE_KEY_BACK");
        mKeyMap.put("cccc00", "REMOTE_KEY_VOICE_COMMAND_END");
    }

    public String getKeyValue(String key){
        if (key == null)
            return null;
        key = key.toLowerCase();
        if (mKeyMap.containsKey(key))
            return mKeyMap.get(key).toString();
        else
            return null;
    }
}
