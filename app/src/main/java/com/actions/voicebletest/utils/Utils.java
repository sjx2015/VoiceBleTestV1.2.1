package com.actions.voicebletest.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.zip.CRC32;

/**
 * Created by chang on 2018/3/23.
 */

public class Utils {

    public static final String TAG = Utils.class.getSimpleName();

    public static void showToast(String message, Context context) {
        Toast toast = null;
        if (toast != null) {
            toast.cancel();
            toast = null;
        }
        toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.show();
    }
     /**
     * 计算出来的位置，y方向就在anchorView的上面和下面对齐显示，x方向就是与屏幕右边对齐显示
     * 如果anchorView的位置有变化，就可以适当自己额外加入偏移来修正
     * @param anchorView  呼出window的view
     * @param contentView   window的内容布局
     * @return window显示的左上角的xOff,yOff坐标
     */
    public static int[] calculatePopWindowPos(final View anchorView, final View contentView) {
        final int windowPos[] = new int[2];
        final int anchorLoc[] = new int[2];
        // 获取锚点View在屏幕上的左上角坐标位置
        anchorView.getLocationOnScreen(anchorLoc);
        final int anchorHeight = anchorView.getHeight();
        // 获取屏幕的高宽
        final int screenHeight = getScreenHeight(anchorView.getContext());
        final int screenWidth = getScreenWidth(anchorView.getContext());
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        // 计算contentView的高宽
        final int windowHeight = contentView.getMeasuredHeight();
        final int windowWidth = contentView.getMeasuredWidth();
        // 判断需要向上弹出还是向下弹出显示
        final boolean isNeedShowUp = (screenHeight - anchorLoc[1] - anchorHeight < windowHeight);
        if (isNeedShowUp) {
            windowPos[0] = screenWidth - windowWidth;
            windowPos[1] = anchorLoc[1] - windowHeight;
        } else {
            windowPos[0] = screenWidth - windowWidth;
            windowPos[1] = anchorLoc[1] + anchorHeight;
        }
        return windowPos;
    }

