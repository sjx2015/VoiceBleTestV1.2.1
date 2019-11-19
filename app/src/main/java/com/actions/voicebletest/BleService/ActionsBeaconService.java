package com.actions.voicebletest.BleService;

import com.actions.voicebletest.R;
import com.actions.voicebletest.VoiceBleTestApplication;

import java.util.HashMap;

public class ActionsBeaconService extends InfoService {
 
    public static final String UUID_SERVICE = "a3c87500-8ed3-4bdf-8a39-a01bebede295";
 
    public static final String UUID_BROADCAST_INTERVAL = "a3c87501-8ed3-4bdf-8a39-a01bebede295";
    public static final String UUID_CONNECTION_CONFIGURATION = "a3c87502-8ed3-4bdf-8a39-a01bebede295";
    public static final String UUID_BROADCAST_CHANNEL = "a3c87503-8ed3-4bdf-8a39-a01bebede295";
    public static final String UUID_BROADCAST_CHANNEL_ENABLE = "a3c87504-8ed3-4bdf-8a39-a01bebede295";
    public static final String UUID_BROADCAST_CONTENT = "a3c87505-8ed3-4bdf-8a39-a01bebede295";
 
    private static final HashMap<String, String> CHARACTERISTIC_MAP = new HashMap<String, String>();
 
    static { 
        CHARACTERISTIC_MAP.put(UUID_BROADCAST_INTERVAL, VoiceBleTestApplication.getContext().getString(R.string.broadcast_internal_param));
        CHARACTERISTIC_MAP.put(UUID_CONNECTION_CONFIGURATION, VoiceBleTestApplication.getContext().getString(R.string.connect_param));
        CHARACTERISTIC_MAP.put(UUID_BROADCAST_CHANNEL, VoiceBleTestApplication.getContext().getString(R.string.broadcast_select));
        CHARACTERISTIC_MAP.put(UUID_BROADCAST_CHANNEL_ENABLE, VoiceBleTestApplication.getContext().getString(R.string.broadcast_enable));
        CHARACTERISTIC_MAP.put(UUID_BROADCAST_CONTENT, VoiceBleTestApplication.getContext().getString(R.string.broadcast_content_setting));
    } 
 
    @Override
    public String getUUID() {
        return UUID_SERVICE;
    } 
 
    @Override
    public String getName() {
        return "Actions Beacon Service";
    } 
 
    @Override
    public String getCharacteristicName(String uuid) {
        if (!CHARACTERISTIC_MAP.containsKey(uuid)) {
            return "Unknown"; 
        } 
        return CHARACTERISTIC_MAP.get(uuid);
    } 
} 