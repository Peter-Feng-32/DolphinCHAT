package com.example.dolphinchat;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

public class AudioRecorderService extends Service {
    private MediaRecorder recorder;



    public AudioRecorderService() {
        //Need to request permission to record audio.


    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        String fileName = intent.getStringExtra("fileName");
        return START_STICKY;

        //Set up recorder.

    }

}