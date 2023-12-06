package com.example.road_fix_project;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.internal.location.zzbv;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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
    private GeofencingClient geofencingClient;
    String location;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.driver_mode);


        FirebaseUser user= FirebaseAuth.getInstance().getCurrentUser();
        if(user!=null) {
            String uid=user.getUid();
        }

        fusedLocationProviderClient=LocationServices.getFusedLocationProviderClient(this);
        dbRef= FirebaseDatabase.getInstance().getReference("accidents");
        fetchCurrentLocation();

        geofenceAlert=findViewById(R.id.geofence_alert);

        geofenceBroadcastReceiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action=intent.getAction();
                if ("GeofenceEnterEvent".equals(action)) {
                    geofenceAlert.setVisibility(View.VISIBLE);
                } else if ("GeofenceExitEvent".equals(action)) {
                    geofenceAlert.setVisibility(View.GONE);
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(geofenceBroadcastReceiver, new IntentFilter("GeofenceEnterEvent"));
        LocalBroadcastManager.getInstance(this).registerReceiver(geofenceBroadcastReceiver, new IntentFilter("GeofenceExitEvent"));

        Button immediate=(Button)findViewById(R.id.immediate_report_But);
        immediate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
                if(myLocation != null && user != null) {
                    double Latitude=myLocation.getLatitude();
                    double Longitude=myLocation.getLongitude();

                    location = Latitude+"_"+Longitude;
                    int modeValue=1;
                    String photoPathValue="null";
                    long currentTime = System.currentTimeMillis();
                    String typeValue="운전자 신고";

                    Map<String, Object> locationData = new HashMap<>();
                    locationData.put("location", location);
                    locationData.put("mode", modeValue); // modeValue는 해당 mode의 값을 설정해야 합니다.
                    locationData.put("photoPath", photoPathValue); // photoPathValue는 사진 경로를 설정해야 합니다.
                    locationData.put("time", currentTime);
                    locationData.put("type", typeValue); // typeValue는 해당 type의 값을 설정해야 합니다.

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
            Marker addedMarker;
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    for(DataSnapshot reportSnapshot : userSnapshot.getChildren()) {
                        // 신고 내역을 읽어옵니다.
                        String location = reportSnapshot.child("location").getValue(String.class);
                        Log.d("MyApp", "location: " +location);
                        int mode=reportSnapshot.child("mode").getValue(Integer.class);
                        String photoPath=null;
                        if(mode==0) {
                            photoPath=reportSnapshot.child("photoPath").getValue(String.class);
                            Log.d("MyApp", "photoPath: " + photoPath);
                        }

                        if (location != null) {
                            String[] latLng = location.split("_");
                            Log.d("MyApp", "latLng: " +latLng.length);
                            double latitude = Double.parseDouble(latLng[0]);
                            double longitude = Double.parseDouble(latLng[1]);

                            // 신고 시간과 신고 타입을 읽어옵니다.
                            long time = reportSnapshot.child("time").getValue(Long.class);
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
                            String timeString = sdf.format(new Date(time));

                            String type = reportSnapshot.child("type").getValue(String.class);

                            String snippet = "신고 날짜: " + timeString + "\n신고 타입: " + type;

                            // 신고 위치에 마커를 추가합니다.
                            LatLng latLngObj = new LatLng(latitude, longitude);
                            MarkerOptions marker = new MarkerOptions();
                            marker.position(latLngObj).title("즉시 신고 위치").snippet(snippet);
                            gMapObj.addMarker(marker);
                            addGeofence(latLngObj, 100);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // 에러 발생 시 처리
                Log.w("driver_mode", "Failed to read value.", error.toException());
            }
        });

        gMapObj.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;  // 기본 InfoWindow를 사용하지 않습니다.
            }

            @Override
            public View getInfoContents(Marker marker) {
                // 레이아웃을 로드합니다.
                View view = getLayoutInflater().inflate(R.layout.accident_information, null);

                // 뷰를 설정합니다.
                TextView title = view.findViewById(R.id.title);
                TextView snippet = view.findViewById(R.id.snippet);
                title.setText(marker.getTitle());
                snippet.setText(marker.getSnippet());
                return view;
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

                            if(gMapObj!=null) {
                                LatLng myLatLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                                gMapObj.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 18));
                            }

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
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