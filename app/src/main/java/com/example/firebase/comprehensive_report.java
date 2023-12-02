package com.example.firebase;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class comprehensive_report extends AppCompatActivity {
    private ImageView mImageView;
    private TextView mTextViewAccidentType;
    private TextView mTextViewLocation;
    private static final String TAG = "comprehensive_report";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.comprehensive_report);

        SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE);
        String selectedType = sharedPreferences.getString("selectedType", "");
        String photoPath = sharedPreferences.getString("photoPath", "");
        String locationStr = sharedPreferences.getString("location", "");
        int selectedMode = sharedPreferences.getInt("selectedMode", -1);
        mImageView = findViewById(R.id.imageView);
        mTextViewAccidentType = findViewById(R.id.textView_accident_type);
        mTextViewLocation = findViewById(R.id.textView_coordinates);

        // 사고 유형을 TextView에 설정
        mTextViewAccidentType.setText(selectedType);
        mTextViewLocation.setText("위치: " + locationStr);
        Button report_Complete= (Button)findViewById(R.id.report_complete);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("accidents");
        Map<String, Object> accident = new HashMap<>();
        long currentTime = System.currentTimeMillis();
        accident.put("location", locationStr);
        accident.put("time", currentTime);
        accident.put("mode", selectedMode);
        accident.put("type", selectedType);
        accident.put("photoPath", photoPath);

        final TextToSpeech[] tts = new TextToSpeech[1];

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            // 이미지를 가져올 경로를 지정
            StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(photoPath);

            // 최대 이미지 크기를 지정(예: 5MB)
            long ONE_MEGABYTE = 1024 * 1024 * 5;

            storageRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    // Data for "images/uid/your_image_name.jpg" is returned, use this as needed
                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    mImageView.setImageBitmap(bmp);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle any errors
                }
            });
        }
        report_Complete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myRef.push().setValue(accident)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                // Write was successful!
                                Log.d(TAG, "Successfully wrote to database.");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Write failed
                                Log.w(TAG, "Failed to write to database.", e);
                            }
                        });
                String message = "신고되었습니다";
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();

                // TTS 설정
                tts[0] = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status != TextToSpeech.ERROR) {
                            tts[0].setLanguage(Locale.KOREAN);
                            tts[0].speak(message, TextToSpeech.QUEUE_FLUSH, null);
                        }
                    }
                });
                Intent intent = new Intent(comprehensive_report.this, mode_choice.class);
                startActivity(intent);
            }
        });


    }

}
