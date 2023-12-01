package com.example.road_fix_project;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import java.util.ArrayList;
import java.util.List;

public class report_details_mode extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ReportAdapter adapter;
    private List<Report> reportList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_details_mode);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        reportList = new ArrayList<>();
        adapter = new ReportAdapter(reportList);
        recyclerView.setAdapter(adapter);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // 로그인한 사용자의 ID를 가져옵니다.
            String userId = user.getUid();

            // 사용자의 신고 내역을 읽기 위한 참조를 생성합니다.
            DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("Locations").child(userId);


            // ValueEventListener를 추가하여 데이터 변경을 감지합니다.
            mDatabase.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    reportList.clear();
                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                        String date = childSnapshot.child("timestamp").getValue(String.class);
                        Integer status = childSnapshot.child("reportStatus").getValue(Integer.class);
                        reportList.add(new Report(date, status));
                    }
                    adapter.notifyDataSetChanged();
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