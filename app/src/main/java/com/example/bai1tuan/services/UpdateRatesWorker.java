package com.example.bai1tuan.services;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;



import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UpdateRatesWorker extends Worker {
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
            }

            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure();
        }
    }
}