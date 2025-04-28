package com.example.bai1tuan.ui.gallery;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.example.bai1tuan.Adapter.ConvertAdapter;
import com.example.bai1tuan.database.AppDatabase;
import com.example.bai1tuan.database.ConversionHistory;
import com.example.bai1tuan.databinding.FragmentGalleryBinding;

import java.util.List;

public class GalleryFragment extends Fragment {

    private FragmentGalleryBinding binding;
    private AppDatabase db;
    private ConvertAdapter convertAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Khởi tạo RecyclerView với LayoutManager
        RecyclerView recyclerView = binding.recyclerViewHistory;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // Khởi tạo Adapter
        convertAdapter = new ConvertAdapter();
        recyclerView.setAdapter(convertAdapter);

        // Khởi tạo Database
        db = Room.databaseBuilder(requireContext(),
                AppDatabase.class, "conversion_db")
                .allowMainThreadQueries()
                .build();

        // Load dữ liệu lịch sử
        loadHistoryData();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload dữ liệu mỗi khi fragment được hiển thị
        loadHistoryData();
    }

    private void loadHistoryData() {
        // Lấy danh sách lịch sử từ database
        List<ConversionHistory> historyList = db.conversionHistoryDao().getAllHistory();
        // Cập nhật RecyclerView
        convertAdapter.setData(historyList);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}