package com.actions.voicebletest.log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.util.Log;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by Rooster on 2015/12/18.
 */
public class LogcatManager {
    private static final String TAG = LogcatManager.class.getSimpleName();
    private static LogcatManager ourInstance = null;

    private Context mContext;
    private LogcatThread mLogcatThread;
    private File mCacheDir = null;
    private String mLogcatFile = null;
    private boolean isStorageBusy = false;
    private boolean enable = false;
    private LogcatManager mLogcatManager;

    public static LogcatManager getInstance(Context context) {
        if (ourInstance == null) {
            synchronized (LogcatManager.class) {
                if (ourInstance == null) {
                    ourInstance = new LogcatManager(context);
                }
            }
        }
        return ourInstance;
    }

    private LogcatManager(Context context) {
        mContext = context;
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addDataScheme("file");
        Log.d(TAG, "registerReceiver");
        mContext.registerReceiver(mBroadcastReceiver, filter);
    }

    private void createThread() {
        mLogcatThread = new LogcatThread();
        mLogcatThread.setLogFilePath(mLogcatFile);
    }

    public void startLogcat() {
        if (enable) {
            createThread();
            if (mLogcatThread != null && !isStorageBusy) {
                mLogcatThread.start();
            }
        }
    }

    private boolean hasDone() {
        if (mLogcatThread != null) {
            return mLogcatThread.getThreadState() == LogcatThread.STATE_DONE;
        }
        return false;
    }

    public void onResume() {
        if (enable && hasDone() && !isStorageBusy) {
            mLogcatThread = new LogcatThread();
            mLogcatThread.setLogFilePath(mLogcatFile);
            mLogcatThread.setAppend();
            mLogcatThread.start();
        }
    }

    public void onPause() {
        if (enable && mLogcatThread != null && mLogcatThread.isAlive()) {
            mLogcatThread.setState(LogcatThread.STATE_DONE);
        }
    }

    public void init() {
        registerReceiver();

        if (!enable) {
            return;
        }
        String packageName = mContext.getPackageName();
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        if (!externalStorageDirectory.canWrite()) {
            String[] paths;
            String extSdCard = null;
            try {
                StorageManager sm = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
                paths = (String[]) sm.getClass().getMethod("getVolumePaths", new Class[0]).invoke(sm, new Object[0]);
                String esd = Environment.getExternalStorageDirectory().getPath();
                for (int i = 0; i < paths.length; i++) {
                    if (paths[i].equals(esd)) {
                        continue;
                    }
                    File sdFile = new File(paths[i]);
                    if (sdFile.canWrite()) {
                        extSdCard = paths[i];
                        Log.i(TAG, "extsdcard:" + extSdCard);
                    }
                }
                mLogcatFile = extSdCard + "/" + packageName + "/" + packageName + ".log";
                mCacheDir = new File(extSdCard + "/" + packageName);
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                mLogcatFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + packageName + "/"
                        + packageName + ".log";
                mCacheDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + packageName);
            }
        } else {
            mLogcatFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + packageName + "/"
                    + packageName + ".log";
            mCacheDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + packageName);
        }

        Log.i(TAG, "data:" + mLogcatFile);

        if (mCacheDir != null && !mCacheDir.isDirectory()) {
            mCacheDir.mkdir();
        }

        if (mCacheDir != null && mCacheDir.exists() && mCacheDir.canRead() && mCacheDir.canWrite()) {
            isStorageBusy = false;
        } else {
            isStorageBusy = true;
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String actions = intent.getAction();
            if (actions.equals(Intent.ACTION_MEDIA_EJECT)) {
                if (mLogcatThread != null && mLogcatThread.isAlive()) {
                    mLogcatThread.setState(LogcatThread.STATE_DONE);
                }
                isStorageBusy = true;
            } else if (actions.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                isStorageBusy = false;
            }
        }
    };

    public void release() {
        if (mLogcatThread != null && mLogcatThread.getThreadState() != LogcatThread.STATE_DONE) {
            mLogcatThread.setState(LogcatThread.STATE_DONE);
        }
        Log.d(TAG, "unregisterReceiver");
        mContext.unregisterReceiver(mBroadcastReceiver);
        ourInstance = null;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getCacheDir(){
        return mCacheDir.getAbsolutePath();
    }
}
