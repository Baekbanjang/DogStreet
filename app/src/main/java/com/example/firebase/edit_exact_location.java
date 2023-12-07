package com.example.firebase;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class edit_exact_location extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnCameraIdleListener {
    GoogleMap gMapObj;
    Marker centerMarker;
    FusedLocationProviderClient fusedLocationProviderClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_exact_location);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Button location_select = findViewById(R.id.location_select);
        location_select.setOnClickListener(new View.OnClickListener() { //위치 선택하기 버튼
            @Override
            public void onClick(View view) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (centerMarker != null) {
                    LatLng position = centerMarker.getPosition(); //마커의 위치를 저장

                    //위치 정보를 다른 클래스에서 공유할 수 있도록 설정
                    SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE);
                    SharedPreferences.Editor myEdit = sharedPreferences.edit();
                    myEdit.putString("selectedLocation", position.latitude + "_" + position.longitude);
                    myEdit.apply();

                    Intent intent = new Intent(edit_exact_location.this, report_edit.class);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMapObj = googleMap;
        gMapObj.setOnCameraIdleListener(this);

        // 위치 권한이 있는지 확인
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // '내 위치' 버튼 활성화
            gMapObj.setMyLocationEnabled(true);
        } else {
            // 위치 권한 요청
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
        }

        double latitude = getIntent().getDoubleExtra("latitude", 0);
        double longitude = getIntent().getDoubleExtra("longitude", 0);
        LatLng location = new LatLng(latitude, longitude);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));

        // 초기 마커 위치 설정
        LatLng center = gMapObj.getCameraPosition().target;
        centerMarker = gMapObj.addMarker(new MarkerOptions().position(center));
    }

    @Override
    public void onCameraIdle() {
        // 카메라 이동이 끝나면 호출됩니다.
        // 마커의 위치를 지도의 중심으로 이동합니다.
        LatLng center = gMapObj.getCameraPosition().target;
        if (centerMarker != null) {
            centerMarker.setPosition(center);
        } else {
            centerMarker = gMapObj.addMarker(new MarkerOptions().position(center));
        }
    }
}
