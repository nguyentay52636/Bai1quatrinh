package com.example.bai1tuan.services;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class ExchangeRateResponse {
    @SerializedName("result")
    private String result;

    @SerializedName("base_code")
    private String baseCode;

    @SerializedName("conversion_rates")
    private Map<String, Double> conversionRates;

    public String getResult() {
        return result;
    }

    public String getBaseCode() {
        return baseCode;
    }

    public Map<String, Double> getConversionRates() {
        return conversionRates;
    }

    public boolean isSuccess() {
        return "success".equals(result);
    }
}
