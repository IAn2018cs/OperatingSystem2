package cn.ian2018.operatingsystem2;

import android.util.Log;

/**
 * Created by 陈帅 on 2017/4/24/024.
 */

public class Logs {
    private static final String TAG = "操作系统";

    public static void d (String msg) {
        Log.d(TAG, msg);
    }

    public static void i (String msg) {
        Log.i(TAG, msg);
    }

    public static void e (String msg) {
        Log.e(TAG, msg);
    }

    public static void v (String msg) {
        Log.v(TAG, msg);
    }

    public static void w (String msg) {
        Log.w(TAG, msg);
    }
}
