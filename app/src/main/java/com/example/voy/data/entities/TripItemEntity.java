package com.example.voy.data.entities;

import static androidx.room.ForeignKey.CASCADE;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.example.voy.enums.TripItemType;

@Entity(tableName = "trip_items",
foreignKeys = @ForeignKey(
        entity = TripEntity.class,
        parentColumns = "id",
        childColumns = "tripId",
        onDelete = CASCADE
),indices = {
        @Index(value = {"tripId", "localUri"}, unique = true),
        @Index("userId")
})
public class TripItemEntity {
    @PrimaryKey
    @NonNull
    public String id;

    @NonNull
    public String tripId;

    @NonNull
    public String userId;
    @NonNull
    public TripItemType type;
    public long timestamp;
    public String localUri;   // content:// or internal file

    public String remoteUrl;  // Firebase Storage Url
    public Double lat;
    public Double lng;
    public String title;
    public String metadataJson;

    public TripItemEntity( @NonNull String id,
                           @NonNull String tripId,
                           @NonNull String userId,
                           @NonNull TripItemType type,
                           long timestamp,
                           String localUri,
                           String remoteUrl,
                           Double lat,
                           Double lng,
                           String title,
                           String metadataJson) {
        this.id = id;
        this.lat = lat;
        this.lng = lng;
        this.localUri = localUri;
        this.metadataJson = metadataJson;
        this.remoteUrl = remoteUrl;
        this.timestamp = timestamp;
        this.title = title;
        this.tripId = tripId;
        this.type = type;
        this.userId = userId;
    }
}
