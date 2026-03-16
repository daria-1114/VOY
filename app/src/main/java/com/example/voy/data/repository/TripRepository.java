package com.example.voy.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.voy.data.dao.TripDao;
import com.example.voy.data.dao.TripItemDao;
import com.example.voy.data.db.AppDatabase;
import com.example.voy.data.entities.TripEntity;
import com.example.voy.data.entities.TripItemEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TripRepository {
    private final TripDao tripDao;
    private final TripItemDao tripItemDao;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    public TripRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.tripDao = db.tripDao();
        this.tripItemDao = db.tripItemDao();
    }

    //---------------------------OBSERVE(listen for changes in db and update ui-automatically through livedata)---------------------
    public LiveData<TripEntity> observeActiveTrip(String userId) {
        return tripDao.observeActiveTrip(userId);
    }

    public LiveData<List<TripEntity>> observeAllTrips(String userId) {
        return tripDao.observeAllTrips(userId);
    }

    public LiveData<List<TripItemEntity>> observeAllItemsForTrip(String userId, String tripId) {
        return tripItemDao.observeAllItemsForTrip(userId, tripId);
    }

    public LiveData<List<TripItemEntity>> observeRecentItemsForTrip(String userId, String tripId, int limit) {
        return tripItemDao.observeRecentItemsForTrip(userId, tripId, limit);
    }

//----------------WRITE(change db)----------------------------------

    public void insertTrip(TripEntity trip) {
        dbExecutor.execute(() -> tripDao.insert(trip));
    }

    public void finishTrip(String userId, String tripId, long endTime) {
        dbExecutor.execute(() -> tripDao.finishTrip(userId, tripId, endTime));
    }

    public void deleteTrip(String userId, String tripId) {
        dbExecutor.execute(() -> tripDao.deleteTrip(userId, tripId));
    }

    public void insertTripItem(TripItemEntity item) {
        dbExecutor.execute(() -> tripItemDao.insertItem(item));
    }

    public void insertTripItems(List<TripItemEntity> items) {
        dbExecutor.execute(() -> tripItemDao.insertItems(items));
    }

    public void deleteTripItem(String userId, String tripId, String itemId) {
        dbExecutor.execute(() -> tripItemDao.deleteItem(userId, tripId, itemId));
    }

    public void checkExistsByUriAsync(String userId, String tripId, String localUri, ExistsCallback callback) {
        dbExecutor.execute(() -> {
            int count = tripItemDao.countByTripAndLocalUri(userId, tripId, localUri);
            boolean exists = count > 0;
            callback.onResult(exists);
        });
    }

    public void updateTripTitle(String userId, String tripId, String newTitle) {
        dbExecutor.execute(() -> tripDao.updateTripTitle(userId, tripId, newTitle));
    }

    public void updateTripNotes(String userId, String tripId, String notes) {
        dbExecutor.execute(() -> tripDao.updateNotes(userId, tripId, notes));
    }

    public void addAttachment(String userId, String tripId, String uri) {
        dbExecutor.execute(() -> {
            TripEntity trip = tripDao.getTripSync(userId, tripId);
            if (trip == null) return;
            java.util.List<String> list = trip.attachments != null
                    ? new java.util.ArrayList<>(trip.attachments) : new java.util.ArrayList<>();
            if (!list.contains(uri)) list.add(uri);
            tripDao.updateAttachments(userId, tripId, list);
        });
    }

    public void removeAttachment(String userId, String tripId, String uri) {
        dbExecutor.execute(() -> {
            TripEntity trip = tripDao.getTripSync(userId, tripId);
            if (trip == null) return;
            java.util.List<String> list = trip.attachments != null
                    ? new java.util.ArrayList<>(trip.attachments) : new java.util.ArrayList<>();
            list.remove(uri);
            tripDao.updateAttachments(userId, tripId, list);
        });
    }
    public LiveData<TripEntity> observeTrip(String userId, String tripId) {
        return tripDao.observeTrip(userId, tripId);
    }
    public interface ExistsCallback {
        void onResult(boolean exists);
    }
}