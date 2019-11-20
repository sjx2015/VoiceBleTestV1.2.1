package com.actions.voicebletest.fragment;


import android.Manifest;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actions.voicebletest.R;
import com.actions.voicebletest.dfu.CancelByUserException;
import com.actions.voicebletest.dfu.DfuData;
import com.actions.voicebletest.dfu.Partition;
import com.actions.voicebletest.dfu.PartitionTable;
import com.actions.voicebletest.dfu.PartitionTpyeMap;
import com.actions.voicebletest.dfu.XmlPartition;
import com.actions.voicebletest.dfu.XmlPartitionCrcenable;
import com.actions.voicebletest.dfu.XmlRoot;
import com.actions.voicebletest.dfu.XmlRootCrcenable;
import com.actions.voicebletest.main.MainActivity;
import com.actions.voicebletest.utils.HexString;
import com.actions.voicebletest.utils.LittleEndian;
import com.actions.voicebletest.utils.SharedpreferencesProvider;
import com.actions.voicebletest.utils.SpeedUtil;
import com.actions.voicebletest.utils.Utils;
import com.actions.voicebletest.utils.ZipUtils;
import com.afollestad.materialdialogs.folderselector.FileChooserDialog;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.exceptions.BleGattCannotStartException;
import com.polidea.rxandroidble2.exceptions.BleGattException;
import com.polidea.rxandroidble2.exceptions.BleGattOperationType;

import org.reactivestreams.Subscription;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import de.mateware.snacky.Snacky;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okio.BufferedSource;
import okio.Okio;


/**
 * A simple {@link Fragment} subclass.
 * This is just a demo fragment with a long scrollable view of editTexts. Don't see this as a reference for anything
 */
public class OtaFragment extends Fragment {
    private static final String TAG = OtaFragment.class.getSimpleName();
    private static final int STORAGE_PERMISSION_RC = 69;
    private static final int GET_FILE_COMPELETE = 1;
    private static final int UPDATE_PROGRESS = 2;
    private static final int VERIFYFILES = 3;
    private static final int SEND_PACKET_ASYNC = 4;
    private static final int UPDATE_UPLOAD_BTN = 5;
    private static final int OTA_SUCCESS = 6;
    private static final int UPDATE_SPEED = 7;
    private static final int BATTERY_LESS_THAN_50 = 8;
    private static final int BATTERY_MORE_THAN_50 = 9;
    private static final int GET_BATTERY_ERROR = 10;
    private static final int UPDATE_TIME = 11;
    private static final int VERIFYFILES_FAILED = 12;
    private static final int CHECK_FILE_CRC32_ERROR = 13;
    private static final int GET_FW_VERSION_SUCCESS = 14;


    private MainActivity mMainActivity = null;
    private File file = null;
    private FileWriter logcatFileWriter = null;
    private int LOGFILELENGTH = 1024 * 1024 * 50;

    public final static UUID wdxcSvcUuid = UUID.fromString("0000fef6-0000-1000-8000-00805f9b34fb");
    public final static UUID wdxcDcUuid = UUID.fromString("005f0002-2ff2-4ed5-b045-4C7463617865");
    public final static UUID wdxcFtcUuid = UUID.fromString("005f0003-2ff2-4ed5-b045-4C7463617865");
    public final static UUID wdxcFtdUuid = UUID.fromString("005f0004-2ff2-4ed5-b045-4C7463617865");
    public final static UUID wdxcAuUuid = UUID.fromString("005f0005-2ff2-4ed5-b045-4C7463617865");
    public final static UUID batterySvcUuid = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    public static final UUID batteryValUuid = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    public static final UUID firmwareVelUuid = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
    public static final UUID CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private BluetoothGattCharacteristic wdxcWdxsDc, wdxcWdxsFtc, wdxcWdxsFtd, wdxcWdxsAu;

    /*! File transfer control characteristic operations */
    private final int WDXS_FTC_OP_NONE = 0;
    private final int WDXS_FTC_OP_GET_REQ = 1;
    private final int WDXS_FTC_OP_GET_RSP = 2;
    private final int WDXS_FTC_OP_PUT_REQ = 3;
    private final int WDXS_FTC_OP_PUT_RSP = 4;
    private final int WDXS_FTC_OP_ERASE_REQ = 5;
    private final int WDXS_FTC_OP_ERASE_RSP = 6;
    private final int WDXS_FTC_OP_VERIFY_REQ = 7;
    private final int WDXS_FTC_OP_VERIFY_RSP = 8;
    private final int WDXS_FTC_OP_ABORT = 9;
    private final int WDXS_FTC_OP_EOF = 10;
    private final int WDXS_FTC_OP_PACKET_RECEIVED = 11;
    private final int WDXS_FTC_OP_RESET = 12;
    private final int WDXS_FTC_OP_GET_VERSION_REQ = 13;
    private final int WDXS_FTC_OP_GET_VERSION_RSP = 14;

    /* WDXS File Transfer Control Command Message Lengths */
    private final int WDXS_FTC_ABORT_LEN = 3;
    private final int WDXS_FTC_ERASE_LEN = 3;
    private final int WDXS_FTC_GET_VERSION_LEN = 3;
    private final int WDXS_FTC_VERIFY_LEN = 3;
    private final int WDXS_FTC_PUT_LEN = 16;
    private final int WDXS_FTC_GET_LEN = 12;

    /*! Device configuration characteristic message header length */
    private final int WDXS_DC_HDR_LEN = 2;
    /*! Device configuration characteristic operations */
    private final int WDXS_DC_OP_GET = 0x01;         /*! Get a parameter value */
    private final int WDXS_DC_OP_SET = 0x02;         /*! Set a parameter value */
    private final int WDXS_DC_OP_UPDATE = 0x03;         /*! Send an update of a parameter value */

    /*! Device control characteristic parameter IDs */
    private final int WDXS_DC_ID_CONN_UPDATE_REQ = 0x01;         /*! Connection Parameter Update Request */
    private final int WDXS_DC_ID_CONN_PARAM = 0x02;         /*! Current Connection Parameters */
    private final int WDXS_DC_ID_DISCONNECT_REQ = 0x03;         /*! Disconnect Request */
    private final int WDXS_DC_ID_CONN_SEC_LEVEL = 0x04;         /*! Connection Security Level */
    private final int WDXS_DC_ID_SECURITY_REQ = 0x05;         /*! Security Request */
    private final int WDXS_DC_ID_SERVICE_CHANGED = 0x06;         /*! Service Changed */
    private final int WDXS_DC_ID_DELETE_BONDS = 0x07;         /*! Delete Bonds */
    private final int WDXS_DC_ID_ATT_MTU = 0x08;         /*! Current ATT MTU */
    private final int WDXS_DC_ID_BATTERY_LEVEL = 0x20;         /*! Battery level */
    private final int WDXS_DC_ID_MODEL_NUMBER = 0x21;         /*! Device Model */
    private final int WDXS_DC_ID_FIRMWARE_REV = 0x22;         /*! Device Firmware Revision */
    private final int WDXS_DC_ID_ENTER_DIAGNOSTICS = 0x23;         /*! Enter Diagnostic Mode */
    private final int WDXS_DC_ID_DIAGNOSTICS_COMPLETE = 0x24;         /*! Diagnostic Complete */
    private final int WDXS_DC_ID_DISCONNECT_AND_RESET = 0x25;         /*! Disconnect and Reset */


    private boolean isDFUServiceFound = false;
    private boolean isSetupNotification = false;
    private boolean isFileValidated = false;

    private static OtaFragment instance = null;

    private Button mSelectFileBtn = null;
    private String mFilePath = "";
    private List<File> mFileList = null;
    private List<String> mBinPath = new ArrayList<String>();
    private int mCurUpdateIndex = 0;
    private XmlRoot xmlRoot = null;
    private XmlRootCrcenable xmlRootCrcenable = null;
    private TextView mFileNameTextView = null;
    private TextView mFileSizeTextView = null;
    private Button mUploadBtn = null;
    private ProgressBar mProgressBar = null;
    private TextView mTextPercentage = null;
    private TextView mTextSpeedView = null;
    private TextView mTextSendingFileName = null;
    private TextView getmTextSendingFileSize = null;

    private RxBleConnection rxBleConnection = null;
    private Subscription updateSubscription;
    private Disposable disposable;

    public static final int NO_TRANSFER = 0;
    public static final int START_TRANSFER = 1;
    public static final int FINISHED_TRANSFER = 2;
    public static final int TRANSFER_ERROR = 3;
    public static final int VERIFY_FAILED = 4;
    private int mFileTransferStatus = NO_TRANSFER;

    private Uri mFileStreamUri;
    private InputStream mFileStream;
    private int BYTES_IN_ONE_PACKET = 20;

    private DfuData.wdxcCb_t wdxcCb = new DfuData.wdxcCb_t();
    private int mProgress = 0;
    private long startTime = 0;
    private int dcNextop = 0;
    private long bytesSend = 0;
    private long bytesSending = 0;
    private PartitionTable mPartitionTable = null;
    private int mPartTableSize = 384;
    private byte[] mPartTableByte = null;
    private boolean mCrcEnable = false;
    private boolean mHasCrcEnable = false;
    private PartitionTpyeMap mPartitionTypeMap = new PartitionTpyeMap();
    private Partition mPart = null;
    private Partition mPartErase = null;
    private XmlPartition mXmlPart = null;
    private int index = 0;
    private int mBatteryVal = 0;
    private TextView mTimingTextView = null;
    private int mTiming = 0;
    private long mCrc32Val = 0;
    private TextView mOtaTipsTextView = null;
    private TextView mFwVersionTextView = null;
    private String mVersion = "";
    private int mOffset = 0;
    //private Aes mAes = new Aes();

    private boolean mOTANotStop = false;

    private String mFirmwareVersion = null;
    private String mSpeed = "";
    private Runnable task = new Runnable() {
        @Override
        public void run() {
            mHandler.sendEmptyMessage(UPDATE_SPEED);
            mHandler.postDelayed(this, 1000);
        }
    };

