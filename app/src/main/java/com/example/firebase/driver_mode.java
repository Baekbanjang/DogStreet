package com.example.firebase;

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

    // onCreate: 생성 메소드
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.driver_mode);

        // 현재 사용자 아이디 확보
        FirebaseUser user= FirebaseAuth.getInstance().getCurrentUser();
        if(user!=null) {
            String uid=user.getUid();
        }

        // 현재 위치를 확보할 수 있는 변수, 파이어베이스 DB 위치 확보
        fusedLocationProviderClient=LocationServices.getFusedLocationProviderClient(this);
        dbRef= FirebaseDatabase.getInstance().getReference("accidents");

        // fechCurrentLocation() 메소드 실행
        fetchCurrentLocation();

        // GeofenceBroadcasReceiver 전달시 발생하는 이벤트
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

        // 즉시 신고 보턴 클릭시 발생 이벤트
        Button immediate=(Button)findViewById(R.id.immediate_report_But);
        immediate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
                if(myLocation != null && user != null) {
                    // DB에 들어갈 값의 형식 지정
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

                    // "즉시 신고되었습니다." 텍스트를 보이게 함
                    TextView immediateReportText = findViewById(R.id.immediate_report_text);
                    immediateReportText.setVisibility(View.VISIBLE);

                    // 5초 후에 "즉시 신고되었습니다." 텍스트를 숨김
                    immediateReportText.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            immediateReportText.setVisibility(View.GONE);
                        }
                    }, 5000);  // 5000 밀리초(=5초) 후에 실행합니다.
                }
            }
        });

        // 사용자 마커에 가까워 지면 알림 발생. creaeNotificationChannel 메ㅗ드 실행
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

        // Firebase Realtime Database에서 모든 위치 정보를 읽어와 지도에 표시
        dbRef.addValueEventListener(new ValueEventListener() {
            Marker addedMarker;
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    for(DataSnapshot reportSnapshot : userSnapshot.getChildren()) {
                        // 신고 내역을 읽어옴
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

                            // 신고 시간과 신고 타입을 읽어옴.
                            long time = reportSnapshot.child("time").getValue(Long.class);
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
                            String timeString = sdf.format(new Date(time));

                            String type = reportSnapshot.child("type").getValue(String.class);

                            String snippet = "신고 날짜: " + timeString + "\n신고 타입: " + type;

                            // 신고 위치에 마커를 추가.
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
                // 에러 발생 시 처리, Log를 통해 오류내용 확인 가능
                Log.w("driver_mode", "Failed to read value.", error.toException());
            }
        });

        // 지도에 표시되는 마커 클릭 시 정보 표시
        gMapObj.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                // 레이아웃을 로드
                View view = getLayoutInflater().inflate(R.layout.accident_information, null);

                // 뷰를 설정
                TextView title = view.findViewById(R.id.title);
                TextView snippet = view.findViewById(R.id.snippet);
                title.setText(marker.getTitle());
                snippet.setText(marker.getSnippet());
                return view;
            }
        });
    }

    // 주소를 확보하는 메소드
    private String getAddress(double latitude, double longitude) throws IOException {
        Geocoder geocoder=new Geocoder(this, Locale.getDefault());
        List<Address> addressList=geocoder.getFromLocation(latitude, longitude, 1);
        return addressList.get(0).getAddressLine(0).toString();
    }

    // 위치 권한 확인 메소드
    private void fetchCurrentLocation() {
        // 위치 접근 권한 확인: ACCESS_FINE_LOCATION과 ACCESS_COARSE_LOCATION 권한이 있는지 확인
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
            // 권한이 없다면, 사용자에게 권한을 요청
            ActivityCompat.requestPermissions(
                    this,
                    new String[] {
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    1000
            );
            return; // 권한 요청 후 메서드 종료
        }
        // 권한이 이미 부여되어 있다면, 현재 위치를 가져오는 작업을 수행

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
                            // 위치 업데이트가 발생하면 해당 코드가 실행
                            // location 객체에는 새로운 위치 정보가 존재
                            myLocation = location;
                            // 위치가 업데이트될 때마다 Google Map을 로드
                            SupportMapFragment mapFragment;
                            mapFragment=(SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                            mapFragment.getMapAsync(driver_mode.this);

                            // 사용자 이동 시 지도가 같이 움직이는 카메라
                            if(gMapObj!=null) {
                                LatLng myLatLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                                gMapObj.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 18));
                            }

                        }
                    }
                },
                Looper.getMainLooper());
    }

    // Geofence 생성 메소드
    private void addGeofence(LatLng latLng, float radius) {
        // 새로운 Geofence 객체를 생성. 해당 Geofence는 특정 위도와 경도를 중심으로 하며, 반지름은 radius로 설정
        Geofence geofence = new Geofence.Builder()
                .setRequestId("myGeofence") // Geofence의 ID를 설정
                .setCircularRegion(latLng.latitude, latLng.longitude, radius) // Geofence의 형태와 위치를 설정
                .setExpirationDuration(Geofence.NEVER_EXPIRE) // Geofence의 만료 시간을 설정. 만료하지 않도록 설정함
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER) // 알림을 받을 Geofence의 전환 유형을 설정. Geofence에 들어갈 때 알림을 받도록 설정
                .build();

        // GeofencingRequest 객체를 생성. 이 객체는 Geofence를 추가할 때 필요한 설정을 담고 있음
        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();

        // GeofencingClient 객체 생성
        GeofencingClient geofencingClient = LocationServices.getGeofencingClient(this);

        // 위치 권한을 확인
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없다면, 사용자에게 권한을 요청
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
            return;
        }

        // Geofence를 추가
        geofencingClient.addGeofences(geofencingRequest, getGeofencePendingIntent())
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // 추가 성공 시 로그 출력
                        Log.i("Geofencing", "Geofence added successfully");
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // 추가 실패 시 로그 출력
                        Log.e("Geofencing", "Failed to add Geofence", e);
                    }
                });
    }

    private PendingIntent getGeofencePendingIntent() {
        // Geofence 이벤트를 받을 BroadcastReceiver를 지정하는 Intent를 생성
        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        // PendingIntent를 반환. PendingIntent는 Geofence 이벤트가 발생했을 때 시스템이 GeofenceBroadcastReceiver를 호출하도록 실행.
        // FLAG_UPDATE_CURRENT는 이미 PendingIntent가 존재한다면, 그 PendingIntent의 extra data를 새로운 Intent로 업데이트하라는 의미
    }

    // 채널 생성 메소드.  알림을 보내기 전에 알림 채널을 등록해야함.
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 알림 채널의 이름, 설명, 중요도 설정
            CharSequence name = "Geofence Channel";
            String description = "Channel for Geofence notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("channelId", name, importance);
            channel.setDescription(description);

            // 시스템의 NotificationManager 서비스를 가져와 알림 채널을 시스템에 등록
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