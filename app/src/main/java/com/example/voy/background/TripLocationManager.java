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
    private static final String TAG = "TripLocationManager_PowerSave";
    private float totalDistanceMeters = 0f;
    private Location previousLocation = null;
    private final Context context;
    private final FusedLocationProviderClient fusedClient;
    private volatile Location lastLocation;

    public TripLocationManager(Context context) {
        this.context = context;
        this.fusedClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @SuppressLint("MissingPermission")
    public void requestSingleUpdate(){
        if(!hasPermission()){
            Log.w(TAG, "Cannot fetch location- no permission");
            return;
        }
        Log.d(TAG, "requesting location update...");
        CancellationTokenSource token = new CancellationTokenSource();
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.getToken())
                .addOnSuccessListener(loc ->{
                    if(loc != null){
                        updateDistanceMeters(loc);
                        lastLocation = loc;
                        Log.i(TAG, "Location success: LAT=" + loc.getLatitude() +" LNG="+loc.getLongitude());
                    }else{
                        Log.w(TAG, "Location came back null");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "failed to get single update",e));
    }

    private void updateDistanceMeters(Location loc) {
        if(previousLocation != null){
            float dist = previousLocation.distanceTo(loc);
            totalDistanceMeters += dist;
            Log.d(TAG, "Distance added:"+dist+"meters total: "+totalDistanceMeters);
        }
        previousLocation = loc;
    }

    public void stop(){
        Log.d(TAG,"Location Manager stopped");
    }
    @Nullable
    public Location getLastLocation(){
        return lastLocation;
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
