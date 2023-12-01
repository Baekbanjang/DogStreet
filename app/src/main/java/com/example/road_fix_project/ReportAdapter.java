package com.example.road_fix_project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {
    private List<Report> reportList;

    public ReportAdapter(List<Report> reportList) {
        this.reportList=reportList;
    }

    public static class ReportViewHolder extends RecyclerView.ViewHolder {
        public TextView tvDate;
        public TextView tvStatus;
        public TextView tvOrder;

        public ReportViewHolder(View view) {
            super(view);
            tvDate = view.findViewById(R.id.tv_date);
            tvStatus = view.findViewById(R.id.tv_status);
            tvOrder=view.findViewById(R.id.tv_order);
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
        holder.tvDate.setText("날짜: " + report.date);
        holder.tvStatus.setText("상태: " + report.status);

        // 순서를 표시
        int order = position + 1; // position은 0부터 시작하므로 1을 더합니다.
        holder.tvOrder.setText(String.valueOf(order));
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }
}
