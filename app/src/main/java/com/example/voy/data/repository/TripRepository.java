package com.example.voy.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.example.voy.data.dao.LandmarkDao;
import com.example.voy.data.dao.TripDao;
import com.example.voy.data.dao.TripItemDao;
import com.example.voy.data.db.AppDatabase;
import com.example.voy.data.entities.LandmarkEntity;
import com.example.voy.data.entities.TripEntity;
import com.example.voy.data.entities.TripItemEntity;
import com.example.voy.enums.TripItemType;

import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TripRepository {
    private final TripDao tripDao;
    private final TripItemDao tripItemDao;
    private final LandmarkDao landmarkDao;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    public TripRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.tripDao = db.tripDao();
        this.tripItemDao = db.tripItemDao();
        this.landmarkDao = db.landmarkDao();
    }
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

    public void deleteTripItem(String userId, String tripId, String itemId) {
        dbExecutor.execute(() -> tripItemDao.deleteItem(userId, tripId, itemId));
    }

    public void updateTripTitle(String userId, String tripId, String newTitle) {
        dbExecutor.execute(() -> tripDao.updateTripTitle(userId, tripId, newTitle));
    }
    public void activatePlannedTrip(String userId, String tripId, String newTitle) {
        dbExecutor.execute(() -> tripDao.activatePlannedTrip(userId, tripId, newTitle));
    }

    public void updateTripNotes(String userId, String tripId, String notes) {
        dbExecutor.execute(() -> tripDao.updateNotes(userId, tripId, notes));
    }
    public void updateItemNotes(String itemId, String notes) {
        dbExecutor.execute(() -> tripItemDao.updateNotes(itemId, notes));
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
    public void upsertStepsForDay(String itemId, String tripId, String userId, String dayLabel, long timestamp, int rawSensorTotal){
        dbExecutor.execute(()->{
            String json = tripItemDao.getMetadataJson(itemId);
            int baseline;
            int stepsToday;
            try{
                if(json == null){
                    baseline = rawSensorTotal;
                }else{
                    baseline = new JSONObject(json).optInt("baselineCounter", rawSensorTotal);
                    if(rawSensorTotal < baseline) baseline = rawSensorTotal;
                }
            } catch (Exception e) {
                baseline = rawSensorTotal;
            }
            stepsToday = Math.max(0, rawSensorTotal - baseline);
            String title = dayLabel +" - " + stepsToday + " steps";
            String meta = buildStepsMeta(stepsToday, dayLabel, baseline);
            TripItemEntity card = new TripItemEntity(
                    itemId, tripId, userId, TripItemType.STEPS,
                    timestamp, null, null, null, null, title, meta);
            tripItemDao.insertItem(card);
            tripItemDao.updateStepsCard(itemId, title, meta);
        });
    }
    public LiveData<TripEntity> observeTrip(String userId, String tripId) {
        return tripDao.observeTrip(userId, tripId);
    }
    public void checkSystemLockAsync(String userId, ExistsCallback callback) {
        dbExecutor.execute(() -> {
            boolean isLocked = tripDao.hasActiveOrPlannedTripSync(userId);
            new Handler(Looper.getMainLooper()).post(() ->{
                callback.onResult(isLocked);
            });
        });
    }

    public void insertLandmark(LandmarkEntity landmark){
        dbExecutor.execute(() -> landmarkDao.insert(landmark));
    }
    public void updateLandmarkCoordinates(String landmarkId, double lat, double lng) {
        dbExecutor.execute(() -> landmarkDao.updateCoordinates(landmarkId, lat, lng));
    }
    public LiveData<List<LandmarkEntity>> observeLandmarks(String tripId){
        return landmarkDao.observeForTrip(tripId);
    }

    public void markLandmarkVisited(String id){
        dbExecutor.execute(() -> landmarkDao.markVisited(id));
    }
    public void deleteLandmark(String id){
        dbExecutor.execute(() -> landmarkDao.delete(id));
    }
    public List<LandmarkEntity> getUnvisitedLandmarks(String tripId) {
        return landmarkDao.getUnvisitedForTrip(tripId);
    }
    private String buildStepsMeta(int steps, String dayLabel, int baselineCounter) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("steps", steps);
            obj.put("dayLabel", dayLabel);
            obj.put("baselineCounter", baselineCounter);
            return obj.toString();
        } catch (Exception e) {
            return null;
        }
    }
    public boolean hasMediaStoreId(String tripId, long mediaStoreId){
        return tripItemDao.countByMediaStoreId(tripId, mediaStoreId) > 0;
    }
    public interface ExistsCallback {
        void onResult(boolean exists);
    }
}