package com.example.voy.data.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.voy.data.converter.StringListConverter;

import java.util.List;

@Entity(tableName = "trips")
public class TripEntity {
    @PrimaryKey
    @NonNull
    public String id;
    @NonNull
    public String userId;
    public String title;
    public long startTime;
    @Nullable
    public Long endTime;
    @NonNull
    public String status; //active or finished
    @ColumnInfo(name = "notes")
    public String notes;

    @ColumnInfo(name = "attachments")
    @TypeConverters(StringListConverter.class)
    public List<String> attachments;

    public TripEntity(long startTime, @NonNull String id, @Nullable Long endTime, @NonNull String status, String title, @NonNull String userId) {
        this.startTime = startTime;
        this.id = id;
        this.endTime = endTime;
        this.status = status;
        this.title = title;
        this.userId = userId;
    }

    @NonNull
    public String getId() {
        return id;
    }
}
