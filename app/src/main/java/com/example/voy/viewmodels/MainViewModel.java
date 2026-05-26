package com.example.voy.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.voy.data.entities.TripEntity;
import com.example.voy.data.repository.TripRepository;

import java.util.List;

public class MainViewModel extends AndroidViewModel {
    private final TripRepository tripRepository;
    public MainViewModel(@NonNull Application application) {
        super(application);
        this.tripRepository = new TripRepository(application);
    }

    public LiveData<List<TripEntity>> observeAllTrips(String userId){
        return tripRepository.observeAllTrips(userId);
    }
    public LiveData<TripEntity> observeActiveTrip(String userId){
        return tripRepository.observeActiveTrip(userId);
    }

    public void insertTrip(TripEntity trip){
        tripRepository.insertTrip(trip);
    }
    public void finishTrip(String userId, String tripId, long endTime){
        tripRepository.finishTrip(userId, tripId, endTime);
    }
    public void deleteTrip(String userId, String tripId) {
        tripRepository.deleteTrip(userId, tripId);
    }

    public void checkSystemLockAsync(String userId, TripRepository.ExistsCallback callback) {
        tripRepository.checkSystemLockAsync(userId, callback);
    }
}
