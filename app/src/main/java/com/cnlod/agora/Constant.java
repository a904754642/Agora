package com.cnlod.agora;

/**
 * Created by beryl on 2017/11/6.
 */

public class Constant {

    public static int CALL_IN = 0x01;
    public static int CALL_OUT = 0x02;

    private static long timeLast;

    public static boolean isFastlyClick() {
        if (System.currentTimeMillis() - timeLast < 1500) {
            timeLast = System.currentTimeMillis();
            return true;
        } else {
            timeLast = System.currentTimeMillis();
            return false;
        }
    }

    public static String userId1 = "2";
    public static String userId2 = "22";
    public static String userId3 = "34";
}
