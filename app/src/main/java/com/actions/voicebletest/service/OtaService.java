package com.actions.voicebletest.service;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.WindowManager;

import com.actions.voicebletest.VoiceBleTestApplication;
import com.actions.voicebletest.log.Log;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.polidea.rxandroidble2.scan.ScanSettings;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

import static com.polidea.rxandroidble2.scan.ScanCallbackType.CALLBACK_TYPE_ALL_MATCHES;

/**
 * desc   : 后台播放语音
 * author : wangshanhai
 * email  : ilikeshatang@gmail.com
 * date   : 2017/11/3 11:28
 */
public class OtaService extends Service {
    private static final String TAG = OtaService.class.getSimpleName();
    private RxBleClient rxBleClient;
    private Disposable scanDisposable;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
        rxBleClient = VoiceBleTestApplication.getRxBleClient(this);
        startOtaCheck();
    }


    private String startOtaCheck() {
//        String url_str = "https://github.com/lfkabc/Ble-controller/raw/master/";
        String url_str = "https://github.com/sjx2015/Ble-controller/raw/master/";
        String url_version = url_str + "version.txt";
        int HttpResult; // 服务器返回的状态
        String content = null;
        try {
            URL url = new URL(url_version); // 创建URL
            URLConnection urlconn = url.openConnection(); // 试图连接并取得返回状态码
            urlconn.connect();
            HttpURLConnection httpconn = (HttpURLConnection) urlconn;
            HttpResult = httpconn.getResponseCode();
            if (HttpResult != HttpURLConnection.HTTP_OK) {
                System.out.print("无法连接到");
            } else {
                InputStreamReader isReader = new InputStreamReader(urlconn.getInputStream(), "UTF-8");
                BufferedReader reader = new BufferedReader(isReader);
                StringBuffer buffer = new StringBuffer();
                content = reader.readLine();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String mFirmwareVersion = getFirmwareVersionFromController();
        if(mFirmwareVersion != null)
            Log.e(TAG,"mFirmwareVersion.substring(7)="+mFirmwareVersion.substring(7));
        if ( (content != null) && (mFirmwareVersion !=null) ){
            Log.d(TAG, "content=" + content);
            if (content.indexOf(mFirmwareVersion.substring(7)) == -1) //不匹配
                return content;
        }
        /*
        if (content != null) {
            String mFirmwareVersion = getFirmwareVersionFromController();
            int currentVersion = str2Version(mFirmwareVersion);
            int serverVersion = str2Version(content);
            Log.e(TAG, "getNewVersion() current:" + currentVersion + "  new:" + serverVersion);
            return currentVersion < serverVersion ? content : null;
            //lfk for debug
            //return content;
        }
    */
        return null;

    }

    private int str2Version(String str) {
        String temp = str.substring(5);
        temp = temp.replace("_", "");
        return Integer.parseInt(temp);
    }

    private String getFirmwareVersionFromController() {

        return null;
    }


    private void stopScan() {
        scanDisposable.dispose();
    }

    private void startScan() {
        final List<ScanResult> data = new ArrayList<>();
        ScanResult scanResult = null;
        int rssi = 0;
        BluetoothManager bluetoothManager = (BluetoothManager) VoiceBleTestApplication.getContext().getSystemService(Context.BLUETOOTH_SERVICE);
        List<BluetoothDevice> devices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
        Log.d(TAG, "devices size: " + devices.size());
        Log.d(TAG, "bond size: " + bluetoothManager.getAdapter().getBondedDevices().size());
        Set<RxBleDevice> rxBondDevices = rxBleClient.getBondedDevices();
        Set<BluetoothDevice> bonddevices = bluetoothManager.getAdapter().getBondedDevices();
        for (RxBleDevice device : rxBondDevices) {
            if (device.getBluetoothDevice().getType() == BluetoothDevice.DEVICE_TYPE_LE) {
                RxBleDevice bleDevice = device;
                Log.d(TAG, "bond ble: " + bleDevice.getName());
                scanResult = new ScanResult(bleDevice, rssi, System.currentTimeMillis(), CALLBACK_TYPE_ALL_MATCHES, null);
                //if (bleDevice.getName() != null && bleDevice.getName().contains(nameFilter)) {
                Log.d("xxx", "get mac: " + bleDevice.getMacAddress());
                if (bleDevice.getMacAddress().startsWith("F4:4E:FD")) {
                    data.add(scanResult);
                }
            }
        }
        for (BluetoothDevice device : devices) {
            if (device.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
                RxBleDevice bleDevice = rxBleClient.getBleDevice(device.getAddress());
                bleDevice.establishConnection(false)
                        .observeOn(AndroidSchedulers.mainThread())
                        .flatMap(rxBleConnection -> // Set desired interval.
                                Observable.interval(2, TimeUnit.SECONDS).flatMapSingle(sequence -> rxBleConnection.readRssi()))
                        .subscribe(new Consumer<Integer>() {
                                       @Override
                                       public void accept(Integer integer) throws Exception {
                                           ScanResult scanResult = new ScanResult(bleDevice, rssi, System.currentTimeMillis(), CALLBACK_TYPE_ALL_MATCHES, null);
                                           //if (bleDevice.getName() != null && bleDevice.getName().contains(nameFilter)) {
                                           Log.d("xxx", "get mac: " + bleDevice.getMacAddress());
                                           if (bleDevice.getMacAddress().startsWith("F4:4E:FD")) {
                                               data.add(scanResult);
                                           }
                                       }
                                   },
                                new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable throwable) throws Exception {
                                        Log.d(TAG, throwable.toString());
                                    }
                                }
                        );
                Log.d(TAG, "ble already connected！" + device.getName());
                scanResult = new ScanResult(bleDevice, rssi, System.currentTimeMillis(), CALLBACK_TYPE_ALL_MATCHES, null);
                //if (bleDevice.getName() != null && bleDevice.getName().contains(nameFilter)) {
                Log.d("xxx", "get mac: " + bleDevice.getMacAddress());
                if (bleDevice.getMacAddress().startsWith("F4:4E:FD")) {
                    data.add(scanResult);
                }
            }
        }
        scanDisposable = rxBleClient.scanBleDevices(
                new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                        .build(),
                new ScanFilter.Builder()
                        //.setDeviceAddress("B4:99:4C:34:DC:8B")
                        // add custom filters if needed
                        .build()
        )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<ScanResult>() {
                    @Override
                    public void accept(ScanResult scanResult) throws Exception {
                        String name = "";
                        if (scanResult != null && scanResult.getBleDevice() != null && scanResult.getBleDevice().getName() != null)
                            name = scanResult.getBleDevice().getName();
                        //if (name.contains(nameFilter)) {
                        Log.d("xxx", "get mac: " + scanResult.getBleDevice().getMacAddress());
                        if (scanResult.getBleDevice().getMacAddress().startsWith("F4:4E:FD")) {
                            data.add(scanResult);
                        }
                    }
                }, null);
        //.subscribe(resultsAdapter::addScanResult, this::onScanFailure);
    }


    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
        builder.setMessage("Hello,My name is Message.");
        builder.setNegativeButton("CANCEL", null);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        final Dialog dialog = builder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
