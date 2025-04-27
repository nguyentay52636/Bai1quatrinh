package com.example.bai1tuan.ui.gallery;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.example.bai1tuan.database.AppDatabase;
import com.example.bai1tuan.database.ConversionHistory;
import com.example.bai1tuan.databinding.FragmentGalleryBinding;

import java.util.List;
import java.util.Locale;

public class GalleryFragment extends Fragment {

    private FragmentGalleryBinding binding;
    private AppDatabase db;
    private HistoryAdapter historyAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Setup RecyclerView
        RecyclerView recyclerView = binding.recyclerViewHistory;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        historyAdapter = new HistoryAdapter();
        recyclerView.setAdapter(historyAdapter);

        // Setup Database
        db = Room.databaseBuilder(requireContext(),
                AppDatabase.class, "conversion_db")
                .allowMainThreadQueries()
                .build();

        // Load history
        loadHistory();

        return root;
    }

    private void loadHistory() {
        List<ConversionHistory> historyList = db.conversionHistoryDao().getAllHistory();
        historyAdapter.setHistoryList(historyList);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {
        private List<ConversionHistory> historyList;

        public void setHistoryList(List<ConversionHistory> historyList) {
            this.historyList = historyList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new HistoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
            ConversionHistory history = historyList.get(position);
            holder.text1.setText(String.format(Locale.getDefault(),
                    "%.2f %s → %.2f %s",
                    history.inputAmount, history.fromCurrency,
                    history.resultAmount, history.toCurrency));
            holder.text2.setText(history.date);
        }

        @Override
        public int getItemCount() {
            return historyList != null ? historyList.size() : 0;
        }

        static class HistoryViewHolder extends RecyclerView.ViewHolder {
            TextView text1;
            TextView text2;

            HistoryViewHolder(View view) {
                super(view);
                text1 = view.findViewById(android.R.id.text1);
                text2 = view.findViewById(android.R.id.text2);
            }
        }
    }
}