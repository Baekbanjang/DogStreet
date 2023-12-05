package com.example.road_fix_project;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.common.api.ApiException;
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
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build();

        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
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
            Log.d("NotificationChannel", "Notification channel created");
        }
    }
}



/*
package com.example.road_fix_project;

        import android.Manifest;
        import android.app.NotificationChannel;
        import android.app.NotificationManager;
        import android.app.PendingIntent;
        import android.content.BroadcastReceiver;
        import android.content.Context;
        import android.content.Intent;
        import android.content.IntentFilter;
        import android.content.pm.PackageManager;
        import android.location.Address;
        import android.location.Geocoder;
        import android.location.Location;
        import android.os.Build;
        import android.os.Bundle;
        import android.os.Handler;
        import android.os.Looper;
        import android.util.Log;
        import android.view.View;
        import android.widget.Button;
        import android.widget.TextView;
        import android.widget.Toast;

        import androidx.annotation.NonNull;
        import androidx.appcompat.app.AppCompatActivity;
        import androidx.core.app.ActivityCompat;
        import androidx.localbroadcastmanager.content.LocalBroadcastManager;

        import com.google.android.gms.common.api.ApiException;
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
    private TextView geofenceAlert;
    private BroadcastReceiver geofenceBroadcastReceiver;
    // 권한 요청 코드
    private static final int REQUEST_LOCATION_PERMISSION_CODE=1000;

    // GeofencingClient
    private GeofencingClient geofencingClient;

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


        geofenceAlert=findViewById(R.id.geofence_alert);
        geofenceBroadcastReceiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                geofenceAlert.setVisibility(View.VISIBLE);
            }
        };

        Button immediate=(Button)findViewById(R.id.immediate_repot_But);
        TextView immediateReportText=(TextView)findViewById(R.id.immediate_report_text);
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

                    immediateReportText.setVisibility(View.VISIBLE);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            immediateReportText.setVisibility(View.GONE);
                        }
                    }, 5000);
                }
            }
        });

        // 사용자 마커에 가까워 지면 알림 발생
        createNotificationChannel();

        // GeofencingClient 초기화
        geofencingClient=LocationServices.getGeofencingClient(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMapObj=googleMap;

        //위치 권한이 있는지 확인
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION_CODE);
            return;
        }
        // '내 위치' 버튼 활성화
        gMapObj.setMyLocationEnabled(true);

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
                        addGeofence(latLng, 10);
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

    // 주소 정보 표시 함수
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
                    REQUEST_LOCATION_PERMISSION_CODE
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

        /*fusedLocationProviderClient.requestLocationUpdates(locationRequest,
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

                            if(gMapObj!=null) {
                                LatLng myLatLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                                gMapObj.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 17));
                            }
                        }
                    }
                },
                Looper.getMainLooper());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode==REQUEST_LOCATION_PERMISSION_CODE) {
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                // 권한이 부여된 경우
                fetchCurrentLocation();
            }
            else {
                // 권한 거부된 경우
                Toast.makeText(this, "Location permission is required to use this feature.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addGeofence(LatLng latLng, float radius) {
        Geofence geofence = new Geofence.Builder()
                .setRequestId("myGeofence")
                .setCircularRegion(latLng.latitude, latLng.longitude, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build();

        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
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

                        // 네트워크 에러 처리
                        if(e instanceof ApiException) {
                            ApiException apiException=(ApiException) e;
                            if(apiException.getStatusCode()==GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE) {
                                Toast.makeText(driver_mode.this, "\"Geofence is not available. Check your network connection.", Toast.LENGTH_SHORT).show();
                            }
                        }
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

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(geofenceBroadcastReceiver, new IntentFilter("GeofenceEvent"));
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Geofence 제거
        geofencingClient.removeGeofences(getGeofencePendingIntent())
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.i("Geofencing", "Geofence removed successfully");
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Geonfencing", "Failed to remove Geofence", e);
                    }
                });
        LocalBroadcastManager.getInstance(this).unregisterReceiver(geofenceBroadcastReceiver);
    }
}
*/