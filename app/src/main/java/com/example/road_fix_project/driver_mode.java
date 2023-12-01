package com.example.road_fix_project;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
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
                    Map<String, Object> locationData = new HashMap<>();
                    locationData.put("latitude", myLocation.getLatitude());
                    locationData.put("longitude", myLocation.getLongitude());
                    dbRef.child(user.getUid()).push().setValue(locationData);
                }
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMapObj=googleMap;
        LatLng latLong=new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
        MarkerOptions marker=new MarkerOptions();
        marker.position(latLong).title("Location");
        try{
            marker.snippet(getAddress(myLocation.getLatitude(), myLocation.getLongitude()));
        } catch(IOException e) {
            e.printStackTrace();
        }
        marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker));
        gMapObj.animateCamera(CameraUpdateFactory.newLatLng(latLong));
        gMapObj.animateCamera(CameraUpdateFactory.newLatLngZoom(latLong,15f));
        myMarker=gMapObj.addMarker(marker);
        myMarker.showInfoWindow();

        //위치 권한이 있는지 확인
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // '내 위치' 버튼 활성화
            gMapObj.setMyLocationEnabled(true);
        } else {
            // 위치 권한 요청
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
        }
    }

    private String getAddress(double latitude, double longitude) throws IOException {
        Geocoder geocoder=new Geocoder(this, Locale.getDefault());
        List<Address> addressList=geocoder.getFromLocation(latitude, longitude, 1);
        return addressList.get(0).getAddressLine(0).toString();
    }

    private void fetchCurrentLocation() {
        // 서울역의 위도와 경도로 myLocation을 설정합니다.
        /*myLocation = new Location("");
        myLocation.setLatitude(37.555744);
        myLocation.setLongitude(126.970431);

        // 위치 업데이트를 요청하지 않고, 바로 지도를 로딩합니다.
        SupportMapFragment mapFragment;
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(driver_mode.this);*/
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
        Task<Location> task=fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location!=null) {
                    myLocation=location;
                    SupportMapFragment mapFragment;
                    mapFragment=(SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                    mapFragment.getMapAsync(driver_mode.this);
                }
            }
        });
    }
}
