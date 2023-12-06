package com.example.firebase;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class select_types_MainActivity extends AppCompatActivity {
    private String selectedType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_types);

        Button roadkill = (Button)findViewById(R.id.roadkill);
        Button drainage_system = (Button)findViewById(R.id.drainage_system);
        Button road_facilities = (Button)findViewById(R.id.road_facilities);
        Button sinkhole = (Button)findViewById(R.id.sinkhole);
        Button pothole = (Button)findViewById(R.id.pothole);
        Button etc = (Button)findViewById(R.id.etc);
        Button select_type = (Button)findViewById(R.id.select_type);

        View.OnClickListener typeButtonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button clickedButton = (Button) v;
                selectedType = clickedButton.getText().toString();
            }
        };

        roadkill.setOnClickListener(typeButtonClickListener);
        drainage_system.setOnClickListener(typeButtonClickListener);
        road_facilities.setOnClickListener(typeButtonClickListener);
        sinkhole.setOnClickListener(typeButtonClickListener);
        pothole.setOnClickListener(typeButtonClickListener);
        etc.setOnClickListener(typeButtonClickListener);

        select_type.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(select_types_MainActivity.this, take_picture.class);
                SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE);
                SharedPreferences.Editor myEdit = sharedPreferences.edit();
                myEdit.putString("selectedType", selectedType);
                myEdit.commit();
                startActivity(intent);
            }
        });

    }
}