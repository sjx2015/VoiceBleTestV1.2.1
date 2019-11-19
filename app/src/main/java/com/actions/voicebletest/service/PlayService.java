package com.actions.voicebletest.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.io.IOException;

/**
 * desc   : 后台播放语音
 * author : wangshanhai
 * email  : ilikeshatang@gmail.com
 * date   : 2017/11/3 11:28
 */
public class PlayService extends Service implements MediaPlayer.OnCompletionListener {
    private static final String TAG = PlayService.class.getSimpleName();

    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PLAYING = 2;
    private static final int STATE_PAUSE = 3;
    private static final int STATE_PAUSE_LAST = 4;

    private MediaPlayer mPlayer = new MediaPlayer();

    private int mPlayState = STATE_IDLE;

    private String playUrl = "";

    public static boolean isPlaying = false;

    public void setListener(UpdateUiListener listener) {
        this.listener = listener;
    }

    private UpdateUiListener listener;

    public void setPosition(int position) {
        this.position = position;
    }

    private int position = -1;
    private int last_position = -1;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what){
                case STATE_PLAYING:
                    listener.startPlaying(position);
                    break;
                case STATE_PAUSE:
                    listener.finishPlaying(position);
                    break;
                case STATE_PAUSE_LAST:
                    listener.finishPlaying(last_position);
                    break;
            }
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
        mPlayer.setOnCompletionListener(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new PlayBinder();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("url")) {
            playUrl = intent.getStringExtra("url");
        }
        return START_NOT_STICKY;
    }


    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.e(TAG, "onCompletion 100%");
        mHandler.sendEmptyMessage(STATE_PAUSE);
        quit();
    }

    public void play(String url, int position) {
        this.last_position = this.position;
        if (last_position != -1){
            mHandler.sendEmptyMessage(STATE_PAUSE_LAST);
        }
        this.position = position;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (isPlaying) {
                    if (playUrl != null) {
                        mHandler.sendEmptyMessage(STATE_PAUSE);
                    }
                }
                try {
                    mPlayer.reset();
                    mPlayer.setDataSource(url);
                    mPlayer.prepareAsync();
                    mPlayState = STATE_PREPARING;
                    mPlayer.setOnPreparedListener(mPreparedListener);
                    mPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
                    isPlaying = true;
                    playUrl = url;
                    mHandler.sendEmptyMessage(STATE_PLAYING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            if (isPreparing()) {
                start();
            }
        }
    };

    private MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            Log.e("onBufferingUpdate--->", percent + "%");
        }
    };


    void start() {
        if (!isPreparing() && !isPausing()) {
            return;
        }

        mPlayer.start();
        mPlayState = STATE_PLAYING;


    }

    public void stopPlaying() {
        // stop play voice
        if (mPlayer != null) {
            mPlayer.stop();
        }
    }


    public void stop() {
        if (isIdle()) {
            return;
        }

        mPlayer.reset();
        mPlayState = STATE_IDLE;
    }


    public boolean isPlaying() {
        return mPlayState == STATE_PLAYING;
    }

    public boolean isPausing() {
        return mPlayState == STATE_PAUSE;
    }

    public boolean isPreparing() {
        return mPlayState == STATE_PREPARING;
    }

    public boolean isIdle() {
        return mPlayState == STATE_IDLE;
    }


    @Override
    public void onDestroy() {
        if (mPlayer != null) {
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
        }
        super.onDestroy();
        Log.i(TAG, "onDestroy: " + getClass().getSimpleName());
    }

    public void quit() {
        stop();
        stopSelf();
    }

    public class PlayBinder extends Binder {
        public PlayService getService() {
            return PlayService.this;
        }
    }
}
