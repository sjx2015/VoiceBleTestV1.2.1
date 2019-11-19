package com.actions.voicebletest.utils;

/**
 * Created by chang on 2018/6/5.
 */

public class LittleEndian {

    public static int ByteArrayToInt(byte[] b) {
        return ByteArrayToInt(b, 0);
    }

    public static int ByteArrayToInt(byte[] b, int index) {
        return ((b[index + 3] & 0xff) << 24) + ((b[index + 2] & 0xff) << 16) + ((b[index + 1] & 0xff) << 8) + (b[index] & 0xff);
    }

    public static short ByteArrayToShort(byte[] b) {
        return ByteArrayToShort(b, 0);
    }

    public static int ByteArray16ToInt(byte[] b, int index) {
        return (int) (((b[index + 1] & 0xff) << 8) + (b[index] & 0xff));
    }

    public static int ByteArray16ToShort(byte[] b) {
        return ByteArrayToShort(b, 0);
    }

    public static short ByteArrayToShort(byte[] b, int index) {
        return (short) (((b[index + 1] & 0xff) << 8) + (b[index] & 0xff));
    }

    public static short ByteToShort(byte b){
        return (short)(b & 0xff);
    }

    public static byte ShortToByte(short b){
        return (byte) (b & 0xff);
    }

    public static long ByteArrayToLong(byte[] b) {
        return ByteArrayToInt(b, 0);
    }

    public static long ByteArrayToLong(byte[] b, int index) {
        return ((b[index + 3] & 0xff) << 24) + ((b[index + 2] & 0xff) << 16) + ((b[index + 1] & 0xff) << 8) + (b[index] & 0xff);
    }

    public static void fillByteArrayInt(byte[] b, int index, int value) {
        b[index++] = (byte) ((value >> 0) & 0xff);
        b[index++] = (byte) ((value >> 8) & 0xff);
        b[index++] = (byte) ((value >> 16) & 0xff);
        b[index++] = (byte) ((value >> 24) & 0xff);
    }

    public static void fillByteArrayShort(byte[] b, int index, short value) {
        b[index++] = (byte) ((value >> 0) & 0xff);
        b[index++] = (byte) ((value >> 8) & 0xff);
    }

    public static void fillByteArrayShort(byte[] b, int index, int value) {
        b[index++] = (byte) ((value >> 0) & 0xff);
        b[index++] = (byte) ((value >> 8) & 0xff);
    }

    public static void fillByteArrayLong(byte[] b, int index, long value) {
        b[index++] = (byte) ((value >> 0) & 0xff);
        b[index++] = (byte) ((value >> 8) & 0xff);
        b[index++] = (byte) ((value >> 16) & 0xff);
        b[index++] = (byte) ((value >> 24) & 0xff);
    }
}
