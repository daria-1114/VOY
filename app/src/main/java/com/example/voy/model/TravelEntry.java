package com.example.voy.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName = "travel_entries")
public class TravelEntry {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String title;
    private String location;
    private String notes;
    private long dateMillis;

    private double latitude;
    private double longitude;

    private String photoPath;


    public long dateMillis() {
        return dateMillis;
    }

    public void setDateMillis(long dateMillis) {
        this.dateMillis = dateMillis;
    }

    public String title() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String photoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public String notes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public double longitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String location() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public double latitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public int id() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
