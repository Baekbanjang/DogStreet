package com.example.firebase;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
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
import com.google.android.gms.tasks.OnFailureListener;
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

public class driver_mode extends AppCompatActivity implements OnMapReadyCallback {
    GoogleMap gMapObj;
    FusedLocationProviderClient fusedLocationProviderClient;
    Marker myMarker;
    Location myLocation;
    DatabaseReference dbRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.driver_mode);


        FirebaseUser user= FirebaseAuth.getInstance().getCurrentUser();
        if(user!=null) {
            String uid=user.getUid();
        }

        fusedLocationProviderClient=LocationServices.getFusedLocationProviderClient(this);
        dbRef= FirebaseDatabase.getInstance().getReference("Locations");
        fetchCurrentLocation();

        Button immediate=(Button)findViewById(R.id.immediate_repot_But);
        immediate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
                if(myLocation != null && user != null) {
                    // 현재 날짜와 시간을 가져옵니다.
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    String currentDateAndTime = sdf.format(new Date());

                    int reportStatus = 2;

                    Map<String, Object> locationData = new HashMap<>();
                    locationData.put("latitude", myLocation.getLatitude());
                    locationData.put("longitude", myLocation.getLongitude());
                    locationData.put("timestamp", currentDateAndTime);
                    locationData.put("reportStatus", reportStatus);

                    dbRef.child(user.getUid()).push().setValue(locationData);
                }
            }
        });

        // 사용자 마커에 가까워 지면 알림 발생
        createNotificationChannel();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMapObj=googleMap;

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
                        addGeofence(latLng, 100);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // 에러 발생 시 처리
                Log.w("driver_mode", "Failed to read value.", error.toException());
            }
        });
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

        // 위치 요청 객체 생성(안드로이드11 API레벨 30이상에서 사용 중지)
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);  // 위치 업데이트 간격 설정 (1000ms = 1s)

        // 대체코드
        /*LocationRequest locationRequest = new LocationRequest.Builder()
        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        .setInterval(1000)
        .build();*/

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
                            // 위치가 업데이트될 때마다 Google Map을 로드합니다.
                            SupportMapFragment mapFragment;
                            mapFragment=(SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                            mapFragment.getMapAsync(driver_mode.this);
                        }
                    }
                },
                Looper.getMainLooper());
    }

    private void addGeofence(LatLng latLng, float radius) {
        Geofence geofence = new Geofence.Builder()
                .setRequestId("myGeofence")
                .setCircularRegion(latLng.latitude, latLng.longitude, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();

        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL)
                .addGeofence(geofence)
                .build();

        GeofencingClient geofencingClient = LocationServices.getGeofencingClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
            return;
        }

        geofencingClient.addGeofences(geofencingRequest, getGeofencePendingIntent())
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i("Geofencing", "Geofence added successfully");
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Geofencing", "Failed to add Geofence", e);
                    }
                });
    }

    private PendingIntent getGeofencePendingIntent() {
        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Geofence Channel";
            String description = "Channel for Geofence notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("channelId", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

}