package com.example.voy.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.voy.data.entities.TripItemEntity;

import java.util.List;

@Dao
public interface TripItemDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertItem(TripItemEntity item);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertItems(List<TripItemEntity> items);

    @Query("SELECT * FROM trip_items WHERE userId= :userId AND tripId= :tripId ORDER BY timestamp ASC")
    LiveData<List<TripItemEntity>> observeAllItemsForTrip(String userId, String tripId); // gets all the items for a trip in ascending order

    @Query("SELECT * FROM trip_items WHERE userId= :userId AND tripId= :tripId ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<TripItemEntity>> observeRecentItemsForTrip(String userId, String tripId, int limit); //gets items for main page display

    @Query("DELETE FROM trip_items WHERE userId = :userId AND tripId= :tripId AND id= :itemId")
    void deleteItem(String userId, String tripId, String itemId);

    @Query("SELECT COUNT(*) FROM trip_items WHERE userId = :userId AND tripId = :tripId AND localUri = :localUri")
    int countByTripAndLocalUri(String userId, String tripId, String localUri); //counts by uri to check if the item is not already in the table

    @Query("DELETE FROM trip_items WHERE userId=:userId AND tripId=:tripId AND localUri=:localUri")
    void deleteByTripAndLocalUri(String userId, String tripId, String localUri); // deletes by uri in case the item is deleted from the device

    @Query("UPDATE trip_items SET notes = :notes WHERE id = :itemId")
    void updateNotes(String itemId, String notes);
}
