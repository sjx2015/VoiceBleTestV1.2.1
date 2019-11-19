package com.actions.voicebletest.BleService;

import com.actions.voicebletest.R;
import com.actions.voicebletest.VoiceBleTestApplication;

import java.util.HashMap;

public class ActionsTransmissionService extends InfoService {

    public static final String UUID_RECEIVE_DATA = "001120a1-2233-4455-6677-889912345678";
    public static final String UUID_SEND_DATA = "001120a2-2233-4455-6677-889912345678";
    public static final String UUID_CONTROL = "001120a3-2233-4455-6677-889912345678";

    public static final String UUID_SERVICE = "001120a0-2233-4455-6677-889912345678";
    private static final HashMap<String, String> CHARACTERISTIC_MAP = new HashMap<String, String>();

    static {
        CHARACTERISTIC_MAP.put(UUID_RECEIVE_DATA, VoiceBleTestApplication.getContext().getString(R.string.data_receive_channel));
        CHARACTERISTIC_MAP.put(UUID_SEND_DATA, VoiceBleTestApplication.getContext().getString(R.string.data_send_channel));
        CHARACTERISTIC_MAP.put(UUID_CONTROL, VoiceBleTestApplication.getContext().getString(R.string.command_control_channel));
    }

    @Override
    public String getUUID() {
        return UUID_SERVICE;
    }

    @Override
    public String getName() {
        return "Actions Transmission Service";
    }

    @Override
    public String getCharacteristicName(String uuid) {
        if (!CHARACTERISTIC_MAP.containsKey(uuid)) {
            return "Unknown";
        }
        return CHARACTERISTIC_MAP.get(uuid);
    }
} 