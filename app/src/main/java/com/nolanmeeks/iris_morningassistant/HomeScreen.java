package com.nolanmeeks.iris_morningassistant;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.design.widget.CoordinatorLayout;
import android.os.Bundle;
import android.content.Intent;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

import pub.devrel.easypermissions.EasyPermissions;


public class HomeScreen extends AppCompatActivity implements View.OnClickListener{
    FloatingActionButton fab;
    FloatingActionButton fab1;
    FloatingActionButton fab2;
    FloatingActionButton fab3;
    CoordinatorLayout rootLayout;

    int GPS_PERMISSIONS;
    public static LocationManager locMan;
    public static Geocoder geocoder;
    public static LocationListener locListener;

    //Save the FAB's active status
    //false -> fab = close
    //true -> fab = open
    private boolean FAB_Status = false;

    //Animations
    Animation show_fab_1;
    Animation hide_fab_1;
    Animation show_fab_2;
    Animation hide_fab_2;
    Animation show_fab_3;
    Animation hide_fab_3;
    Animation rotate_fab;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;


    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { CalendarScopes.CALENDAR_READONLY };

    private TextView calendarButton;
    private ProgressDialog mProgress;
    GoogleAccountCredential mCredential;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the view from activity_main.xml
        setContentView(R.layout.activity_home_screen);

        // Locate the button in activity_main.xml
        Button weatherButton = (Button) findViewById(R.id.WeatherButton);
        weatherButton.setOnClickListener(this);
        locationSetup();
        displayWeather();

        //display calendar events of today
        calendarButton = (TextView) findViewById(R.id.CalendarButton);

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Getting your events ...");

        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        if(mCredential.getSelectedAccountName() == null) {
            chooseAccount(mCredential);
        } else {
            new CalendarActivity.MakeRequestTask(mCredential, calendarButton, mProgress, null).execute();
        }

        calendarButton.setOnClickListener(this);



        Button alarmButton = (Button) findViewById(R.id.AlarmButton);
        alarmButton.setOnClickListener(this);

        rootLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);

        //Floating Action Buttons
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab1 = (FloatingActionButton) findViewById(R.id.fab_1);
        fab2 = (FloatingActionButton) findViewById(R.id.fab_2);
        fab3 = (FloatingActionButton) findViewById(R.id.fab_3);

        //Animations
        show_fab_1 = AnimationUtils.loadAnimation(getApplication(), R.anim.fab1_show);
        hide_fab_1 = AnimationUtils.loadAnimation(getApplication(), R.anim.fab1_hide);
        show_fab_2 = AnimationUtils.loadAnimation(getApplication(), R.anim.fab2_show);
        hide_fab_2 = AnimationUtils.loadAnimation(getApplication(), R.anim.fab2_hide);
        show_fab_3 = AnimationUtils.loadAnimation(getApplication(), R.anim.fab3_show);
        hide_fab_3 = AnimationUtils.loadAnimation(getApplication(), R.anim.fab3_hide);
        rotate_fab = AnimationUtils.loadAnimation(getApplication(), R.anim.fab_rotate);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!FAB_Status) {
                    //Display FAB menu
                    expandFAB();
                    FAB_Status = true;
                } else {
                    //Close FAB menu
                    hideFAB();
                    FAB_Status = false;
                }
            }
        });

        fab1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplication(), "Floating Action Button 1", Toast.LENGTH_SHORT).show();
                Intent newAlarm = new Intent(HomeScreen.this, newAlarm.class);
                startActivity(newAlarm);
            }
        });

        fab2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplication(), "Floating Action Button 2", Toast.LENGTH_SHORT).show();
                Intent newCalEvent = new Intent(HomeScreen.this, newCalEvent.class);
                startActivity(newCalEvent);
            }
        });

        fab3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplication(), "Floating Action Button 3", Toast.LENGTH_SHORT).show();
                Intent settings = new Intent(HomeScreen.this, SettingsActivity.class);
                startActivity(settings);
            }
        });
    }
    // Capture button clicks
    public void onClick(View clicked) {
        switch(clicked.getId()){

            // Start Weather Activity
            case R.id.WeatherButton:
                Intent weatherIntent = new Intent(HomeScreen.this, WeatherActivity.class);
                startActivity(weatherIntent);
                break;

            // Start Calendar Activity
            case R.id.CalendarButton:
                Intent calendarIntent = new Intent(HomeScreen.this, CalendarActivity.class);
                startActivity(calendarIntent);
                break;

            // Start Alarm Activity
            case R.id.AlarmButton:
                // Start Alarm
                Intent alarmIntent = new Intent(HomeScreen.this, AlarmActivity.class);
                startActivity(alarmIntent);
                break;


            default:
                break;
        }

    }
    private void expandFAB() {

        //Main Floating Action Button
        fab.startAnimation(rotate_fab);
        fab.setClickable(true);

        //Floating Action Button 1
        FrameLayout.LayoutParams layoutParams1 = (FrameLayout.LayoutParams) fab1.getLayoutParams();
        layoutParams1.rightMargin += (int) (fab1.getWidth() * 1.7);
        layoutParams1.bottomMargin += (int) (fab1.getHeight() * 0.25);
        fab1.setLayoutParams(layoutParams1);
        fab1.startAnimation(show_fab_1);
        fab1.setClickable(true);

        //Floating Action Button 2
        FrameLayout.LayoutParams layoutParams2 = (FrameLayout.LayoutParams) fab2.getLayoutParams();
        layoutParams2.rightMargin += (int) (fab2.getWidth() * 1.5);
        layoutParams2.bottomMargin += (int) (fab2.getHeight() * 1.5);
        fab2.setLayoutParams(layoutParams2);
        fab2.startAnimation(show_fab_2);
        fab2.setClickable(true);

        //Floating Action Button 3
        FrameLayout.LayoutParams layoutParams3 = (FrameLayout.LayoutParams) fab3.getLayoutParams();
        layoutParams3.rightMargin += (int) (fab3.getWidth() * 0.25);
        layoutParams3.bottomMargin += (int) (fab3.getHeight() * 1.7);
        fab3.setLayoutParams(layoutParams3);
        fab3.startAnimation(show_fab_3);
        fab3.setClickable(true);
    }


    private void hideFAB() {

        //Main Floating Action Button
        fab.startAnimation(rotate_fab);
        fab.setClickable(true);

        //Floating Action Button 1
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) fab1.getLayoutParams();
        layoutParams.rightMargin -= (int) (fab1.getWidth() * 1.7);
        layoutParams.bottomMargin -= (int) (fab1.getHeight() * 0.25);
        fab1.setLayoutParams(layoutParams);
        fab1.startAnimation(hide_fab_1);
        fab1.setClickable(false);

        //Floating Action Button 2
        FrameLayout.LayoutParams layoutParams2 = (FrameLayout.LayoutParams) fab2.getLayoutParams();
        layoutParams2.rightMargin -= (int) (fab2.getWidth() * 1.5);
        layoutParams2.bottomMargin -= (int) (fab2.getHeight() * 1.5);
        fab2.setLayoutParams(layoutParams2);
        fab2.startAnimation(hide_fab_2);
        fab2.setClickable(false);

        //Floating Action Button 3
        FrameLayout.LayoutParams layoutParams3 = (FrameLayout.LayoutParams) fab3.getLayoutParams();
        layoutParams3.rightMargin -= (int) (fab3.getWidth() * 0.25);
        layoutParams3.bottomMargin -= (int) (fab3.getHeight() * 1.7);
        fab3.setLayoutParams(layoutParams3);
        fab3.startAnimation(hide_fab_3);
        fab3.setClickable(false);
    }

    public void displayWeather() {
        AsyncTask a = new Weather().execute("current");
        try {
            HashMap<String, String> data = (HashMap<String, String>) a.get();
            String display = "Unable to fetch Weather data";
            Button weather = (Button) findViewById(R.id.WeatherButton);
            if (data != null) display = String.format("%s: \nCurrent Temperature: %s\n %s\n",
                    ProcessJSON.location,data.get("temp"),data.get("condition"));
            weather.setText(display);
            String condition = data.get("condition").toLowerCase().replaceAll(" ","");
            int res = WeatherActivity.getIcon(condition, data.get("day?").equals("true"));
            weather.setCompoundDrawablesWithIntrinsicBounds( 0,
                    0, res, 0 );
        } catch (Exception e) {

        }
    }

    public void locationSetup() {
        locMan = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        geocoder = new Geocoder(this, Locale.getDefault());
        locListener = new MyLocationListener();

        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if(permissionCheck == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    GPS_PERMISSIONS);
        }
        else {
            locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locMan.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 5000, 10, HomeScreen.locListener);

        }
    }

    private void chooseAccount(GoogleAccountCredential credential) {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                credential.setSelectedAccountName(accountName);
                new CalendarActivity.MakeRequestTask(mCredential, calendarButton, mProgress, null).execute();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    calendarButton.setText(
                            "This app requires Google Play Services. Please install " +
                                    "Google Play Services on your device and relaunch this app.");
                } else {
                    new CalendarActivity.MakeRequestTask(mCredential, calendarButton, mProgress, null).execute();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        new CalendarActivity.MakeRequestTask(mCredential, calendarButton, mProgress, null).execute();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    new CalendarActivity.MakeRequestTask(mCredential, calendarButton, mProgress, null).execute();
                }
                break;
        }
    }
}