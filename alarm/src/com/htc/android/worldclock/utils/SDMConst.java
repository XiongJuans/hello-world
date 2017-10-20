package com.htc.android.worldclock.utils;

public class SDMConst {
    public static final int CONNECTION_TIMEOUT = 5000; // ms
    public static final int READ_TIMEOUT = 5000; // ms

    // public static final String SOUNDSET = "Soundset";
    // public static final String RINGTONE = "Ringtone";
    public static final String NOTIFICATION = "Notification";
    public static final String ALARM = "Alarm";
    public static final String SOUNDSET_TYPE = "Soudset_Type";

    public static final String SOUNDSET_ARG1 = "Soundset_Lists";
    public static final String SOUNDSET_ARG2 = "Soundset_List_";

    public static final String RINGTONE_ARG1 = "Ringtone_Lists";
    public static final String RINGTONE_ARG2 = "Ringtone_List_";

    public static final String NOTIFICATION_ARG1 = "Notification_Lists";
    public static final String NOTIFICATION_ARG2 = "Notification_List_";

    public static final String ALARM_ARG1 = "Alarm_Lists";
    public static final String ALARM_ARG2 = "Alarm_List_";

    // Source type id
    public static final int ID_SRC_T_UNKNOWN = 0;
    public static final int ID_SRC_T_DOWNLOAD = 1 << 0;
    public static final int ID_SRC_T_CUSTOM = 1 << 1;
    public static final int ID_SRC_T_SYSTEM = 1 << 2;

    // Reference type id
    public static final int ID_UNKNOWN = 0;
    public static final int ID_SOUNDSET = 1;
    public static final int ID_RINGTONE = 2;
    public static final int ID_NOTIFICATION = 3;
    public static final int ID_ALARM = 4;
    public static final int ID_CALENDAR = 5;
    public static final int ID_MSG = 6;
    public static final int ID_EMAIL = 7;
    public static final int ID_FS1 = 8;
    public static final int ID_FS2 = 9;

    public static final String REF_T_SOUNDSET = "Soundset";
    public static final String REF_T_RINGTONE = "S_Ringtone";
    public static final String REF_T_NOTIFICATION = "S_Notify";
    public static final String REF_T_ALARM = "S_Alarm";
    public static final String REF_T_CALENDAR = "Calendar";
    public static final String REF_T_MSG = "Message";
    public static final String REF_T_EMAIL = "EMail";
    public static final String REF_T_FS1 = "FS1";
    public static final String REF_T_FS2 = "FS2";

    public static final String SOUNDSET_PREFERENCE = "SoundSetPref";

    // Element of Shared preference
    public static final String SOUNDSET_CREATENEW = "SoundSetCreateNewIndex";
    public static final String SOUNDSET_CURSELGUID = "SoundSetGUID";
    public static final String CUR_URI = "SoundUri";
    public static final String SORT_MODE = "ItemSort";

    public static final String SDMLISTTYPE = "SDMListType";
    public static final String SDMCATEGORY = "SDMCategory";
    public static final String SDMPICKERTYPE = "SDMPickerType";
    public static final String SDMSAVEORNOT = "SDMSaveOrNot";
    public static final String SDMDEFSTRURI = "SDMDefStrUri";
    public static final String SDMPICKERTITLE = "SDMPicketTitle";

    public static final int DIALOG_ADDNEW = 1;
    public static final int DIALOG_NAMECONFIRM = 1;

    public static final String DETAIL_PARCEL = "DetailParcel";
    public static final String DETAIL_DESCRIPTION = "DetailDescription";
    public static final String DETAIL_CREATOR = "DetailCreator";
    public static final String DETAIL_CREATEDATE = "DetailCreateDate";
    public static final String DETAIL_CATEGORY = "DetailCategory";
    public static final String DETAIL_SIZE = "DetailSize";
    public static final String DETAIL_LABEL = "DetailLabel";
    public static final String DETAIL_TITLE_SOUNDSET_NAME = "SoundSetName";

    public static final String CUSTOM_LANGUAGE = "custom";

    public static final int MSG_LOAD_SOUNDSET_LIST_FROM_DATABASE = 0;
    public static final int MSG_LOAD_RINGTONE_LIST_FROM_DATABASE = 1;
    public static final int MSG_LOAD_NOTIFICATION_LIST_FROM_DATABASE = 2;
    public static final int MSG_LOAD_ALARM_LIST_FROM_DATABASE = 3;
    public static final int MSG_DISPLAY_PROGRESS_DIALOG = 4;
    public static final int MSG_DISMISS_PROGRESS_DIALOG = 5;
    public static final int MSG_RECEIVE_SDM_SERVICE_RESULT_INTENT = 6;
    public static final int MSG_DISPLAY_NO_ACTIVE_NETWORK_DIALOG = 7;
    public static final int MSG_DISPLAY_FEE_WARNING_DIALOG = 8;

    public static final int CAT_UNKNOWN = 0;
    public static final int CAT_CLASSICAL = 1;
    public static final int CAT_COUNTRY = 2;
    public static final int CAT_JAZZ = 3;
    public static final int CAT_ELECTRONIC = 4;
    public static final int CAT_FOLK = 5;
    public static final int CAT_HIPHOP = 6;
    public static final int CAT_POP = 7;
    public static final int CAT_MODERN = 8;
    public static final int CAT_SIMPLE = 9;
    public static final int CAT_ROCK = 10;
    public static final int CAT_WORLD = 11;
    public static final int CAT_SOUNDFX = 12;
    public static final int CAT_INSTRUMENTS = 13;

    public static final int SDM_DL_UNKNOWN = 0;
    public static final int SDM_DL_NOT_DOWNLOAD = 1;
    public static final int SDM_DL_DOWNLOADED = 2;
    public static final int SDM_DL_DOWNLOADING = 3;

    public static final int SORT_MODE_DATE = 1;
    public static final int SORT_MODE_NAME = 2;
    public static final int SORT_MODE_LIKE = 3;
    public static final int SORT_MODE_DOWNLOADS = 4;

    // Request code
    public static final int PICKER_CREATE = 0;
}
