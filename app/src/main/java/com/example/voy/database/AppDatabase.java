package com.example.voy.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.voy.dao.TravelEntryDao;
import com.example.voy.model.TravelEntry;

@Database(entities = {TravelEntry.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    public abstract TravelEntryDao travelEntryDao();

    private static AppDatabase INSTANCE;

    public static synchronized AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "travel_db")
                    .allowMainThreadQueries() // remove for production, use background thread
                    .build();
        }
        return INSTANCE;
    }
}
