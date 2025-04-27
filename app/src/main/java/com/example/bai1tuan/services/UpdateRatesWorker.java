package com.example.bai1tuan.services;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UpdateRatesWorker extends Worker {
    private static final String PREF_NAME = "exchange_rates";
    private static final String KEY_RATES = "rates";

    public UpdateRatesWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(Key.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ExchangeRateApiService apiService = retrofit.create(ExchangeRateApiService.class);
            Call<ExchangeRateResponse> call = apiService.getExchangeRates(Key.API_KEY, "USD");

            Response<ExchangeRateResponse> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                // Lưu response vào SharedPreferences
                SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                
                Gson gson = new Gson();
                String ratesJson = gson.toJson(response.body().getConversionRates());
                editor.putString(KEY_RATES, ratesJson);
                editor.apply();
                
                return Result.success();
            }

            return Result.failure();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure();
        }
    }
}