    private Runnable time = new Runnable() {
        @Override
        public void run() {
            mHandler.sendEmptyMessage(UPDATE_TIME);
            mHandler.postDelayed(this, 1000);
        }
    };

    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(VoiceTestFragment.BROADCAST_RECEIVE)) {
                String msg = intent.getStringExtra("notify");
                Log.d(TAG, msg);
                if (msg.equals("select_ota_file")) {
                    mFilePath = intent.getStringExtra("path");
                    //SharedpreferencesProvider.saveSharePerferences(SharedpreferencesProvider.OTA_PATH, mFilePath);
                    setText();
                } else if (msg.equals("ota_reset")) {
                    reset();
                }
            }
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case GET_FILE_COMPELETE:
                    OnGetFileCompleted();
                    break;
                case UPDATE_PROGRESS:
                    updateProgressBar(mProgress);
                    break;
                case VERIFYFILES:
                    WdxcVerifyFiles();
                    break;
                case SEND_PACKET_ASYNC:
                    sendPacketAsync();
                    break;
                case UPDATE_UPLOAD_BTN:
                    updateUploadButton();
                    break;
                case OTA_SUCCESS:
                    //mCurUpdateIndex++;
                    if (mCurUpdateIndex + 1 <= mBinPath.size()) {
                        startUploadBin();
                    } else {
                        Utils.showToast("Successful File Transfer Validation!", mMainActivity);
                        setLogText("Successful File Transfer Validation!");
                        mOtaTipsTextView.setText(R.string.ota_success);
                        File file = new File(mFilePath);
                        if (file.exists()) {
                            file.delete();
                        }
                        if (mOTANotStop) {
                            setLogText("ota again!");
                            mCurUpdateIndex = 0;
                            bootLoaderSendTimes = 1;
                            startUploadBin();
                        } else {
                            WdxcFtcSendReset();
                            setLogText("WdxcFtcSendReset!");
                            mUploadBtn.setEnabled(false);
                            mHandler.removeCallbacks(time);
                        }
                        //mCurUpdateIndex = 0;
                        //startUploadBin();
                    }
                    break;
                case UPDATE_SPEED:
                    mTextSpeedView.setText(mSpeed);
                    Log.d(TAG, "speed:  " + mSpeed);
                    break;
                case BATTERY_LESS_THAN_50:
                    String log = "Battery level: " + mBatteryVal + "%, Less than or equal to 30%\n Not allow to Updating!";
                    Utils.showToast(log, mMainActivity);
                    setLogText(log);
                    restState();
                    break;
                case BATTERY_MORE_THAN_50:
                    WdxcFtcSendUpdateConnParam(10, 10, 0, 800);
                    WdxcFtcSendGetVersion();
                    break;
                case GET_FW_VERSION_SUCCESS:
                    mFwVersionTextView.setText(mVersion);
                    WdxcFtcSendAbort(wdxcCb.mOTAHandle);
                    unzipOTA();
                    Log.d(TAG, "startUpload()");
                    setLogText("startUpload()");
                    startUpload();
                    index = 0;
                    mTiming = 0;
                    mHandler.removeCallbacks(time);
                    mHandler.postDelayed(time, 10);
                    break;
                case GET_BATTERY_ERROR:
                    log = "Get Battery Level Error!";
                    Utils.showToast(log, mMainActivity);
                    break;
                case UPDATE_TIME:
                    mTiming++;
                    mTimingTextView.setText(Utils.formatSecondsDuration(mTiming));
                    break;
                case VERIFYFILES_FAILED:
                    Utils.showToast("File validation failed!", mMainActivity);
                    break;
                case CHECK_FILE_CRC32_ERROR:
                    Utils.showToast("Check File crc32 failed!", mMainActivity);
                    break;
            }
        }
    };

    public void setRxBleConnection(RxBleConnection bleConnection) {
        this.rxBleConnection = bleConnection;
        if (rxBleConnection != null) {
            Log.d(TAG, rxBleConnection.toString());
            startToFoundDfuService();
        }
    }

    private void initBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(VoiceTestFragment.BROADCAST_RECEIVE);
        getActivity().registerReceiver(receiver, filter);
    }

    public OtaFragment() {
        // Required empty public constructor
    }

    public static OtaFragment getInstance() {
        if (instance == null) {
            instance = new OtaFragment();
        }

        return instance;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Log.d(TAG, "onCreate()");
        mMainActivity = (MainActivity) getActivity();
        initBroadcastReceiver();
        setLogText("rxble: " + (rxBleConnection == null ? "null" : rxBleConnection.toString()));
        if (rxBleConnection != null && !isDFUServiceFound) {
            //startToFoundDfuService();//TCL p561U 发现services比较慢，概率出现过同时两个startToFoundDfuService执行，故注释掉
        }

    }

    private void createOtaLogFile() {
        if (logcatFileWriter == null) {
            String logPaht = mMainActivity.getCacheDirPath() + "/ota.log";
            file = new File(logPaht);
            try {
                logcatFileWriter = new FileWriter(logPaht, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void setLogText(String log) {
        if (mMainActivity != null) {
            mMainActivity.setLogText(log);
            /*try {
                createOtaLogFile();
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
                String date = df.format(new Date()) + "  ";// new Date()为获取当前系统时间
                logcatFileWriter.append(date + " -->: " + log);
                logcatFileWriter.append("\n");
                if (file.length() > LOGFILELENGTH) {
                    if (mMainActivity != null) {
                        String logPath = mMainActivity.getCacheDirPath() + "/ota.log";
                        Log.i(TAG, "Logcat File is > 50M. clear");
                        logcatFileWriter = new FileWriter(logPath);
                        logcatFileWriter.write("");
                        logcatFileWriter.flush();
                        logcatFileWriter = new FileWriter(logPath, true);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }*/
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //WdxcDcSetDisconnectAndReset();
        Log.d(TAG, "onDestroy");
        getActivity().unregisterReceiver(receiver);
        try {
            if (logcatFileWriter != null) {
                logcatFileWriter.close();
                logcatFileWriter = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        // don't look at this layout it's just a listView to show how to handle the keyboard
        View view = inflater.inflate(R.layout.fragment_ota, container, false);

        mSelectFileBtn = view.findViewById(R.id.action_selectfile);
        mSelectFileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFileChooser();
            }
        });
        mFileNameTextView = view.findViewById(R.id.tv_file_name);
        mFileSizeTextView = view.findViewById(R.id.tv_file_size);
        mUploadBtn = view.findViewById(R.id.action_upload);
        mUploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Utils.isNoFastClick()) {
                    onUploadClick();
                } else {
                    Utils.showToast("click too fast!", getContext());
                }
            }
        });
        mProgressBar = view.findViewById(R.id.progress_bar);
        mTextPercentage = view.findViewById(R.id.tv_progress);
        mTextSpeedView = view.findViewById(R.id.tv_speed);
        mTextSendingFileName = view.findViewById(R.id.tv_bin_file_name);
        getmTextSendingFileSize = view.findViewById(R.id.tv_bin_file_size);
        mTimingTextView = view.findViewById(R.id.timing);
        mOtaTipsTextView = view.findViewById(R.id.ota_tips);
        mFwVersionTextView = view.findViewById(R.id.tv_fw_version);
        mHandler.removeCallbacks(task);
        mHandler.removeCallbacks(time);

        mFilePath = SharedpreferencesProvider.getSharePerferences(SharedpreferencesProvider.OTA_PATH);
        setText();

        if (mFileTransferStatus == FINISHED_TRANSFER) {
            mOtaTipsTextView.setText(R.string.ota_success);
            mUploadBtn.setEnabled(false);
            updateProgressBar(mProgress);
            mTextSpeedView.setText(mSpeed);
            String name = mBinPath.get(mBinPath.size() - 1);
            name = name.substring(name.lastIndexOf("/") + 1) + "    " + (mBinPath.size()) + "/" + mBinPath.size();
            mTextSendingFileName.setText(name);
            getmTextSendingFileSize.setText(Utils.getFileSize(mBinPath.get(mBinPath.size() - 1)) + "");
            mTimingTextView.setText(Utils.formatSecondsDuration(mTiming));
        } else if (mFileTransferStatus == START_TRANSFER) {
            String name = mBinPath.get(mCurUpdateIndex);
            name = name.substring(name.lastIndexOf("/") + 1) + "    " + (mCurUpdateIndex + 1) + "/" + mBinPath.size();
            mTextSendingFileName.setText(name);
            getmTextSendingFileSize.setText(Utils.getFileSize(mBinPath.get(mCurUpdateIndex)) + "");
            mTimingTextView.setText(Utils.formatSecondsDuration(mTiming));
            mHandler.postDelayed(task, 20);
            mHandler.postDelayed(time, 20);
        }

        return view;
    }

    private void setText() {
        if (mFilePath != null && mFilePath.contains("/")) {
            int start = mFilePath.lastIndexOf("/");
            String name = mFilePath.substring(start + 1, mFilePath.length());
            mFileNameTextView.setText(name);
            mFileSizeTextView.setText(Utils.getFileSize(mFilePath) + "");
            mUploadBtn.setEnabled(true);
        }
    }

    private void updateProgressBar(int progress) {
        Log.d(TAG, "progress: " + progress + " bytes received: " + wdxcCb.mOTATxCount);
        mProgressBar.setProgress(progress);
        mTextPercentage.setText(Integer.toString(progress) + "%");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //add the values which need to be saved from the drawer to the bundle
        super.onSaveInstanceState(outState);
    }


    private void checkAndDownload() {
        showTips(R.string.ota_checking);
        Log.e(TAG, "checkAndDownload()");
        String url = "https://github.com/lfkabc/Ble-controller/raw/master/";
        String url_version = url + "version.txt";
        String newVersion = getNewVersion(url_version);
        Log.e(TAG, "new version from server:" + newVersion);
        if (newVersion != null) {
            showTips(R.string.ota_download);
            String path = downloadNewVersion(url, newVersion + "_zs110a_ota.zip");
            if (path != null) {
                /*mFilePath = path;
                SharedpreferencesProvider.saveSharePerferences(SharedpreferencesProvider.OTA_PATH, mFilePath);
                setText();
                onUploadClick();*/
                selectOtaFile(path);
            } else {
                showTips(R.string.local_update);
            }
        } else {
            showTips(R.string.local_update);
        }

    }

    private void showTips(int resourceId) {
        if (isAdded()) {
            Snacky.builder()
                    .setActivity(mMainActivity)
                    .setText(getString(resourceId))
                    .centerText()
                    .setDuration(Snacky.LENGTH_INDEFINITE)
                    .info()
                    .show();
        }

    }

    private void selectOtaFile(String path) {
        Intent intent = new Intent();
        intent.setAction(VoiceTestFragment.BROADCAST_RECEIVE);
        intent.putExtra("notify", "select_ota_file");
        intent.putExtra("path", path);
        mMainActivity.sendBroadcast(intent);
        showTips(R.string.ota_tips);
    }

    private String getNewVersion(String url_version) {
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
        if (content != null) {
            int currentVersion = str2Version(mFirmwareVersion);
            int serverVersion = str2Version(content);
            Log.e(TAG, "getNewVersion() current:" + currentVersion + "  new:" + serverVersion);
            //return currentVersion < serverVersion ? content : null;
            return content;
        }

        return null;

    }

    private String downloadNewVersion(String url, String fileName) {
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;
        HttpURLConnection httpURLConnection = null;
        String result = null;

        File pathSd = Environment.getExternalStorageDirectory();
        File file = new File(pathSd, fileName);
        Log.i(TAG, "file: " + file);
        if (!file.exists()) {  //倘若没有这个文件
            try {
                Log.i(TAG, "create file " + file);
                file.createNewFile();  //创建这个文件
            } catch (IOException e) {
                Log.i(TAG, "fail !!!!!!!!!!!!!! create file " + file);
                e.printStackTrace();
            }
        }

        try {
            URL downloadUrl = new URL(url + fileName);
            Log.e(TAG, "url:" + downloadUrl);
            httpURLConnection = (HttpURLConnection) downloadUrl.openConnection();
            httpURLConnection.connect();
            int code = httpURLConnection.getResponseCode();
            if (code == 200) {
                int fileSize = httpURLConnection.getContentLength();
                Log.i(TAG, "file size： " + fileSize);
                inputStream = httpURLConnection.getInputStream();
                fileOutputStream = new FileOutputStream(file);
                byte[] b = new byte[1024];
                int size = 0;
                while ((size = inputStream.read(b)) != -1) {
                    fileOutputStream.write(b, 0, size);
                }
                result = file.getAbsolutePath();

            } else {
                Log.e(TAG, "no internet?? ");
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }

                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.i(TAG, "download success");
        return result;
    }

    private int str2Version(String str) {
        String temp = str.substring(5);
        temp = temp.replace("_", "");
        return Integer.parseInt(temp);
    }


    public void showFileChooser() {
        if (ActivityCompat.checkSelfPermission(
                getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    getActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_RC);
            return;
        }
        String path = SharedpreferencesProvider.getSharePerferences(SharedpreferencesProvider.OTA_PATH);
        FileChooserDialog.Builder build = new FileChooserDialog.Builder(getContext());

        if (path != null && !path.equals("")) {
            int end = path.lastIndexOf("/");
            String dir = path.substring(0, end);
            build.initialPath(dir);
        }

        build.extensionsFilter(".zip")
                .show(getActivity());
    }

    //新版cache含有crc校验，返回true，旧版false
    private boolean hasEnable_crc(String xmlPath) {
        try {
            File file = new File(xmlPath);
            InputStream is = new FileInputStream(file);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            //获得Document对象
            Document document = builder.parse(is);
            NodeList parttitionList = document.getElementsByTagName("partition");
            for (int i = 0; i < parttitionList.getLength(); i++) {
                Node node_partition = parttitionList.item(i);
                NodeList childNodes = node_partition.getChildNodes();
                for (int j = 0; j < childNodes.getLength(); j++) {
                    Node childNode = childNodes.item(j);
                    Log.d(TAG, childNode.getNodeName() + " -- " + childNode.getTextContent());
                    if ("enable_crc".equals(childNode.getNodeName())) {
                        mHasCrcEnable = true;
                    }
                    if ("enable_crc".equals(childNode.getNodeName()) && childNode.getTextContent().equalsIgnoreCase("true")) {
                        Log.d(TAG, "enable_crc true");
                        return true;
                    }
                }
            }
            if (is != null)
                is.close();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "enable_crc false");
        return false;
    }

    private void unzipOTA() {
        try {
            mCurUpdateIndex = 0;
            bootLoaderSendTimes = 1;
            mBinPath.clear();
            ZipUtils.UnZipFolder(mFilePath, mMainActivity.getCacheDirPath());
            mFileList = ZipUtils.GetFileList(mFilePath, true, true);
            String xmlPath = "";
            for (int i = 0; i < mFileList.size(); i++) {
                if (mFileList.get(i).getName().contains(".xml")) {
                    xmlPath = mMainActivity.getCacheDirPath() + mFileList.get(i).getAbsolutePath();
                }
            }
            mHasCrcEnable = false;
            if (hasEnable_crc(xmlPath)) {
                mPartTableSize = 384 / 32 * 34;
                mCrcEnable = true;
                bootloaderContentLen = 3 * 1024 / 32 * 34;
            } else {
                mPartTableSize = 384;
                mCrcEnable = false;
                bootloaderContentLen = 3 * 1024;
            }
            mPartTableByte = new byte[mPartTableSize];
            File f = new File(xmlPath);

            if (mHasCrcEnable) {
                Serializer ser = new Persister();
                xmlRootCrcenable = ser.read(XmlRootCrcenable.class, f);

                String bootName = null;
                for (int i = 0; i < xmlRootCrcenable.getmXmlPartitons().size(); i++) {
                    if (xmlRootCrcenable.getmXmlPartitons().get(i).getType().equals("BOOT")) {
                        bootName = xmlRootCrcenable.getmXmlPartitons().get(i).getFile();
                    }
                }
                String loadBinPath = null;
                for (int i = 0; i < mFileList.size(); i++) {
                    if (bootName != null && mFileList.get(i).getAbsolutePath().contains(bootName)) {
                        loadBinPath = mMainActivity.getCacheDirPath() + mFileList.get(i).getAbsolutePath();
                    } else if (mFileList.get(i).getAbsolutePath().contains(".bin")) {
                        mBinPath.add(mMainActivity.getCacheDirPath() + mFileList.get(i).getAbsolutePath());
                    }
                }
                if (loadBinPath != null)
                    mBinPath.add(loadBinPath);
                xmlRoot = new XmlRoot();
                xmlRoot.setVersion(xmlRootCrcenable.getVersion());
                xmlRoot.setPartitionsNum(xmlRootCrcenable.getPartitionsNum());
                List<XmlPartition> xmlPartitionList = new ArrayList<>();
                for (int i = 0; i < xmlRootCrcenable.getmXmlPartitons().size(); i++) {
                    XmlPartitionCrcenable xmlPartitionCrcenable = xmlRootCrcenable.getmXmlPartitons().get(i);
                    XmlPartition xmlPartition = new XmlPartition(xmlPartitionCrcenable.getType(), xmlPartitionCrcenable.getName(), xmlPartitionCrcenable.getFile(),
                            xmlPartitionCrcenable.getAddress(), xmlPartitionCrcenable.getFw_id(), xmlPartitionCrcenable.getCrc32());
                    xmlPartitionList.add(xmlPartition);
                }
                xmlRoot.setmXmlPartitons(xmlPartitionList);
            } else {
                Serializer ser = new Persister();
                xmlRoot = ser.read(XmlRoot.class, f);

                String bootName = null;
                for (int i = 0; i < xmlRoot.getmXmlPartitons().size(); i++) {
                    if (xmlRoot.getmXmlPartitons().get(i).getType().equals("BOOT")) {
                        bootName = xmlRoot.getmXmlPartitons().get(i).getFile();
                    }
                }
                String loadBinPath = null;
                for (int i = 0; i < mFileList.size(); i++) {
                    if (bootName != null && mFileList.get(i).getAbsolutePath().contains(bootName)) {
                        loadBinPath = mMainActivity.getCacheDirPath() + mFileList.get(i).getAbsolutePath();
                    } else if (mFileList.get(i).getAbsolutePath().contains(".bin")) {
                        mBinPath.add(mMainActivity.getCacheDirPath() + mFileList.get(i).getAbsolutePath());
                    }
                }
                if (loadBinPath != null)
                    mBinPath.add(loadBinPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getFirmwareValue() {
        rxBleConnection.readCharacteristic(firmwareVelUuid)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        characteristicValue -> {
                            // Read characteristic value.
                            mFirmwareVersion = new String(characteristicValue);
                            String log = "kkkkk firmware version: " + mFirmwareVersion;
                            Log.e(TAG, log);
                            setLogText(log);
                        },
                        throwable -> {
                            // Handle an error here.
                            Log.e(TAG, "get firmware version fail ..........!!!!!!!");
                        }
                );
    }


    private void getBatteryValue() {
        mBatteryVal = 0;
        rxBleConnection.readCharacteristic(batteryValUuid)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        characteristicValue -> {
                            // Read characteristic value.
                            String log = "Battery Level: " + Integer.valueOf(HexString.bytesToHex(characteristicValue), 16) + "%";
                            Log.d(TAG, log);
                            setLogText(log);
                            mBatteryVal = Integer.valueOf(HexString.bytesToHex(characteristicValue), 16);
                            if (mBatteryVal > 30) {
                                mHandler.sendEmptyMessage(BATTERY_MORE_THAN_50);
                            } else {
                                mHandler.sendEmptyMessage(BATTERY_LESS_THAN_50);
                            }
                        },
                        throwable -> {
                            // Handle an error here.
                            mHandler.sendEmptyMessage(GET_BATTERY_ERROR);
                        }
                );
    }

    private void onUploadClick() {
        if (mFileTransferStatus == START_TRANSFER) {
            onCancelUpload();
        } else {
            if (isDFUServiceFound) {
                mFileTransferStatus = START_TRANSFER;
                mHandler.sendEmptyMessage(UPDATE_UPLOAD_BTN);
                mTiming = 0;
                getBatteryValue();
            } else {
                Utils.showToast("DFU device is not found!", mMainActivity);
                setLogText("DFU device is not found!");
            }
        }
    }

    private void onCancelUpload() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
        mFileTransferStatus = NO_TRANSFER;
        mHandler.sendEmptyMessage(UPDATE_UPLOAD_BTN);
        mHandler.removeCallbacks(task);
        mHandler.removeCallbacks(time);
        WdxcFtcSendAbort(wdxcCb.mOTAHandle);
    }

    private void startUploadBin() {
        try {
            String name = mBinPath.get(mCurUpdateIndex);
            name = name.substring(name.lastIndexOf("/") + 1) + "    " + (mCurUpdateIndex + 1) + "/" + mBinPath.size();
            mTextSendingFileName.setText(name);
            setLogText("startUploadBin: " + name);
            getmTextSendingFileSize.setText(Utils.getFileSize(mBinPath.get(mCurUpdateIndex)) + "");
            InputStream hexStream = new FileInputStream(mBinPath.get(mCurUpdateIndex));
            openFile(hexStream);
            mHandler.sendEmptyMessage(GET_FILE_COMPELETE);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "error opening file:" + " " + e);
            setLogText("startUploadBin(): " + e.toString());
        }
    }

    private void startUpload() {
        WdxcGetFiles(0);//0分区表，1地址偏移
        wdxcCb.authState = DfuData.WDXS_AU_STATE_AUTHORIZED;
    }

    public void WdxcGetFiles(int fileHdl) {
        wdxcCb.fDlPos = 0;
        wdxcCb.fileCount = 0;
        wdxcCb.maxFiles = DfuData.WSF_EFS_MAX_FILES;
        WdxcFtcSendGetReq(fileHdl, 0, mPartTableSize, 0);
    }

    private void WdxcFtcSendGetReq(int fileHdl, int offset, int len, int type) {
        Log.d(TAG, "getDfuFlist");
        if (isDFUServiceFound) {
            byte[] data = new byte[DfuData.WDXS_FTC_GET_LEN];
            data[0] = DfuData.WDXS_FTC_OP_GET_REQ;           /* opCode(1byte)        */
            data[1] = (byte) (fileHdl & 0xFF);         /* fileHdl(2byte)         */
            data[2] = (byte) ((fileHdl >> 8) & 0xFF);
            data[3] = (byte) (offset & 0xFF);         /* offset(4byte)          */
            data[4] = (byte) ((offset >> 8) & 0xFF);
            data[5] = (byte) ((offset >> 16) & 0xFF);
            data[6] = (byte) ((offset >> 24) & 0xFF);
            data[7] = (byte) (len & 0xFF);            /*   len(4byte)            */
            data[8] = (byte) ((len >> 8) & 0xFF);
            data[9] = (byte) ((len >> 16) & 0xFF);
            data[10] = (byte) ((len >> 24) & 0xFF);
            data[11] = (byte) type;                   /*  ftPrefXferType         */
            Log.d(TAG, "WdxcFtcSendGetReqhandle: " + fileHdl + "offset: " + offset + "len: " + len);
            setLogText("WdxcFtcSendGetReqhandle: " + fileHdl + " offset: " + offset + " len: " + len);
            onWriteFtc(data);
            wdxcCb.fileHdl = fileHdl;
        }
    }

    private void onWriteFtc(byte[] data) {
        if (mMainActivity.isConnected()) {
            rxBleConnection.writeCharacteristic(wdxcFtcUuid, data)
                    .observeOn(Schedulers.io())
                    .subscribe(
                            bytes -> onWriteFtcSuccess(),
                            this::onWriteFailure
                    );
        }
    }

    private void onWriteDc(byte[] data) {
        if (mMainActivity.isConnected()) {
            rxBleConnection.writeCharacteristic(wdxcDcUuid, data)
                    .observeOn(Schedulers.io())
                    .subscribe(
                            bytes -> onWriteDcSuccess(),
                            this::onWriteFailure
                    );
        }
    }

    private void onWriteFtcSuccess() {
        //noinspection ConstantConditions
        Log.d(TAG, "onWriteFtcSuccess");
        setLogText("onWriteFtcSuccess");
    }

    private void onWriteDcSuccess() {
        //noinspection ConstantConditions
        Log.d(TAG, "onWriteDcSuccess");
        setLogText("onWriteDcSuccess");
    }

    private void onWriteFailure(Throwable throwable) {
        //noinspection ConstantConditions
        Log.d(TAG, "onWriteFailure");
        setLogText("onWriteFailure: " + throwable.toString());
    }

    public void openFile(InputStream stream) {
        try {
            wdxcCb.mPacketNumber = 0;
            //HexInputStream class convert file format from Hex to Binary

            /*	mFileStream = new HexInputStream(stream);*/
            mFileStream = stream;
            wdxcCb.mFileSize = mFileStream.available();
            wdxcCb.mTotalPackets = getNumberOfPackets();
            Log.d(TAG, "File Size: " + wdxcCb.mFileSize);
            setLogText("File Size: " + wdxcCb.mFileSize);
        } catch (IOException e) {
            Log.e(TAG, "error opening file:" + " " + e);
            setLogText("error opening file:" + " " + e);
        }
    }

    private int getNumberOfPackets() {
        int numOfPackets = (int) (wdxcCb.mFileSize / BYTES_IN_ONE_PACKET);
        if ((wdxcCb.mFileSize % BYTES_IN_ONE_PACKET) > 0) {
            numOfPackets++;
        }
        return numOfPackets;
    }

    private void updateUploadButton() {
        if (mFileTransferStatus == START_TRANSFER) {
            mUploadBtn.setText(R.string.upload_cancel);
        } else {
            mUploadBtn.setText(R.string.upload_ota);
        }
    }

    private void startToFoundDfuService() {
        Log.d(TAG, "startToFoundDfuService...");
        setLogText("startToFoundDfuService");
        rxBleConnection.discoverServices()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        service -> {
                            Log.d(TAG, "DfuServiceFound!");
                            setLogText("Founding DFU Service and startSetupNotification!");
                            isDFUServiceFound = true;
                            startSetupNotification();
                            wdxcWdxsFtc = service.getCharacteristic(wdxcFtcUuid).blockingGet();
                            wdxcWdxsFtd = service.getCharacteristic(wdxcFtdUuid).blockingGet();
                            wdxcWdxsDc = service.getCharacteristic(wdxcDcUuid).blockingGet();
                            //wdxcWdxsAu = service.getCharacteristic(wdxcAuUuid);

                            getFirmwareValue();
                            new Thread() {
                                @Override
                                public void run() {
                                    SystemClock.sleep(2000);
                                    checkAndDownload();
                                }
                            }.start();
                        },
                        this::onConnectionFailureDfuNotFound
                );
    }

    private void onConnectionFailureDfuNotFound(Throwable throwable) {
        //noinspection ConstantConditions
        Log.d(TAG, "onConnectionFailureDfuNotFound");
        setLogText("onConnectionFailureDfuNotFound");
        Utils.showToast("DFU Service NotFound!", getContext());
        reset();
    }

    public void restState() {
        mFileTransferStatus = NO_TRANSFER;
    }

    public void reset() {
        bytesSend = 0;
        bytesSending = 0;
        wdxcCb = new DfuData.wdxcCb_t();
        wdxcCb.mOTATxCount = 0;
        startTime = 0;
        mProgress = 0;
        isDFUServiceFound = false;
        mHandler.removeCallbacks(time);
    }

    private void startSetupNotification() {
        Log.d(TAG, "startSetupNotification: ");
        setupNotificationFtc(wdxcFtcUuid);
        setupNotificationFtd(wdxcFtdUuid);
        setupNotificationdc(wdxcDcUuid);
        //setupNotificationAu(wdxcAuUuid);
    }

    private void setupNotificationFtc(UUID uuid) {
        rxBleConnection.setupNotification(uuid)
                //.doOnNext(notificationObservable -> this::notificationHasBeenSetUp)
                .flatMap(notificationObservable -> notificationObservable)
                .subscribeOn(Schedulers.io())
                .subscribe(this::onNotificationReceivedFtc, this::onNotificationSetupFailure);
    }

    private void onNotificationReceivedFtc(byte[] bytes) {
        //noinspection ConstantConditions
        Log.d(TAG, "onNotificationReceivedFtc: " + HexString.byteArrayToHexString(bytes));
        wdxcParseFtc(bytes);
    }

    private void setupNotificationFtd(UUID uuid) {
        rxBleConnection.setupNotification(uuid)
                //.doOnNext(notificationObservable -> runOnUiThread(this::notificationHasBeenSetUp))
                .flatMap(notificationObservable -> notificationObservable)
                .subscribeOn(Schedulers.io())
                .subscribe(this::onNotificationReceivedFtd, this::onNotificationSetupFailure);
    }

    private void onNotificationReceivedFtd(byte[] bytes) {
        //noinspection ConstantConditions
        if (index >= mPartTableSize)
            index = 0;
        System.arraycopy(bytes, 0, mPartTableByte, index, bytes.length);
        index += bytes.length;
        Log.d(TAG, index + " len");
        setLogText("onNotificationReceivedFtd: " + HexString.byteArrayToHexString(bytes));
        setLogText("onNotificationReceivedFtd total len:" + index);
        if (index == mPartTableSize) {
            setLogText("onNotificationReceivedFtd " + mPartTableSize + " bytes");
            if (mCrcEnable) {
                byte[] table = new byte[384];
                for (int i = 0; i < 384 / 32; i++) {
                    System.arraycopy(mPartTableByte, i * 34, table, i * 32, 32);
                }
                mPartitionTable = new PartitionTable(table);
            } else {
                mPartitionTable = new PartitionTable(mPartTableByte);
            }
            mHandler.sendEmptyMessage(OTA_SUCCESS);
        }
        /*if(index == mPartTableSize){
            FileOutputStream stream = null;
            try {
                String tPath = mMainActivity.getCacheDirPath() + "/table0625.bin";
                stream = new FileOutputStream(tPath);
                stream.write(mPartTableByte);
                stream.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }*/
        Log.d(TAG, "onNotificationReceivedFtd: " + HexString.byteArrayToHexString(bytes));
        //wdxcParseFtd(bytes);
    }

    private void setupNotificationdc(UUID uuid) {
        rxBleConnection.setupNotification(uuid)
                //.doOnNext(notificationObservable -> runOnUiThread(this::notificationHasBeenSetUp))
                .flatMap(notificationObservable -> notificationObservable)
                .subscribeOn(Schedulers.io())
                .subscribe(this::onNotificationReceivedDc, this::onNotificationSetupFailure);
    }

    private void onNotificationReceivedDc(byte[] bytes) {
        //noinspection ConstantConditions
        wdxcParseDc(bytes);
        Log.d(TAG, "onNotificationReceivedDc");
        setLogText("onNotificationReceivedDc: " + HexString.byteArrayToHexString(bytes));
    }

    private void onNotificationSetupFailure(Throwable throwable) {
        //noinspection ConstantConditions
        Log.d(TAG, "onNotificationSetupFailure:" + throwable);
        setLogText("OTA onNotificationSetupFailure: " + throwable.toString());
    }

    private void notificationHasBeenSetUp() {
        //noinspection ConstantConditions
        Log.d(TAG, "notificationHasBeenSetUp");
    }

    void wdxcParseFtc(byte[] data) {
        int opCode = data[0];
        int handle = ((data[2] & 0xFF) << 8) + (data[1] & 0xFF);
        if ((opCode == DfuData.WDXS_FTC_OP_EOF) || (opCode == DfuData.WDXS_FTC_OP_ABORT)) {
            Log.d(TAG, "handle: " + handle + "opCode: " + opCode);
            setLogText("wdxcParseFtc  handle: " + handle + " opCode: " + opCode);
            wdxcCb.fileHdl = DfuData.WSF_EFS_INVALID_HANDLE;
        }
        if (handle == DfuData.WDXS_FLIST_HANDLE) {
            if (opCode == DfuData.WDXS_FTC_OP_GET_RSP) {
                int status = data[3];
                Log.d(TAG, "WDXS_FTC_OP_GET_RSP status: " + status + "from handle: " + handle);
                setLogText("wdxcParseFtc  WDXS_FTC_OP_GET_RSP status: " + status + " from handle: " + handle);
            } else if (opCode == DfuData.WDXS_FTC_OP_EOF) {
                /* File Discovery complete */
                //mHandler.sendEmptyMessage(GET_FILE_COMPELETE);

            }
        } else if (handle == wdxcCb.mOTAHandle) {
            if (opCode == DfuData.WDXS_FTC_OP_PUT_RSP) {
                int status = data[3];
                Log.d(TAG, "WDXS_FTC_OP_PUT_RSP status: " + status + "from handle: " + handle);
                setLogText("wdxcParseFtc  WDXS_FTC_OP_PUT_RSP status: " + status + " from handle: " + handle);
                if (status == 0) {
                    startUploadingFile();
                }
            }
            //This is packet received notification
            //This returns total number of bytes successfully transfered so far
            //here we transfer next packet of 20 bytes
            else if (opCode == DfuData.WDXS_FTC_OP_PACKET_RECEIVED) {
                int status = data[3];
                int TxCount = ((data[5] & 0xFF) << 8) + (data[4] & 0xFF);
                Log.d(TAG, "Packet received TxCount: " + TxCount + "status: " + status);
                wdxcCb.mOTATxCount += TxCount;
                if (mPart.getType() == mPartitionTypeMap.getType("BOOT")) {
                    if (bootLoaderSendTimes == 1) {
                        mProgress = (int) (wdxcCb.mOTATxCount * 1.0 / bootLoaderTableData.length * 100);
                    } else if (bootLoaderSendTimes == 2) {
                        mProgress = (int) (wdxcCb.mOTATxCount * 1.0 / bootLoaderContentData.length * 100);
                    }
                } else {
                    mProgress = (int) (wdxcCb.mOTATxCount * 1.0 / wdxcCb.mFileSize * 100);
                }
                mHandler.sendEmptyMessage(UPDATE_PROGRESS);
                long currentTime = System.currentTimeMillis();
                Log.d(TAG, "startTime: " + startTime + "currentTime: " + currentTime + "wdxcCb.mOTATxCount: " + wdxcCb.mOTATxCount);
                String speed = SpeedUtil.calculateSpeed(startTime, currentTime, wdxcCb.mOTATxCount);
                mSpeed = speed;
                if (wdxcCb.mOTATxCount == wdxcCb.mFileSize) {
                    onFileTransferCompleted();
                    setLogText("wdxcParseFtc  Successful File transfer!");
                }
            }
            //After all the bytes of file has been transfered this notification will be received
            // if file has been transfered successfully the we will validate transfered file
            else if (opCode == DfuData.WDXS_FTC_OP_EOF) {
                Log.d(TAG, "Successful File transfer! EOF!");
                setLogText("wdxcParseFtc WDXS_FTC_OP_EOF Received!");
                mHandler.sendEmptyMessage(VERIFYFILES);
                //onFileTransferCompleted();
                //mProgress = 100;
                //mHandler.sendEmptyMessage(UPDATE_PROGRESS);
                //WdxcVerifyFiles();
                //mHandler.sendEmptyMessage(VERIFYFILES);
            }
            //After sending file validation this notification will be received
            //if file has been validated successfully then we will activate and reset device
            else if (opCode == DfuData.WDXS_FTC_OP_VERIFY_RSP) {
                int status = data[3];
                Log.d(TAG, "WDXS_FTC_OP_VERIFY_RSP status: " + status + "from handle: " + handle);
                setLogText("wdxcParseFtc  WDXS_FTC_OP_VERIFY_RSP status: " + status + "from handle: " + handle);
                if (status == 0) {
                    Log.d(TAG, "Successful File Transfer Validation!");
                    if (mPart.getType() == mPartitionTypeMap.getType("BOOT") && mPartErase != null) {
                        if (mOTANotStop)
                            ;
                        else if (bootLoaderSendTimes == 1)
                            bootLoaderSendTimes = 2;
                        else if (bootLoaderSendTimes == 2) {
                            bootLoaderSendTimes = 3;
                            int offset = ((int) mPartErase.getOffset() / 0x1000) * 0x1000;
                            long len = mPart.getOffset() - mPartErase.getOffset();
                            if (mCrcEnable) {
                                offset = offset / 32 * 34;
                            }
                            if (offset == 0)
                                WdxcFtcSendEraseReq(1, offset, (((int) Math.abs(mPart.getOffset() - mPartErase.getOffset()) + 0x1000 - 1) / 0x1000) * 0x1000);
                            else
                                WdxcFtcSendEraseReq(1, offset, (((int) Math.abs(len) + 0x1000 - 1) / 0x1000) * 0x1000);
                        }
                    }
                    setLogText("wdxcParseFtc  Successful File Transfer Validation!");
                    onFileTransferValidation();
                } else {
                    Log.e(TAG, "File validation failed" + status);
                    setLogText("wdxcParseFtc  File validation failed" + status);
                    mHandler.sendEmptyMessage(VERIFYFILES_FAILED);
                    //mCallbacks.onError(ERROR_FILE_VALIDATION, status);
                }
                if (mCurUpdateIndex + 1 > mBinPath.size()) {
                    if (status == 0)
                        mFileTransferStatus = FINISHED_TRANSFER;
                    else
                        mFileTransferStatus = VERIFY_FAILED;
                    mHandler.sendEmptyMessage(UPDATE_UPLOAD_BTN);
                }
            } else if (opCode == DfuData.WDXS_FTC_OP_ERASE_RSP) {
                Log.d(TAG, "WDXS_FTC_OP_ERASE_RSP" + handle);
                setLogText("wdxcParseFtc  WDXS_FTC_OP_ERASE_RSP" + handle);
                if (mPart.getType() == mPartitionTypeMap.getType("BOOT")) {
                    if (bootLoaderSendTimes == 1) {
                        if (mCrcEnable)
                            WdxcFtcSendPutReq(wdxcCb.mOTAHandle, (mOffset + 3 * 1024) / 32 * 34, wdxcCb.mFileSize - bootloaderContentLen, wdxcCb.mFileSize - bootloaderContentLen, 0);
                        else
                            WdxcFtcSendPutReq(wdxcCb.mOTAHandle, mOffset + bootloaderContentLen, wdxcCb.mFileSize - bootloaderContentLen, wdxcCb.mFileSize - bootloaderContentLen, 0);
                    }
                } else if (mCrcEnable)
                    WdxcFtcSendPutReq(wdxcCb.mOTAHandle, mOffset / 32 * 34, wdxcCb.mFileSize, wdxcCb.mFileSize, 0);
                else
                    WdxcFtcSendPutReq(wdxcCb.mOTAHandle, mOffset, wdxcCb.mFileSize, wdxcCb.mFileSize, 0);//default offset=0
            } else if (opCode == DfuData.WDXS_FTC_OP_GET_VERSION_RSP) {
                byte[] version = new byte[16];
                for (int i = 0; i < data.length - 4; i++) {
                    version[i] = data[i + 4];
                }
                mVersion = new String(version);
                setLogText("FW Version: " + mVersion);
                mHandler.sendEmptyMessage(GET_FW_VERSION_SUCCESS);
            }
        }
    }

    private void wdxcParseFtd(byte[] data) {
        if (wdxcCb.fileHdl == DfuData.WDXS_FLIST_HANDLE) /* read flist */ {
            wdxsParseFileList(data);
            //WdxcFtcSendGetReq(DfuData.WDXS_FLIST_HANDLE, wdxcCb.fDlPos, 10, 0); /* read again */
        }
    }

    void wdxsParseFileList(byte[] pValue) {
        int pos = 0;
        int len = pValue.length;

        /* Depending on the MTU, blocks of FTD data from the file list may end mid-value.
         * Maintain a global position called wdxcCb.fDlPos, and process FTD data byte by byte */

        while (pos < len) {
            if (wdxcCb.fDlPos < DfuData.WDXS_FLIST_HDR_SIZE) {
                /* Ignore file list header */
            } else {
                /* Find the file index and the position within the file index (the mark) */
                byte mark = (byte) ((wdxcCb.fDlPos - DfuData.WDXS_FLIST_HDR_SIZE) % DfuData.WDXS_FLIST_RECORD_SIZE);
                byte file = (byte) ((wdxcCb.fDlPos - DfuData.WDXS_FLIST_HDR_SIZE) / DfuData.WDXS_FLIST_RECORD_SIZE);

                /* Ignore data if there is insufficient space in wdxcCb.pFileList */
                if (file >= wdxcCb.maxFiles) {
                    return;
                }

                //pInfo = &wdxcCb.FileList[file];

                /* Process a byte of data */
                switch (mark) {
                    case 0:
                        wdxcCb.fileCount++;
                        wdxcCb.FileList[file].handle = pValue[pos];
                        break;
                    case 1:
                        wdxcCb.FileList[file].handle |= ((short) (pValue[pos] & 0xFF)) << 8;
                        break;
                    case 2:
                        wdxcCb.FileList[file].attributes.type = pValue[pos];
                        break;
                    case 3:
                        wdxcCb.FileList[file].attributes.permissions = pValue[pos];
                        break;
                    case 4:
                        wdxcCb.FileList[file].size = pValue[pos];
                        break;
                    case 5:
                        wdxcCb.FileList[file].size |= ((int) (pValue[pos] & 0xFF)) << 8;
                        break;
                    case 6:
                        wdxcCb.FileList[file].size |= ((int) (pValue[pos] & 0xFF)) << 16;
                        break;
                    case 7:
                        wdxcCb.FileList[file].size |= ((int) (pValue[pos] & 0xFF)) << 24;
                        break;
                    default:
                        if (mark > 7 && mark < 8 + DfuData.WSF_EFS_NAME_LEN) {
                            wdxcCb.FileList[file].attributes.name[mark - 8] = pValue[pos];
                        } else {
                            wdxcCb.FileList[file].attributes.version[mark - (8 + DfuData.WSF_EFS_NAME_LEN)] = pValue[pos];
                        }
                        break;
                }
            }

            wdxcCb.fDlPos++;
            pos++;
        }
    }

    void wdxcParseDc(byte[] data) {
        byte[] pValue = data;
        int len = pValue.length;
        if (len < DfuData.WDXS_DC_HDR_LEN) return;
        int op = (int) (pValue[0] & 0xFF);
        int id = (int) (pValue[1] & 0xFF);
        if (op == DfuData.WDXS_DC_OP_UPDATE) {
            switch (id) {
                case DfuData.WDXS_DC_ID_CONN_PARAM: {
                    int status = (int) (pValue[2] & 0xFF);
                    int connInterval = (int) (pValue[3] & 0xFF) + ((int) (pValue[4] & 0xFF) >> 8);
                    int connLatency = (int) (pValue[5] & 0xFF) + ((int) (pValue[6] & 0xFF) >> 8);
                    int supTimeout = (int) (pValue[5] & 0xFF) + ((int) (pValue[6] & 0xFF) >> 8);
                    Log.d(TAG, "status: " + status + "connInterval: " + connInterval + "connLatency: " + connLatency + "supTimeout: " + supTimeout);
                    break;
                }
                case DfuData.WDXS_DC_ID_CONN_SEC_LEVEL: {
                    int SecLevel = (int) (pValue[2] & 0xFF);
                    Log.d(TAG, "SecLevel: " + SecLevel);
                    break;
                }
                case DfuData.WDXS_DC_ID_ATT_MTU: {
                    int attMtu = ((int) (pValue[2] & 0xFF)) + (((int) (pValue[3] & 0xFF)) >> 8);
                    Log.d(TAG, "attMtu: " + attMtu);
                    //this.mtu = (this.mtu < attMtu)?this.mtu:attMtu;
                    break;
                }
                case DfuData.WDXS_DC_ID_BATTERY_LEVEL: {
                    int batteryLevel = (int) (pValue[2] & 0xFF);
                    Log.d(TAG, "batteryLevel: " + batteryLevel);
                    break;
                }
                case DfuData.WDXS_DC_ID_MODEL_NUMBER: {
                    byte[] pModelTxt = new byte[DfuData.WDXS_DC_LEN_DEVICE_MODEL];
                    for (int i = 0; i < DfuData.WDXS_DC_LEN_FIRMWARE_REV; i++)
                        pModelTxt[i] = pValue[DfuData.WDXS_DC_HDR_LEN + i];
                    String sModelTxt = new String(pModelTxt);
                    Log.d(TAG, "sModelTxt: " + sModelTxt);
                    break;
                }
                case DfuData.WDXS_DC_ID_FIRMWARE_REV: {
                    byte[] pFirmwareRev = new byte[DfuData.WDXS_DC_LEN_FIRMWARE_REV];
                    for (int i = 0; i < DfuData.WDXS_DC_LEN_FIRMWARE_REV; i++)
                        pFirmwareRev[i] = pValue[DfuData.WDXS_DC_HDR_LEN + i];
                    String sFirmwareRev = new String(pFirmwareRev);
                    Log.d(TAG, "sFirmwareRev: " + sFirmwareRev);
                    break;
                }
            }
        }
    }


    public void OnGetFileCompleted() {
        Log.d(TAG, "OnDiscoverFileCompleted");
        WdxcPutFiles("OTA Firmware");
    }

    public void WdxcPutFiles(String OtaFIleName) {
        if (bootLoaderSendTimes == 2) {
            if (mCrcEnable)
                WdxcFtcSendPutReq(wdxcCb.mOTAHandle, mOffset / 32 * 34, bootloaderContentLen, bootloaderContentLen, 0);
            else
                WdxcFtcSendPutReq(wdxcCb.mOTAHandle, mOffset, bootloaderContentLen, bootloaderContentLen, 0);
            return;
        }
        int type = 0;
        int part_index = 0;
        XmlPartition xmlPart1 = null, xmlPart2 = null;
        for (int i = 0; i < xmlRoot.getmXmlPartitons().size(); i++) {
            XmlPartition xmlPartition = xmlRoot.getmXmlPartitons().get(i);
            if (mBinPath.get(mCurUpdateIndex).contains(xmlPartition.getFile())) {
                type = mPartitionTypeMap.getType(xmlPartition.getType());
                if (xmlPart1 == null) {
                    xmlPart1 = xmlPartition;
                } else if (xmlPart2 == null) {
                    xmlPart2 = xmlPartition;
                }
            }
        }
        int part1 = -1, part2 = -1;
        for (int i = 0; i < 15; i++) {
            Partition partition = mPartitionTable.getParts()[i];
            if (partition.getType() == type) {
                if (part1 == -1) {
                    part1 = i;
                } else if (part2 == -1) {
                    part2 = i;
                    break;
                }
            }
        }
        mPart = null;
        int part_id = -1;
        if (part1 == -1 || xmlPart1 == null) {
            Utils.showToast("Can't get partition table information!", mMainActivity);
            return;
        } else if (part2 == -1) {
            mPart = mPartitionTable.getParts()[part1];
            mXmlPart = xmlPart1;
        } else if (mPartitionTable.getParts()[part1].getSeq() < mPartitionTable.getParts()[part2].getSeq()) {
            mPart = mPartitionTable.getParts()[part1];
            mPartErase = mPartitionTable.getParts()[part2];
            part_id = part1;
            short seq = (short) (mPartitionTable.getParts()[part2].getSeq() + 1);
            mPart.setSeq(seq);
            mXmlPart = xmlPart1;
        } else {
            mPart = mPartitionTable.getParts()[part2];
            mPartErase = mPartitionTable.getParts()[part1];
            part_id = part2;
            short seq = (short) (mPartitionTable.getParts()[part1].getSeq() + 1);
            mPart.setSeq(seq);
            mXmlPart = xmlPart2;
        }
        String val = mXmlPart.getAddress();
        if (val.toLowerCase().startsWith("0x")) {
            val = val.substring(2);
            mOffset = Integer.parseInt(val, 16);
        } else {
            mOffset = Integer.parseInt(val);
        }
        Log.d(TAG, mXmlPart.toString() + " offset  ->>>>>>:" + mOffset);
        if (mPart.getOffset() != mOffset) {
            mPart.setOffset(mOffset);
            mPartitionTable.getParts()[part_id].setSeq(mPart.getSeq());
            mPartitionTable.getParts()[part_id].setOffset(mOffset);
        }
        /*for (int j = 0; j < xmlRoot.getmXmlPartitons().size(); j++) {
            XmlPartition xmlPartition = xmlRoot.getmXmlPartitons().get(j);
            if (mBinPath.get(mCurUpdateIndex).contains(xmlPartition.getFile())) {
                for(int k=0; k < mPartitionTable.getPart_cnt(); k++){
                    Partition partition = mPartitionTable.getParts()[k];
                    if (partition.getType().equals(xmlPartition.getType()) && partition.getId() != xmlPartition.getFw_id()){
                        String val = xmlPartition.getAddress();
                        if (val.toLowerCase().startsWith("0x")) {
                            val = val.substring(2);
                            offset = Integer.parseInt(val, 16);
                        } else {
                            offset = Integer.parseInt(val);
                        }
                        Log.d(TAG, xmlPartition.toString() + " offset  ->>>>>>:" + offset);
                    }
                }
            }
        }*/
        //wdxcCb.mOTAHandle = 1;
        if (bootLoaderSendTimes == 1) {
            int offset = (mOffset / 0x1000) * 0x1000;
            if (mPart.getType() != mPartitionTypeMap.getType("BOOT") && mCrcEnable) {
                offset = offset / 32 * 34;
            }
            WdxcFtcSendEraseReq(wdxcCb.mOTAHandle, offset, ((wdxcCb.mFileSize + 0x1000 - 1) / 0x1000) * 0x1000);
        }
    }

    private void WdxcFtcSendPutReq(int fileHdl, int offset, int len, int fileSize, int type) {
        Log.d(TAG, "WdxcFtcSendPutReq");
        if (isDFUServiceFound) {
            byte[] data = new byte[DfuData.WDXS_FTC_PUT_LEN];
            data[0] = DfuData.WDXS_FTC_OP_PUT_REQ;             /* opCode(1byte)        */
            data[1] = (byte) (fileHdl & 0xFF);          /* handle(2byte)         */
            data[2] = (byte) ((fileHdl >> 8) & 0xFF);
            data[3] = (byte) (offset & 0xFF);           /* offset(4byte)         */
            data[4] = (byte) ((offset >> 8) & 0xFF);
            data[5] = (byte) ((offset >> 16) & 0xFF);
            data[6] = (byte) ((offset >> 24) & 0xFF);
            data[7] = (byte) (len & 0xFF);              /*   len(4byte)           */
            data[8] = (byte) ((len >> 8) & 0xFF);
            data[9] = (byte) ((len >> 16) & 0xFF);
            data[10] = (byte) ((len >> 24) & 0xFF);
            data[11] = (byte) (fileSize & 0xFF);        /*   len(4byte)           */
            data[12] = (byte) ((fileSize >> 8) & 0xFF);
            data[13] = (byte) ((fileSize >> 16) & 0xFF);
            data[14] = (byte) ((fileSize >> 24) & 0xFF);
            data[15] = (byte) type;                     /* ftPrefXferType         */
            Log.d(TAG, "WdxcFtcSendPutReq" + "handle: " + fileHdl + "offset: " + offset + "fileSize: " + fileSize);
            setLogText("WdxcFtcSendPutReq" + " handle: " + fileHdl + " offset: " + offset + " fileSize: " + fileSize);
            onWriteFtc(data);
            wdxcCb.fileHdl = fileHdl;
        }
    }

    private void WdxcFtcSendEraseReq(int fileHdl, int offset, int len) {
        Log.d(TAG, "WdxcFtcSendEraseReq");
        if (isDFUServiceFound) {
            byte[] data = new byte[DfuData.WDXS_FTC_PUT_LEN];
            data[0] = DfuData.WDXS_FTC_OP_ERASE_REQ;             /* opCode(1byte)        */
            data[1] = (byte) (fileHdl & 0xFF);          /* handle(2byte)         */
            data[2] = (byte) ((fileHdl >> 8) & 0xFF);
            data[3] = (byte) (offset & 0xFF);           /* offset(4byte)         */
            data[4] = (byte) ((offset >> 8) & 0xFF);
            data[5] = (byte) ((offset >> 16) & 0xFF);
            data[6] = (byte) ((offset >> 24) & 0xFF);
            data[7] = (byte) (len & 0xFF);              /*   len(4byte)           */
            data[8] = (byte) ((len >> 8) & 0xFF);
            data[9] = (byte) ((len >> 16) & 0xFF);
            data[10] = (byte) ((len >> 24) & 0xFF);
            Log.d(TAG, "WdxcFtcSendEraseReq" + "handle: " + fileHdl + "offset: " + offset + "len: " + len);
            setLogText("WdxcFtcSendEraseReq" + " handle: " + fileHdl + " offset: " + offset + " len: " + len);
            onWriteFtc(data);
        }
    }

    void WdxcFtcSendAbort(int fileHdl) {
        Log.d(TAG, "WdxcFtcSendAbort");
        if (isDFUServiceFound) {
            byte[] data = new byte[WDXS_FTC_ERASE_LEN];
            data[0] = WDXS_FTC_OP_ABORT;         /* opCode(1byte)         */
            data[1] = (byte) (fileHdl & 0xFF);         /* handle(2byte)         */
            data[2] = (byte) ((fileHdl >> 8) & 0xFF);
            onWriteFtc(data);
            Log.d(TAG, "WdxcFtcSendAbort" + "handle: " + fileHdl);
            wdxcCb.fileHdl = fileHdl;
        }
    }

    public void WdxcFtcSendGetVersion() {
        if (isDFUServiceFound) {
            wdxcCb.mOTAHandle = 1;
            byte[] data = new byte[DfuData.WDXS_FTC_GET_VERSION_LEN];
            data[0] = DfuData.WDXS_FTC_OP_GET_VERSION_REQ;         /* opCode(1byte)         */
            data[1] = (byte) wdxcCb.mOTAHandle;
            data[2] = (byte) 0;
            onWriteFtc(data);
            Log.d(TAG, "WdxcFtcSendGetVersion");
            setLogText("WdxcFtcSendGetVersion");
        }
    }

    public void WdxcFtcSendReset() {
        if (isDFUServiceFound) {
            byte[] data = new byte[DfuData.WDXS_FTC_SYSTEM_RESET_LEN];
            data[0] = DfuData.WDXS_FTC_OP_RESET;         /* opCode(1byte)         */
            data[1] = (byte) 0;
            data[2] = (byte) 0;
            onWriteFtc(data);
            Log.d(TAG, "WdxcFtcSendReset");
        }
    }

    public void WdxcFtcSendUpdateConnParam(int interval_min, int interval_max, int latency, int timeout) {
        if (isDFUServiceFound) {
            byte[] data = new byte[DfuData.WDXS_DC_ID_CONN_UPDATE_LEN];
            data[0] = DfuData.WDXS_DC_ID_CONN_PARAM;         /* opCode(1byte)         */
            data[1] = DfuData.WDXS_DC_ID_CONN_UPDATE_REQ;
            data[2] = (byte) (interval_min & 0xFF);
            data[3] = (byte) ((interval_min >> 8) & 0xFF);
            data[4] = (byte) (interval_max & 0xFF);
            data[5] = (byte) ((interval_max >> 8) & 0xFF);
            data[6] = (byte) (latency & 0xFF);
            data[7] = (byte) ((latency >> 8) & 0xFF);
            data[8] = (byte) (timeout & 0xFF);
            data[9] = (byte) ((timeout >> 8) & 0xFF);
            onWriteDc(data);
            Log.d(TAG, "WdxcFtcSendReset");
        }
    }

    public void WdxcDcSetDisconnectAndReset() {
        if (isDFUServiceFound) {
            byte[] data = new byte[WDXS_DC_HDR_LEN];
            data[0] = (byte) WDXS_DC_OP_SET;                          /* opCode(1byte)        */
            data[1] = (byte) WDXS_DC_ID_DISCONNECT_AND_RESET;         /* id(1byte)         */
            onWriteFtc(data);
            setLogText("WdxcDcSetDisconnectAndReset");
            Log.d(TAG, "WdxcDcSetDisconnectAndReset");
        }
    }

    /**
     * After Receive Firmware Image we will start uploading file by sending each time packet of 20 bytes and wait for packet Received notification
     */
    private void startUploadingFile() {
        Log.d(TAG, "Preparing to send file");
        setLogText("Preparing to send file");
        wdxcCb.mOTATxCount = 0;
        wdxcCb.mPacketNumber = 0;
        wdxcCb.isLastPacket = false;
        mHandler.sendEmptyMessage(SEND_PACKET_ASYNC);
        //sendPacketSync();
        onFileTransferStarted();
    }

    public void onFileTransferStarted() {
        Log.d(TAG, "onFileTransferStarted()");
        setLogText("onFileTransferStarted()");
        startTime = System.currentTimeMillis();
    }

    public void onFileTransferValidation() {
        Log.d(TAG, "onFileTransferValidation()");
        setLogText("onFileTransferValidation()");
        isFileValidated = true;
        mCurUpdateIndex++;
        if (mPart.getType() == mPartitionTypeMap.getType("BOOT") && bootLoaderSendTimes == 2)
            mCurUpdateIndex--;
        mHandler.sendEmptyMessage(OTA_SUCCESS);
    }

    public void onFileTransferCompleted() {
        long currentTime = System.currentTimeMillis();
        long costTime = currentTime - startTime;
        Log.d(TAG, "onFileTransferCompleted() costTime:" + costTime);
        String speed = SpeedUtil.calculateSpeed(startTime, currentTime, wdxcCb.mFileSize);
        Log.d(TAG, "mFileSize: " + wdxcCb.mFileSize);
        Log.d(TAG, "receive speed: " + speed);
        //setUploadButtonText(R.string.dfu_action_upload);
        //WdxcVerifyFiles();
    }

    public void WdxcVerifyFiles() {
        WdxcFtcSendVerifyFile(wdxcCb.mOTAHandle);
    }

    private void WdxcFtcSendVerifyFile(int fileHdl) {
        Log.d(TAG, "WdxcFtcSendVerifyFile");
        if (isDFUServiceFound) {
            byte[] data = new byte[DfuData.WDXS_FTC_VERIFY_LEN];
            data[0] = DfuData.WDXS_FTC_OP_VERIFY_REQ;         /* opCode(1byte)         */
            data[1] = (byte) (fileHdl & 0xFF);         /* handle(2byte)         */
            data[2] = (byte) ((fileHdl >> 8) & 0xFF);
            int index = 3;
            int offset = (int) mPart.getOffset();
            int size = wdxcCb.mFileSize;
            if (mPart.getType() == mPartitionTypeMap.getType("BOOT")) {
                if (bootLoaderSendTimes == 1) {
                    offset = mOffset + bootloaderContentLen;
                    size = wdxcCb.mFileSize - bootloaderContentLen;
                    if (mCrcEnable)
                        offset = (mOffset + 3 * 1024) / 32 * 34;
                } else if (bootLoaderSendTimes == 2) {
                    offset = mOffset;
                    size = bootloaderContentLen;
                    if (mCrcEnable)
                        offset = mOffset / 32 * 34;
                }
            } else if (mCrcEnable)
                offset = (int) mPart.getOffset() / 32 * 34;
            LittleEndian.fillByteArrayInt(data, index, offset);
            index += 4;
            LittleEndian.fillByteArrayInt(data, index, size);
            index += 4;
            LittleEndian.fillByteArrayLong(data, index, mCrc32Val);
            Log.d(TAG, "fileLen: " + size + " offset: " + offset + " crc: " + mCrc32Val);
            Log.d(TAG, "WdxcFtcSendVerifyFile" + "handle: " + fileHdl);
            setLogText("WdxcFtcSendVerifyFile: " + "handle: " + fileHdl + " fileSize: " + wdxcCb.mFileSize + " offset: " + mPart.getOffset() + " crc32: " + mCrc32Val);
            onWriteFtc(data);
            wdxcCb.fileHdl = fileHdl;
        }
    }

    private boolean checkCRC32(String filePath) {
        String crc32 = mXmlPart.getCrc32();
        long val = 0;
        if (crc32.contains("0x"))
            val = Long.parseLong(crc32, 16);
        else
            val = Long.parseLong(crc32);
        if (Utils.calculateCRC32(filePath) == val)
            return true;
        return false;
    }

    byte[] mData = {0};
    int writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE;
    int bootLoaderPacketTable = 0;
    int bootLoaderPacketContent = 0;
    byte[] bootLoaderTableData = {0};
    byte[] bootLoaderContentData = {0};
    int bootloaderContentLen = 3 * 1024;
    int bootLoaderSendTimes = 1;

    //@TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void sendPacketAsync() {
        wdxcCb.mPacketNumber++;
        String filePath = mBinPath.get(mCurUpdateIndex);
        if (filePath == null)
            return;
        if (!checkCRC32(filePath)) {
            mHandler.sendEmptyMessage(CHECK_FILE_CRC32_ERROR);
            return;
        }
        bytesSending = 0;
        mSpeed = "";
        bytesSend = 0;
        wdxcCb.mOTATxCount = 0;
        int mtu = rxBleConnection.getMtu();
        Log.d(TAG, "current Mtu: " + rxBleConnection.getMtu());
        setLogText("sendPacketAsync current Mtu: " + rxBleConnection.getMtu());
        writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE;
        setLogText("Phone MODEL: " + Build.MODEL);
        /*if (Build.MODEL.equals("G620-L75") || Build.MODEL.equals("CHM-UL00")){//这两台手机使用no response方式发送，一段时间后无法收到回调，不兼容
            writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
            setLogText("BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT");
        }*/
        disposable = Observable.just(filePath)
                .map(s -> {
                    BufferedSource bufferedSource = null;
                    byte[] bytes = null;
                    try {
                        bufferedSource = Okio.buffer(Okio.source(new File(filePath)));
                        bytes = bufferedSource.readByteArray();
                        wdxcCb.mFileSize = bytes.length;
                        bufferedSource.close();
                        if (mPart.getType() == mPartitionTypeMap.getType("BOOT")) {
                            setLogText("upload type : BOOT ");
                            byte[] table = mPartitionTable.toLEByteArray();
                            String tPath = mMainActivity.getCacheDirPath() + "/load_table.bin";
                            String tPar = mMainActivity.getCacheDirPath() + "/table.bin";
                            if (mCrcEnable) {
                                byte[] crctable = crcTable(table);
                                System.arraycopy(crctable, 0, bytes, bootloaderContentLen, 384 / 32 * 34);
                            } else {
                                System.arraycopy(table, 0, bytes, bootloaderContentLen, 384);
                            }
                            FileOutputStream stream = new FileOutputStream(tPath);
                            FileOutputStream streamPar = new FileOutputStream(tPar);
                            try {
                                stream.write(bytes);
                                streamPar.write(table);
                            } finally {
                                stream.close();
                                streamPar.close();
                            }
                        }
                        mCrc32Val = Utils.calculateCRC32(bytes);
                        return bytes;
                    } catch (Throwable t) {
                        throw Exceptions.propagate(t);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(bytes -> {
                    long loadFileFinishTime = System.currentTimeMillis();
                    Log.d(TAG, "finishLoadFile() called" + loadFileFinishTime);
                    Log.d(TAG, "finishLoadFile() bytes.length" + bytes.length);
                    bytesSend = bytes.length;
                    Log.d(TAG, "ftdUuid:  " + wdxcFtdUuid.toString());
                    mHandler.removeCallbacks(task);
                    mHandler.postDelayed(task, 200);
                })
                /*.flatMap(bytes -> rxBleConnection.createNewLongWriteBuilder()
                        .setCharacteristicUuid(wdxcFtdUuid) // required or the .setCharacteristic()
                        // .setCharacteristic() alternative if you have a specific BluetoothGattCharacteristic
                        .setBytes(bytes)
                        // .setWriteOperationRetryStrategy(retryStrategy) // if you'd like to retry batch write operations on failure, provide your own retry strategy
                         //.setMaxBatchSize(23) // optional -> default 20 or current MTU
                        // .setWriteOperationAckStrategy(ackStrategy) // optional to postpone writing next batch
                        .build()
                )*/
                .flatMap(bytesSend -> rxBleConnection.queue((bluetoothGatt, rxBleGattCallback, scheduler) -> Observable.create(
                        emitter -> {
                            //bluetoothGatt.requestMtu(120);
                            Log.i("START optimizedSend", bytesSend.length + "");
                            final AtomicBoolean writeCompleted = new AtomicBoolean(false);
                            final AtomicBoolean ackCompleted = new AtomicBoolean(false);
                            final AtomicInteger batchesSent = new AtomicInteger(1);
                            int packetLen = mtu - 3;
                            batchesSent.set(1);
                            mData = Arrays.copyOfRange(bytesSend, (batchesSent.get() - 1) * packetLen, packetLen * batchesSent.get());
                            int i = 0;
                            if (bytesSend.length % packetLen != 0)
                                i = 1;
                            final int totalPacket = bytesSend.length / packetLen + i;
                            if (mPart.getType() == mPartitionTypeMap.getType("BOOT")) {
                                if (bootLoaderSendTimes == 1) {
                                    bootLoaderPacketTable = (bytesSend.length - bootloaderContentLen) / packetLen;
                                    if ((bytesSend.length - bootloaderContentLen) % packetLen != 0)
                                        bootLoaderPacketTable++;
                                    bootLoaderPacketContent = bootloaderContentLen / packetLen;
                                    if ((bootloaderContentLen) % packetLen != 0)
                                        bootLoaderPacketContent++;
                                    bootLoaderContentData = Arrays.copyOfRange(bytesSend, 0, bootloaderContentLen);
                                    bootLoaderTableData = Arrays.copyOfRange(bytesSend, bootloaderContentLen, bytesSend.length);
                                    mData = Arrays.copyOfRange(bootLoaderTableData, (batchesSent.get() - 1) * packetLen, packetLen * batchesSent.get());
                                    mCrc32Val = Utils.calculateCRC32(bootLoaderTableData);
                                    Log.d(TAG, "bootloader crc32 1: " + mCrc32Val);
                                    Log.d(TAG, "bootloader totalPacket: " + (bootLoaderPacketTable + bootLoaderPacketContent));
                                } else {
                                    mCrc32Val = Utils.calculateCRC32(bootLoaderContentData);
                                }
                            }
                            Log.d(TAG, "totalPacket: " + totalPacket);
                            setLogText("bytes length: " + bytesSend.length + " totalPacket: " + totalPacket);
                            wdxcWdxsFtd.setValue(mData);
                            wdxcWdxsFtd.setWriteType(writeType);
                            final Runnable writeNextBatch = () -> {
                                if (!bluetoothGatt.writeCharacteristic(wdxcWdxsFtd)) {
                                    emitter.onError(new BleGattCannotStartException(bluetoothGatt, BleGattOperationType.CHARACTERISTIC_WRITE));
                                } else {
                                    Log.i("SEND", String.valueOf(batchesSent.get()));
                                    //batchesSent.incrementAndGet();
                                }
                            };
                            final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
                                @Override
                                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                                    batchesSent.incrementAndGet();
                                    if (mFileTransferStatus == NO_TRANSFER) {
                                        emitter.onError(new CancelByUserException("OTA Cancel by user"));
                                    }
                                    if (status != BluetoothGatt.GATT_SUCCESS) {
                                        emitter.onError(new BleGattException(gatt, status, BleGattOperationType.CHARACTERISTIC_WRITE));
                                    } else if (mPart.getType() == mPartitionTypeMap.getType("BOOT")) {
                                        Log.d(TAG, "send bootloader bin packet: " + batchesSent.get());
                                        if (bootLoaderSendTimes == 1) {
                                            if (batchesSent.get() == bootLoaderPacketTable + 1) {
                                                if (ackCompleted.get()) {
                                                    batchesSent.set(1);
                                                    ackCompleted.set(false);
                                                    emitter.onComplete();
                                                    //emitter.onNext(mData);
                                                    //writeNextBatch.run();
                                                    Log.d(TAG, "compelete: " + bootLoaderPacketTable);
                                                    setLogText("onCharacteristicWrite compelete: " + bootLoaderPacketTable);
                                                } else {
                                                    writeCompleted.set(true);
                                                }
                                            } else {
                                                int end = packetLen * batchesSent.get() > bootLoaderTableData.length ? bootLoaderTableData.length : packetLen * batchesSent.get();
                                                mData = Arrays.copyOfRange(bootLoaderTableData, (batchesSent.get() - 1) * packetLen, end);
                                                characteristic.setValue(mData);
                                                wdxcWdxsFtd.setWriteType(writeType);
                                                writeNextBatch.run();
                                                emitter.onNext(mData);
                                            }
                                        } else if (bootLoaderSendTimes == 2) {
                                            if (batchesSent.get() == bootLoaderPacketContent + 1) {
                                                if (ackCompleted.get()) {
                                                    batchesSent.set(1);
                                                    ackCompleted.set(false);
                                                    emitter.onComplete();
                                                    //emitter.onNext(mData);
                                                    //writeNextBatch.run();
                                                    Log.d(TAG, "compelete: " + bootLoaderPacketContent);
                                                    setLogText("onCharacteristicWrite compelete: " + bootLoaderPacketContent);
                                                } else {
                                                    writeCompleted.set(true);
                                                }
                                            } else {
                                                int end = packetLen * batchesSent.get() > bootLoaderContentData.length ? bootLoaderContentData.length : packetLen * batchesSent.get();
                                                mData = Arrays.copyOfRange(bootLoaderContentData, (batchesSent.get() - 1) * packetLen, end);
                                                characteristic.setValue(mData);
                                                wdxcWdxsFtd.setWriteType(writeType);
                                                writeNextBatch.run();
                                                emitter.onNext(mData);
                                            }
                                        }
                                    } else if (batchesSent.get() == totalPacket + 1) {
                                        if (ackCompleted.get()) {
                                            batchesSent.set(1);
                                            ackCompleted.set(false);
                                            emitter.onComplete();
                                            //emitter.onNext(mData);
                                            //writeNextBatch.run();
                                            Log.d(TAG, "compelete: " + totalPacket);
                                            setLogText("onCharacteristicWrite compelete: " + totalPacket);
                                        } else {
                                            writeCompleted.set(true);
                                        }
                                    } else {
                                        int end = packetLen * batchesSent.get() > bytesSend.length ? bytesSend.length : packetLen * batchesSent.get();
                                        mData = Arrays.copyOfRange(bytesSend, (batchesSent.get() - 1) * packetLen, end);
                                        characteristic.setValue(mData);
                                        wdxcWdxsFtd.setWriteType(writeType);
                                        writeNextBatch.run();
                                        emitter.onNext(mData);
                                    }
                                }

                                @Override
                                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                                    final byte[] bytes = characteristic.getValue();
                                    Log.i("ACK", Arrays.toString(bytes) + "/" + bytes.length + "/" + System.identityHashCode(bytes));
                                    //characteristic.setValue(mData);
                                    if (writeCompleted.get()) {
                                        batchesSent.set(0);
                                        writeCompleted.set(false);
                                        emitter.onComplete();
                                        //emitter.onNext(mData);
                                        //writeNextBatch.run();
                                        Log.d(TAG, "compelete dd: " + totalPacket);
                                        setLogText("onCharacteristicChanged compelete dd: " + totalPacket);
                                    } else {
                                        ackCompleted.set(true);
                                    }
                                }

                            };

                            rxBleGattCallback.setNativeCallback(bluetoothGattCallback);

                            Log.i("SEND", String.valueOf(batchesSent.get()));
                            if (!bluetoothGatt.writeCharacteristic(wdxcWdxsFtd)) {
                                emitter.onError(new BleGattCannotStartException(bluetoothGatt, BleGattOperationType.CHARACTERISTIC_WRITE));
                            } else {
                                //batchesSent.incrementAndGet();
                            }
                        }
                        //Emitter.BackpressureMode.NONE
                )))
                .observeOn(Schedulers.io())
                .subscribe(
                        new Consumer<Object>() {
                            @Override
                            public void accept(Object object) throws Exception {
                                //Log.d(TAG, "Consumer<Object>: " + object.toString().length());
                            }
                        },
                        /*new Consumer<byte[]>() {
                            @Override
                            public void accept(byte[] bytes) throws Exception {
                                long oldSize = bytesSending;
                                long newSize = oldSize + bytes.length;
                                Log.d(TAG,"onNext sent oldSize " + oldSize + " newSize " + newSize);
                                //int progress = (int)(wdxcCb.mOTATxCount * 1.0/wdxcCb.mFileSize * 100);
                                bytesSending = newSize;
                                if (bytesSending <= bytes.length) {
                                    Log.d(TAG,"first show speed");
                                    mHandler.sendEmptyMessage(UPDATE_SPEED);
                                }
                            }
                        },*/
                        new Consumer<Throwable>() {//相当于onError
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                Log.e(TAG, "onError");
                                Log.e(TAG, throwable.toString());
                                setLogText("onError: " + throwable.toString());
                                throwable.printStackTrace();
                                mFileTransferStatus = TRANSFER_ERROR;
                                mHandler.sendEmptyMessage(UPDATE_UPLOAD_BTN);
                                mHandler.removeCallbacks(task);
                                //Toast.makeText(mMainActivity, "Upload error！" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }, new Action() {//相当于onComplete，注意这里是Action
                            @Override
                            public void run() throws Exception {
                                setLogText("sendPacketAsync finish!");
                                long finishTime = System.currentTimeMillis();
                                Log.d(TAG, "onCompleted finishTime " + finishTime + " bytesSend: " + bytesSend);
                                String speed = SpeedUtil.calculateSpeed(startTime, finishTime, bytesSend);
                                Log.d(TAG, "onCompleted speed " + speed);
                                mSpeed = speed;
                                //mTextSpeedView.setText(speed);
                                mHandler.sendEmptyMessage(UPDATE_SPEED);
                                mHandler.removeCallbacks(task);
                                /*if (mPart.getType() == mPartitionTypeMap.getType("BOOT") && mPartErase != null){
                                    WdxcFtcSendEraseReq(1, (int)mPartErase.getOffset(), (int)Math.abs(mPart.getOffset()-mPartErase.getOffset()));
                                }*/
                            }
                        }
                );
    }

    private void onWriteSuccess() {
        //noinspection ConstantConditions

    }

    private byte[] crcTable(byte[] table) {
        int len = table.length / 32 * 34;
        byte[] crctable = new byte[len];
        for (int i = 0; i < table.length / 32; i++) {
            byte[] temp = new byte[32];
            System.arraycopy(table, i * 32, temp, 0, 32);
            System.arraycopy(temp, 0, crctable, i * 34, 32);
            int crc = Utils.crc16_calculate(temp);
            byte[] crcshort = new byte[2];
            LittleEndian.fillByteArrayShort(crcshort, 0, crc);
            System.arraycopy(crcshort, 0, crctable, (i + 1) * 32 + i * 2, 2);
        }
        return crctable;
    }
}
