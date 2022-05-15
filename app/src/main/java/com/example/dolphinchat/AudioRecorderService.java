package com.example.dolphinchat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;


public class AudioRecorderService extends Service {
    private static boolean isRunning = false;
    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int SAMPLE_RATE = 44100; //CHANGE THIS AS NEEDED.
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE_RECORDING = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);


    private String fileName;
    private AudioRecord audioRecord;
    private AudioRecordThread audioRecordThread;

    private final class AudioRecordThread extends Thread{
        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            try {
                audioRecord = new AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE_RECORDING);
            } catch (Exception e){
                Log.e("AudioRecordThread", "error initializing ");
                e.printStackTrace();
            }
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) { // check for proper initialization
                Log.e("AudioRecordThread", "error initializing ");
                return;
            }

            audioRecord.startRecording();
            Log.i("AudioRecordThread", "Starting loop");
            //Get recording data and perform all necessary tasks on it.
            short[] data = new short[BUFFER_SIZE_RECORDING/2];
            WavWriter wavWriter = new WavWriter(SAMPLE_RATE);
            wavWriter.start();
            Log.w("WavWriter", "Starting writing to path: " + wavWriter.getPath());

            while(isRunning) {
                //Read audio data
                int numShortsRead = audioRecord.read(data, 0, data.length);
                //Write audio data to WAV file.
                wavWriter.pushAudioShort(data, numShortsRead);
                Log.i("AudioRecordThread", "Wrote " + numShortsRead + " shorts to wav file.");
                //Do processing with tensorflow lite here?
            }
            Log.w("WavWriter" , "Seconds written: " + wavWriter.secondsWritten() + " ");
            wavWriter.stop();
            audioRecord.stop();

        }
    }



    public AudioRecorderService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        isRunning = true;
        Toast.makeText(this, "audio recording service started", Toast.LENGTH_SHORT).show();

        //Send notification for service start.

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification =
                new Notification.Builder(this, "CHANNEL_DEFAULT_IMPORTANCE")
                        .setContentTitle("Recording Service")
                        .setContentText("Recording Service Running")
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentIntent(pendingIntent)
                        .setTicker("Recording Service Running")
                        .build();

        // Notification ID cannot be 0.
        startForeground(2, notification);


        fileName = intent.getStringExtra("fileName");
        audioRecordThread = new AudioRecordThread();
        audioRecordThread.start();
        return START_STICKY;

        //Set up recorder.

    }

    @Override
    public void onDestroy() {
        isRunning = false;
        Toast.makeText(this, "audio recording service done", Toast.LENGTH_SHORT).show();
    }
    public static boolean isRunning() {
        return isRunning;
    }


}