    /**
     * 获取屏幕高度(px)
     */
    public static int getScreenHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }
    /**
     * 获取屏幕宽度(px)
     */
    public static int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    //根据时间长短计算语音条宽度:200dp
    public synchronized static int getVoiceLineWight2(Context context, long seconds) {
        //1-2s是最短的。2-10s每秒增加一个单位。10-60s每10s增加一个单位。
        int width = getScrWidth(context) / 2;
        if (seconds >= width) {
            return dip2px(context, width * 2/3);
        }
        if (seconds <= 60) {
            return dip2px(context, width/3);
        } else {
            //90~170
            return dip2px(context, width*1/2);
        }
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     * @return px
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 获得屏幕宽度
     * @param context
     * @return
     */
    public static int getScrWidth(Context context)
    {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        int width = outMetrics.widthPixels;// 屏幕宽度（像素）
        int height= outMetrics.heightPixels; // 屏幕高度（像素）
        float density = outMetrics.density;//屏幕密度（0.75 / 1.0 / 1.5）
        int densityDpi = outMetrics.densityDpi;//屏幕密度dpi（120 / 160 / 240）
        //屏幕宽度算法:屏幕宽度（像素）/屏幕密度
        int screenWidth = (int) (width/density);//屏幕宽度(dp)
        int screenHeight = (int)(height/density);//屏幕高度(dp)
        return screenWidth;
    }

    /**
     * 获得屏幕宽度
     * @param context
     * @return
     */
    public static int getScrWidthPix(Context context)
    {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        int width = outMetrics.widthPixels;// 屏幕宽度（像素）
        int height= outMetrics.heightPixels; // 屏幕高度（像素）
        float density = outMetrics.density;//屏幕密度（0.75 / 1.0 / 1.5）
        int densityDpi = outMetrics.densityDpi;//屏幕密度dpi（120 / 160 / 240）
        //屏幕宽度算法:屏幕宽度（像素）/屏幕密度
        int screenWidth = (int) (width/density);//屏幕宽度(dp)
        int screenHeight = (int)(height/density);//屏幕高度(dp)
        return width;
    }

    // 复制文件
    public static void copyFile(File sourceFile, File targetFile) throws IOException {
        BufferedInputStream inBuff = null;
        BufferedOutputStream outBuff = null;
        try {
            inBuff = new BufferedInputStream(new FileInputStream(sourceFile));
            outBuff = new BufferedOutputStream(new FileOutputStream(targetFile));
            byte[] b = new byte[1024];
            int len;
            while ((len = inBuff.read(b)) != -1) {
                outBuff.write(b, 0, len);
            }
            outBuff.flush();
        } finally {
            // 关闭
            if (inBuff != null)
                inBuff.close();
            if (outBuff != null)
                outBuff.close();
        }
    }

    public static Date timeStampToDate(Long time){
        Timestamp ts = new Timestamp(time);
        Date date = new Date();
        try {
            date = ts;
            return date;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String formatSecondsDuration(long value){
        long second = value;// 秒
        long minute = 0;// 分
        long hour = 0;// 小时
        if(second >= 60) {
            minute = (second/60);
            second = (second%60);
            if(minute >= 60) {
                hour = (minute/60);
                minute = (minute%60);
            }
        }
        String result = "00:00";
        if(hour > 0) {
            result = String.format("%02d:%02d:%02d", hour, minute, second);
        }
        else{
            result = String.format("%02d:%02d", minute, second);
        }
        return result;
    }

    public static long getFileSize(String path){
        File f= new File(path);
        long len = -1;
        if (f.exists() && f.isFile()){
            len = f.length();
        }else{
            Log.d(TAG,"file doesn't exist or is not a file");
        }
        return len;
    }

    public static long calculateCRC32(String filePath){
        long val = 0;
        CRC32 crc32 = new CRC32();
        FileInputStream fileInputStream = null;
        try {
            File file = new File(filePath);
            fileInputStream = new FileInputStream(file);
            byte []buffer = new byte[8192];
            int length;
            while ((length = fileInputStream.read(buffer)) != -1){
                crc32.update(buffer,0,length);
            }
            val = crc32.getValue();
        } catch (FileNotFoundException e){
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
        return val;
    }

    public static long calculateCRC32(byte []buffer){
        long val = 0;
        CRC32 crc32 = new CRC32();
        crc32.update(buffer);
        val = crc32.getValue();
        return val;
    }

    public static String getAppVersionName(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            return pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getAppVersionCode(Context context) {
        PackageManager pm = context.getPackageManager();
        String versionName = "";
        try {
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            return Integer.toString(pi.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int crc16_calculate(byte []bytes){
        char i;
        int crc;
        crc = 0xffff;
        int len = bytes.length;
        int j = 0;
        while (len != 0){
            for(i=0x01; (i&0xff) != 0 ;i <<=1){
                if (0 != (crc & 0x8000)){
                    crc <<= 1;
                    crc = crc&0xffff;
                    crc ^= 0x1021;
                    crc = crc&0xffff;
                }else {
                    crc <<= 1;
                    crc = crc&0xffff;
                }
                if (0 != (bytes[j] & i & 0xff)){
                    crc ^= 0x1021;
                    crc = crc&0xffff;
                }
            }
            len--;
            j++;
        }
        crc = (crc>>8)+(crc<<8);
        return crc&0xffff;
    }

    // 两次点击按钮之间的点击间隔不能少于3000毫秒
    private static final int MIN_CLICK_DELAY_TIME = 3000;
    private static long lastClickTime;

    public static boolean isNoFastClick() {
        boolean flag = false;
        long curClickTime = System.currentTimeMillis();
        if ((curClickTime - lastClickTime) >= MIN_CLICK_DELAY_TIME) {
            flag = true;
            lastClickTime = curClickTime;
        }
        return flag;
    }
}
