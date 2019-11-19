package com.actions.voicebletest.db;

import android.provider.BaseColumns;

/**
 * Created by chang on 2018/3/28.
 */

public class MessageReaderContract {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private MessageReaderContract() {}

    /* Inner class that defines the table contents */
    public static class MessageEntry implements BaseColumns {
        public static final String TABLE_NAME = "message";
        public static final String COLUMN_MSG_TYPE = "type";
        public static final String COLUMN_STATE = "state";
        public static final String COLUMN_FROM_USER_NAME = "fromUserName";
        public static final String COLUMN_FROM_USER_AVATAR = "fromUserAvatar";
        public static final String COLUMN_TO_USER_NAME = "toUserName";
        public static final String COLUMN_TO_USER_AVATAR = "toUserAvatar";
        public static final String COLUMN_CONTENT = "content";
        public static final String COLUMN_IS_SEND = "isSend";
        public static final String COLUMN_SEND_SUCCESS = "sendSuccess";
        public static final String COLUMN_TIME = "dateTime";
        public static final String COLUMN_SECONDS = "seconds";
        public static final String COLUMN_VOICE_CONTENT = "voiceContent";

    }
}
