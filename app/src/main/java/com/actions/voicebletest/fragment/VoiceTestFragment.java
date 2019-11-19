package com.actions.voicebletest.fragment;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.actions.voicebletest.PcmPalyer.AudioParam;
import com.actions.voicebletest.PcmPalyer.AudioPlayer;
import com.actions.voicebletest.PcmPalyer.PlayState;
import com.actions.voicebletest.R;
import com.actions.voicebletest.adapter.ChatAdapter;
import com.actions.voicebletest.baidu.AlarmListener;
import com.actions.voicebletest.baidu.recognization.InFileStream;
import com.actions.voicebletest.baidu.recognization.RecogEventAdapter;
import com.actions.voicebletest.bean.Message;
import com.actions.voicebletest.db.MessageReaderContract;
import com.actions.voicebletest.db.MessageReaderDbHelper;
import com.actions.voicebletest.main.MainActivity;
import com.actions.voicebletest.service.PlayService;
import com.actions.voicebletest.service.UpdateUiListener;
import com.actions.voicebletest.utils.AppCache;
import com.actions.voicebletest.utils.DeleteFileUtils;
import com.actions.voicebletest.utils.Logger;
import com.actions.voicebletest.utils.Utils;
import com.actions.voicebletest.widget.PullToRefreshLayout;
import com.actions.voicebletest.widget.PullableListView;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * A simple {@link Fragment} subclass.
 * This is just a demo fragment with a long scrollable view of editTexts. Don't see this as a reference for anything
 */
public class VoiceTestFragment extends Fragment {
    private static final String TAG = VoiceTestFragment.class.getSimpleName();
    public static final String BROADCAST_RECEIVE = "VoiceTestFragment.broadcastreceiver";
    private static final int WAITING_DIALOG_DISMISS = 1;
    private static final int SHOW_RECORD = 2;
    private static final int SHOW_RECORD_MORE = 3;
    private static final int SCROLL_TO = 4;

    private MainActivity mMainActivity = null;
    private PullableListView listView = null;
    private PullToRefreshLayout ptrl;

    List<Message> datas = new ArrayList<Message>();
    private ChatAdapter adapter = null;
    private PlayServiceConnection mPlayServiceConnection;
    MessageReaderDbHelper mDbHelper = null;

    private boolean enableOffline = true; // 测试离线命令词，需要改成true
    private EventManager asr;
    private String runningTestName = "";
    private int index = 0;
    private int retry = 1;
    Lock lock = new ReentrantLock();
    private int mPosition = 0;

    private int mFrequent = 16000;
    private ReentrantLock mLock = new ReentrantLock();
    private int mTotalCount = 0;
    private Cursor mCursor = null;
    private int mScrollTo = 0;

