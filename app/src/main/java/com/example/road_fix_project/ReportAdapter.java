package com.example.road_fix_project;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {
    private List<Report> reportList;
    private int selectedPosition = -1;

    public ReportAdapter(List<Report> reportList) {
        this.reportList=reportList;
    }

    public class ReportViewHolder extends RecyclerView.ViewHolder {
        public TextView tvDate;
        public TextView tvStatus;
        public TextView tvOrder;

        public ReportViewHolder(View view) {
            super(view);
            tvDate = view.findViewById(R.id.tv_date);
            tvStatus = view.findViewById(R.id.tv_status);
            tvOrder=view.findViewById(R.id.tv_order);

            itemView.setOnClickListener(new View.OnClickListener() {
                // 클릭 이벤트가 발생하면 여기의 코드가 실행됩니다.
                // 예를 들어, 새로운 액티비티를 시작하거나, 토스트 메시지를 표시할 수 있습니다.
                @Override
                public void onClick(View view) {
                    selectedPosition = getAdapterPosition();
                    notifyDataSetChanged();
                }
            });
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

        if (selectedPosition == position) {
            holder.itemView.setBackgroundColor(Color.YELLOW);
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }
}
