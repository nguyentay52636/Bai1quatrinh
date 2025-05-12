package com.example.bai1tuan.ui.slideshow;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.bai1tuan.databinding.FragmentSlideshowBinding;
import com.example.bai1tuan.model.ExchangeRateData;
import com.example.bai1tuan.services.ExchangeRateApiService;
import com.example.bai1tuan.services.ExchangeRateResponse;
import com.example.bai1tuan.services.Key;
import com.github.mikephil.charting.charts.CandleStickChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
    private CandleStickChart candleChart;
    private Spinner spinnerCurrency;
    private TextView textViewLastUpdate;
    private ExchangeRateApiService apiService;
    private List<ExchangeRateData> rateDataList = new ArrayList<>();
    private Handler updateHandler;
    private static final long UPDATE_INTERVAL = 60000; // Update every 1 minute
    private String currentTimePeriod = "HOUR"; // Default time period
    private List<String> currencyList = new ArrayList<>();
    private ArrayAdapter<String> currencyAdapter;

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

        updateHandler = new Handler(Looper.getMainLooper());
        candleChart = binding.candleChart;
        spinnerCurrency = binding.spinnerCurrency;
        textViewLastUpdate = binding.textViewLastUpdate;

        currencyAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, currencyList);
        spinnerCurrency.setAdapter(currencyAdapter);
        spinnerCurrency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fetchLatestRate();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        setupCandleChart();
        setupTimePeriodSelection();
        setupApiService();

        binding.buttonRefresh.setOnClickListener(v -> {
            stopAutoUpdate();
            fetchLatestRate();
            startAutoUpdate();
        });

        fetchCurrencyListAndFirstRate();
        startAutoUpdate();

        return root;
    }

    private void setupTimePeriodSelection() {
        binding.radioGroupTimePeriod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == binding.radioHour.getId()) {
                currentTimePeriod = "HOUR";
            } else if (checkedId == binding.radioDay.getId()) {
                currentTimePeriod = "DAY";
            } else if (checkedId == binding.radioMonth.getId()) {
                currentTimePeriod = "MONTH";
            } else if (checkedId == binding.radioYear.getId()) {
                currentTimePeriod = "YEAR";
            }
            updateCandleChartData();
        });
    }

    private void setupCandleChart() {
        candleChart.getDescription().setEnabled(false);
        candleChart.setDrawGridBackground(false);
        candleChart.setBackgroundColor(Color.WHITE);
        candleChart.setExtraOffsets(10f, 10f, 10f, 20f);

        XAxis xAxis = candleChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.parseColor("#424242"));
        xAxis.setTextSize(13f);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#F0F0F0"));
        xAxis.setGridLineWidth(1.1f);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawLabels(true);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            private final SimpleDateFormat sdfDay = new SimpleDateFormat("dd/MM", Locale.getDefault());
            private final SimpleDateFormat sdfMonth = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
            private final SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy", Locale.getDefault());

            @Override
            public String getFormattedValue(float value) {
                Date date = new Date((long) value);
                switch (currentTimePeriod) {
                    case "HOUR":
                        return sdf.format(date);
                    case "DAY":
                        return sdfDay.format(date);
                    case "MONTH":
                        return sdfMonth.format(date);
                    case "YEAR":
                        return sdfYear.format(date);
                    default:
                        return sdf.format(date);
                }
            }
        });

        YAxis leftAxis = candleChart.getAxisLeft();
        leftAxis.setTextColor(Color.parseColor("#424242"));
        leftAxis.setTextSize(13f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#F0F0F0"));
        leftAxis.setGridLineWidth(1.1f);
        leftAxis.setAxisLineColor(Color.parseColor("#424242"));
        leftAxis.setDrawAxisLine(true);
        leftAxis.setDrawLabels(true);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                String currency = spinnerCurrency.getSelectedItem().toString();
                switch (currency) {
                    case "VND": return String.format(Locale.getDefault(), "%,.0f", value);
                    case "JPY": return String.format(Locale.getDefault(), "%,.0f", value);
                    case "KRW": return String.format(Locale.getDefault(), "%,.0f", value);
                    default: return String.format(Locale.getDefault(), "%.2f", value);
                }
            }
        });
        leftAxis.setLabelCount(6, true);
        leftAxis.setSpaceTop(8f);
        leftAxis.setSpaceBottom(8f);

        candleChart.getAxisRight().setEnabled(false);

        candleChart.getLegend().setEnabled(true);
        candleChart.getLegend().setTextColor(Color.parseColor("#424242"));
        candleChart.getLegend().setTextSize(14f);
        candleChart.getLegend().setFormSize(14f);
        candleChart.getLegend().setXEntrySpace(10f);
        candleChart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        candleChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        candleChart.getLegend().setOrientation(Legend.LegendOrientation.HORIZONTAL);
        candleChart.getLegend().setDrawInside(false);

        candleChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, Highlight h) {
                if (e instanceof CandleEntry) {
                    CandleEntry ce = (CandleEntry) e;
                    String currency = spinnerCurrency.getSelectedItem().toString();
                    String formattedRate = String.format(Locale.getDefault(), "O: %.2f, H: %.2f, L: %.2f, C: %.2f", ce.getOpen(), ce.getHigh(), ce.getLow(), ce.getClose());
                    binding.textViewCurrentRate.setText("1 USD = " + formattedRate + " " + currency);
                }
            }
            @Override
            public void onNothingSelected() {}
        });
    }

    private void updateCandleChartData() {
        // TEST: Giả lập dữ liệu nến OHLC nếu chưa có đủ dữ liệu thực tế
        List<CandleEntry> candleEntries = new ArrayList<>();
        if (rateDataList.size() < 40) {
            long now = System.currentTimeMillis();
            double lastClose = 25 + Math.random() * 5;
            for (int i = 0; i < 40; i++) {
                double open = lastClose;
                double close = open + (Math.random() - 0.5) * 2;
                double high = Math.max(open, close) + Math.random();
                double low = Math.min(open, close) - Math.random();
                lastClose = close;
                candleEntries.add(new com.github.mikephil.charting.data.CandleEntry(
                    now - (40 - i) * 60000,
                    (float) high,
                    (float) low,
                    (float) open,
                    (float) close
                ));
            }
        } else {
            Calendar calendar = Calendar.getInstance();
            long currentTime = calendar.getTimeInMillis();
            long startTime;
            switch (currentTimePeriod) {
                case "HOUR":
                    calendar.add(Calendar.HOUR, -1);
                    break;
                case "DAY":
                    calendar.add(Calendar.DAY_OF_MONTH, -1);
                    break;
                case "MONTH":
                    calendar.add(Calendar.MONTH, -1);
                    break;
                case "YEAR":
                    calendar.add(Calendar.YEAR, -1);
                    break;
            }
            startTime = calendar.getTimeInMillis();
            for (int i = 0; i < rateDataList.size(); i++) {
                com.example.bai1tuan.model.ExchangeRateData data = rateDataList.get(i);
                if (data.getTimestamp() >= startTime && data.getTimestamp() <= currentTime) {
                    float value = (float) data.getRate();
                    candleEntries.add(new com.github.mikephil.charting.data.CandleEntry(
                        data.getTimestamp(), value, value, value, value));
                }
            }
        }
        updateCandleChartWithEntries(candleEntries);
    }

    private void updateCandleChartWithEntries(List<CandleEntry> entries) {
        if (entries.isEmpty()) return;

        String selectedCurrency = spinnerCurrency.getSelectedItem().toString();
        CandleDataSet dataSet = new CandleDataSet(entries, "USD/" + selectedCurrency);

        // Màu sắc chuyên nghiệp như TradingView
        int green = Color.parseColor("#26A69A"); // xanh tăng
        int red = Color.parseColor("#EF5350");   // đỏ giảm

        dataSet.setShadowColorSameAsCandle(true);
        dataSet.setShadowWidth(6f);
        dataSet.setDecreasingColor(red);
        dataSet.setDecreasingPaintStyle(Paint.Style.FILL);
        dataSet.setIncreasingColor(green);
        dataSet.setIncreasingPaintStyle(Paint.Style.FILL);
        dataSet.setNeutralColor(Color.parseColor("#BDBDBD"));
        dataSet.setBarSpace(0.02f);
        dataSet.setDrawValues(false);
        dataSet.setHighlightLineWidth(2.5f);
        dataSet.setHighlightEnabled(true);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(14f);
        dataSet.setDrawIcons(false);
        dataSet.setDrawHorizontalHighlightIndicator(false);
        dataSet.setDrawVerticalHighlightIndicator(true);

        CandleData candleData = new CandleData(dataSet);
        candleChart.setData(candleData);
        candleChart.setAutoScaleMinMaxEnabled(true);
        candleChart.animateX(1000);
        candleChart.invalidate();
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
                        long currentTime = System.currentTimeMillis();
                        rateDataList.add(new ExchangeRateData(currentTime, rate));
                        updateCandleChartData();
                        updateLastUpdateTime();
                    }
                }
            }

            @Override
            public void onFailure(Call<ExchangeRateResponse> call, Throwable t) {}
        });
    }

    private void updateLastUpdateTime() {
        String currentTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                .format(new Date());
        textViewLastUpdate.setText("Cập nhật lúc: " + currentTime);
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

    private void startAutoUpdate() {
        updateHandler.postDelayed(updateRunnable, UPDATE_INTERVAL);
    }

    private void stopAutoUpdate() {
        updateHandler.removeCallbacks(updateRunnable);
    }

    private void setupApiService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Key.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ExchangeRateApiService.class);
    }

    private void fetchCurrencyListAndFirstRate() {
        Call<ExchangeRateResponse> call = apiService.getExchangeRates(Key.API_KEY, "USD");
        call.enqueue(new Callback<ExchangeRateResponse>() {
            @Override
            public void onResponse(Call<ExchangeRateResponse> call, Response<ExchangeRateResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ExchangeRateResponse data = response.body();
                    if (data.isSuccess()) {
                        Map<String, Double> rates = data.getConversionRates();
                        currencyList.clear();
                        currencyList.addAll(rates.keySet());
                        currencyAdapter.notifyDataSetChanged();
                        // Chọn USD mặc định nếu có
                        int usdIndex = currencyList.indexOf("USD");
                        if (usdIndex >= 0) spinnerCurrency.setSelection(usdIndex);
                        fetchLatestRate();
                    }
                }
            }
            @Override
            public void onFailure(Call<ExchangeRateResponse> call, Throwable t) {}
        });
    }

    @Override
    public void onDestroyView() {
        stopAutoUpdate();
        super.onDestroyView();
        binding = null;
    }
}