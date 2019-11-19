package com.actions.voicebletest.dfu;

/**
 * Created by zhongchangwen on 2017/7/3.
 */

public class DfuData {

    public static final int WSF_EFS_MAX_FILES            =   6;
    public static final int WSF_EFS_INVALID_HANDLE       =   0xFFFF;        /*! Invalid Handle */
    public static final int WSF_EFS_NAME_LEN             =   16;            /* File name length in bytes */
    public static final int WSF_EFS_VERSION_LEN          =   16;            /* File version length in bytes */

    /*! WDXS File List Configuration */
    public static final int WDXS_FLIST_HANDLE            =   0 ;            /*! File List handle */
    public static final int WDXS_FLIST_FORMAT_VER        =   1 ;            /*! File List version */
    public static final int WDXS_FLIST_HDR_SIZE          =   7 ;            /*! File List header length */
    public static final int WDXS_FLIST_RECORD_SIZE       =   40 ;           /*! File List record length */
    public static final int WDXS_FLIST_MAX_LEN           =   (WDXS_FLIST_HDR_SIZE + (WDXS_FLIST_RECORD_SIZE * (WSF_EFS_MAX_FILES-1)));

    /*! File transfer control characteristic operations */
    public static final int WDXS_FTC_OP_NONE = 0;
    public static final int WDXS_FTC_OP_GET_REQ = 1;
    public static final int WDXS_FTC_OP_GET_RSP = 2;
    public static final int WDXS_FTC_OP_PUT_REQ = 3;
    public static final int WDXS_FTC_OP_PUT_RSP = 4;
    public static final int WDXS_FTC_OP_ERASE_REQ = 5;
    public static final int WDXS_FTC_OP_ERASE_RSP = 6;
    public static final int WDXS_FTC_OP_VERIFY_REQ = 7;
    public static final int WDXS_FTC_OP_VERIFY_RSP = 8;
    public static final int WDXS_FTC_OP_ABORT = 9;
    public static final int WDXS_FTC_OP_EOF = 10;
    public static final int WDXS_FTC_OP_PACKET_RECEIVED = 11;
    public static final int WDXS_FTC_OP_RESET = 12;
    public static final int WDXS_FTC_OP_GET_VERSION_REQ = 13;
    public static final int WDXS_FTC_OP_GET_VERSION_RSP = 14;

    /* WDXS File Transfer Control Command Message Lengths */
    public static final int WDXS_FTC_ABORT_LEN     =        3;
    public static final int WDXS_FTC_ERASE_LEN     =        3;
    public static final int WDXS_FTC_GET_VERSION_LEN =      3;
    public static final int WDXS_FTC_SYSTEM_RESET_LEN =      3;
    public static final int WDXS_FTC_VERIFY_LEN    =        15;
    public static final int WDXS_FTC_PUT_LEN       =        16;
    public static final int WDXS_FTC_GET_LEN       =        12;
    public static final int WDXS_DC_ID_CONN_UPDATE_LEN       =        10;

    /*! Device configuration characteristic message header length */
    public static final int WDXS_DC_HDR_LEN         =        2;
    /*! Device configuration characteristic operations */
    public static final int WDXS_DC_OP_GET                 = 0x01;         /*! Get a parameter value */
    public static final int WDXS_DC_OP_SET                 = 0x02;         /*! Set a parameter value */
    public static final int WDXS_DC_OP_UPDATE              = 0x03;         /*! Send an update of a parameter value */

    /*! Device control characteristic parameter IDs */
    public static final int WDXS_DC_ID_CONN_UPDATE_REQ    =  0x01;         /*! Connection Parameter Update Request */
    public static final int WDXS_DC_ID_CONN_PARAM         =  0x02;         /*! Current Connection Parameters */
    public static final int WDXS_DC_ID_DISCONNECT_REQ     =  0x03;         /*! Disconnect Request */
    public static final int WDXS_DC_ID_CONN_SEC_LEVEL     =  0x04;         /*! Connection Security Level */
    public static final int WDXS_DC_ID_SECURITY_REQ       =  0x05;         /*! Security Request */
    public static final int WDXS_DC_ID_SERVICE_CHANGED    =  0x06;         /*! Service Changed */
    public static final int WDXS_DC_ID_DELETE_BONDS       =  0x07;         /*! Delete Bonds */
    public static final int WDXS_DC_ID_ATT_MTU            =  0x08;         /*! Current ATT MTU */
    public static final int WDXS_DC_ID_BATTERY_LEVEL      =  0x20;         /*! Battery level */
    public static final int WDXS_DC_ID_MODEL_NUMBER       =  0x21;         /*! Device Model */
    public static final int WDXS_DC_ID_FIRMWARE_REV       =  0x22;         /*! Device Firmware Revision */
    public static final int WDXS_DC_ID_ENTER_DIAGNOSTICS  =  0x23;         /*! Enter Diagnostic Mode */
    public static final int WDXS_DC_ID_DIAGNOSTICS_COMPLETE =0x24;         /*! Diagnostic Complete */
    public static final int WDXS_DC_ID_DISCONNECT_AND_RESET =0x25;         /*! Disconnect and Reset */

