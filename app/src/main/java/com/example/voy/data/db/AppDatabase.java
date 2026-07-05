package com.example.voy.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.voy.data.converter.StringListConverter;
import com.example.voy.data.converter.TripItemTypeConverter;
import com.example.voy.data.dao.LandmarkDao;
import com.example.voy.data.dao.TripDao;
import com.example.voy.data.dao.TripItemDao;
import com.example.voy.data.entities.LandmarkEntity;
import com.example.voy.data.entities.TripEntity;
import com.example.voy.data.entities.TripItemEntity;

@Database(
        entities = {TripEntity.class, TripItemEntity.class, LandmarkEntity.class},
        version = 6,
        exportSchema = false
)
@TypeConverters({TripItemTypeConverter.class, StringListConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;
    private static final String DB_NAME = "voy_journal_db";
    public abstract TripDao tripDao();
    public abstract TripItemDao tripItemDao();
    public abstract LandmarkDao landmarkDao();

    public static AppDatabase getInstance(Context context){
        if(INSTANCE == null){
            synchronized (AppDatabase.class){
                if(INSTANCE == null){
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, DB_NAME).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}
