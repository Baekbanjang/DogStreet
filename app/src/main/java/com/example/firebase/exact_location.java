package com.example.firebase;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.location.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class exact_location extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnCameraIdleListener {
    GoogleMap gMapObj;
    FusedLocationProviderClient fusedLocationProviderClient;
    Marker centerMarker;
    Location myLocation;
    DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.exact_location);

        FirebaseUser user= FirebaseAuth.getInstance().getCurrentUser();
        if(user!=null) {
            String uid=user.getUid();
        }

        fusedLocationProviderClient=LocationServices.getFusedLocationProviderClient(this);
        dbRef= FirebaseDatabase.getInstance().getReference("pedestrian_Locations");
        fetchCurrentLocation();

        Button location_select=(Button)findViewById(R.id.location_select);
        location_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
                if(centerMarker != null && user != null) {
                    // 현재 날짜와 시간을 가져옵니다.
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    String currentDateAndTime = sdf.format(new Date());

                    int reportStatus = 2;

                    LatLng position = centerMarker.getPosition();

                    Map<String, Object> locationData = new HashMap<>();
                    locationData.put("latitude", position.latitude);
                    locationData.put("longitude", position.longitude);
                    locationData.put("timestamp", currentDateAndTime);
                    locationData.put("reportStatus", reportStatus);

                    dbRef.child(user.getUid()).push().setValue(locationData);
                }
                Intent intent = new Intent(exact_location.this, comprehensive_report.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMapObj = googleMap;
        gMapObj.setOnCameraIdleListener(this);

        //위치 권한이 있는지 확인
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // '내 위치' 버튼 활성화
            gMapObj.setMyLocationEnabled(true);
        } else {
            // 위치 권한 요청
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
        }

        // Firebase Realtime Database에서 모든 위치 정보를 읽어와 지도에 표시합니다.
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    for(DataSnapshot reportSnapshot : userSnapshot.getChildren()) {
                        // 신고 내역을 읽어옵니다.
                        double latitude = reportSnapshot.child("latitude").getValue(Double.class);
                        double longitude = reportSnapshot.child("longitude").getValue(Double.class);

                        // 신고 위치에 마커를 추가합니다.
                        LatLng latLng = new LatLng(latitude, longitude);
                        MarkerOptions marker = new MarkerOptions();
                        marker.position(latLng).title("도로 파손 지역");
                        // marker.snippet(getAddress(latitude, longitude));  // <-- 주소 정보를 표시하려면 이 줄의 주석을 해제하세요.
                        gMapObj.addMarker(marker);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // 에러 발생 시 처리
                Log.w("driver_mode", "Failed to read value.", error.toException());
            }
        });

        // 초기 마커 위치 설정
        LatLng center = gMapObj.getCameraPosition().target;
        centerMarker = gMapObj.addMarker(new MarkerOptions().position(center));
    }

    @Override
    public void onCameraIdle() {
        // 카메라 이동이 끝나면 호출됩니다.
        // 마커의 위치를 지도의 중심으로 이동합니다.
        LatLng center = gMapObj.getCameraPosition().target;
        centerMarker.setPosition(center);
    }

    private String getAddress(double latitude, double longitude) throws IOException {
        Geocoder geocoder=new Geocoder(this, Locale.getDefault());
        List<Address> addressList=geocoder.getFromLocation(latitude, longitude, 1);
        return addressList.get(0).getAddressLine(0).toString();
    }

    private void fetchCurrentLocation() {
        if(ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        )
                != PackageManager.PERMISSION_GRANTED&&
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                )
                        !=PackageManager.PERMISSION_GRANTED
        ){
            ActivityCompat.requestPermissions(
                    this,
                    new String[] {
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    1000
            );
            return;
        }

        // 위치 요청 객체 생성
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);  // 위치 업데이트 간격 설정 (1000ms = 1s)

        fusedLocationProviderClient.requestLocationUpdates(locationRequest,
                new LocationCallback() {
                    @Override
                    public void onLocationResult(@NonNull LocationResult locationResult) {
                        if(locationResult==null) {
                            return;
                        }
                        for(Location location : locationResult.getLocations()) {
                            // 위치 업데이트가 발생하면 여기의 코드가 실행됩니다.
                            // location 객체에는 새로운 위치 정보가 있습니다.
                            myLocation = location;
                        }
                    }
                },
                Looper.getMainLooper());
        Task<Location> task=fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location!=null) {
                    String locationStr = location.getLatitude() + "_" + location.getLongitude();
                    SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE);
                    SharedPreferences.Editor myEdit = sharedPreferences.edit();
                    myEdit.putString("location", locationStr);
                    myEdit.commit();
                    myLocation=location;
                    SupportMapFragment mapFragment;
                    mapFragment=(SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                    mapFragment.getMapAsync(exact_location.this);
                }
            }
        });
    }
}
