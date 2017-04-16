package com.nolanmeeks.iris_morningassistant;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;

import java.util.ArrayList;

/**
 * Created by wip on 4/16/17.
 */

public class AlarmSync extends AsyncTask<String, Void, ArrayList<AlarmData>> {
    public ArrayList<AlarmData> doInBackground(String ... a) {
        ArrayList<AlarmData> ret = new ArrayList<AlarmData>();

        String[] values = {AlarmsOpenHelper.COLUMN_ID,
                AlarmsOpenHelper.COLUMN_HOUR,
                AlarmsOpenHelper.COLUMN_MINUTE,
                AlarmsOpenHelper.COLUMN_DAYS,
                AlarmsOpenHelper.COLUMN_STATUS};

        Cursor c = HomeScreen.db.query(AlarmsOpenHelper.TABLE_NAME,
                values, "",null,null,null,null);

        while(c.moveToNext()) {
            ret.add(new AlarmData(
                    c.getString(c.getColumnIndexOrThrow(values[1]))+":"+
                            c.getString(c.getColumnIndexOrThrow(values[2])),
                    c.getInt(c.getColumnIndexOrThrow(values[4])) == 1,
                    c.getString(c.getColumnIndexOrThrow(values[3])),
                    c.getInt(c.getColumnIndexOrThrow(values[0]))));
        }
        return ret;
    }
}

class AlarmsOpenHelper extends SQLiteOpenHelper {
    //Taken from - http://stackoverflow.com/questions/23166628/how-to-store-alarms-in-sqlite-using-android#25777851
    public static final String TABLE_NAME       = "ALARMS";
    public static final String COLUMN_ID        = "ID";
    public static final String COLUMN_HOUR      = "HOUR";
    public static final String COLUMN_MINUTE    = "MINUTE";
    public static final String COLUMN_DAYS    = "DAYS";
    public static final String COLUMN_STATUS    = "STATUS";
    public static final String COLUMN_DATE      = "DATE";
    public static final String COLUMN_NAME      = "NAME";
    public static final String COLUMN_TONE      = "TONE";
    private static final int DATABASE_VERSION   = 4;
    private static final String DATABASE_NAME   = "alarm_app.db";
    private static final String TABLE_CREATE    =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID        + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_HOUR      + " TEXT NOT NULL, " +
                    COLUMN_MINUTE    + " TEXT NOT NULL, " +
                    COLUMN_DAYS    + " TEXT, " +
                    COLUMN_STATUS   + " INTEGER, " +
                    COLUMN_DATE      + " TEXT, " +
                    COLUMN_NAME      + " TEXT, " +
                    COLUMN_TONE      + " TEXT);";

    AlarmsOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}

class AlarmData {
    String time;
    Boolean status;
    String days;
    int Alarm_id;
    public AlarmData(String t, Boolean s, String w, int id) {
        time =t;
        status = s;
        days = w;
        Alarm_id = id;
    }
}
