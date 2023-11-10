package com.example.firebase;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class mode_choice extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mode_choice);

        Button driveModeButton=(Button) findViewById(R.id.driveModeBut);
        Button pedestrianModeButton=(Button) findViewById(R.id.pedestrianModeBut);
        driveModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(mode_choice.this, driver_mode.class);

                startActivity(intent);
            }
        });
        pedestrianModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(mode_choice.this, select_types_MainActivity.class);

                startActivity(intent);
            }
        });
    }
}