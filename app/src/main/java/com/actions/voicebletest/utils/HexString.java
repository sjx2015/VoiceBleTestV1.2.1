package com.actions.voicebletest.utils;

public class HexString {

    private HexString() {
        // Utility class.
    }

    private final static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];

        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }

        return new String(hexChars);
    }

    public static byte[] hexToBytes(String hexRepresentation) {
        if (hexRepresentation.length() % 2 == 1) {
            throw new IllegalArgumentException("hexToBytes requires an even-length String parameter");
        }

        int len = hexRepresentation.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexRepresentation.charAt(i), 16) << 4)
                    + Character.digit(hexRepresentation.charAt(i + 1), 16));
        }

        return data;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len/2];

        for(int i = 0; i < len; i+=2){
            data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }

        return data;
    }

    final protected static char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
    public static String byteArrayToHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length*2];
        int v;

        for(int j=0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j*2] = hexArray[v>>>4];
            hexChars[j*2 + 1] = hexArray[v & 0x0F];
        }

        return new String(hexChars);
    }
}
