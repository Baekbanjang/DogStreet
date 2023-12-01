package com.example.road_fix_project;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class report_details_mode extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_details_mode);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // 로그인한 사용자의 ID를 가져옵니다.
            String userId = user.getUid();

            // 사용자의 신고 내역을 읽기 위한 참조를 생성합니다.
            DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("Locations").child(userId);

            // ListView와 데이터를 연결하기 위한 ArrayAdapter를 생성합니다.
            final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1);
            ListView listView = findViewById(R.id.report_list);
            listView.setAdapter(adapter);

            // ValueEventListener를 추가하여 데이터 변경을 감지합니다.
            mDatabase.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // 데이터가 변경될 때마다 ArrayAdapter를 새로운 데이터로 업데이트합니다.
                    adapter.clear();
                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                        // 신고 내역을 읽어옵니다.
                        double latitude = childSnapshot.child("latitude").getValue(Double.class);
                        double longitude = childSnapshot.child("longitude").getValue(Double.class);
                        String report = "사용자: "+userId+" 위도: " + latitude + ", 경도: " + longitude;
                        adapter.add(report);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // 에러 발생 시 처리
                    Log.w("report_details_mode", "Failed to read value.", databaseError.toException());
                }
            });
        }
    }
}