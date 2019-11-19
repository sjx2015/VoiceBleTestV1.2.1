package com.actions.voicebletest.scan;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.actions.voiceble.manager.VoiceBleManager;
import com.actions.voicebletest.R;
import com.actions.voicebletest.VoiceBleTestApplication;
import com.actions.voicebletest.main.MainActivity;
import com.actions.voicebletest.utils.SharedpreferencesProvider;
import com.actions.voicebletest.utils.Utils;
import com.example.zhouwei.library.CustomPopWindow;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.exceptions.BleScanException;
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.polidea.rxandroidble2.scan.ScanSettings;
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

import static com.polidea.rxandroidble2.scan.ScanCallbackType.CALLBACK_TYPE_ALL_MATCHES;
import static com.trello.rxlifecycle2.android.ActivityEvent.PAUSE;

public class ScanActivity extends RxAppCompatActivity {
    public static final String TAG = ScanActivity.class.getSimpleName();
    private static final int REQUEST_PERMISSION_CODE = 1;

    private int rssi = 0;
    private ScanResult scanResult = null;

    private List<String> needPermission;
    private String[] permissionArray = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.READ_PHONE_STATE,
    };

    @BindView(R.id.ble_name_filter)
    EditText bleNameFilter;
    @BindView(R.id.scan_results)
    RecyclerView recyclerView;
    private RxBleClient rxBleClient;
    private VoiceBleManager voiceBleManager;
    private Disposable scanDisposable;
    private ScanResultsAdapter resultsAdapter;
    private CustomPopWindow popWindow;
    private String nameFilter = "";

    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        ButterKnife.bind(this);
        //rxBleClient = VoiceBleTestApplication.getRxBleClient(this);

        rxBleClient = VoiceBleTestApplication.getRxBleClient(this);
        configureResultList();
        askMultiplePermission();
        nameFilter = SharedpreferencesProvider.getSharePerferences(SharedpreferencesProvider.BLE_NAME_FILTER);
        if (!nameFilter.isEmpty()){
            bleNameFilter.setText(nameFilter);
            bleNameFilter.setSelection(nameFilter.length());
        }
    }

    public void onScanToggleClick() {

        if (isScanning()) {
            stopScan();
        } else {
            startScan();
        }

        updateButtonUIState();
    }

    private void stopScan() {
        scanDisposable.dispose();
    }

    private void startScan() {
        resultsAdapter.clearScanResults();
        nameFilter = bleNameFilter.getText().toString().trim();
        SharedpreferencesProvider.saveSharePerferences(SharedpreferencesProvider.BLE_NAME_FILTER,nameFilter);
        BluetoothManager bluetoothManager = (BluetoothManager) VoiceBleTestApplication.getContext().getSystemService(Context.BLUETOOTH_SERVICE);
        List<BluetoothDevice> devices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
        Log.d(TAG, "devices size: " + devices.size());
        Log.d(TAG, "bond size: "+ bluetoothManager.getAdapter().getBondedDevices().size());
        Set<RxBleDevice> rxBondDevices = rxBleClient.getBondedDevices();
        Set<BluetoothDevice> bonddevices = bluetoothManager.getAdapter().getBondedDevices();
        //devices.addAll(bluetoothManager.getAdapter().getBondedDevices());
        for(RxBleDevice device:rxBondDevices){
            if(device.getBluetoothDevice().getType() == BluetoothDevice.DEVICE_TYPE_LE) {
                RxBleDevice bleDevice = device;
                Log.d(TAG, "bond ble: " + bleDevice.getName());
                scanResult = new ScanResult(bleDevice, rssi, System.currentTimeMillis(), CALLBACK_TYPE_ALL_MATCHES, null);
                if (bleDevice.getName() != null && bleDevice.getName().contains(nameFilter)) {
                    resultsAdapter.addScanResult(scanResult);
                }
            }
        }
        for(BluetoothDevice device : devices) {
            if(device.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
                RxBleDevice bleDevice = rxBleClient.getBleDevice(device.getAddress());
                rssi = 0;
                scanResult = null;
                bleDevice.establishConnection( false)
                        .compose(bindUntilEvent(PAUSE))
                        .observeOn(AndroidSchedulers.mainThread())
                        .flatMap(rxBleConnection -> // Set desired interval.
                                Observable.interval(2, TimeUnit.SECONDS).flatMapSingle(sequence -> rxBleConnection.readRssi()))
                        .subscribe(new Consumer<Integer>() {
                                       @Override
                                       public void accept(Integer integer) throws Exception {
                                           rssi = (int)integer;
                                           Log.d(TAG, "getRssi: " + rssi);
                                           scanResult = new ScanResult(bleDevice,rssi,System.currentTimeMillis(),CALLBACK_TYPE_ALL_MATCHES,null);
                                           if (bleDevice.getName() != null && bleDevice.getName().contains(nameFilter)) {
                                               resultsAdapter.addScanResult(scanResult);
                                           }
                                       }
                                   },
                                new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable throwable) throws Exception {
                                        Log.d(TAG,throwable.toString());
                                    }
                                }
                        );
                Log.d(TAG, "ble already connected！" + device.getName());
                scanResult = new ScanResult(bleDevice,rssi,System.currentTimeMillis(),CALLBACK_TYPE_ALL_MATCHES,null);
                if (bleDevice.getName() != null && bleDevice.getName().contains(nameFilter)) {
                    resultsAdapter.addScanResult(scanResult);
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
                .doFinally(this::dispose)
                .subscribe(new Consumer<ScanResult>() {
                    @Override
                    public void accept(ScanResult scanResult) throws Exception {
                        String name = "";
                        if (scanResult != null && scanResult.getBleDevice() !=null && scanResult.getBleDevice().getName() !=null)
                            name = scanResult.getBleDevice().getName();
                        if (name.contains(nameFilter)) {
                            resultsAdapter.addScanResult(scanResult);
                        }
                    }
                },this::onScanFailure);
                //.subscribe(resultsAdapter::addScanResult, this::onScanFailure);
    }

    private void configureResultList() {
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager recyclerLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(recyclerLayoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(ScanActivity.this, DividerItemDecoration.VERTICAL));
        resultsAdapter = new ScanResultsAdapter();
        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        recyclerView.setAdapter(resultsAdapter);
        ScanResultsAdapter.OnAdapterItemClickListener onAdapterItemClickListener = new ScanResultsAdapter.OnAdapterItemClickListener() {
            @Override
            public void onAdapterViewClick(View view) {
                final int childAdapterPosition = recyclerView.getChildAdapterPosition(view);
                final ScanResult itemAtPosition = resultsAdapter.getItemAtPosition(childAdapterPosition);
                onAdapterItemClick(itemAtPosition);
            }

            @Override
            public void onMenuAdapterViewClick(View view) {
                final int childAdapterPosition = recyclerView.getChildAdapterPosition(view);
                final ScanResult itemAtPosition = resultsAdapter.getItemAtPosition(childAdapterPosition);
                Log.d(TAG, "OnMenu clicked: position " + childAdapterPosition);
                View v = view.findViewById(R.id.more_option);
                View contentView = LayoutInflater.from(ScanActivity.this).inflate(R.layout.item_scan_menu_more, null);
                handleLogic(contentView, itemAtPosition);
                int windowPos[] = Utils.calculatePopWindowPos(v, contentView);
                popWindow = new CustomPopWindow.PopupWindowBuilder(ScanActivity.this)
                        .setView(contentView)//显示的布局，还可以通过设置一个View
                        //.size(600,400) //设置显示的大小，不设置就默认包裹内容
                        .setFocusable(true)//是否获取焦点，默认为ture
                        .setOutsideTouchable(true)//是否PopupWindow 以外触摸dissmiss
                        .create()//创建PopupWindow
                        .showAtLocation(view, Gravity.TOP | Gravity.START, windowPos[0], windowPos[1]);//显示PopupWindow
            }
        };
        resultsAdapter.setOnAdapterItemClickListener(onAdapterItemClickListener);
    }

    /**
     * 处理弹出显示内容、点击事件等逻辑
     *
     * @param contentView
     */
    private void handleLogic(View contentView, ScanResult itemAtPosition) {
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (popWindow != null) {
                    popWindow.dissmiss();
                }
                String showContent = "";
                switch (v.getId()) {
                    case R.id.auto_connect:
                        showContent = "cilck auto connect " + itemAtPosition.getBleDevice().getMacAddress();
                        connectBle(itemAtPosition, true);
                        break;
                    case R.id.bond:
                        createBond(itemAtPosition);
                        showContent = "click bond " + itemAtPosition.getBleDevice().getMacAddress();
                        break;
                    case R.id.bond_connect:
                        createBond(itemAtPosition);
                        connectBle(itemAtPosition, false);
                        showContent = "click bond and connect " + itemAtPosition.getBleDevice().getMacAddress();
                        break;
                }
                Log.d(TAG, showContent);
            }
        };
        contentView.findViewById(R.id.auto_connect).setOnClickListener(listener);
        TextView view = contentView.findViewById(R.id.bond);
        int bondState = itemAtPosition.getBleDevice().getBluetoothDevice().getBondState();
        if (bondState == BluetoothDevice.BOND_BONDED) {
            view.setText(R.string.delete_bond);
        } else if (bondState == BluetoothDevice.BOND_NONE) {
            view.setText(R.string.bond);
        } else {
            view.setText(R.string.bonding);
        }
        contentView.findViewById(R.id.bond_connect).setOnClickListener(listener);
        view.setOnClickListener(listener);
        resultsAdapter.notifyDataSetChanged();
    }

    private void connectBle(ScanResult scanResults, boolean autoConnect) {
        final String macAddress = scanResults.getBleDevice().getMacAddress();
        final String bleName = scanResults.getBleDevice().getName();
        final Intent intent = new Intent(ScanActivity.this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_MAC_ADDRESS, macAddress);
        intent.putExtra(MainActivity.EXTRA_BLE_NAME, bleName);
        intent.putExtra(MainActivity.EXTRA_AUTO_CONNECT, autoConnect);
        startActivity(intent);
    }

    @TargetApi(19)
    private void createBond(ScanResult itemAtPosition) {
        int bondsState = itemAtPosition.getBleDevice().getBluetoothDevice().getBondState();
        if (bondsState == BluetoothDevice.BOND_BONDED) {
            try {
                BluetoothDevice device = itemAtPosition.getBleDevice().getBluetoothDevice();
                Method m = device.getClass()
                        .getMethod("removeBond", (Class[]) null);
                m.invoke(device, (Object[]) null);
            } catch (Exception e) {

            }
            return;
        }
        if(bondsState == BluetoothDevice.BOND_NONE) {
        if (Build.VERSION.SDK_INT > 18) {
            BluetoothDevice device = itemAtPosition.getBleDevice().getBluetoothDevice();
            device.createBond();
            // }
        } else {
            try {
                BluetoothDevice device = itemAtPosition.getBleDevice().getBluetoothDevice();
                Method m = device.getClass()
                        .getMethod("createBond", (Class[]) null);
                m.invoke(device, (Object[]) null);
            } catch (Exception e) {

            }
        }
           // Toast.makeText(ScanActivity.this, "API at 18 does not support bond!", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isScanning() {
        return scanDisposable != null;
    }

    //@TargetApi(19)
    private void onAdapterItemClick(ScanResult scanResults) {
        connectBle(scanResults, false);
    }

    private void handleBleScanException(BleScanException bleScanException) {
        final String text;
        boolean log = false;

        switch (bleScanException.getReason()) {
            case BleScanException.BLUETOOTH_NOT_AVAILABLE:
                text = "Bluetooth is not available";
                break;
            case BleScanException.BLUETOOTH_DISABLED:
                text = "Enable bluetooth and try again";
                break;
            case BleScanException.LOCATION_PERMISSION_MISSING:
                text = "On Android 6.0 location permission is required. Implement Runtime Permissions";
                break;
            case BleScanException.LOCATION_SERVICES_DISABLED:
                text = "Location services needs to be enabled on Android 6.0";
                requestSettings();
                break;
            case BleScanException.SCAN_FAILED_ALREADY_STARTED:
                text = "Scan with the same filters is already started";
                break;
            case BleScanException.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                text = "Failed to register application for bluetooth scan";
                break;
            case BleScanException.SCAN_FAILED_FEATURE_UNSUPPORTED:
                text = "Scan with specified parameters is not supported";
                break;
            case BleScanException.SCAN_FAILED_INTERNAL_ERROR:
                text = "Scan failed due to internal error";
                break;
            case BleScanException.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES:
                text = "Scan cannot start due to limited hardware resources";
                break;
            case BleScanException.UNDOCUMENTED_SCAN_THROTTLE:
                text = String.format(
                        Locale.getDefault(),
                        "Android 7+ does not allow more scans. Try in %d seconds",
                        secondsTill(bleScanException.getRetryDateSuggestion())
                );
                break;
            case BleScanException.UNKNOWN_ERROR_CODE:
            case BleScanException.BLUETOOTH_CANNOT_START:
            default:
                text = "Unable to start scanning";
                break;
        }
        Log.w("EXCEPTION", text, bleScanException);
        if (log) {
            Log.d(TAG, text);
        } else {
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        }
    }

    private long secondsTill(Date retryDateSuggestion) {
        return TimeUnit.MILLISECONDS.toSeconds(retryDateSuggestion.getTime() - System.currentTimeMillis());
    }

    @Override
    public void onPause() {
        super.onPause();

        if (isScanning()) {
            /*
             * Stop scanning in onPause callback. You can use rxlifecycle for convenience. Examples are provided later.
             */
            scanDisposable.dispose();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.d(TAG, "onResume");

        if (resultsAdapter != null)
            resultsAdapter.clearScanResults();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        menu.clear();

        getMenuInflater().inflate(R.menu.menu_scan, menu);
        onScanToggleClick();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_scan) {
            onScanToggleClick();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void onScanFailure(Throwable throwable) {

        if (throwable instanceof BleScanException) {
            handleBleScanException((BleScanException) throwable);
        }
    }

    private void dispose() {
        scanDisposable = null;
        //resultsAdapter.clearScanResults();
        updateButtonUIState();
    }

    private void updateButtonUIState() {
        getSupportActionBar().setTitle(isScanning() ? R.string.scanning_ble : R.string.app_name);
        if (menu !=null && menu.findItem(R.id.action_scan) != null)
            menu.findItem(R.id.action_scan).setTitle(isScanning() ? R.string.stop_scan : R.string.start_scan);
    }

    private void requestSettings() {
        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(myIntent);
    }

    public void askMultiplePermission() {
        needPermission = new ArrayList<>();
        for (String permissionName :
                permissionArray) {
            if (!checkIsAskPermission(this, permissionName)) {
                needPermission.add(permissionName);
            }
        }

        if (needPermission.size() > 0) {
            //开始申请权限
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {
                    ActivityCompat.requestPermissions(this, needPermission.toArray(new String[needPermission.size()]), REQUEST_PERMISSION_CODE);
                }
            } else {
            //获取数据
        }

    }

    public  boolean checkIsAskPermission(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            return false;
        } else {
            return true;
        }

    }

    public  boolean checkIsAskPermissionState(Map<String, Integer> maps, String[] list) {
        for (int i = 0; i < list.length; i++) {
            if (maps.get(list[i]) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;

    }

    private void requestPermission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_PERMISSION_CODE);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_CODE: {
                Map<String, Integer> permissionMap = new HashMap<>();
                for (String name :
                        needPermission) {
                    permissionMap.put(name, PackageManager.PERMISSION_GRANTED);
                    Log.d(TAG, name + "permission was granted");
                    if(name.equals(Manifest.permission.ACCESS_COARSE_LOCATION)){
                        onScanToggleClick();
                    }
                }

                for (int i = 0; i < permissions.length; i++) {
                    permissionMap.put(permissions[i], grantResults[i]);
                }
                if (checkIsAskPermissionState(permissionMap, permissions)) {
                    //获取数据
                } else {
                    //提示权限获取不完成，可能有的功能不能使用
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}
