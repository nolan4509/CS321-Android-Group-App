package com.nolanmeeks.iris_morningassistant;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TimePicker;

import java.util.ArrayList;
import java.util.Calendar;

//lalalalalalala
public class AlarmActivity extends AppCompatActivity {
    TimePicker alarmTimePicker;
    PendingIntent pendingIntent;

    int id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);
        id = getIntent().getIntExtra("id", -1);
        boolean update = id != -1;

        alarmTimePicker = (TimePicker) findViewById(R.id.timePicker);

        if (update) {
            AlarmData alarm = getAlarm(id);
            alarmTimePicker.setHour(Integer.parseInt(alarm.time.split(":")[0]));
            alarmTimePicker.setMinute(Integer.parseInt(alarm.time.split(":")[1]));

            String days[] = alarm.days.split(",");
            for(String day : days) {
                switch ((day)) {
                    case "S":
                        ((CheckBox)findViewById(R.id.sun)).setChecked(true);
                        break;
                    case "M":
                        ((CheckBox)findViewById(R.id.mon)).setChecked(true);
                        break;
                    case "T":
                        ((CheckBox)findViewById(R.id.tues)).setChecked(true);
                        break;
                    case "W":
                        ((CheckBox)findViewById(R.id.wed)).setChecked(true);
                        break;
                    case "Th":
                        ((CheckBox)findViewById(R.id.thur)).setChecked(true);
                        break;
                    case "F":
                        ((CheckBox)findViewById(R.id.fri)).setChecked(true);
                        break;
                    case "Sa":
                        ((CheckBox)findViewById(R.id.sat)).setChecked(true);
                        break;
                }
            }
        }
        else {
            ((Button)findViewById(R.id.CRUD_button)).setText("Create New Alarm");
            findViewById(R.id.delete).setVisibility(View.GONE);
        }
    }

    public void onClick(View view) {
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.SUNDAY);
        calendar.set(Calendar.HOUR_OF_DAY, alarmTimePicker.getHour());
        calendar.set(Calendar.MINUTE, alarmTimePicker.getMinute());
        Intent intent = new Intent(this, Alarm_Receiver.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

        ArrayList<String> days = new ArrayList<String>();

        if(((CheckBox)findViewById(R.id.sun)).isChecked()) days.add("S");
        if(((CheckBox)findViewById(R.id.mon)).isChecked()) days.add("M");
        if(((CheckBox)findViewById(R.id.tues)).isChecked()) days.add("T");
        if(((CheckBox)findViewById(R.id.wed)).isChecked()) days.add("W");
        if(((CheckBox)findViewById(R.id.thur)).isChecked()) days.add("Th");
        if(((CheckBox)findViewById(R.id.fri)).isChecked()) days.add("F");
        if(((CheckBox)findViewById(R.id.sat)).isChecked()) days.add("Sa");

        System.out.println("Here");
        createAlarm(calendar,days);
    }

    public static AlarmData getAlarm(int id) {
        String[] values = {AlarmsOpenHelper.COLUMN_ID,
                AlarmsOpenHelper.COLUMN_HOUR,
                AlarmsOpenHelper.COLUMN_MINUTE,
                AlarmsOpenHelper.COLUMN_DAYS,
                AlarmsOpenHelper.COLUMN_STATUS};

        Cursor c = HomeScreen.db.query(AlarmsOpenHelper.TABLE_NAME,
                values, "ID = "+id,null,null,null,null);

        c.moveToNext();
        AlarmData ret = new AlarmData(c.getString(c.getColumnIndexOrThrow(values[1]))+":"+
                c.getString(c.getColumnIndexOrThrow(values[2])),
                c.getInt(c.getColumnIndexOrThrow(values[4])) == 1,
                c.getString(c.getColumnIndexOrThrow(values[3])),
                c.getInt(c.getColumnIndexOrThrow(values[0])));
        return ret;
    }


    private void createAlarm(Calendar cal,ArrayList<String> days) {
        String hour = ""+cal.get(Calendar.HOUR_OF_DAY);
        String min = ""+cal.get(Calendar.MINUTE);
        String week = days.toString().replace("]","").replace("[","");
        if (id == -1) addAlarmToDatabase(hour,min,week);
        else updateAlarmDB(hour,min,week);

        Intent home = new Intent(AlarmActivity.this,HomeScreen.class);
        startActivity(home);
    }

    public void deleteAlarm(View v) {
        String delete = String.format("DELETE FROM %s WHERE %s = %d",
                AlarmsOpenHelper.TABLE_NAME, AlarmsOpenHelper.COLUMN_ID, id);
        HomeScreen.db.execSQL(delete);
        Intent home = new Intent(AlarmActivity.this,HomeScreen.class);
        startActivity(home);
    }
    public void updateAlarmDB(String hour, String minute, String days) {
        String insert = String.format("UPDATE %s SET %s = '%s', %s = '%s', %s = '%s' WHERE %s = %d",
                AlarmsOpenHelper.TABLE_NAME, AlarmsOpenHelper.COLUMN_HOUR, hour,
                AlarmsOpenHelper.COLUMN_MINUTE, minute, AlarmsOpenHelper.COLUMN_DAYS,
                days, AlarmsOpenHelper.COLUMN_ID, id);
        System.out.println(insert);
        HomeScreen.db.execSQL(insert);
    }

    static void addAlarmToDatabase(String hour, String minute, String days) {
        String insert = String.format("INSERT INTO %s(%s,%s,%s) VALUES('%s','%s','%s')",
                AlarmsOpenHelper.TABLE_NAME, AlarmsOpenHelper.COLUMN_HOUR,
                AlarmsOpenHelper.COLUMN_MINUTE, AlarmsOpenHelper.COLUMN_DAYS,
                hour, minute, days);
        System.out.println(insert);
        HomeScreen.db.execSQL(insert);
    }
}
