package com.actions.voicebletest.dfu;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by chang on 2018/6/5.
 */

public class PartitionTpyeMap {
    private Map<String, Integer> mPartitionType = new HashMap<String, Integer>();

    public PartitionTpyeMap(){
        mPartitionType.put("RESERVE", 0);
        mPartitionType.put("BOOT", 1);
        mPartitionType.put("SYSTEM", 2);
        mPartitionType.put("RECOVERY", 3);
        mPartitionType.put("DATA", 4);
        mPartitionType.put("DTM", 5);
    }

    public int getType(String key){
        return mPartitionType.get(key);
    }
}
