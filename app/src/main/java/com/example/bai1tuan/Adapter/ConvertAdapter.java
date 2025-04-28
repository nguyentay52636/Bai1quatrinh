package com.example.bai1tuan.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bai1tuan.R;
import com.example.bai1tuan.database.ConversionHistory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConvertAdapter extends RecyclerView.Adapter<ConvertAdapter.ConvertViewHolder> {
    private List<ConversionHistory> historyList;

    public ConvertAdapter() {
        this.historyList = new ArrayList<>();
    }

    @NonNull
    @Override
    public ConvertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_convert_history, parent, false);
        return new ConvertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConvertViewHolder holder, int position) {
        ConversionHistory history = historyList.get(position);
        
        // Hiển thị ngày giờ
        holder.tvDateTime.setText(history.date);
        
        // Hiển thị số tiền và đơn vị tiền tệ gốc
        holder.tvFromAmount.setText(String.format(Locale.getDefault(),
                "%.2f %s", history.inputAmount, history.fromCurrency));
        
        // Hiển thị số tiền và đơn vị tiền tệ đích
        holder.tvToAmount.setText(String.format(Locale.getDefault(),
                "%.2f %s", history.resultAmount, history.toCurrency));
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public void setData(List<ConversionHistory> newList) {
        this.historyList = newList;
        notifyDataSetChanged();
    }

    public static class ConvertViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateTime;
        TextView tvFromAmount;
        TextView tvToAmount;

        public ConvertViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvFromAmount = itemView.findViewById(R.id.tvFromAmount);
            tvToAmount = itemView.findViewById(R.id.tvToAmount);
        }
    }
}
