package com.actions.voicebletest.BleService;

/**
 * Created by chenxiangjie on 2016/11/14.
 */

public abstract class InfoService {
    private final static String TAG = InfoService.class.getSimpleName();

    protected InfoService() {
    }

    public abstract String getUUID();

    public abstract String getName();

    public abstract String getCharacteristicName(String uuid);
}