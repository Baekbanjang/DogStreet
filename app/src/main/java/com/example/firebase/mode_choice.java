package com.example.firebase;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class mode_choice extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mode_choice);

        Button reportBut=(Button)findViewById(R.id.reportBut);
        Button driveModeButton=(Button) findViewById(R.id.driveModeBut);
        Button pedestrianModeButton=(Button) findViewById(R.id.pedestrianModeBut);
        SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE);
        driveModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(mode_choice.this, driver_mode.class);
                startActivity(intent);
                SharedPreferences.Editor myEdit = sharedPreferences.edit();
                myEdit.putInt("selectedMode", 1); // 운전자 모드 선택 시 1 저장
                myEdit.commit();
            }
        });
        pedestrianModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(mode_choice.this, select_types_MainActivity.class);
                startActivity(intent);
                SharedPreferences.Editor myEdit = sharedPreferences.edit();
                myEdit.putInt("selectedMode", 0); // 보행자 모드 선택 시 0 저장
                myEdit.commit();
            }
        });
        reportBut.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent=new Intent(mode_choice.this, report_details_mode.class);
                startActivity(intent);
            }
        });

    }
}