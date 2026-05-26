package com.example.voy.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.voy.data.entities.LandmarkEntity;
import com.example.voy.data.entities.TripEntity;
import com.example.voy.data.entities.TripItemEntity;
import com.example.voy.data.repository.TripRepository;

import java.util.List;

public class TravelViewModel extends AndroidViewModel {
    private final TripRepository tripRepository;
    public TravelViewModel(@NonNull Application application) {
        super(application);
        this.tripRepository = new TripRepository(application);
    }

    public LiveData<TripEntity> observeTrip(String userId, String tripId){
        return tripRepository.observeTrip(userId, tripId);
    }
    public LiveData<List<TripItemEntity>> observeAllItemsForTrip(String userId, String tripId){
        return tripRepository.observeAllItemsForTrip(userId, tripId);
    }
    public LiveData<List<LandmarkEntity>> observeLandmarks(String tripId) {
        return tripRepository.observeLandmarks(tripId);
    }
    public void updateTripTitle(String userId, String tripId, String newTitle) {
        tripRepository.updateTripTitle(userId, tripId, newTitle);
    }
    public void updateTripNotes(String userId, String tripId, String notes) {
        tripRepository.updateTripNotes(userId, tripId, notes);
    }

    public void updateItemNotes(String itemId, String notes) {
        tripRepository.updateItemNotes(itemId, notes);
    }

    public void deleteTripItem(String userId, String tripId, String itemId) {
        tripRepository.deleteTripItem(userId, tripId, itemId);
    }

    public void addAttachment(String userId, String tripId, String uri) {
        tripRepository.addAttachment(userId, tripId, uri);
    }

    public void removeAttachment(String userId, String tripId, String uri) {
        tripRepository.removeAttachment(userId, tripId, uri);
    }
    public void insertLandmark(LandmarkEntity landmark) {
        tripRepository.insertLandmark(landmark);
    }

    public void deleteLandmark(String id) {
        tripRepository.deleteLandmark(id);
    }

    public void updateLandmarkCoordinates(String id, double lat, double lng) {
        tripRepository.updateLandmarkCoordinates(id,lat,lng);
    }
}
