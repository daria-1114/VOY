package com.example.voy.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.voy.data.entities.LandmarkEntity;

import java.util.List;

@Dao
public interface LandmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(LandmarkEntity landmarkEntity);
    @Query("UPDATE landmarks SET lat = :lat, lng = :lng WHERE id = :id")
    void updateCoordinates(String id, double lat, double lng);

    @Query("SELECT * FROM landmarks WHERE tripId = :tripId ORDER BY dayNumber ASC, createdAt ASC")
    LiveData<List<LandmarkEntity>> observeForTrip(String tripId);

    @Query("SELECT * FROM landmarks WHERE tripId= :tripId AND isVisited = 0 ")
    List<LandmarkEntity> getUnvisitedForTrip(String tripId);

    @Query("UPDATE landmarks SET isVisited = 1 WHERE id= :id")
    void markVisited(String id);

    @Query("DELETE from landmarks WHERE id= :id")
    void delete(String id);
}
