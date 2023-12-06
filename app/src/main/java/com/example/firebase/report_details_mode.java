package com.example.firebase;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class report_details_mode extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ReportAdapter adapter;
    private List<Report> reportList;
    private String selectedItemKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_details_mode);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        reportList = new ArrayList<>();
        adapter = new ReportAdapter(reportList);
        recyclerView.setAdapter(adapter);
        Button report_edit = (Button)findViewById(R.id.report_edit);
        Button report_delete = (Button)findViewById(R.id.report_delete);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // 로그인한 사용자의 ID를 가져옵니다.
            String userId = user.getUid();

            // 사용자의 신고 내역을 읽기 위한 참조를 생성합니다.
            DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("accidents").child(userId);

//             ValueEventListener를 추가하여 데이터 변경을 감지합니다.
            mDatabase.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    reportList.clear();
                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                        String location = childSnapshot.child("location").getValue(String.class);
                        String photoPath = childSnapshot.child("photoPath").getValue(String.class);
                        String type = childSnapshot.child("type").getValue(String.class);
                        long time = childSnapshot.child("time").getValue(long.class);
                        int mode = childSnapshot.child("mode").getValue(int.class);

                        Report report = new Report(location, mode, photoPath, time, type);
                        report.setKey(childSnapshot.getKey());
                        reportList.add(report);
                    }
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // 에러 발생 시 처리
                    Log.w("report_details_mode", "Failed to read value.", databaseError.toException());
                }
            });

            report_edit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Report r = adapter.getSelectedItemKey();
                    selectedItemKey = r.getKey();
                    if (selectedItemKey != null) { // 선택된 아이템이 있으면
                        Intent intent = new Intent(report_details_mode.this, report_edit.class);
                        intent.putExtra("selectedItemKey", selectedItemKey);
                        intent.putExtra("location", r.getLocation());
                        intent.putExtra("mode", String.valueOf(r.getMode()));
                        intent.putExtra("photoPath", r.getPhotoPath());
                        intent.putExtra("time", String.valueOf(r.getTime()));
                        intent.putExtra("type", r.getType());

                        startActivity(intent);
                    }
                }
            });
            report_delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String selectedItemKey = adapter.getSelectedItemKey().getKey(); // 선택된 아이템의 키를 가져옵니다.
                    if (selectedItemKey != null) { // 선택된 아이템이 있으면
                        mDatabase.child(selectedItemKey).removeValue();
                    }
                }
            });
        }
    }
}
