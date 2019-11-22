package com.actions.voicebletest.main;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actions.voicebletest.BleService.ActionsTransmissionService;
import com.actions.voicebletest.R;
import com.actions.voicebletest.VoiceBleTestApplication;
import com.actions.voicebletest.bean.KeyMapping;
import com.actions.voicebletest.db.MessageReaderContract;
import com.actions.voicebletest.db.MessageReaderContract.MessageEntry;
import com.actions.voicebletest.db.MessageReaderDbHelper;
import com.actions.voicebletest.fragment.OtaFragment;
import com.actions.voicebletest.fragment.VoiceTestFragment;
import com.actions.voicebletest.jni.DecodeJni;
import com.actions.voicebletest.log.LogcatManager;
import com.actions.voicebletest.utils.HexString;
import com.actions.voicebletest.utils.Utils;
import com.actions.voicebletest.log.Log;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.folderselector.FileChooserDialog;
import com.andreabaccega.formedittextvalidator.Validator;
import com.andreabaccega.widget.FormEditText;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import org.angmarch.views.NiceSpinner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.mateware.snacky.Snacky;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

import static com.trello.rxlifecycle2.android.ActivityEvent.DESTROY;

/**
 * Created by chang on 2018/3/21.
 */

public class MainActivity extends RxAppCompatActivity implements FileChooserDialog.FileCallback {
    public static final String TAG = MainActivity.class.getSimpleName();
    public static final String EXTRA_MAC_ADDRESS = "extra_mac_address";
    public static final String EXTRA_BLE_NAME = "extra_ble_name";
    public static final String EXTRA_AUTO_CONNECT = "extra_auto_connect";
    private static final int FRAGMENT_VOICE_TEST = 1;
    private static final int FRAGMENT_OTA = 0;

    private static final int CONNECTED = 100;
    private static final int CONNECTTING = 101;
    private static final int DISCONNECTED = 102;
    private static final int DISCONNECTTING = 103;
    private static final int RECONNECT = 104;
    private static final int ONNOTIFY = 105;
    private static final int SHOW_LOG = 106;

