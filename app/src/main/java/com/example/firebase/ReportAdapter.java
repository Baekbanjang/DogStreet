package com.example.firebase;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {
    private List<Report> reportList;
    private int selectedPos = RecyclerView.NO_POSITION;

    public ReportAdapter(List<Report> reportList) {
        this.reportList = reportList;
    }

    public class ReportViewHolder extends RecyclerView.ViewHolder {
        public TextView tvType;
        public TextView tvTime;
        public TextView tvMode;

        public ReportViewHolder(View view) {
            super(view);
            tvType = view.findViewById(R.id.tv_type);
            tvTime = view.findViewById(R.id.tv_time);
            tvMode = view.findViewById(R.id.tv_mode);
        }
    }

    @Override
    public ReportViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false);
        return new ReportViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ReportViewHolder holder, int position) {
        Report report = reportList.get(position);
        holder.tvType.setText("유형: " + report.getType());
        holder.tvTime.setText("시간: " + report.getTime());

        // mode 값에 따라 보행자 모드 또는 운전자 모드를 표시합니다.
        String mode = (report.getMode() == 0) ? "보행자 모드" : "운전자 모드";
        holder.tvMode.setText("모드: " + mode);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notifyItemChanged(selectedPos); // 이전에 선택된 아이템 뷰를 업데이트합니다.
                selectedPos = holder.getAdapterPosition(); // 클릭된 아이템의 위치를 저장합니다.
                notifyItemChanged(selectedPos); // 새로 선택된 아이템 뷰를 업데이트합니다.
            }
        });
        holder.itemView.setSelected(selectedPos == position);
    }
    public int getSelectedPos() {
        return selectedPos;
    }
    public Report getSelectedItemKey() {
        if (selectedPos != RecyclerView.NO_POSITION) {
            return reportList.get(selectedPos); // 선택된 아이템의 키를 반환합니다.
        } else {
            return null;
        }
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }
}