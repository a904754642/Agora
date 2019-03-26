package com.cnlod.agora.util;

import android.util.Log;
import android.widget.Toast;

import com.cnlod.agora.AGApplication;


public class Ls {
    public static void e(String msg) {
        Log.e("-LogUtil-", msg);
    }

    public static void w(String msg) {
        Log.w("-LogUtil-", msg);
    }

    public static void d(String msg){
        Log.d("-LogUtil-",msg);
    }

    public static void ts(String msg) {
        e(msg);
        Toast.makeText(AGApplication.the(), msg, Toast.LENGTH_SHORT).show();
    }
}