    private AudioPlayer mAudioPlayer;
    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BROADCAST_RECEIVE)) {
                String msg = intent.getStringExtra("notify");
                Log.d(TAG, "notify: " + msg);
                if (msg.equals("init")) {
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            initRecog();
                            initAudioSetting();
                        }
                    };
                    new Thread(runnable).start();
                } else if (msg.equals("addMessage")){
                    String path = intent.getStringExtra("path");
                    long seconds = intent.getLongExtra("seconds", 0);
                    Log.d(TAG, "addMessage: " + path);
                    Message message = new Message(
                            com.actions.voicebletest.bean.Message.MSG_TYPE_VOICE,
                            com.actions.voicebletest.bean.Message.MSG_STATE_SUCCESS,
                            "Tom", "avatar", "Jerry", "avatar",
                            null,
                            false, false,
                            new Date(System.currentTimeMillis()));
                    message.setVoicePath(path);
                    message.setSeconds(seconds);
                    datas.add(message);
                    mTotalCount++;
                    adapter.refresh(datas);
                    listView.setSelection(listView.getBottom());
                    insertValue(message);
                } else if (msg.equals("addTextMessage")){
                    String text = intent.getStringExtra("text");
                    Log.d(TAG, "addMessage: " + text);
                    Message message = new Message(
                            Message.MSG_TYPE_TEXT,
                            com.actions.voicebletest.bean.Message.MSG_STATE_SUCCESS,
                            "Tom", "avatar", "Jerry", "avatar",
                            text,
                            false, false,
                            new Date(System.currentTimeMillis()));
                    datas.add(message);
                    mTotalCount++;
                    adapter.refresh(datas);
                    listView.setSelection(listView.getBottom());
                    insertValue(message);
                }
                else if (msg.equals("showAudioSetting")){
                    mFrequent = intent.getIntExtra("frequent", 16000);
                    initAudioSetting();
                }
                else if (msg.equals("clearData")){
                    mMainActivity.showWaitingDialog(R.string.clearing_data, R.string.please_wait);
                    clearData();
                }
            }
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case WAITING_DIALOG_DISMISS:
                    if (mMainActivity != null)
                        mMainActivity.dismissWaitingDialog();
                    if (adapter != null)
                        adapter.refresh(datas);
                    break;
                case SHOW_RECORD:
                    if (mMainActivity != null)
                        mMainActivity.dismissWaitingDialog();
                    adapter = new ChatAdapter(getContext(), datas, getOnChatItemClickListener());
                    listView.setAdapter(adapter);
                    adapter.refresh(datas);
                    listView.setSelection(mScrollTo-1);
                    break;
                case SHOW_RECORD_MORE:
                    adapter.refresh(datas);
                    adapter.notifyDataSetChanged();
                    break;
                case SCROLL_TO:
                    if (mScrollTo > 0) {
                        listView.setSelection(mScrollTo - 1);
                    } else {
                       Utils.showToast(getString(R.string.nomore_data), mMainActivity);
                    }
                    ptrl.refreshFinish(PullToRefreshLayout.SUCCEED);
                    break;

            }
        }
    };

    private static VoiceTestFragment instance = null;

    public VoiceTestFragment() {
        // Required empty public constructor
    }

    public static VoiceTestFragment getInstance() {
        if (instance == null) {
            instance = new VoiceTestFragment();
        }

        return instance;
    }

    protected Handler handler;

    private UpdateUiListener updateUiListener = new UpdateUiListener() {
        @Override
        public void startPlaying(int position) {
            datas.get(position).setPlaying(true);
            adapter.refresh(datas);
        }

        @Override
        public void finishPlaying(int position) {
            datas.get(position).setPlaying(false);
            adapter.refresh(datas);
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Intent intent = new Intent();
        intent.setClass(getContext(), PlayService.class);
        mPlayServiceConnection = new PlayServiceConnection();
        mDbHelper = new MessageReaderDbHelper(getActivity());
        mMainActivity = (MainActivity)getActivity();

        InFileStream.setContext(getActivity());
        handler = new Handler() {

            /*
             * @param msg
             */
            @Override
            public void handleMessage(android.os.Message msg) {
                super.handleMessage(msg);
                handleMsg(msg);
            }

        };
        initBroadcastReceiver();
    }

    void test(){
        Message message = new Message(
                com.actions.voicebletest.bean.Message.MSG_TYPE_VOICE,
                com.actions.voicebletest.bean.Message.MSG_STATE_SUCCESS,
                "Tom", "avatar", "Jerry", "avatar",
                "/storage/emulated/0/com.actions.voicebletest/adcInput.pcm",
                false, false,
                new Date(System.currentTimeMillis()));
        insertValue(message);
    }

    private void initBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BROADCAST_RECEIVE);
        getActivity().registerReceiver(receiver, filter);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public class LoadMoreListener implements PullToRefreshLayout.OnRefreshListener
    {

        @Override
        public void onRefresh(final PullToRefreshLayout pullToRefreshLayout)
        {
            // 下拉刷新操作
            mScrollTo = 0;
            if (mMainActivity == null)
                return;
            mMainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLock.lock();
                    mScrollTo = readDataMore();
                    mHandler.sendEmptyMessage(SCROLL_TO);
                    mLock.unlock();
                }
            });
        }

        @Override
        public void onLoadMore(final PullToRefreshLayout pullToRefreshLayout)
        {
            // 加载操作
            new Handler()
            {
                @Override
                public void handleMessage(android.os.Message msg)
                {
                    // 千万别忘了告诉控件加载完毕了哦！
                    pullToRefreshLayout.loadmoreFinish(PullToRefreshLayout.SUCCEED);
                }
            }.sendEmptyMessageDelayed(0, 5000);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        // don't look at this layout it's just a listView to show how to handle the keyboard
        View view = inflater.inflate(R.layout.fragment_voice_test, container, false);

        listView = view.findViewById(R.id.list_view);
        ptrl = ((PullToRefreshLayout) view.findViewById(R.id.refresh_view));
        ptrl.setOnRefreshListener(new LoadMoreListener());
        listView.setSelector(android.R.color.transparent);

        //initListView();
        if (mMainActivity != null)
            mMainActivity.showWaitingDialog(R.string.loading_title, R.string.loading_text);
        new Thread(new Runnable() {
            @Override
            public void run() {
                readDataBase();
            }
        }).start();

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //add the values which need to be saved from the drawer to the bundle
        super.onSaveInstanceState(outState);
    }

    public void initAudioSetting() {
        if (mAudioPlayer != null)
            mAudioPlayer.release();
        mAudioPlayer = new AudioPlayer(handler);
        // 获取音频参数
        AudioParam audioParam = getAudioParam();
        mAudioPlayer.setAudioParam(audioParam);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        datas.clear();
        adapter.refresh(datas);
        if (mAudioPlayer != null) {
            mAudioPlayer.release();
        }
        getActivity().unregisterReceiver(receiver);
    }

    private void play(String file) {
        mAudioPlayer.reset();
        // 获取音频数据
        byte[] data;
        try {
            data = getPCMData(file);
        }catch (OutOfMemoryError error){
            error.printStackTrace();
            Utils.showToast(getString(R.string.pcm_too_big), mMainActivity);
            return;
        }
        mAudioPlayer.setDataSource(data);

        // 音频源就绪
        mAudioPlayer.prepare();

        if (data == null) {
            Log.d(TAG, file + "：该路径下不存在文件！");
            Utils.showToast(getString(R.string.pcm_null), mMainActivity);
        } else {
            mAudioPlayer.play();
        }
    }


    protected void initRecog() {
        Logger.setHandler(handler);
        asr = EventManagerFactory.create(getActivity(), "asr");
        AlarmListener listener = new AlarmListener(handler);
        asr.registerListener(new RecogEventAdapter(listener));
        if (enableOffline) {
            loadOfflineEngine(); // 测试离线命令词请开启, 测试 ASR_OFFLINE_ENGINE_GRAMMER_FILE_PATH 参数时开启
        }
    }

    private boolean begin(String file) {
        index++;
        runningTestName = file;
        Map<String, Object> defaultParams = new HashMap<String, Object>();
        defaultParams.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);
        defaultParams.put(SpeechConstant.VAD,"touch");
        String filename = file;//"com/baidu/android/voicedemo/test/" + testCase.getFileName();
        if (filename == null)
            return false;

        InputStream is = null;//this.getClass().getClassLoader().getResourceAsStream(filename);

        try {
            File f = new File(filename);
            is = new FileInputStream(f);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (is == null) {
            Log.d(TAG, "filename:" + filename + " does  not exist");
            return false;
            //begin(file);
        } else {
            InFileStream.reset();
            InFileStream.setInputStream(is);
            defaultParams.put(SpeechConstant.IN_FILE,
                    "#com.actions.voicebletest.baidu.recognization.InFileStream.create16kStream()");
            Log.i(TAG, "file:" + filename);
            String json = new JSONObject(defaultParams).toString();
            Log.i(TAG, runningTestName + " ," + json);
            asr.send(SpeechConstant.ASR_START, json, null, 0, 0);
        }
        return true;
    }

    protected void handleMsg(android.os.Message msg) {
        int what = msg.what;
        switch (what) {
            case 901:
                if (mMainActivity != null)
                    mMainActivity.setRecognitionText(msg.obj.toString());
                msg.obj = runningTestName + ": success : " + msg.obj.toString();
                Log.d(TAG, msg.obj.toString());
                break;
            case 900:
                msg.obj = runningTestName + " : finish and exit\n";
                Log.d(TAG, msg.obj.toString());
                if (index < retry) {
                    boolean success = begin(runningTestName);
                    if (!success) {

                    }
                } else {
                    index = 0;
                }
                break;
            case -801:
                if (mMainActivity != null)
                    mMainActivity.setRecognitionText("Error:\n"+msg.obj.toString());
                msg.obj = runningTestName + " error:" + msg.obj.toString();
                Log.d(TAG, msg.obj.toString());
                // index= 9999999; 立即停止
                break;
        }
        int obj = msg.arg1;
        Log.d(TAG, "playing State: " + obj);
        switch (obj){
            case PlayState.MPS_PLAYING:
                if (datas.size() <= mPosition)
                    break;
                datas.get(mPosition).setPlaying(true);
                adapter.notifyDataSetChanged();
                break;
            case PlayState.MPS_UNINIT:
            case PlayState.MPS_PREPARE:
            case PlayState.MPS_PAUSE:
                if (datas.size() <= mPosition)
                    break;
                datas.get(mPosition).setPlaying(false);
                adapter.notifyDataSetChanged();
                break;
        }
    }

    private void loadOfflineEngine() {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put(SpeechConstant.DECODER, 2);
        params.put(SpeechConstant.ASR_OFFLINE_ENGINE_GRAMMER_FILE_PATH, "assets://baidu_speech_grammar.bsg");
        params.put(SpeechConstant.VAD,"touch");
        params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);
        asr.send(SpeechConstant.ASR_KWS_LOAD_ENGINE, new JSONObject(params).toString(), null, 0, 0);
    }

    private void initListView() {
        byte[] emoji = new byte[]{
                (byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x81
        };
        Message message = new Message(Message.MSG_TYPE_TEXT,
                Message.MSG_STATE_SUCCESS, "\ue415", "avatar", "Jerry", "avatar",
                new String(emoji), false, true, new Date(System.currentTimeMillis()
                - (1000 * 60 * 60 * 24) * 8));
        Message message1 = new Message(Message.MSG_TYPE_TEXT,
                Message.MSG_STATE_SUCCESS, "Tom", "avatar", "Jerry", "avatar",
                "以后的版本支持链接高亮喔:http://www.kymjs.com支持http、https、svn、ftp开头的链接",
                false, true, new Date(System.currentTimeMillis() - (1000 * 60 * 60 * 24) * 8));
        Message message2 = new Message(Message.MSG_TYPE_VOICE,
                Message.MSG_STATE_SUCCESS, "Tom", "avatar", "Jerry", "avatar",
                "http://static.oschina.net/uploads/space/2015/0611/103706_rpPc_1157342.png",
                false, false, new Date(
                System.currentTimeMillis() - (1000 * 60 * 60 * 24) * 7));
        message2.setSeconds(9);
        Message message3 = new Message(Message.MSG_TYPE_VOICE,
                Message.MSG_STATE_SUCCESS, "Tom", "avatar", "Jerry", "avatar",
                "http://static.oschina.net/uploads/space/2015/0611/103706_rpPc_1157342.png",
                false, false, new Date(
                System.currentTimeMillis() - (1000 * 60 * 60 * 24) * 7));
        message3.setSeconds(30);

        datas.add(message2);
        datas.add(message3);
        datas.add(message2);
        datas.add(message3);
        datas.add(message2);
        datas.add(message3);
        datas.add(message2);
        datas.add(message3);

        adapter = new ChatAdapter(getContext(), datas, getOnChatItemClickListener());
        listView.setAdapter(adapter);
        adapter.refresh(datas);
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
        long newRowId = db.insert(MessageReaderContract.MessageEntry.TABLE_NAME, null, values);
        Log.d(TAG, "insertValue: " + newRowId);
    }

    private void clearData(){
        Runnable clear = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "clearData");
                if (datas.size() > 0) {
                    datas.clear();
                    String dir = mMainActivity.getCacheDirPath() + "/pcm";
                    DeleteFileUtils.delete(dir);
                    mMainActivity.makePcmDirectory();
                    mDbHelper.deleteAllRow();
                    mHandler.sendEmptyMessage(WAITING_DIALOG_DISMISS);
                }
            }
        };
        new Thread(clear).start();

        /*
        Cursor cursor = readAllData();
        if (cursor.getCount() > 0) {
            Log.d(TAG, "readdatabase: getCount :" + cursor.getCount());
            cursor.moveToFirst();
            do {
                long itemId = cursor.getLong(
                        cursor.getColumnIndexOrThrow(MessageReaderContract.MessageEntry._ID)
                );
                String content = cursor.getString(cursor.getColumnIndexOrThrow(MessageReaderContract.MessageEntry.COLUMN_CONTENT));
                if (cursor.getInt(cursor.getColumnIndexOrThrow(MessageReaderContract.MessageEntry.COLUMN_MSG_TYPE)) == Message.MSG_TYPE_VOICE) {
                    content = cursor.getString(cursor.getColumnIndexOrThrow(MessageReaderContract.MessageEntry.COLUMN_VOICE_CONTENT));
                    DeleteFileUtils.delete(content);
                }
                Log.d(TAG, "delete row id " + itemId);
                mDbHelper.deleteRow(itemId);
            } while (cursor.moveToNext());
        } else {
            Log.d(TAG, "clearData: no item");
        }*/
    }

    private Cursor readAllData(){
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

// Define a projection that specifies which columns from the database
// you will actually use after this query.
        String[] projection = {
                MessageReaderContract.MessageEntry._ID,
                MessageReaderContract.MessageEntry.COLUMN_MSG_TYPE,
                MessageReaderContract.MessageEntry.COLUMN_STATE,
                MessageReaderContract.MessageEntry.COLUMN_CONTENT,
                MessageReaderContract.MessageEntry.COLUMN_TIME,
                MessageReaderContract.MessageEntry.COLUMN_SECONDS,
                MessageReaderContract.MessageEntry.COLUMN_IS_SEND,
                MessageReaderContract.MessageEntry.COLUMN_VOICE_CONTENT
        };

        // Filter results WHERE "title" = 'My Title'
        String selection = MessageReaderContract.MessageEntry.COLUMN_IS_SEND + " = ?";
        String[] selectionArgs = {"0"};

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                MessageReaderContract.MessageEntry.COLUMN_TIME + " ASC";

        Cursor cursor = db.query(
                MessageReaderContract.MessageEntry.TABLE_NAME,                     // The table to query
                projection,                               // The columns to return
                null,                                // The columns for the WHERE clause
                null,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );
        return cursor;
    }

    private void readDataBase() {
        Cursor cursor = readAllData();
        mTotalCount = cursor.getCount();
        mCursor = cursor;

        if (datas.size() > 0) {
            datas.clear();
            if (adapter != null)
                adapter.refresh(datas);
        }
        int index = 0;
        if (cursor.getCount() > 100){
            index = cursor.getCount() - 100;
            mScrollTo = 100;
        }else {
            mScrollTo = cursor.getCount();
        }
        if (cursor.getCount() > 0) {
            Log.d(TAG, "readdatabase: getCount :" + cursor.getCount());
            //cursor.moveToFirst();
            cursor.moveToPosition(index);
            do {
                long itemId = cursor.getLong(
                        cursor.getColumnIndexOrThrow(MessageReaderContract.MessageEntry.COLUMN_TIME)
                );
                boolean isSend = false;
                if (cursor.getInt(cursor.getColumnIndexOrThrow(MessageReaderContract.MessageEntry.COLUMN_IS_SEND)) == 0) {
                    isSend = false;
                } else {
                    isSend = true;
                }
                String content = cursor.getString(cursor.getColumnIndexOrThrow(MessageReaderContract.MessageEntry.COLUMN_CONTENT));
                long date = cursor.getLong(cursor.getColumnIndexOrThrow(MessageReaderContract.MessageEntry.COLUMN_TIME));
                long seconds = cursor.getLong(cursor.getColumnIndexOrThrow(MessageReaderContract.MessageEntry.COLUMN_SECONDS));
                Message message = null;
                if (cursor.getInt(cursor.getColumnIndexOrThrow(MessageReaderContract.MessageEntry.COLUMN_MSG_TYPE)) == Message.MSG_TYPE_VOICE) {
                    content = cursor.getString(cursor.getColumnIndexOrThrow(MessageReaderContract.MessageEntry.COLUMN_VOICE_CONTENT));
                    message = new Message(
                            Message.MSG_TYPE_VOICE,
                            Message.MSG_STATE_SUCCESS,
                            "Tom", "avatar", "Jerry", "avatar",
                            content,
                            isSend, false,
                            new Date(date));
                    message.setSeconds(seconds);
                    message.setVoicePath(content);
                } else {
                    message = new Message(
                            Message.MSG_TYPE_TEXT,
                            Message.MSG_STATE_SUCCESS,
                            "Tom", "avatar", "Jerry", "avatar",
                            content,
                            isSend, false,
                            new Date(date));
                }
                Log.d(TAG, "readdatabase: id:" + itemId);
                datas.add(message);
            } while (cursor.moveToNext());
        } else {
            Log.d(TAG, "readdatabase: no item");
        }
        mHandler.sendEmptyMessage(SHOW_RECORD);
    }

    private int readDataMore() {
        Cursor cursor = mCursor;

        int add = mTotalCount - cursor.getCount();
        int count = datas.size() - add;
        int readCount = cursor.getCount() - count ;
        final int addCount = 20;
        if (readCount <= 0)
            return 0;
        else if (readCount > addCount){
            readCount = addCount;
        }
        int index = 0;
        index = cursor.getCount() - datas.size() - readCount;

        if (cursor.getCount() > 0) {
            Log.d(TAG, "readdatabase: getCount :" + cursor.getCount());
            //cursor.moveToFirst();
            cursor.moveToPosition(index);
            int i = 0;
            do {
                long itemId = cursor.getLong(
                        cursor.getColumnIndexOrThrow(MessageReaderContract.MessageEntry.COLUMN_TIME)
                );
                boolean isSend = false;
                if (cursor.getInt(cursor.getColumnIndexOrThrow(MessageReaderContract.MessageEntry.COLUMN_IS_SEND)) == 0) {
                    isSend = false;
                } else {
                    isSend = true;
                }
                String content = cursor.getString(cursor.getColumnIndexOrThrow(MessageReaderContract.MessageEntry.COLUMN_CONTENT));
                long date = cursor.getLong(cursor.getColumnIndexOrThrow(MessageReaderContract.MessageEntry.COLUMN_TIME));
                long seconds = cursor.getLong(cursor.getColumnIndexOrThrow(MessageReaderContract.MessageEntry.COLUMN_SECONDS));
                Message message = null;
                if (cursor.getInt(cursor.getColumnIndexOrThrow(MessageReaderContract.MessageEntry.COLUMN_MSG_TYPE)) == Message.MSG_TYPE_VOICE) {
                    content = cursor.getString(cursor.getColumnIndexOrThrow(MessageReaderContract.MessageEntry.COLUMN_VOICE_CONTENT));
                    message = new Message(
                            Message.MSG_TYPE_VOICE,
                            Message.MSG_STATE_SUCCESS,
                            "Tom", "avatar", "Jerry", "avatar",
                            content,
                            isSend, false,
                            new Date(date));
                    message.setSeconds(seconds);
                    message.setVoicePath(content);
                } else {
                    message = new Message(
                            Message.MSG_TYPE_TEXT,
                            Message.MSG_STATE_SUCCESS,
                            "Tom", "avatar", "Jerry", "avatar",
                            content,
                            isSend, false,
                            new Date(date));
                }
                Log.d(TAG, "readdatabase: id:" + itemId);
                datas.add(0, message);
                i++;
            } while (cursor.moveToNext() && i < addCount);
        } else {
            Log.d(TAG, "readdatabase: no item");
        }
        mHandler.sendEmptyMessage(SHOW_RECORD_MORE);
        return readCount;
    }

    /**
     * @return 聊天列表内存点击事件监听器
     */
    private OnChatItemClickListener getOnChatItemClickListener() {
        return new OnChatItemClickListener() {
            @Override
            public void onPhotoClick(int position) {

            }

            @Override
            public void onTextClick(int position) {
            }

            @Override
            public void onFaceClick(int position) {
            }

            @Override
            public void onVoiceClick(int position, ImageView imageView) {
                if (mMainActivity != null)
                    mMainActivity.setLogText( "onVoiceClick: " + position + " prePos: "+ mPosition);
                if (position >= datas.size())
                    return;
                Log.d(TAG, "onVoiceClick: " + position + " prePos: "+ mPosition + " path: "+datas.get(position).getContent());
                if (mPosition < datas.size())
                    datas.get(mPosition).setPlaying(false);

                mPosition = position;
                String conten = datas.get(position).getVoicePath();
                if (conten == null)
                    return;
                play(conten);
            }

            @Override
            public void onTranslateClick(int position){
                if (mMainActivity != null)
                    mMainActivity.setLogText( "onTranslateClick: " + position);
                if (position >= datas.size())
                    return;
                String conten = datas.get(position).getVoicePath();
                if (conten == null)
                    return;
                begin(conten);
                mMainActivity.showTextDialog();
            }
        };
    }

    private class PlayServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            final PlayService playService = ((PlayService.PlayBinder) service).getService();
            Log.d(TAG, "PlayServiceConnection onService Connected");
            playService.setListener(updateUiListener);
            AppCache.setPlayService(playService);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "PlayServiceConnection onService DisConnected");
        }
    }

    /*
     * 获得PCM音频数据参数
     */
    public AudioParam getAudioParam() {
        AudioParam audioParam = new AudioParam();
        audioParam.mFrequency = mFrequent;
        audioParam.mChannel = AudioFormat.CHANNEL_CONFIGURATION_MONO;
        audioParam.mSampBit = AudioFormat.ENCODING_PCM_16BIT;

        return audioParam;
    }

    public byte[] getPCMData(String filePath) {

        File file = new File(filePath);
        if (file == null) {
            return null;
        }

        FileInputStream inStream;
        try {
            inStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        byte[] data_pack = null;
        if (inStream != null) {
            long size = file.length();

            //ActivityManager.getLargeMemoryClass();
            data_pack = new byte[(int) size];
            try {
                inStream.read(data_pack);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            }

        }

        return data_pack;
    }

    /**
     * 聊天列表中对内容的点击事件监听
     */
    public interface OnChatItemClickListener {
        void onPhotoClick(int position);

        void onTextClick(int position);

        void onFaceClick(int position);

        void onVoiceClick(int position, ImageView view);

        void onTranslateClick(int position);
    }
}
