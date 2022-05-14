package com.example.dolphinchat;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    Button startHelloServiceButton = (Button) findViewById(R.id.startHelloServiceButton);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Set up hello service button to start hello service.
        startHelloServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startHelloService();
            }
        });

    }

    private void startHelloService() {
        Intent intent = new Intent(this, HelloService.class);
        startService(intent);
    }

    private void startAudioRecordingService() {

    }
}