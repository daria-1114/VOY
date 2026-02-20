package com.example.voy.data.dao;



import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.voy.data.entities.TripEntity;

import java.util.List;

@Dao
public interface TripDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insert(TripEntity trip);

    @Update
    void update(TripEntity trip);
    @Query("SELECT * FROM trips WHERE userId = :userId AND status = 'ACTIVE' ORDER BY startTime DESC LIMIT 1")
    TripEntity getActiveTrip(String userId);
    @Query("SELECT * FROM trips WHERE userId = :userId AND status = 'ACTIVE' ORDER BY startTime DESC LIMIT 1")
    LiveData<TripEntity> observeActiveTrip(String userId);
    @Query("SELECT * FROM trips WHERE userId = :userId ORDER BY startTime DESC")
    LiveData<List<TripEntity>> observeAllTrips(String userId); // get all trips based on the date added

    @Query("DELETE FROM trips WHERE userId = :userId AND id= :tripId")
    void deleteTrip(String userId, String tripId); // delete a trip

    @Query("UPDATE trips SET endTime= :endTime, status = 'FINISHED' WHERE id= :tripId AND userId= :userId")
    void finishTrip(String userId, String tripId, long endTime); //mark trip as finished


}
