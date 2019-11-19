package com.actions.voicebletest.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.actions.voicebletest.db.MessageReaderContract.MessageEntry;
/**
 * Created by chang on 2018/3/28.
 */

public class MessageReaderDbHelper extends SQLiteOpenHelper {
    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String BLOB_TYPE = " BLOB";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + MessageEntry.TABLE_NAME + " (" +
                    MessageEntry._ID + " INTEGER PRIMARY KEY," +
                    MessageEntry.COLUMN_MSG_TYPE + INTEGER_TYPE + COMMA_SEP +
                    MessageEntry.COLUMN_STATE + INTEGER_TYPE + COMMA_SEP +
                    MessageEntry.COLUMN_FROM_USER_NAME + TEXT_TYPE + COMMA_SEP +
                    MessageEntry.COLUMN_FROM_USER_AVATAR + TEXT_TYPE + COMMA_SEP +
                    MessageEntry.COLUMN_TO_USER_NAME + TEXT_TYPE + COMMA_SEP +
                    MessageEntry.COLUMN_TO_USER_AVATAR + TEXT_TYPE + COMMA_SEP +
                    MessageEntry.COLUMN_CONTENT + TEXT_TYPE + COMMA_SEP +
                    MessageEntry.COLUMN_IS_SEND + INTEGER_TYPE + COMMA_SEP +
                    MessageEntry.COLUMN_SEND_SUCCESS + INTEGER_TYPE + COMMA_SEP +
                    MessageEntry.COLUMN_TIME + INTEGER_TYPE + COMMA_SEP +
                    MessageEntry.COLUMN_SECONDS + INTEGER_TYPE + COMMA_SEP +
                    MessageEntry.COLUMN_VOICE_CONTENT + TEXT_TYPE + " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + MessageEntry.TABLE_NAME;
    private static final String SQL_DELETE_MESSAGE =
            "DELETE FROM " + MessageEntry.TABLE_NAME + " WHERE _ID IN (%d)";
    private static final String SQL_DELETE_ALL_MESSAGE =
            "DELETE FROM " + MessageEntry.TABLE_NAME;
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "MessageReader.db";

    public MessageReaderDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void deleteRow(long id){
        String sql = String.format(SQL_DELETE_MESSAGE, id);
        getWritableDatabase().execSQL(sql);
    }

    public void deleteAllRow(){
        String sql = String.format(SQL_DELETE_ALL_MESSAGE);
        getWritableDatabase().execSQL(sql);
    }

}
