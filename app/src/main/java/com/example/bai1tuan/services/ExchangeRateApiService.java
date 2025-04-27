package com.example.bai1tuan.services;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ExchangeRateApiService {
    @GET("{apiKey}/latest/{base}")
    Call<ExchangeRateResponse> getExchangeRates(
            @Path("apiKey") String apiKey,
            @Path("base") String baseCurrency
    );
}