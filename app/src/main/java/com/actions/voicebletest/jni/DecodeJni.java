package com.actions.voicebletest.jni;

/**
 * Created by chang on 2018/4/13.
 */

public class DecodeJni {
    public final static int ASC_I = 1;
    public final static int ASC_II = 2;
    public final static int ASC_III = 3;
    public final static int ASC_IV = 4;
    public final static int ASC_V = 5;

    static {
        System.loadLibrary("asc_dec");
        System.loadLibrary("DecodeJni");
    }

    public native short  Init();

    public native short  decodeInit(short codec);

    public native short[] Decode(short[] bytes, short len, short codec);

}
