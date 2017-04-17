package com.nolanmeeks.iris_morningassistant;


import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.util.Random;


public class RingtonePlayingService extends Service {

    MediaPlayer media_song;
    int startId;
    boolean isRunning;
    //public static Context context;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);

        // fetch the extra string from the alarm on/alarm off values
        String state = intent.getExtras().getString("extra");
        // fetch the whale choice integer values
        Integer whale_sound_choice = intent.getExtras().getInt("whale_choice");

        Log.e("Ringtone extra is ", state);
        Log.e("Whale choice is ", whale_sound_choice.toString());

        // put the notification here, test it out

        // notification
        // set up the notification service
        NotificationManager notify_manager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        // set up an intent that goes to the Main Activity
        Intent intent_main_activity = new Intent(this.getApplicationContext(), HomeScreen.class);
        // set up a pending intent
        PendingIntent pending_intent_main_activity = PendingIntent.getActivity(this, 0,
                intent_main_activity, 0);

        // make the notification parameters
        Notification notification_popup = new Notification.Builder(this)
                .setContentTitle("An alarm is going off!")
                .setContentText("Click me!")
                .setSmallIcon(R.drawable.ic_action_call)
                .setContentIntent(pending_intent_main_activity)
                .setAutoCancel(true)
                .build();




        // this converts the extra strings from the intent
        // to start IDs, values 0 or 1
        assert state != null;
        switch (state) {
            case "alarm on":
                startId = 1;
                break;
            case "alarm off":
                startId = 0;
                Log.e("Start ID is ", state);
                break;
            default:
                startId = 1;
                break;
        }
        // if else statements

        // if there is no music playing, and the user pressed "alarm on"
        // music should start playing
        if (!this.isRunning && startId == 1) {
            Log.e("there is no music, ", "and you want start");

            this.isRunning = true;
            this.startId = 0;
            int song;

            // set up the start command for the notification
            notify_manager.notify(0, notification_popup);
            // play the whale sound depending on the passed whale choice id

            if (whale_sound_choice == 0) {
                // play a randomly picked audio file

                int minimum_number = 1;
                int maximum_number = 12;

                Random random_number = new Random();
                whale_sound_choice = random_number.nextInt(maximum_number) + minimum_number;
                Log.e("random number is ", String.valueOf(whale_sound_choice));
            }
            if (whale_sound_choice == 1) {
                song =  R.raw.humpback_bubblenet_and_vocals;
            }
            else if (whale_sound_choice == 2) {
                song = R.raw.humpback_contact_call_moo;
            }
            else if (whale_sound_choice == 3) {
                song = R.raw.humpback_contact_call_whup;
            }
            else if (whale_sound_choice == 4) {
                song = R.raw.humpback_feeding_call;
            }
            else if (whale_sound_choice == 5) {
                song = R.raw.humpback_flipper_splash;
            }
            else if (whale_sound_choice == 6) {
                song = R.raw.humpback_tail_slaps_with_propeller_whine;
            }
            else if (whale_sound_choice == 7) {
                song = R.raw.humpback_whale_song;
            }
            else if (whale_sound_choice == 8) {
                song = R.raw.humpback_whale_song_with_outboard_engine_noise;
            }
            else if (whale_sound_choice == 9) {
                song = R.raw.humpback_wheeze_blows;
            }
            else if (whale_sound_choice == 10) {
                song = R.raw.killerwhale_echolocation_clicks;
            }
            else if (whale_sound_choice == 11) {
                song = R.raw.killerwhale_offshore;
            }
            else if (whale_sound_choice == 12) {
                song = R.raw.killerwhale_resident;
            }
            else if (whale_sound_choice == 13) {
                song = R.raw.killerwhale_transient;
            }
            else {
                song = R.raw.killerwhale_resident;
            }

            media_song = MediaPlayer.create(this, song);
            media_song.start();
        }

        // if there is music playing, and the user pressed "alarm off"
        // music should stop playing
        else if (this.isRunning && startId == 0) {
            Log.e("there is music, ", "and you want end");

            // stop the ringtone
            media_song.stop();
            media_song.reset();

            this.isRunning = false;
            this.startId = 0;
        }

        // these are if the user presses random buttons
        // just to bug-proof the app
        // if there is no music playing, and the user pressed "alarm off"
        // do nothing
        else if (!this.isRunning && startId == 0) {
            Log.e("there is no music, ", "and you want end");

            this.isRunning = false;
            this.startId = 0;

        }

        // if there is music playing and the user pressed "alarm on"
        // do nothing
        else if (this.isRunning && startId == 1) {
            Log.e("there is music, ", "and you want start");

            this.isRunning = true;
            this.startId = 1;

        }

        // can't think of anything else, just to catch the odd event
        else {
            Log.e("else ", "somehow you reached this");

        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        // Tell the user we stopped.
        Log.e("on Destroy called", "ta da");

        super.onDestroy();
        this.isRunning = false;
    }



}