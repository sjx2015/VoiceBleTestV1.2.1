package com.actions.voicebletest;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.internal.RxBleLog;

import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Created by chang on 2018/3/21.
 */

public class VoiceBleTestApplication extends Application{
    public static final String TAG = VoiceBleTestApplication.class.getSimpleName();
    private RxBleClient rxBleClient;
    private static Context mContext;


    public static RxBleClient getRxBleClient(Context context) {
        VoiceBleTestApplication application = (VoiceBleTestApplication) context.getApplicationContext();
        return application.rxBleClient;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        rxBleClient = RxBleClient.create(this);
        RxBleClient.setLogLevel(RxBleLog.VERBOSE);
    }

    public static Context getContext(){
        return mContext;
    }

    /**
     * RxJava2 当取消订阅后(dispose())，RxJava抛出的异常后续无法接收(此时后台线程仍在跑，可能会抛出IO等异常),全部由RxJavaPlugin接收，需要提前设置ErrorHandler
     * 详情：http://engineering.rallyhealth.com/mobile/rxjava/reactive/2017/03/15/migrating-to-rxjava-2.html#Error Handling
     */
    private void setRxJavaErrorHandler() {
        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Log.d(TAG, "RxJavaErrorHandler: " + throwable.toString());
            }
        });
    }

}
