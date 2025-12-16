package com.example.voy.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.voy.model.TravelEntry;

import java.util.List;

@Dao
public interface TravelEntryDao {

    @Insert
    void insert(TravelEntry entry);

    @Query("SELECT * FROM travel_entries")
    List<TravelEntry> getAllEntries();

    @Query("SELECT * FROM travel_entries WHERE id = :id")
    TravelEntry getEntryById(int id);
}
