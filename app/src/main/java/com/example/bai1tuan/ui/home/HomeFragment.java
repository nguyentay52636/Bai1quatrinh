package com.example.bai1tuan.ui.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.room.Room;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;


import com.example.bai1tuan.database.AppDatabase;
import com.example.bai1tuan.database.ConversionHistory;
import com.example.bai1tuan.databinding.FragmentHomeBinding;
import com.example.bai1tuan.services.ExchangeRateApiService;
import com.example.bai1tuan.services.ExchangeRateResponse;
import com.example.bai1tuan.services.Key;
import com.example.bai1tuan.services.UpdateRatesWorker;


import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private Spinner spinnerFrom, spinnerTo;
    private EditText editTextAmount;
    private Button buttonConvert, buttonHistory;
    private TextView textViewResult, textViewStatus;

    private ExchangeRateApiService apiService;
    private Map<String, Double> conversionRates;
    private AppDatabase db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize views
        spinnerFrom = binding.spinnerFrom;
        spinnerTo = binding.spinnerTo;
        editTextAmount = binding.editTextAmount;
        buttonConvert = binding.buttonConvert;
        textViewResult = binding.textViewResult;
        textViewStatus = binding.textViewStatus;

        // Setup spinner data
        String[] currencies = {"USD", "EUR", "VND", "JPY", "CNY", "KRW"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, currencies);
        spinnerFrom.setAdapter(adapter);
        spinnerTo.setAdapter(adapter);

        // Setup Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Key.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ExchangeRateApiService.class);

        // Setup Room DB
        db = Room.databaseBuilder(requireContext(),
                        AppDatabase.class, "conversion_db")
                .allowMainThreadQueries() // Tạm cho phép để đơn giản, sản phẩm thật thì dùng AsyncTask/Coroutine
                .build();

        fetchExchangeRates("USD"); // Load mặc định USD khi vào app
        setupDailyUpdate();

        buttonConvert.setOnClickListener(v -> convertCurrency());



        return root;
    }

    private void updateStatus(String message) {
        if (textViewStatus != null) {
            textViewStatus.setText(message);
        }
    }

    private void fetchExchangeRates(String baseCurrency) {
        Log.d("ExchangeRates", "Starting fetchExchangeRates for " + baseCurrency);
        updateStatus("Đang tải tỷ giá...");
        
        // First try to load from SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("exchange_rates", Context.MODE_PRIVATE);
        String ratesJson = prefs.getString("rates", null);
        if (ratesJson != null) {
            try {
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, Double>>(){}.getType();
                conversionRates = gson.fromJson(ratesJson, type);
                Log.d("ExchangeRates", "Loaded rates from SharedPreferences: " + ratesJson);
                if (conversionRates != null && !conversionRates.isEmpty()) {
                    Log.d("ExchangeRates", "Successfully loaded rates, size: " + conversionRates.size());
                    updateStatus("Đã tải tỷ giá từ bộ nhớ");
                } else {
                    Log.e("ExchangeRates", "Loaded rates are null or empty");
                    updateStatus("Đang tải tỷ giá mới...");
                }
            } catch (Exception e) {
                Log.e("ExchangeRates", "Error loading rates from SharedPreferences", e);
                e.printStackTrace();
                updateStatus("Đang tải tỷ giá mới...");
            }
        } else {
            Log.d("ExchangeRates", "No rates found in SharedPreferences");
            updateStatus("Đang tải tỷ giá mới...");
        }

        // Then fetch fresh rates from API
        Log.d("ExchangeRates", "Making API call to: " + Key.BASE_URL + Key.API_KEY + "/latest/" + baseCurrency);
        Call<ExchangeRateResponse> call = apiService.getExchangeRates(Key.API_KEY, baseCurrency);
        call.enqueue(new Callback<ExchangeRateResponse>() {
            @Override
            public void onResponse(Call<ExchangeRateResponse> call, Response<ExchangeRateResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ExchangeRateResponse exchangeResponse = response.body();
                    if (exchangeResponse.isSuccess()) {
                        conversionRates = exchangeResponse.getConversionRates();
                        Log.d("ExchangeRates", "Received new rates from API: " + conversionRates);
                        if (conversionRates != null && !conversionRates.isEmpty()) {
                            // Save to SharedPreferences
                            SharedPreferences.Editor editor = prefs.edit();
                            Gson gson = new Gson();
                            String newRatesJson = gson.toJson(conversionRates);
                            editor.putString("rates", newRatesJson);
                            editor.apply();
                            Log.d("ExchangeRates", "Saved new rates to SharedPreferences. Base currency: " + exchangeResponse.getBaseCode());
                            updateStatus("Tỷ giá đã được cập nhật");
                        } else {
                            Log.e("ExchangeRates", "API returned null or empty rates");
                            updateStatus("Lỗi: API trả về tỷ giá rỗng");
                        }
                    } else {
                        Log.e("ExchangeRates", "API response indicates failure: " + exchangeResponse.getResult());
                        updateStatus("Lỗi: " + exchangeResponse.getResult());
                    }
                } else {
                    Log.e("ExchangeRates", "API response not successful: " + response.code() + 
                        ", Error body: " + (response.errorBody() != null ? response.errorBody().toString() : "null"));
                    updateStatus("Lỗi tải tỷ giá: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ExchangeRateResponse> call, Throwable t) {
                Log.e("ExchangeRates", "API call failed: " + t.getMessage(), t);
                updateStatus("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    private void convertCurrency() {
        if (conversionRates == null) {
            Toast.makeText(requireContext(), "Chưa có tỷ giá!", Toast.LENGTH_SHORT).show();
            return;
        }

        String fromCurrency = spinnerFrom.getSelectedItem().toString();
        String toCurrency = spinnerTo.getSelectedItem().toString();
        String amountStr = editTextAmount.getText().toString();

        if (amountStr.isEmpty()) {
            Toast.makeText(requireContext(), "Nhập số tiền!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);

            // Vì API trả về tỷ giá với USD là base currency
            if (fromCurrency.equals("USD")) {
                // Nếu from là USD, chỉ cần nhân với tỷ giá của to currency
                double toRate = conversionRates.get(toCurrency);
                double result = amount * toRate;
                textViewResult.setText(String.format(Locale.getDefault(), "%.2f %s", result, toCurrency));
            } else if (toCurrency.equals("USD")) {
                // Nếu to là USD, chia cho tỷ giá của from currency
                double fromRate = conversionRates.get(fromCurrency);
                double result = amount / fromRate;
                textViewResult.setText(String.format(Locale.getDefault(), "%.2f %s", result, toCurrency));
            } else {
                // Nếu cả hai đều không phải USD, đầu tiên chuyển sang USD rồi chuyển sang currency đích
                double fromRate = conversionRates.get(fromCurrency);
                double toRate = conversionRates.get(toCurrency);
                double result = (amount / fromRate) * toRate;
                textViewResult.setText(String.format(Locale.getDefault(), "%.2f %s", result, toCurrency));
            }

            // Lưu vào lịch sử
//            saveHistory(fromCurrency, toCurrency, amount, Double.parseDouble(textViewResult.getText().toString().split(" ")[0]));
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Số tiền không hợp lệ!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Lỗi chuyển đổi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

//    private void saveHistory(String from, String to, double inputAmount, double resultAmount) {
//        ConversionHistory history = new ConversionHistory();
//        history.fromCurrency = from;
//        history.toCurrency = to;
//        history.inputAmount = inputAmount;
//        history.resultAmount = resultAmount;
//        history.date = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
//
//        db.conversionHistoryDao().insert(history);
//    }

    private void setupDailyUpdate() {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(UpdateRatesWorker.class,
                1, TimeUnit.DAYS)
                .build();

        WorkManager.getInstance(requireContext()).enqueue(workRequest);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}