    private boolean firstConnect = true;

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.frame_container)
    FrameLayout frameLayout;

    private Drawer result = null;
    private Snacky.Builder snackyBuilder = null;
    private Menu menu;

    private String macAddress;
    private String bleName;
    private boolean autoConnect;

    private UUID characteristicUuidControl, characteristicUuidReceive;
    private RxBleDevice bleDevice;

    MessageReaderDbHelper mDbHelper = null;
    private LogcatManager mLogcatManager = null;
    private static final boolean DEBUG = true;

    private Disposable connectionDisposable;
    private RxBleConnection rxBleConnection;

    //private DecodeJni mDecodeJni = new DecodeJni();
    private short mDecodeAlgrithm = 0;//解码算法种类
    private Lock mLock = new ReentrantLock();
    private File mFile;
    private File mFileEncode;
    private String mPath;
    private String mPathEncode;
    private boolean mNotificationReceive = false;
    private boolean mNotificationControl = false;
    private long mBytesWrite = 0;
    private boolean mDecode40Bytes = false;
    private byte[] mBytesReceived = new byte[40];
    private short mBitstreamlen = 0;

    private MaterialDialog mDialogAudioSetting = null;
    private NiceSpinner mSpinnerFrequent = null;
    private MaterialDialog mDialogMtuSetting = null;
    private MaterialDialog mDialogClearData = null;
    private MaterialDialog mDialogWaiting = null;
    private MaterialDialog mDialogShowText = null;
    private MaterialDialog mDialogShowLog = null;
    private TextView mTextRecognition = null;
    private ProgressBar mProgressBarDialog = null;
    private FormEditText mMtuEditText = null;
    private EditText mLogTextView = null;
    private String mLogContent = "";
    private int mFrequent = 16000;
    private boolean mInit = false;
    private int num = 0;
    private long start = 0;
    private int mConnectLostCount = 0;
    private IDrawerItem mOtaDrawerItem = null;

    private KeyMapping mKeyMap = new KeyMapping();

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case CONNECTED:
                    showConnectSuccess();
                    //result.setSelection(mOtaDrawerItem);
                    break;
                case CONNECTTING:
                    if (firstConnect) {
                        showConnecting();
                        firstConnect = false;
                    } else {
                        showReConnecting();
                    }
                    break;
                case DISCONNECTED:
                    showDisconnected();
                    break;
                case RECONNECT:
                    onReConnect();
                    break;
                case ONNOTIFY:
                    onNotify();
                    break;
                case SHOW_LOG:
                    if (mLogTextView != null) {
                        mLogTextView.setText(mLogContent);
                        mLogTextView.setSelection(mLogTextView.getText().length());
                    }
                    break;
            }
        }
    };

    private Runnable connectTask = new Runnable() {
        @Override
        public void run() {
            //Log.d(TAG, "connectTask running: " );
            mHandler.sendEmptyMessage(RECONNECT);
            mHandler.postDelayed(this, 3000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mDbHelper = new MessageReaderDbHelper(MainActivity.this);

        Intent intent = getIntent();
        macAddress = intent.getStringExtra(EXTRA_MAC_ADDRESS);
        bleName = intent.getStringExtra(EXTRA_BLE_NAME);
        autoConnect = intent.getBooleanExtra(EXTRA_AUTO_CONNECT, false);

        characteristicUuidControl = UUID.fromString(ActionsTransmissionService.UUID_CONTROL);
        characteristicUuidReceive = UUID.fromString(ActionsTransmissionService.UUID_RECEIVE_DATA);

        frameLayout.setVisibility(View.GONE);

        // Handle Toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("   " + bleName);
        getSupportActionBar().setSubtitle("   " + getString(R.string.mac_address, macAddress));

        OtaFragment.getInstance().restState();
        //Create the drawer
/*        mOtaDrawerItem = new PrimaryDrawerItem().withName(R.string.ota).withIcon(getResources().getDrawable(R.mipmap.ic_file_upload_black_48dp));
        result = new DrawerBuilder(this)
                //this layout have to contain child layouts
                .withRootView(R.id.drawer_container)
                .withToolbar(toolbar)
                .withDisplayBelowStatusBar(false)
                .withActionBarDrawerToggleAnimated(true)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName(R.string.voice).withIcon(getResources().getDrawable(R.mipmap.ic_mic_black_48dp)),
                        mOtaDrawerItem
                        //new SectionDrawerItem().withName(R.string.drawer_item_section_header),
                        //new SecondaryDrawerItem().withName(R.string.drawer_item_settings).withIcon(FontAwesome.Icon.faw_cog),
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        // do something with the clicked item :D

                        Fragment f = null;
                        switch (position) {
                            case FRAGMENT_VOICE_TEST:
                                f = VoiceTestFragment.getInstance();
                                getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, f, VoiceTestFragment.class.getSimpleName()).commit();
                                break;
                            case FRAGMENT_OTA:
                                f = OtaFragment.getInstance();
                                getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, f, OtaFragment.class.getSimpleName()).commit();
                                break;
                        }
                        result.closeDrawer();
                        return true;
                    }
                })
                .withSavedInstance(savedInstanceState)
                .build();

 */


        if (savedInstanceState == null) {
            Fragment f = OtaFragment.getInstance();
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, f, OtaFragment.class.getSimpleName()).commit();
        }

        //if (!checkStoragePermission()) {
        //    exitWithoutWriteStoragePermission();
        //} else {
            initLogcatRecord();
            makePcmDirectory();
            //discoverServices();
            //onConnect();
            //test();
            bleDevice = VoiceBleTestApplication.getRxBleClient(this).getBleDevice(macAddress);
            bleDevice.observeConnectionStateChanges()
                    .compose(bindUntilEvent(DESTROY))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onConnectionStateChange);
            showIniting();
            mHandler.postDelayed(connectTask, 3000);
       // }
        Log.d(TAG, "bleConnect state: " + bleDevice.getConnectionState().toString());
        setErrorHandler();
        //mDecodeJni.Init();
        //test(null);//testt
    }

    public void onActionButtonClick(View v) {
        showLogDialog();
        Log.d(TAG, "onActionButtonClick: ");
    }


    private void setErrorHandler() {
        RxJavaPlugins.setErrorHandler((Throwable e) -> {
            if (e instanceof UndeliverableException) {
                e = e.getCause();
            }
            if ((e instanceof IOException) || (e instanceof SocketException)) {
                // fine, irrelevant network problem or API that throws on cancellation
                return;
            }
            if (e instanceof InterruptedException) {
                // fine, some blocking code was interrupted by a dispose call
                return;
            }
            if ((e instanceof NullPointerException) || (e instanceof IllegalArgumentException)) {
                // that's likely a bug in the application
                //Thread.currentThread().getUncaughtExceptionHandler().handleException(Thread.currentThread(), e);
                return;
            }
            if (e instanceof IllegalStateException) {
                // that's a bug in RxJava or in a custom operator
                //Thread.currentThread().getUncaughtExceptionHandler().handleException(Thread.currentThread(), e);
                return;
            }
            Log.d(TAG, "setErrorHandler: " + e);
            setLogText(e.toString());
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        menu.clear();

        getMenuInflater().inflate(R.menu.menu_audio_setting, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.audio_setting) {
            showAudioSettingialog();
            return true;
        } else if (id == R.id.mtu) {
            showMtuSetting();
            return true;
        } else if (id == R.id.clear) {
            showClearDataDialog();
            return true;
        }else if (id == R.id.reconnect) {
            mHandler.sendEmptyMessage(RECONNECT);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

 */

    public class CiaoValidator extends Validator {

        public CiaoValidator() {
            super("mtu range at 23 - 517");
        }

        public boolean isValid(EditText et) {
            int val = Integer.valueOf(et.getText().toString());
            return val >= 23 && val <= 517;
        }

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public boolean setMtu(int mtu) {
        Log.d(TAG, "setMtu" + mtu);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mtu >= 23) {
                if (rxBleConnection != null) {
                    rxBleConnection.requestMtu(mtu)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(AndroidSchedulers.mainThread())
                            .subscribe(this::onMtuReceived, this::onConnectionFailure);
                    ;
                    return true;
                }
            }
        }
        return false;
    }

    private void onMtuReceived(Integer mtu) {
        //noinspection ConstantConditions
        Log.d(TAG, "MTU received: " + mtu);
        setLogText("MTU received: " + mtu);
        Toast.makeText(MainActivity.this, "Set MTU Success: " + mtu, Toast.LENGTH_SHORT).show();
    }

    private void showMtuSetting() {
        if (mDialogMtuSetting == null) {
            mDialogMtuSetting = new MaterialDialog.Builder(this)
                    .title(R.string.mtu)
                    .customView(R.layout.dialog_mtu_setting, true)
                    .cancelable(false)
                    .build();
            mMtuEditText = mDialogMtuSetting.getCustomView().findViewById(R.id.ed_mtu);
            mMtuEditText.addValidator(new CiaoValidator());
            if (rxBleConnection != null) {
                mMtuEditText.setText(rxBleConnection.getMtu() + "");
                mMtuEditText.setSelection((rxBleConnection.getMtu() + "").length());
            }
            mDialogMtuSetting.getCustomView().findViewById(R.id.mtu_ok).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mMtuEditText.testValidity()) {
                        setMtu(Integer.valueOf(mMtuEditText.getText().toString()));
                        mDialogMtuSetting.dismiss();
                    }
                }
            });

        }
        mDialogMtuSetting.show();
    }

    public void showClearDataDialog() {
        if (mDialogClearData == null) {
            mDialogClearData = new MaterialDialog.Builder(this)
                    .title(R.string.clear_title)
                    .content(R.string.clear_content, true)
                    .positiveText(R.string.ok)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            ClearData();
                        }
                    })
                    .negativeText(R.string.cancel)
                    .show();
        }
        mDialogClearData.show();
    }

    private void showAudioSettingialog() {
        if (mDialogAudioSetting == null) {
            mDialogAudioSetting = new MaterialDialog.Builder(this)
                    .title(R.string.audio_setting)
                    .customView(R.layout.dialog_audio_setting, true)
                    .negativeText(R.string.ok)
                    .cancelable(false)
                    .build();
            mSpinnerFrequent = mDialogAudioSetting.getCustomView().findViewById(R.id.nice_spinner_frequent);
            List<String> dataset = new LinkedList<>(Arrays.asList("16k", "8k"));
            mSpinnerFrequent.attachDataSource(dataset);
            mSpinnerFrequent.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String item = dataset.get(position);
                    Log.d(TAG, "onItemSelected: " + item);
                    Intent intent = new Intent();
                    intent.setAction(VoiceTestFragment.BROADCAST_RECEIVE);
                    if (item.equals("16k")) {
                        mFrequent = 16000;
                    } else {
                        mFrequent = 8000;
                    }
                    intent.putExtra("frequent", mFrequent);
                    intent.putExtra("notify", "showAudioSetting");
                    sendBroadcast(intent);


                } // to close the onItemSelected

                public void onNothingSelected(AdapterView<?> parent) {
                    Log.d(TAG, "onNothingSelected");
                }
            });
        }
        mDialogAudioSetting.show();
    }

    public void showWaitingDialog(int title, int text) {
        if (mDialogWaiting == null) {
            mDialogWaiting = new MaterialDialog.Builder(MainActivity.this)
                    .title(title)
                    .content(text)
                    .progress(true, 0)
                    .progressIndeterminateStyle(true)
                    .cancelable(false)
                    .show();
        }
        mDialogWaiting.show();
        Log.d(TAG, "showWaitingDialog");
    }

    public void dismissWaitingDialog() {
        if (mDialogWaiting != null) {
            mDialogWaiting.dismiss();
            Log.d(TAG, "dismissWaitingDialog");
        }
    }

    public void showTextDialog() {
        if (mDialogShowText == null) {
            mDialogShowText = new MaterialDialog.Builder(MainActivity.this)
                    .title(R.string.recog)
                    .customView(R.layout.dialog_show_text, true)
                    .negativeText(R.string.ok)
                    .cancelable(false)
                    .build();
            mTextRecognition = mDialogShowText.getCustomView().findViewById(R.id.tv_recognition);
            mProgressBarDialog = mDialogShowText.getCustomView().findViewById(R.id.progress_recognition);
        }
        if (mTextRecognition != null)
            mTextRecognition.setText(R.string.recognizing);
        if (mProgressBarDialog != null)
            mProgressBarDialog.setVisibility(View.VISIBLE);
        mDialogShowText.show();
    }

    public void setRecognitionText(String text) {
        mProgressBarDialog.setVisibility(View.GONE);
        mTextRecognition.setText(text);
    }

    public void showLogDialog() {
        if (mDialogShowLog == null) {
            mDialogShowLog = new MaterialDialog.Builder(MainActivity.this)
                    .title(R.string.log)
                    .customView(R.layout.dialog_show_log, true)
                    .negativeText(R.string.ok)
                    .cancelable(false)
                    .build();
            mLogTextView = mDialogShowLog.getCustomView().findViewById(R.id.tv_log);
        }
        if (mLogTextView != null) {
            mLogTextView.setText(mLogContent);
            mLogTextView.setSelection(mLogContent.length());
        }
        mDialogShowLog.show();
    }

    public void hideDialogLog(){
        if (mDialogShowLog != null)
            mDialogShowLog.dismiss();
    }

    public void setLogText(String logText){
        if (mLogContent.length() >= 1*1024*1024 )
            mLogContent = "";
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        String date = df.format(new Date())+ "  ";// new Date()为获取当前系统时间
        mLogContent += date + logText + "\n";
        //mHandler.sendEmptyMessage(SHOW_LOG);
    }

    private void onConnectionReceived(RxBleConnection connection) {
        //noinspection ConstantConditions
        setLogText("device connected");
        this.rxBleConnection = connection;
        OtaFragment.getInstance().setRxBleConnection(rxBleConnection);
        updateUI(RxBleConnection.RxBleConnectionState.CONNECTED);
        mHandler.sendEmptyMessage(ONNOTIFY);
    }

    private void updateUI() {
        if (isConnected()) {
            updateUI(RxBleConnection.RxBleConnectionState.CONNECTED);
        } else {
            updateUI(RxBleConnection.RxBleConnectionState.DISCONNECTED);
        }
    }

    private void onConnectionStateChange(RxBleConnection.RxBleConnectionState newState) {

        updateUI(newState);
    }

    private void onConnect() {
        if (isConnected()) {
            //triggerDisconnect();
            updateUI(RxBleConnection.RxBleConnectionState.CONNECTED);
        } else {
            connectionDisposable = bleDevice.establishConnection(autoConnect)
                    .compose(bindUntilEvent(DESTROY))
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(this::dispose)
                    .subscribe(this::onConnectionReceived, this::onConnectionFailureLost);
        }
    }

    private void dispose() {
        connectionDisposable = null;
        updateUI();
    }

    private void triggerDisconnect() {
        if (connectionDisposable != null) {
            connectionDisposable.dispose();
        }
    }

    private void onReConnect() {
        //Log.d(TAG, "bleDevice.getConnectionState()：" + bleDevice.getConnectionState());
        if (bleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.DISCONNECTED) {
            onConnect();
        }
    }

    public void onNotify() {

        if (isConnected()) {
            rxBleConnection
                    .setupNotification(characteristicUuidReceive)
                    .doOnNext(notificationObservable -> runOnUiThread(this::notificationHasBeenSetUp))
                    .flatMap(notificationObservable -> notificationObservable)
                    .observeOn(Schedulers.io())
                    .subscribe(this::onNotificationReceived, this::onNotificationSetupFailure);

            rxBleConnection
                    .setupNotification(characteristicUuidControl)
                    .doOnNext(notificationObservable -> runOnUiThread(this::notificationHasBeenSetUpControl))
                    .flatMap(notificationObservable -> notificationObservable)
                    .observeOn(AndroidSchedulers.mainThread())
                    /*.retryWhen(errors -> errors.flatMap(error -> {
                                if (error instanceof BleDisconnectedException ){
                                    Log.d("Retry", "Retrying");
                                    return Observable.just(new Object());
                                } else {
                                    return Observable.error(error);
                                }
                            }
                    ))*/
                    .subscribe(this::onNotificationReceivedControl, this::onNotificationSetupFailure);
        }
    }

    public boolean isConnected() {
        return bleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
    }

    private void onConnectionFailure(Throwable throwable) {
        //noinspection ConstantConditions
        Log.d(TAG, "Connection error: " + throwable);
        setLogText(throwable.toString());
        updateUI(RxBleConnection.RxBleConnectionState.DISCONNECTED);
        frameLayout.setVisibility(View.GONE);
        if (mPath != null && mBytesWrite != 0) {
            addPcmMessage();
        }
    }

    private void onConnectionFailureLost(Throwable throwable) {
        //noinspection ConstantConditions
        mConnectLostCount++;
        Log.d(TAG, "onConnectionFailureLost error: " + throwable);
        setLogText(throwable.toString());
        updateUI(RxBleConnection.RxBleConnectionState.DISCONNECTED);
        frameLayout.setVisibility(View.GONE);
        if (mPath != null && mBytesWrite != 0) {
            addPcmMessage();
        }
        if (mConnectLostCount >= 5){
            int bondsState = bleDevice.getBluetoothDevice().getBondState();
            if (bondsState == BluetoothDevice.BOND_BONDED) {
                try {
                    BluetoothDevice device = bleDevice.getBluetoothDevice();
                    Method m = device.getClass()
                            .getMethod("removeBond", (Class[]) null);
                    m.invoke(device, (Object[]) null);
                    setLogText("mConnectLostCount > 5, removeBond!");
                    mConnectLostCount = 0;
                } catch (Exception e) {

                }
                return;
            }
        }
    }

    private void updateUI(RxBleConnection.RxBleConnectionState newState) {
        if (newState == RxBleConnection.RxBleConnectionState.CONNECTED) {
            mHandler.sendEmptyMessage(CONNECTED);
            frameLayout.setVisibility(View.VISIBLE);
        } else if (newState == RxBleConnection.RxBleConnectionState.CONNECTING) {
            mHandler.sendEmptyMessage(CONNECTTING);
        } else if (newState == RxBleConnection.RxBleConnectionState.DISCONNECTED) {
            mHandler.sendEmptyMessage(DISCONNECTED);
            reset();
        } else if (newState == RxBleConnection.RxBleConnectionState.DISCONNECTING) {
            mHandler.sendEmptyMessage(DISCONNECTTING);
        }
    }

    private void showConnectSuccess() {
        if (snackyBuilder == null) {
            snackyBuilder = Snacky.builder();
        }
        snackyBuilder
                .setActivity(MainActivity.this)
                .setText(getString(R.string.connect_success))
                .setDuration(Snacky.LENGTH_SHORT)
                .info()
                .show();
        //sendBroadcastInit();
    }

    private void showIniting() {
        if (snackyBuilder == null) {
            snackyBuilder = Snacky.builder();
        }
        snackyBuilder
                .setActivity(MainActivity.this)
                .setText(getString(R.string.init))
                .centerText()
                .setDuration(Snacky.LENGTH_INDEFINITE)
                .info()
                .show();
    }

    private void showConnecting() {
        if (snackyBuilder == null) {
            snackyBuilder = Snacky.builder();
        }
        snackyBuilder
                .setActivity(MainActivity.this)
                .setText(getString(R.string.connecting))
                .centerText()
                .setDuration(Snacky.LENGTH_INDEFINITE)
                .info()
                .show();
    }

    private void showReConnecting() {
        if (snackyBuilder == null) {
            snackyBuilder = Snacky.builder();
        }
        snackyBuilder
                .setActivity(MainActivity.this)
                .setText(getString(R.string.reconnecting))
                .centerText()
                .setDuration(Snacky.LENGTH_INDEFINITE)
                .info()
                .show();
    }

    private void showDisconnected() {
        if (snackyBuilder == null) {
            snackyBuilder = Snacky.builder();
        }
        snackyBuilder
                .setActivity(MainActivity.this)
                .setText(getString(R.string.disconnected))
                .centerText()
                .setDuration(Snacky.LENGTH_INDEFINITE)
                .info()
                .show();
        otaReset();
    }

    private void showNotificationError() {
        if (snackyBuilder == null) {
            snackyBuilder = Snacky.builder();
        }
        snackyBuilder
                .setActivity(MainActivity.this)
                .setText(getString(R.string.notification_error))
                .centerText()
                .setDuration(Snacky.LENGTH_INDEFINITE)
                .info()
                .show();
    }

    private void showFindingService() {
        if (snackyBuilder == null) {
            snackyBuilder = Snacky.builder();
        }
        snackyBuilder
                .setActivity(MainActivity.this)
                .setText(getString(R.string.service_founding))
                .centerText()
                .setDuration(Snacky.LENGTH_INDEFINITE)
                .info()
                .show();
    }

    private void showNotFindService() {
        if (snackyBuilder == null) {
            snackyBuilder = Snacky.builder();
        }
        snackyBuilder
                .setActivity(MainActivity.this)
                .setText(getString(R.string.service_not_found))
                .centerText()
                .setDuration(Snacky.LENGTH_INDEFINITE)
                .info()
                .show();
    }

    private void showDisconnecting() {
        if (snackyBuilder == null) {
            snackyBuilder = Snacky.builder();
        }
        snackyBuilder
                .setActivity(MainActivity.this)
                .setText(getString(R.string.disconnecting))
                .centerText()
                .setDuration(Snacky.LENGTH_INDEFINITE)
                .info()
                .show();
    }

    public void showOtaSuccessfully() {
        if (snackyBuilder == null) {
            snackyBuilder = Snacky.builder();
        }
        snackyBuilder
                .setActivity(MainActivity.this)
                .setText(getString(R.string.ota_success))
                .centerText()
                .setDuration(Snacky.LENGTH_INDEFINITE)
                .info()
                .show();
    }

    private void reset() {
        mNotificationReceive = false;
        mNotificationControl = false;
    }

    private void notificationHasBeenSetUp() {
        //noinspection ConstantConditions
        mNotificationReceive = true;
        notificationSuccess();
    }

    private void notificationHasBeenSetUpControl() {
        //noinspection ConstantConditions
        mNotificationControl = true;
        notificationSuccess();

    }

    private void notificationSuccess() {
        if (mNotificationReceive && mNotificationControl) {
            Toast.makeText(this, "Notifications has been set up", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Notifications has been set up");
            setLogText("Voice Notifications has been set up");
            frameLayout.setVisibility(View.VISIBLE);
            sendBroadcastInit();
        }
    }

    private void sendBroadcastInit() {
        Intent intent = new Intent();
        intent.setAction(VoiceTestFragment.BROADCAST_RECEIVE);
        intent.putExtra("notify", "init");
        sendBroadcast(intent);
    }

    public void makePcmDirectory() {
        if (mLogcatManager != null) {
            File folder = new File(mLogcatManager.getCacheDir() + File.separator + "pcm");
            if (!folder.exists()) {
                folder.mkdirs();
            }
        }
    }
/*
    private void test(byte[] encodeBytes) {
        mDecodeAlgrithm = 0;
        mPath = mLogcatManager.getCacheDir() + "/conference.bs.pcm";
        int bufferSize = 20; // 设置缓冲区大小
        byte buffer[] = new byte[bufferSize]; // 缓冲区字节数组
        String path = mLogcatManager.getCacheDir() + "/conference.bs";
        try {
            if (!mInit) {
                mDecodeAlgrithm = 5;
                mBitstreamlen = mDecodeJni.decodeInit(mDecodeAlgrithm);
                mInit = true;
                String CPU_ABI = android.os.Build.CPU_ABI;
                Log.d(TAG, "CPU_ABI = " + CPU_ABI);
                setLogText("CPU_ABI = " + CPU_ABI);
                Log.d(TAG, "mBitstreamlen = " + mBitstreamlen);
            }
            File f = new File(path);
            InputStream fis = new FileInputStream(f);
            BufferedInputStream bis = new BufferedInputStream(fis, bufferSize);
            long fileSize = f.length(); // 文件总字节数
            int haveRead = 0; // 已读取字节数
            int readSize = -1; // 记录每次实际读取字节数
            while (null != bis && (readSize = bis.read(buffer)) != -1 && (readSize == 2*mBitstreamlen) ) {
                haveRead += readSize;
                decodeTest(buffer);
                //Log.d(TAG,"已经复制： " + haveRead + " Byte 完成" + haveRead * 100 / fileSize + "% 单次读取：" + readSize + " Byte");
            }
            bis.close();
            addPcmMessage();
            mPath = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
*/
    private void getPath() {
        String time = String.valueOf(System.currentTimeMillis());
        mPath = mLogcatManager.getCacheDir() + "/pcm/" + time + ".pcm";
        //mPathEncode = mLogcatManager.getCacheDir() + "/pcm/" + time + ".en";
        mBytesWrite = 0;
    }

    public String getCacheDirPath() {
        return mLogcatManager.getCacheDir();
    }
/*
    private void decode(byte[] encodeBytes) {
        if (!verifyDecodeAlgrithm())
            return;
        if (!mInit) {
            mBitstreamlen = mDecodeJni.decodeInit(mDecodeAlgrithm);
            mInit = true;
            String CPU_ABI = android.os.Build.CPU_ABI;
            Log.d(TAG, "CPU_ABI = " + CPU_ABI);
            Log.d(TAG, "mInit: " + true + " mDecodeAlgrithm: " + mDecodeAlgrithm);
        }
        Log.d(TAG, "mBitstreamlen: " + mBitstreamlen + " mDecodeAlgrithm: " + mDecodeAlgrithm);

        String str = "";

        short[] shorts = new short[encodeBytes.length / 2];
        ByteBuffer.wrap(encodeBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
        short[] ret = mDecodeJni.Decode(shorts, mBitstreamlen, mDecodeAlgrithm);
        str = "";
        for (int i = 0; i < ret.length; i++) {
            str += Integer.toHexString(ret[i] & 0xffff) + " ";
        }
        Log.d(TAG, str);

        writeShortToFile(ret);
        mBytesWrite += ret.length;
    }

    private void decodeTest(byte[] encodeBytes) {
        //writeByteToFile(encodeBytes);
        Log.d(TAG, "mBitstreamlen: " + mBitstreamlen);
        if (mBitstreamlen*2 != encodeBytes.length)
            return;

        String str = "";

        Log.d(TAG, encodeBytes.length / 2 + "");
        short[] shorts = new short[encodeBytes.length / 2];
        ByteBuffer.wrap(encodeBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
        short[] ret = mDecodeJni.Decode(shorts, mBitstreamlen, mDecodeAlgrithm);
        str = "";
        for (int i = 0; i < ret.length; i++) {
            str += Integer.toHexString(ret[i] & 0xffff) + " ";
        }
        Log.d(TAG, str);

        writeShortToFile(ret);
        mBytesWrite += ret.length;
    }
*/
    private long calculatePcmDuration() {
        //数据量Byte=采样频率Hz×（采样位数/8）× 声道数× 时间s
        //mBytesWrite * 2 表示一个short两个字节
        Log.d(TAG, "mBytesWrite: " + mBytesWrite);
        return (long) (mBytesWrite * 2 / 1.0 / mFrequent / (16 / 8));
    }

    private void writeShortToFile(short[] shorts) {
        ByteBuffer myByteBuffer = ByteBuffer.allocate(shorts.length * 2);
        myByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        ShortBuffer myShortBuffer = myByteBuffer.asShortBuffer();
        myShortBuffer.put(shorts);
        //mPath = mLogcatManager.getCacheDir() + "/pcm/test.pcm";
        //Log.d(TAG, "mPath: " + mPath);
        if (mPath == null)
            return;
        mFile = new File(mPath);
        try {
            mFile.createNewFile();
            FileChannel oFile = new FileOutputStream(mFile, true).getChannel();
            oFile.write(myByteBuffer);
            oFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeByteToFile(byte[] bytes) {
        //mPath = mLogcatManager.getCacheDir() + "/pcm/test.pcm";
        //Log.d(TAG, "mPath: " + mPath);
        if (mPathEncode == null)
            return;
        mFileEncode = new File(mPathEncode);
        try {
            mFileEncode.createNewFile();
            FileOutputStream oFile = new FileOutputStream(mFileEncode, true);
            oFile.write(bytes);
            oFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onNotificationReceived(byte[] bytes) {
        //noinspection ConstantConditions
        Log.d(TAG, "Change: " + HexString.bytesToHex(bytes));
        num++;
        Log.d(TAG, "receive package num: " + num);
        mLock.lock();
        if (mDecodeAlgrithm == DecodeJni.ASC_I) {
            if (mDecode40Bytes) {
                for (int i = 0; i < 20; i++) {
                    mBytesReceived[i + 20] = bytes[i];
                }
                //decode(mBytesReceived);
                mDecode40Bytes = false;
            } else {
                for (int i = 0; i < 20; i++) {
                    mBytesReceived[i] = bytes[i];
                }
                mDecode40Bytes = true;
            }
        } else {
            //decode(bytes);
        }
        mLock.unlock();
    }

    private boolean verifyDecodeAlgrithm(){
        if (mDecodeAlgrithm == DecodeJni.ASC_III || mDecodeAlgrithm == DecodeJni.ASC_II || mDecodeAlgrithm == DecodeJni.ASC_IV || mDecodeAlgrithm == DecodeJni.ASC_I  || mDecodeAlgrithm == DecodeJni.ASC_V)
            return true;
        return false;
    }

    private void onNotificationReceivedControl(byte[] bytes) {
        //noinspection ConstantConditions
        mLock.lock();
        String command = HexString.bytesToHex(bytes);
        Log.d(TAG, "onNotificationReceivedControl: " + command);
        String val = mKeyMap.getKeyValue(command);
        if (val != null)
            addTextMessage(val);
        if (command.equals("0C0200")) {
            Log.d(TAG, "0C0200");//1
            num = 0;
            initDecode(DecodeJni.ASC_III);
            getPath();
        } else if (command.equals("0D0200")) {
            Log.d(TAG, "0D0200");//1, 收两次数据解码一次
            num = 0;
            initDecode(DecodeJni.ASC_I);
            mDecode40Bytes = false;
            getPath();
        } else if (command.equals("0E0200")) {
            Log.d(TAG, "0E0200");
            num = 0;
            initDecode(DecodeJni.ASC_II);
            getPath();
        }else if (command.equals("1E0200")) {
            Log.d(TAG, "1E0200");
            num = 0;
            initDecode(DecodeJni.ASC_V);
            getPath();
        } else if (command.equals("0F0200")) {
            Log.d(TAG, "0F0200");
            num = 0;
            start = System.currentTimeMillis();
            initDecode(DecodeJni.ASC_III);
            getPath();
        } else if (command.equals("100200")) {
            Log.d(TAG, "100200");
            num = 0;
            start = System.currentTimeMillis();
            initDecode(DecodeJni.ASC_IV);
            getPath();
        } else if (command.equals("CCCC00")) {
            Log.d(TAG, "CCCC00");
            if (mBytesWrite > 0 && mPath != null)
                addPcmMessage();
            mPath = null;
            Log.d(TAG, (System.currentTimeMillis() - start) * 1.0 / num + "ms");
        }
        mLock.unlock();

    }

    private void initDecode(int type) {
        mDecodeAlgrithm = (short)type;
        mInit = false;
    }

    private void addPcmMessage() {
        VoiceTestFragment fragment = (VoiceTestFragment)getSupportFragmentManager().findFragmentByTag(VoiceTestFragment.class.getSimpleName());
        long seconds = calculatePcmDuration();
        if (fragment != null && fragment.isVisible()) {
            Intent intent = new Intent();
            intent.setAction(VoiceTestFragment.BROADCAST_RECEIVE);
            intent.putExtra("notify", "addMessage");
            intent.putExtra("path", mPath);
            intent.putExtra("seconds", seconds);
            sendBroadcast(intent);
            getPath();
        } else {
            com.actions.voicebletest.bean.Message message = new com.actions.voicebletest.bean.Message(
                    com.actions.voicebletest.bean.Message.MSG_TYPE_VOICE,
                    com.actions.voicebletest.bean.Message.MSG_STATE_SUCCESS,
                    "Tom", "avatar", "Jerry", "avatar",
                    null,
                    false, false,
                    new Date(System.currentTimeMillis()));
            message.setVoicePath(mPath);
            message.setSeconds(seconds);
            insertValue(message);
        }
    }

    private void ClearData() {
        Intent intent = new Intent();
        intent.setAction(VoiceTestFragment.BROADCAST_RECEIVE);
        intent.putExtra("notify", "clearData");
        sendBroadcast(intent);
    }

    private void addTextMessage(String val) {
        VoiceTestFragment fragment = (VoiceTestFragment)getSupportFragmentManager().findFragmentByTag(VoiceTestFragment.class.getSimpleName());
        if (fragment != null && fragment.isVisible()) {
            Intent intent = new Intent();
            intent.setAction(VoiceTestFragment.BROADCAST_RECEIVE);
            intent.putExtra("notify", "addTextMessage");
            intent.putExtra("text", val);
            sendBroadcast(intent);
        } else {
            com.actions.voicebletest.bean.Message message = new com.actions.voicebletest.bean.Message(
                    com.actions.voicebletest.bean.Message.MSG_TYPE_TEXT,
                    com.actions.voicebletest.bean.Message.MSG_STATE_SUCCESS,
                    "Tom", "avatar", "Jerry", "avatar",
                    val,
                    false, false,
                    new Date(System.currentTimeMillis()));
            insertValue(message);
        }


    }

    private void onNotificationSetupFailure(Throwable throwable) {
        //noinspection ConstantConditions
        //Toast.makeText(this, "Notifications error", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Notifications error:" + throwable);
        setLogText("Notifications error: " + throwable.toString());
        //showNotificationError();
    }

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void exitWithoutWriteStoragePermission() {
        Log.d(TAG, "REQUEST_STORAGE_PERMISSION_CODE permission denied");
        final AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(MainActivity.this);
        normalDialog.setTitle("Tips");
        normalDialog.setMessage("Storage Permission denied!");
        normalDialog.setPositiveButton("Confirm",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //...To-do
                        finish();
                    }
                });
        normalDialog.setCancelable(false).show();
    }

    private void initLogcatRecord() {
        mLogcatManager = LogcatManager.getInstance(this);
        mLogcatManager.setEnable(DEBUG);
        mLogcatManager.init();
        mLogcatManager.startLogcat();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy");
        mHandler.removeCallbacks(connectTask);
        if (mLogcatManager != null) {
            mLogcatManager.release();
        }
    }

    private void test() {
        com.actions.voicebletest.bean.Message message2 = new com.actions.voicebletest.bean.Message(com.actions.voicebletest.bean.Message.MSG_TYPE_VOICE,
                com.actions.voicebletest.bean.Message.MSG_STATE_SUCCESS, "Tom", "avatar", "Jerry", "avatar",
                "http://static.oschina.net/uploads/space/2015/0611/103706_rpPc_1157342.png",
                false, false, new Date(
                System.currentTimeMillis()));
        message2.setVoicePath(LogcatManager.getInstance(this).getCacheDir() + "/luyin1.pcm");
        message2.setSeconds(35);
        //insertValue(message2);
        //readDataBase();
    }

    public void insertValue(com.actions.voicebletest.bean.Message message) {
        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(MessageReaderContract.MessageEntry.COLUMN_MSG_TYPE, message.getType());
        values.put(MessageReaderContract.MessageEntry.COLUMN_STATE, message.getState());
        values.put(MessageReaderContract.MessageEntry.COLUMN_MSG_TYPE, message.getType());
        values.put(MessageReaderContract.MessageEntry.COLUMN_STATE, message.getState());
        values.put(MessageReaderContract.MessageEntry.COLUMN_FROM_USER_NAME, message.getFromUserName());
        values.put(MessageReaderContract.MessageEntry.COLUMN_FROM_USER_AVATAR, message.getFromUserAvatar());
        values.put(MessageReaderContract.MessageEntry.COLUMN_TO_USER_NAME, message.getToUserName());
        values.put(MessageReaderContract.MessageEntry.COLUMN_TO_USER_AVATAR, message.getToUserAvatar());
        values.put(MessageReaderContract.MessageEntry.COLUMN_IS_SEND, message.getIsSend());
        values.put(MessageReaderContract.MessageEntry.COLUMN_SEND_SUCCESS, message.getSendSucces());
        Timestamp ts = new Timestamp(message.getTime().getTime());
        values.put(MessageReaderContract.MessageEntry.COLUMN_TIME, ts.getTime());
        values.put(MessageReaderContract.MessageEntry.COLUMN_SECONDS, message.getSeconds());
        values.put(MessageReaderContract.MessageEntry.COLUMN_VOICE_CONTENT, message.getVoicePath());
        values.put(MessageReaderContract.MessageEntry.COLUMN_CONTENT, message.getContent());

        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(MessageEntry.TABLE_NAME, null, values);
        Log.d(TAG, "insertValue: " + newRowId);
    }

    private void readDataBase() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

// Define a projection that specifies which columns from the database
// you will actually use after this query.
        String[] projection = {
                MessageEntry._ID,
                MessageEntry.COLUMN_MSG_TYPE,
                MessageEntry.COLUMN_STATE,
                MessageEntry.COLUMN_CONTENT,
                MessageEntry.COLUMN_TIME,
                MessageEntry.COLUMN_SECONDS,
                MessageEntry.COLUMN_VOICE_CONTENT
        };

// Filter results WHERE "title" = 'My Title'
        String selection = MessageEntry.COLUMN_IS_SEND + " = ?";
        String[] selectionArgs = {"0"};

// How you want the results sorted in the resulting Cursor
        String sortOrder =
                MessageEntry.COLUMN_TIME + " ASC";

        Cursor cursor = db.query(
                MessageEntry.TABLE_NAME,                     // The table to query
                projection,                               // The columns to return
                null,                                // The columns for the WHERE clause
                null,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );

        if (cursor.getCount() > 0) {
            Log.d(TAG, "readdatabase: getCount :" + cursor.getCount());
            cursor.moveToFirst();
            do {
                long itemId = cursor.getLong(
                        cursor.getColumnIndexOrThrow(MessageEntry.COLUMN_TIME)
                );
                Log.d(TAG, "readdatabase: id:" + itemId);
            } while (cursor.moveToNext());
        } else {
            Log.d(TAG, "readdatabase: no item");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //add the values which need to be saved from the drawer to the bundle
        outState = result.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    private long firstPressedTime;

    @Override
    public void onBackPressed() {
        //handle the back press :D close the drawer first and if the drawer is closed close the activity
        if (result != null && result.isDrawerOpen()) {
            result.closeDrawer();
        } else {
            if (System.currentTimeMillis() - firstPressedTime < 2000) {
                super.onBackPressed();
            } else {
                Toast.makeText(getBaseContext(), "Press again to exit!", Toast.LENGTH_SHORT).show();
                firstPressedTime = System.currentTimeMillis();
            }
        }
    }

    private void selectOtaFile(String path) {
        Intent intent = new Intent();
        intent.setAction(VoiceTestFragment.BROADCAST_RECEIVE);
        intent.putExtra("notify", "select_ota_file");
        intent.putExtra("path", path);
        sendBroadcast(intent);
    }

    private void otaReset() {
        Intent intent = new Intent();
        intent.setAction(VoiceTestFragment.BROADCAST_RECEIVE);
        intent.putExtra("notify", "ota_reset");
        sendBroadcast(intent);
    }

    @Override
    public void onFileSelection(FileChooserDialog dialog, File file) {
        Utils.showToast(file.getAbsolutePath(), MainActivity.this);
        selectOtaFile(file.getAbsolutePath());
    }

    @Override
    public void onFileChooserDismissed(FileChooserDialog dialog) {
        //Utils.showToast("File chooser dismissed!", MainActivity.this);
    }
}
