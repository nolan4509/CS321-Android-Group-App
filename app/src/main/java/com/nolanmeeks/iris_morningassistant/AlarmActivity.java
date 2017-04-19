package com.nolanmeeks.iris_morningassistant;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TimePicker;

import java.util.ArrayList;
import java.util.Calendar;

import static android.view.Gravity.CENTER_HORIZONTAL;
import static android.widget.ListPopupWindow.WRAP_CONTENT;

//lalalalalalala
public class AlarmActivity extends AppCompatActivity {
    TimePicker alarmTimePicker;
    PendingIntent pendingIntent;
    boolean popupSet = false;

    PopupWindow popUpWindow;
    LinearLayout popLayout;
    LinearLayout containerLayout;

    String days[];
    ArrayList<String> newDays;
    int id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);
        id = getIntent().getIntExtra("id", -1);
        boolean update = id != -1;
        newDays = new ArrayList<>();

        alarmTimePicker = (TimePicker) findViewById(R.id.timePicker);
        popLayout = new LinearLayout(this);

        if (update) {
            AlarmData alarm = getAlarm(id);
            alarmTimePicker.setCurrentHour(Integer.parseInt(alarm.time.split(":")[0]));
            alarmTimePicker.setCurrentMinute(Integer.parseInt(alarm.time.split(":")[1]));

            days = alarm.days.split(",");
        }
        else {
            days = new String[0];
            ((Button)findViewById(R.id.CRUD_button)).setText("Create New Alarm");
            findViewById(R.id.delete).setVisibility(View.GONE);
        }
    }

    public void onClick(View view) {
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.SUNDAY);
        calendar.set(Calendar.HOUR_OF_DAY, alarmTimePicker.getCurrentHour());
        calendar.set(Calendar.MINUTE, alarmTimePicker.getCurrentMinute());
        Intent intent = new Intent(this, Alarm_Receiver.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

        System.out.println("Here");
        createAlarm(calendar);
    }

    public void popup(View v){
        if (!popupSet) {
            popUpWindow = new PopupWindow(this);
            setPopUpWindow();
            popupSet = true;
        }
        popUpWindow.showAtLocation(findViewById(R.id.AlarmStuff),
                Gravity.TOP, 50, 10);
        popUpWindow.update(50, 50, findViewById(R.id.AlarmStuff).getWidth(),
                findViewById(R.id.AlarmStuff).getHeight(),true);
    }

    private void setPopUpWindow() {
        popLayout.setOrientation(LinearLayout.VERTICAL);

        CheckBox[] week = new CheckBox[7];
        final CheckBox sun = new CheckBox(this); sun.setText("Sunday"); week[0] = sun;
        final CheckBox mon = new CheckBox(this); mon.setText("Monday"); week[1] = mon;
        final CheckBox tues = new CheckBox(this); tues.setText("Tuesday"); week[2] = tues;
        final CheckBox wed = new CheckBox(this); wed.setText("Wednesday"); week[3] = wed;
        final CheckBox thur = new CheckBox(this); thur.setText("Thursday"); week[4] = thur;
        final CheckBox fri = new CheckBox(this); fri.setText("Friday"); week[5] = fri;
        final CheckBox sat = new CheckBox(this); sat.setText("Saturday"); week[6] = sat;

        for(CheckBox d : week) {
            d.setLayoutParams(findViewById(R.id.repeat).getLayoutParams());
            d.setGravity(CENTER_HORIZONTAL);
            d.setTextColor(Color.WHITE);
            d.setTextSize(20);
        }

        for(String day : days) {
            day = day.trim();
            switch ((day)) {
                case "S":
                    sun.setChecked(true);
                    break;
                case "M":
                    mon.setChecked(true);
                    break;
                case "T":
                    tues.setChecked(true);
                    break;
                case "W":
                    wed.setChecked(true);
                    break;
                case "Th":
                    thur.setChecked(true);
                    break;
                case "F":
                    fri.setChecked(true);
                    break;
                case "Sa":
                    sat.setChecked(true);
                    break;
            }
        }

        popLayout.addView(sun);
        popLayout.addView(mon);
        popLayout.addView(tues);
        popLayout.addView(wed);
        popLayout.addView(thur);
        popLayout.addView(fri);
        popLayout.addView(sat);

        Button back = new Button(this);
        back.setLayoutParams(findViewById(R.id.repeat).getLayoutParams());
        back.setText("Repeat On");

        popLayout.addView(back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popUpWindow.dismiss();
                if(sun.isChecked()) newDays.add("S");
                if(mon.isChecked()) newDays.add("M");
                if(tues.isChecked()) newDays.add("T");
                if(wed.isChecked()) newDays.add("W");
                if(thur.isChecked()) newDays.add("Th");
                if(fri.isChecked()) newDays.add("F");
                if(sat.isChecked()) newDays.add("Sa");
            }
        });

        popUpWindow.setContentView(popLayout);
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


    private void createAlarm(Calendar cal) {
        String hour = ""+cal.get(Calendar.HOUR_OF_DAY);
        String min = ""+cal.get(Calendar.MINUTE);

        if(!popupSet) for(String d : days) newDays.add(d);
        String week = newDays.toString().replace("]","").replace("[","");
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
        String insert = String.format("INSERT INTO %s(%s,%s,%s,%s) VALUES('%s','%s','%s',%d)",
                AlarmsOpenHelper.TABLE_NAME, AlarmsOpenHelper.COLUMN_HOUR,
                AlarmsOpenHelper.COLUMN_MINUTE, AlarmsOpenHelper.COLUMN_DAYS,AlarmsOpenHelper.COLUMN_STATUS,
                hour, minute, days,1);
        System.out.println(insert);
        HomeScreen.db.execSQL(insert);
    }
}
