package com.example.bai1tuan.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ConversionHistoryDao {
    @Insert
    void insert(ConversionHistory history);

    @Query("SELECT * FROM conversion_history ORDER BY id DESC")
    List<ConversionHistory> getAllHistory();
}
