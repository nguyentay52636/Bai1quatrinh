package com.example.bai1tuan.model;

public class ExchangeRateData {
    private long timestamp;
    private double rate;

    public ExchangeRateData(long timestamp, double rate) {
        this.timestamp = timestamp;
        this.rate = rate;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getRate() {
        return rate;
    }
} 