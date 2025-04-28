package com.example.bai1tuan.ui.slideshow;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.bai1tuan.databinding.FragmentSlideshowBinding;
import com.example.bai1tuan.model.ExchangeRateData;
import com.example.bai1tuan.services.ExchangeRateApiService;
import com.example.bai1tuan.services.ExchangeRateResponse;
import com.example.bai1tuan.services.Key;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SlideshowFragment extends Fragment {

    private FragmentSlideshowBinding binding;
    private LineChart lineChart;
    private Spinner spinnerCurrency;
    private TextView textViewLastUpdate;
    private ExchangeRateApiService apiService;
    private List<ExchangeRateData> rateDataList = new ArrayList<>();
    private Handler updateHandler;
    private static final long UPDATE_INTERVAL = 60000; // Update every 1 minute

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            fetchLatestRate();
            updateHandler.postDelayed(this, UPDATE_INTERVAL);
        }
    };

    public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSlideshowBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Khởi tạo handler
        updateHandler = new Handler(Looper.getMainLooper());

        // Khởi tạo views
        lineChart = binding.lineChart;
        spinnerCurrency = binding.spinnerCurrency;
        textViewLastUpdate = binding.textViewLastUpdate;

        // Setup spinner
        setupSpinner();

        // Setup chart
        setupChart();

        // Setup API service
        setupApiService();

        // Setup refresh button
        binding.buttonRefresh.setOnClickListener(v -> {
            stopAutoUpdate();
            fetchLatestRate();
            startAutoUpdate();
        });

        // Load dữ liệu ban đầu và bắt đầu cập nhật tự động
        fetchLatestRate();
        startAutoUpdate();

        return root;
    }

    private void startAutoUpdate() {
        updateHandler.postDelayed(updateRunnable, UPDATE_INTERVAL);
    }

    private void stopAutoUpdate() {
        updateHandler.removeCallbacks(updateRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        startAutoUpdate();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoUpdate();
    }

    private void setupSpinner() {
        String[] currencies = {"EUR", "VND", "JPY", "CNY", "KRW"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, currencies);
        spinnerCurrency.setAdapter(adapter);
        spinnerCurrency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fetchLatestRate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupChart() {
        // Cấu hình chung cho biểu đồ
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setDrawGridBackground(false);
        lineChart.setDrawBorders(true);
        lineChart.setBorderColor(Color.LTGRAY);
        lineChart.setBorderWidth(1f);
        lineChart.setBackgroundColor(Color.WHITE);
        
        // Cấu hình legend (chú thích)
        lineChart.getLegend().setEnabled(true);
        lineChart.getLegend().setTextColor(Color.BLACK);
        lineChart.getLegend().setTextSize(12f);
        
        // Cấu hình trục X
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.LTGRAY);
        xAxis.setGridLineWidth(0.5f);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

            @Override
            public String getFormattedValue(float value) {
                return sdf.format(new Date((long) value));
            }
        });

        // Cấu hình trục Y
        lineChart.getAxisLeft().setTextColor(Color.BLACK);
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisLeft().setGridColor(Color.LTGRAY);
        lineChart.getAxisLeft().setGridLineWidth(0.5f);
        lineChart.getAxisRight().setEnabled(false);
    }

    private void setupApiService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Key.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ExchangeRateApiService.class);
    }

    private void fetchLatestRate() {
        String selectedCurrency = spinnerCurrency.getSelectedItem().toString();
        Call<ExchangeRateResponse> call = apiService.getExchangeRates(Key.API_KEY, "USD");
        
        call.enqueue(new Callback<ExchangeRateResponse>() {
            @Override
            public void onResponse(Call<ExchangeRateResponse> call, Response<ExchangeRateResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ExchangeRateResponse data = response.body();
                    if (data.isSuccess()) {
                        Map<String, Double> rates = data.getConversionRates();
                        double rate = rates.get(selectedCurrency);
                        updateChart(rate);
                        updateLastUpdateTime();
                    }
                }
            }

            @Override
            public void onFailure(Call<ExchangeRateResponse> call, Throwable t) {
                // Xử lý lỗi
            }
        });
    }

    private void updateChart(double newRate) {
        // Thêm dữ liệu mới
        long currentTime = System.currentTimeMillis();
        rateDataList.add(new ExchangeRateData(currentTime, newRate));

        // Giới hạn số điểm dữ liệu (giữ 24 điểm gần nhất)
        if (rateDataList.size() > 24) {
            rateDataList.remove(0);
        }

        // Chuyển đổi dữ liệu cho biểu đồ
        List<Entry> entries = new ArrayList<>();
        for (ExchangeRateData data : rateDataList) {
            entries.add(new Entry(data.getTimestamp(), (float) data.getRate()));
        }

        String selectedCurrency = spinnerCurrency.getSelectedItem().toString();
        
        // Cấu hình đường biểu đồ
        LineDataSet dataSet = new LineDataSet(entries, "USD/" + selectedCurrency);
        dataSet.setColor(Color.rgb(65, 105, 225)); // Màu xanh dương đậm
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(Color.rgb(65, 105, 225));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleRadius(2f);
        dataSet.setValueTextSize(12f);
        dataSet.setDrawValues(true);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Làm mượt đường
        dataSet.setCubicIntensity(0.2f);
        
        // Định dạng giá trị hiển thị theo loại tiền tệ
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                switch (selectedCurrency) {
                    case "VND":
                        return String.format(Locale.getDefault(), "%,.0f đ", value);
                    case "EUR":
                        return String.format(Locale.getDefault(), "€%.2f", value);
                    case "JPY":
                        return String.format(Locale.getDefault(), "¥%.0f", value);
                    case "CNY":
                        return String.format(Locale.getDefault(), "¥%.2f", value);
                    case "KRW":
                        return String.format(Locale.getDefault(), "₩%.0f", value);
                    default:
                        return String.format(Locale.getDefault(), "%.2f", value);
                }
            }
        });

        // Thêm gradient cho area dưới đường
        dataSet.setDrawFilled(true);
        int startColor = Color.argb(150, 65, 105, 225); // Màu xanh dương trong suốt
        int endColor = Color.argb(0, 65, 105, 225); // Trong suốt hoàn toàn
        dataSet.setFillDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{startColor, endColor}));

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        
        // Animation
        lineChart.animateX(500);
        
        // Refresh biểu đồ
        lineChart.invalidate();
        
        // Cập nhật giá trị hiện tại
        updateCurrentRate(newRate, selectedCurrency);
    }

    private void updateCurrentRate(double rate, String currency) {
        String formattedRate;
        switch (currency) {
            case "VND":
                formattedRate = String.format(Locale.getDefault(), "%,.0f VND", rate);
                break;
            case "EUR":
                formattedRate = String.format(Locale.getDefault(), "€%.2f", rate);
                break;
            case "JPY":
                formattedRate = String.format(Locale.getDefault(), "¥%.0f", rate);
                break;
            case "CNY":
                formattedRate = String.format(Locale.getDefault(), "¥%.2f", rate);
                break;
            case "KRW":
                formattedRate = String.format(Locale.getDefault(), "₩%.0f", rate);
                break;
            default:
                formattedRate = String.format(Locale.getDefault(), "%.2f", rate);
        }
        
        // Hiển thị tỉ giá hiện tại
        if (binding.textViewCurrentRate != null) {
            binding.textViewCurrentRate.setText("1 USD = " + formattedRate);
        }
    }

    private void updateLastUpdateTime() {
        String currentTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                .format(new Date());
        textViewLastUpdate.setText("Cập nhật lúc: " + currentTime);
    }

    @Override
    public void onDestroyView() {
        stopAutoUpdate();
        super.onDestroyView();
        binding = null;
    }
}