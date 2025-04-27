package com.example.bai1tuan.ui.home;

import android.content.Intent;
import android.os.Bundle;
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

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private Spinner spinnerFrom, spinnerTo;
    private EditText editTextAmount;
    private Button buttonConvert, buttonHistory;
    private TextView textViewResult;

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
        buttonHistory = binding.buttonHistory;
        textViewResult = binding.textViewResult;

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

    private void fetchExchangeRates(String baseCurrency) {
        Call<ExchangeRateResponse> call = apiService.getExchangeRates(Key.API_KEY, baseCurrency);
        call.enqueue(new Callback<ExchangeRateResponse>() {
            @Override
            public void onResponse(Call<ExchangeRateResponse> call, Response<ExchangeRateResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    conversionRates = response.body().getConversionRates();
                    Toast.makeText(requireContext(), "Tải tỷ giá thành công!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Lỗi tải tỷ giá", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ExchangeRateResponse> call, Throwable t) {
                Toast.makeText(requireContext(), "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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

        double amount = Double.parseDouble(amountStr);

        // Lấy tỷ giá từ base currency sang fromCurrency và toCurrency
        double fromRate = conversionRates.get(fromCurrency);
        double toRate = conversionRates.get(toCurrency);

        // Tính ra số tiền đã chuyển đổi
        double result = amount / fromRate * toRate;

        textViewResult.setText(String.format(Locale.getDefault(), "%.2f %s", result, toCurrency));

        // Lưu vào lịch sử
        saveHistory(fromCurrency, toCurrency, amount, result);
    }

    private void saveHistory(String from, String to, double inputAmount, double resultAmount) {
        ConversionHistory history = new ConversionHistory();
        history.fromCurrency = from;
        history.toCurrency = to;
        history.inputAmount = inputAmount;
        history.resultAmount = resultAmount;
        history.date = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());

        db.conversionHistoryDao().insert(history);
    }

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