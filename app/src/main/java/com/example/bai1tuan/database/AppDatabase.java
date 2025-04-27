package com.example.bai1tuan.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {ConversionHistory.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ConversionHistoryDao conversionHistoryDao();
}
