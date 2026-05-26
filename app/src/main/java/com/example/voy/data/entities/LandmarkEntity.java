package com.example.voy.data.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName="landmarks")
public class LandmarkEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String tripId;
    public String name;
    public Double lat;
    public Double lng;
    public int dayNumber;
    public boolean isVisited;
    public long createdAt;

    public LandmarkEntity(@NonNull String id, String tripId, String name,
                          Double lat, Double lng,int dayNumber, boolean isVisited, long createdAt) {
        this.id        = id;
        this.tripId    = tripId;
        this.name      = name;
        this.lat       = lat;
        this.lng       = lng;
        this.dayNumber = dayNumber;
        this.isVisited = isVisited;
        this.createdAt = createdAt;
    }
}
