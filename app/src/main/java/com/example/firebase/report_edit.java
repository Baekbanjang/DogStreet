package com.example.firebase;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class report_edit extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private Uri photoURI;
    private StorageReference mStorageRef;

    private ImageButton choosePictureButton;
    private ImageView imageView;
    private MapView mapView;
    private GoogleMap googleMap;
    private ImageButton chooseLocationButton;
    private Button reportEditButton;
    private static final int LOCATION_REQUEST_CODE = 1001;
    private Map<String, Object> reportData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_edit);
        Intent intent = getIntent();

        reportData = new HashMap<>();
        reportData.put("location", intent.getStringExtra("location"));
        reportData.put("mode", Integer.valueOf(intent.getStringExtra("mode")));
        reportData.put("photoPath", intent.getStringExtra("photoPath"));
        reportData.put("time", Long.valueOf(intent.getStringExtra("time")));
        reportData.put("type", intent.getStringExtra("type"));

        choosePictureButton = findViewById(R.id.choose_picture);
        reportEditButton = findViewById(R.id.report_edit);
        imageView = findViewById(R.id.image_view);
        mapView = findViewById(R.id.mapView);
        chooseLocationButton = findViewById(R.id.choose_location);
        Spinner spinner = findViewById(R.id.spinner_report_type);


        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.report_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = parent.getItemAtPosition(position).toString();
                // 선택된 사고 유형을 Firebase에 저장
                reportData.put("type", selectedType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 선택하지 않은 경우의 처리
            }
        });

        double latitude = intent.getDoubleExtra("latitude", 0);
        double longitude = intent.getDoubleExtra("longitude", 0);
        String selectedItemKey = intent.getStringExtra("selectedItemKey");
        mStorageRef = FirebaseStorage.getInstance().getReference();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("accidents").child(uid);
            dbRef.child(selectedItemKey).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // 데이터를 가져옵니다.
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map != null) {
                        // 가져온 데이터를 기반으로 UI를 설정합니다.
                        String accidentType = (String) map.get("accidentType");
                        int spinnerPosition = adapter.getPosition(accidentType);
                        spinner.setSelection(spinnerPosition);

                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // 에러 처리
                }
            });
        }

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        choosePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        // Error occurred while creating the File
                    }
                    if (photoFile != null) {
                        photoURI = FileProvider.getUriForFile(report_edit.this, "com.dog_street", photoFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    }
                }
            }
        });

        chooseLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(report_edit.this, edit_exact_location.class);
                startActivityForResult(intent, LOCATION_REQUEST_CODE);
            }
        });

        reportEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 수정하기 버튼 클릭 시 동작 처리
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    String uid = user.getUid();
                    DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("accidents").child(uid);
                    dbRef.child(selectedItemKey).setValue(reportData); // 기존 정보를 새로운 정보로 덮어씁니다.

                }
                Intent intent = new Intent(report_edit.this, report_details_mode.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            uploadImage();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        return image;
    }

    private void uploadImage() {
        if (photoURI != null) {
            String uid = FirebaseAuth.getInstance().getUid();
            if (uid != null) {
                StorageReference userRef = mStorageRef.child("images/" + uid + "/" + photoURI.getLastPathSegment());
                userRef.putFile(photoURI)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                // Handle successful upload
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Handle failed upload
                            }
                        });
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        // 여기에 edit_exact_location 클래스에서 설정한 맵뷰 초기화
    }
}
