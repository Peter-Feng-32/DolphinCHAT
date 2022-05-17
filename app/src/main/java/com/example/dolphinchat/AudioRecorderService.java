package com.example.dolphinchat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import org.tensorflow.lite.support.audio.TensorAudio;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import org.tensorflow.lite.task.audio.classifier.AudioClassifier;
import org.tensorflow.lite.task.audio.classifier.Classifications;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class AudioRecorderService extends Service {
    private static boolean isRunning = false;
    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int SAMPLE_RATE = 44100; //CHANGE THIS AS NEEDED.
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE_RECORDING = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

    Context mContext;

    String modelPath = "lite-model_yamnet_classification_tflite_1.tflite";
    float probabilityThreshold = 0.3f;
    private AudioRecord audioRecord;
    private AudioRecordThread audioRecordThread;
    private AudioClassifier classifier;
    private TensorAudio tensorAudio;
    TensorAudio.TensorAudioFormat format;

    Timer timer;
    Date date;

    private final class AudioRecordThread extends Thread{



        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            date = new Date();
            timer = new Timer();
            try {
                classifier = AudioClassifier.createFromFile(mContext, modelPath);
                tensorAudio = classifier.createInputTensorAudio();
                format = classifier.getRequiredTensorAudioFormat();
                audioRecord = classifier.createAudioRecord();
                Log.w("AudioRecordThread", "Initializing audio record");
            } catch (Exception e){
                Log.e("AudioRecordThread", "error initializing ");
                e.printStackTrace();
            }
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) { // check for proper initialization
                Log.e("AudioRecordThread", "error initializing ");
                return;
            }
            audioRecord.startRecording();

            //Get audio buffer and convert to 16bit short to record.
            //In the future potentially it would be better to keep as floats, but I haven't written functionality for that yet and the library I used doesn't support it.

            //I'm 99.999% sure that the sample rate is set by the model, because you can access it in stuff like classifier.getRequiredTensorAudioFormat();
            //https://developer.android.com/reference/android/media/MediaRecorder.AudioSource
            //Also, audioSource seems to always be 6(VOICE_RECOGNITION) from examining the tensorflow lite library's code in AudioClassifier.class.
            Log.w("AudioRecord", audioRecord.getSampleRate() + " ");

            Log.i("AudioRecordThread", "Starting loop");


            //Looks like the tensor buffer is set to half the audioRecord's buffer.  Not sure if this is relevant or can be changed, but I'll just leave it like this for now.
            int dataBufferSize = audioRecord.getBufferSizeInFrames() /2;
            short[] data = new short[dataBufferSize];
            WavWriter wavWriter = new WavWriter(audioRecord.getSampleRate());
            wavWriter.start();
            Log.w("WavWriter", "Starting writing to path: " + wavWriter.getPath());


            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    //Get recording data and classify
                    int numFloatsRead = tensorAudio.load(audioRecord);
                    List<Classifications> output = classifier.classify(tensorAudio);
                    String outputString = "";
                    if(output.size() > 0) {
                        List<Category> filteredModelOutput = output.get(0).getCategories();

                        for(Category cat : filteredModelOutput) {
                            if(cat.getScore()>probabilityThreshold) {
                                outputString += "\n " + cat.getLabel() + ": " + cat.getScore();
                            }
                        }
                    }

                    Log.w("MyAudioRecordingLogs", "TimeStamp: " + date.getTime() + " NumFloatsRead: " + numFloatsRead+ " SizeBuffer: " + dataBufferSize);
                    Log.w("MyClassifier", outputString);

                    // Convert recorded floats to shorts
                    TensorBuffer dataFloatBuffer = tensorAudio.getTensorBuffer();
                    int numShortsRead = numFloatsRead;
                    float[] dataFloat = dataFloatBuffer.getFloatArray();

                    for(int i = 0; i < dataBufferSize; i++) {
                        data[i] = (short) (36767*dataFloat[i]);
                    }

                    //Write audio data to WAV file.
                    Log.i("AudioRecordThread", "Writing " + numShortsRead + " shorts to wav file.");
                    wavWriter.pushAudioShort(data, numShortsRead);
                    Log.i("AudioRecordThread", "Wrote " + numShortsRead + " shorts to wav file.");
                }
            }, 1, 500);
            while(isRunning) {
                //You can probably put a mutex here to make this more efficient.  I didn't have the energy.  -Peter
            }
            Log.w("WavWriter" , "Seconds written: " + wavWriter.secondsWritten() + " ");
            timer.cancel();
            wavWriter.stop();
            audioRecord.stop();

        }
    }



    public AudioRecorderService() {
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

        //Set up recorder.
        String fileName = intent.getStringExtra("fileName");
        mContext = this;
        audioRecordThread = new AudioRecordThread();
        audioRecordThread.start();
        return START_STICKY;
    }



    @Override
    public void onDestroy() {
        isRunning = false;
        Toast.makeText(this, "audio recording service done", Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static boolean isRunning() {
        return isRunning;
    }


}