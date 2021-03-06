package com.nolanmeeks.iris_morningassistant;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;

import com.google.api.services.calendar.CalendarScopes;
import com.google.api.client.util.DateTime;

import com.google.api.services.calendar.model.*;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;

import android.util.Log;
import android.widget.CalendarView;
import android.widget.TextView;

import java.io.IOException;
import java.util.*;
import java.util.Calendar;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class CalendarActivity extends Activity
        implements EasyPermissions.PermissionCallbacks {

    GoogleAccountCredential mCredential;
    private TextView mOutputText;
    ProgressDialog mProgress;
    private CalendarView mCalendarView;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { CalendarScopes.CALENDAR_READONLY };

 //Creates main screen and auto loads calendar events for the next 24 hrs
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        mOutputText = (TextView) findViewById(R.id.mOutputText);
        mOutputText.setPadding(16, 16, 16, 16);
        mOutputText.setVerticalScrollBarEnabled(true);
        mOutputText.setMovementMethod(new ScrollingMovementMethod());


        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Getting your events ...");

        mCalendarView = (CalendarView) findViewById(R.id.calendarView);
        mCalendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                Calendar c = Calendar.getInstance();
                c.set(year, month, dayOfMonth);
                getResultsFromApi(mCredential, mProgress, mOutputText, c.getTime());
                Log.d("check_dayselect", c.toString());
            }
        });

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        getResultsFromApi(mCredential, mProgress, mOutputText, null);
    }

//use account and date information to call the Calendar api
    public void getResultsFromApi(GoogleAccountCredential credentials,
                                  ProgressDialog dialog,
                                  TextView textView,
                                  Date daySelected) {
//handle if your account isn't selected
        if (credentials.getSelectedAccountName() == null) {
            chooseAccount(credentials);
        } else if (! isDeviceOnline()) {
            textView.setText("No network connection available.");
        } else {
            new MakeRequestTask(credentials, textView, dialog, daySelected).execute();
        }
    }

    //select your google account
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount(GoogleAccountCredential credential) {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                credential.setSelectedAccountName(accountName);
                getResultsFromApi(mCredential, mProgress, mOutputText, null);
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

  //Handles appropriate action when activity is launched
    // ex. google play, google account selection, etc.
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    mOutputText.setText(
                            "This app requires Google Play Services. Please install " +
                                    "Google Play Services on your device and relaunch this app.");
                } else {
                    getResultsFromApi(mCredential, mProgress, mOutputText, null);
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
                        getResultsFromApi(mCredential, mProgress, mOutputText, null);
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi(mCredential, mProgress, mOutputText, null);
                }
                break;
        }
    }





    // asynchronous task that handles the Google Calendar API call.
    public static class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;
        private DateTime selectedDate;
        private Date daySelect;
        private ProgressDialog mProgress;
        private TextView mOutputText;

        //Request Task that accepts account information & the day we need events from
        //assign the value for "daySelected": the date we need to get
        MakeRequestTask(GoogleAccountCredential credential, TextView textView,
                        ProgressDialog progressDialog, Date daySelected) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            if (daySelected != null) {
                Log.d("check_dayselect", daySelected.toString());
                daySelect = daySelected;
                selectedDate = new DateTime(new Date(daySelected.getYear(), daySelected.getMonth(), daySelected.getDate()));
            } else {
                Log.d("check_daySelected", "daySelected is null");
            }
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Calendar API Android Quickstart")
                    .build();
            mProgress = progressDialog;
            mOutputText = textView;
        }


         //Background task to call Google Calendar API.

        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        // Fetch a list of the next 10 events from the primary calendar.
        private List<String> getDataFromApi() throws IOException {
            //check if we clicked on a date, or we should use the current time to get events
            //for the next 24 hrs
            Date d = new Date(System.currentTimeMillis());
            DateTime tomorrow = null;
            if (selectedDate == null) {
                //daySelect = new Date(d.getYear(), d.getMonth(), d.getDate());

                daySelect = new Date(d.getTime());
                selectedDate = new DateTime(daySelect);//selectedDayInMillis);
                tomorrow = new DateTime(d.getTime()+86400000);
            }

            //set upper time limit of event as 24 hours from now
            if (tomorrow == null) {
                tomorrow = new DateTime(new Date(daySelect.getYear(), daySelect.getMonth(), daySelect.getDate() + 1));
            }
            Log.d("check dates_tomorrow", tomorrow.toString());
            Log.d("check dates", daySelect.toString());
            Log.d("check dates_selected", selectedDate.toString());

            //List of 10 event result strings
            List<String> eventStrings = new ArrayList<String>();
            Events events = mService.events().list("primary")
                    .setMaxResults(10)
                    .setTimeMin(selectedDate)
                    .setTimeMax(tomorrow)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
            List<Event> items = events.getItems();

            for (Event event : items) {
                //Log.d("Checking_items", event.toPrettyString());
                DateTime start = event.getStart().getDateTime();
                DateTime end = event.getEnd().getDateTime();

                String startFormat = null;
                String endFormat = null;

                if (start == null || end == null) {

                    start = event.getStart().getDate();
                    end = event.getEnd().getDate();
                } else {
                    startFormat = start.toString().substring(11,16);
                    endFormat = end.toString().substring(11,16);
                }
                eventStrings.add(
                        String.format("%s     %s - %s", event.getSummary(), startFormat, endFormat));
            }
            return eventStrings;
        }


        @Override
        protected void onPreExecute() {
            mOutputText.setText("");
            mProgress.show();
        }

        //display events in text view
        @Override
        protected void onPostExecute(List<String> output) {
            mProgress.dismiss();
            if (output == null || output.size() == 0) {
                mOutputText.setText("No events scheduled.");
            } else {
                output.add(0, "Upcoming Events:");
                mOutputText.setText(TextUtils.join("\n", output));
            }
        }


        //display any errors that caused calendar request to be cancelled
        @Override
        protected void onCancelled() {
            mProgress.dismiss();
            if (mLastError != null) {
                mOutputText.setText("The following error occurred:\n"
                        + mLastError.getMessage());
            } else {
                mOutputText.setText("Request cancelled.");
            }
        }
    }


    //given code from Google API listed below

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

}