    /*! Device control parameter lengths */
    public static final int WDXS_DC_LEN_DATA_FORMAT        = 1 ;           /*! Data format */
    public static final int WDXS_DC_LEN_SEC_LEVEL          = 1 ;           /*! Security Level */
    public static final int WDXS_DC_LEN_ATT_MTU            = 2 ;           /*! ATT MTU */
    public static final int WDXS_DC_LEN_BATTERY_LEVEL      = 1 ;           /*! Battery level */
    public static final int WDXS_DC_LEN_CONN_PARAM_REQ     = 8 ;           /*! Connection parameter request */
    public static final int WDXS_DC_LEN_CONN_PARAM         = 7 ;           /*! Current connection parameters */
    public static final int WDXS_DC_LEN_DIAG_COMPLETE      = 0 ;           /*! Diagnostic complete */
    public static final int WDXS_DC_LEN_DEVICE_MODEL       = 18;          /*! Device Model */
    public static final int WDXS_DC_LEN_FIRMWARE_REV       = 16;          /*! Firmware Revision */

    /*! Authentication characteristic operations */
    public static final int WDXS_AU_OP_START               = 0x01;        /*! Authentication start */
    public static final int WDXS_AU_OP_CHALLENGE           = 0x02;         /*! Authentication challenge */
    public static final int WDXS_AU_OP_REPLY               = 0x03;         /*! Authentication reply */

    /*! Proprietary ATT error codes */
    public static final int WDXS_APP_AUTH_REQUIRED        =  0x80;        /*! Application authentication required */
    public static final int WDXS_AU_ST_INVALID_MESSAGE    =  0x81;        /*! Authentication invalid message */
    public static final int WDXS_AU_ST_INVALID_STATE      =  0x82;        /*! Authentication invalid state */
    public static final int WDXS_AU_ST_AUTH_FAILED        =  0x83;        /*! Authentication failed */

    /*! Authentication characteristic authentication level  */
    public static final int WDXS_AU_LVL_NONE              =  0x00 ;       /*! None */
    public static final int WDXS_AU_LVL_USER              =  0x01;        /*! User level */
    public static final int WDXS_AU_LVL_MAINT             =  0x02;        /*! Maintenance level */
    public static final int WDXS_AU_LVL_DEBUG             =  0x03;        /*! Debug level */

    /*! Authenttication characteristic message parameter lengths */
    public static final int WDXS_AU_MSG_HDR_LEN           =  1 ;          /*! Message header length */
    public static final int WDXS_AU_PARAM_LEN_START       =  2  ;         /*! Authentication start */
    public static final int WDXS_AU_PARAM_LEN_CHALLENGE   =  16;          /*! Authentication challenge */
    public static final int WDXS_AU_PARAM_LEN_REPLY       =  8 ;          /*! Authentication reply */

    /*! Authenttication characteristic random number and key lengths */
    public static  final int WDXS_AU_RAND_LEN             =   16 ;         /*! Authentication Random challenge length (bytes)*/
    public static final int WDXS_AU_KEY_LEN              =   16;          /*! Authentication Key length (bytes) */
    public static final int WDXS_AU_HASH_LEN             =   8;           /*! Authentication Hash length (bytes) */

    public static final int WDXS_AU_STATE_UNAUTHORIZED   =   0x00 ;       /*! Authentication has not started */
    public static final int WDXS_AU_STATE_SEND_START     =   0x01 ;       /*! Authentication sending  start au op */
    public static final int WDXS_AU_STATE_SEND_REPLY     =   0x02 ;       /*! Authentication sending reply au op */
    public static final int WDXS_AU_STATE_AUTHORIZED     =   0x03 ;       /*! Authentication completed successfully */

    public DfuData(){

    }

    public static   byte[] getWdxcAuStartReq(){
        byte[] data = new byte[WDXS_AU_MSG_HDR_LEN + WDXS_AU_PARAM_LEN_START];
        data[0] = WDXS_AU_OP_START;                        /* opCode(1byte)         */
        data[1] = (byte)(WDXS_AU_LVL_USER & 0xFF);         /* authLevel(1byte)      */
        data[2] = (byte)((0 >> 8)& 0xFF);                  /* authMode(1byte)       */
        return data;
    }

    /* File attributes data type */
    public  static class wsfEsfAttributes_t
    {
        public byte[]  name = new byte[WSF_EFS_NAME_LEN];
        public byte[]  version = new byte[WSF_EFS_VERSION_LEN];
        public short 	permissions;
        public byte    type;
    }

    /* File Listing Information */
    public  static class wsfEfsFileInfo_t
    {
        public short     handle;
        public int       size;
        public wsfEsfAttributes_t attributes = new wsfEsfAttributes_t();
    }

    public static class wdxcCb_t{
        /* struct func*/
        public wdxcCb_t()
        {
            int i = 0;
            FileList = new wsfEfsFileInfo_t[WSF_EFS_MAX_FILES];
            for(i = 0; i<WSF_EFS_MAX_FILES;i++)
            {
                FileList[i] = new wsfEfsFileInfo_t();
            }
        }
        /* Operation in progress Control */
        public int fileHdl = WSF_EFS_INVALID_HANDLE;            /* File Handle */

        /* OTA File Control */
        public int mOTAHandle = WSF_EFS_INVALID_HANDLE;
        public int mOTATxCount = 0;
        public int mPacketNumber = 0;
        public int mTotalPackets = 0;
        public int mFileSize = 0;
        public boolean mStopSendingPacket = false;
        public boolean isLastPacket = false;

        /* File Listing Control */
        public wsfEfsFileInfo_t[] FileList;
        public int       maxFiles;           /* Size of pFileList in number of wsfEfsFileInfo_t objects */
        public int       fileCount;          /* Number of files on peer device */
        public int       fDlPos;             /* Position in the download of file information */

        public int authState = WDXS_AU_STATE_UNAUTHORIZED;                /* authentication protocol state */
    }
}
