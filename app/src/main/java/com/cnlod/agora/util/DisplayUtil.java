package com.cnlod.agora.util;

import com.cnlod.agora.AGApplication;

public class DisplayUtil {  // 将px值转换为dip或dp值，保证尺寸大小不变
    public static int px2dip(float pxValue) {
        final float scale = AGApplication.the().getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    // 将dip或dp值转换为px值，保证尺寸大小不变
    public static int dip2px(float dipValue) {
        final float scale = AGApplication.the().getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    // 将px值转换为sp值，保证文字大小不变
    public static int px2sp(float pxValue) {
        final float fontScale = AGApplication.the().getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue / fontScale + 0.5f);
    }

    // 将sp值转换为px值，保证文字大小不变
    public static int sp2px(float spValue) {
        final float fontScale = AGApplication.the().getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    // 屏幕宽度（像素）
    public static int getWindowWidth() {
        return AGApplication.the().getApplicationContext().getResources().getDisplayMetrics().widthPixels;
    }

    // 屏幕高度（像素）
    public static int getWindowHeight() {
        return AGApplication.the().getApplicationContext().getResources().getDisplayMetrics().heightPixels;
    }
}
