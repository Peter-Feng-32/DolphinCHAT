package com.example.dolphinchat;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;

public class MainActivity extends AppCompatActivity {


    private static final String CHANNEL_DEFAULT_ID = "CHANNEL_DEFAULT_IMPORTANCE";
    private static final int REQUEST_RECORD_AUDIO_EXTERNAL_STORAGE_PERMISSION = 200;

    // Requesting permission to RECORD_AUDIO and  WRITE_EXTERNAL_STORAGE
    private boolean permissionToRecordAccepted = false;
    private boolean permissionToWriteExternalStorageAccepted = false;
    private boolean permissionToReadExternalStorageAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_EXTERNAL_STORAGE_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                permissionToWriteExternalStorageAccepted  = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                permissionToReadExternalStorageAccepted  = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted || !permissionToWriteExternalStorageAccepted || !permissionToReadExternalStorageAccepted) {
            Log.w("Permission to Record: ", String.valueOf(permissionToRecordAccepted));
            Log.w("Permission to Write: ", String.valueOf(permissionToWriteExternalStorageAccepted));
            Log.w("Permission to Read: ", String.valueOf(permissionToReadExternalStorageAccepted));
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Create a notification channel so we can send notifications(Required for Foreground Service)
        createNotificationChannelDefault();
        //Request permission to record audio and write external storage.
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_EXTERNAL_STORAGE_PERMISSION);


        //Set up hello service button to start hello foreground service.
        Button startHelloServiceButton = (Button) findViewById(R.id.btn_hello_service);
        startHelloServiceButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                startHelloService();
            }
        });
        Button startAudioRecordingButton = (Button) findViewById(R.id.btn_start_recording);
        startAudioRecordingButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                startAudioRecordingService("my_file");
            }
        });
        Button stopAudioRecordingButton = (Button) findViewById(R.id.btn_stop_recording);
        stopAudioRecordingButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                stopAudioRecordingService();
            }
        });

    }


    private void createNotificationChannelDefault() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "DefaultChannel";
            String description = "Default importance channel";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_DEFAULT_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startHelloService() {
        Context context = getApplicationContext();
        Intent intent = new Intent(this, HelloService.class);
        context.startForegroundService(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startAudioRecordingService(String fileName) {
        Context context = getApplicationContext();
        Intent intent = new Intent(this, AudioRecorderService.class);
        intent.putExtra("fileName", fileName);
        if(!AudioRecorderService.isRunning()) {
            context.startForegroundService(intent);
        }
    }
    private void stopAudioRecordingService() {
        Context context = getApplicationContext();
        Intent intent = new Intent(this, AudioRecorderService.class);
        if(AudioRecorderService.isRunning()) {
            context.stopService(intent);
        } else {
            /*
            For Testing Purposes only.

            File path = new File(Environment.getExternalStorageDirectory().getPath() + "/Record");
            if (!path.exists() && !path.mkdirs()) {
                Log.e("MainActivity", "Failed to make directory: " + path.toString());
            }
            */
        }
    }

}