package com.example.voy.background;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;


import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import org.jspecify.annotations.NonNull;

public class TripLocationManager {
    private static final String TAG = "TripLocationManager";
    private float totalDistanceMeters = 0f;
    private Location previousLocation = null;
    private final Context context;
    private final FusedLocationProviderClient fusedClient;
    private LocationCallback locationCallback;
    private volatile Location lastLocation;

    public TripLocationManager(Context context) {
        this.context = context;
        this.fusedClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public void start(android.os.Looper looper){
        if(!hasPermission()){
            Log.d(TAG, "Location permission not granted - lat/lng will be null");
            return;
        }
        startUpdates(looper);
    }
    public void stop(){
        if(fusedClient != null && locationCallback != null){
            fusedClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
            Log.d(TAG, "Location updates stopped");
        }
    }
    @Nullable
    public Location getLastLocation(){
        return lastLocation;
    }
    @SuppressLint("MissingPermission")
    private void startUpdates(android.os.Looper looper) {
        if(locationCallback != null)return;

        LocationRequest req = new LocationRequest.Builder(30_000L)
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setMinUpdateIntervalMillis(15_000L)
                    .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result){
                Location loc = result.getLastLocation();
                if(loc != null){
                    if (previousLocation != null) {
                        totalDistanceMeters += previousLocation.distanceTo(loc);
                    }
                    previousLocation = loc;
                    lastLocation = loc;
                    Log.d(TAG, "Location updated "+ loc.getLatitude()+", "+ loc.getLongitude()+" total distance "+totalDistanceMeters);
                }
            }
        };
        fusedClient.requestLocationUpdates(req, locationCallback, looper);

        CancellationTokenSource cts =  new CancellationTokenSource();
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(loc ->{
                    if(loc != null){
                        lastLocation = loc;
                    }else{
                        fusedClient.getLastLocation()
                                .addOnSuccessListener(fb ->{
                                    if(fb != null) lastLocation = fb;
                                });
                    }
                })
                .addOnFailureListener(e->{
                    fusedClient.getLastLocation()
                            .addOnSuccessListener(fb ->{
                                if(fb != null) lastLocation = fb;
                            });
                });
    }

    boolean hasPermission() {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    public int getEstimatedSteps(){
        return (int)(totalDistanceMeters/0.762f);
    }

    public void resetDailyDistance(){
        totalDistanceMeters = 0f;
        previousLocation = null;
    }
}
