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
    public interface CurrentLocationCallback {
        void onLocation(@Nullable Location location);
    }
    private static final String TAG = "TripLocationManager_PowerSave";
    private final Context context;
    private final FusedLocationProviderClient fusedClient;
    private volatile Location lastLocation;
    private long lastFetchTime = 0;
    public TripLocationManager(Context context) {
        this.context = context;
        this.fusedClient = LocationServices.getFusedLocationProviderClient(context);
    }
    @SuppressLint("MissingPermission")
    public void requestCurrentLocation(CurrentLocationCallback callback){
        if(!hasPermission()){
            Log.w(TAG, "No permission for current location");
            callback.onLocation(null);
            return;
        }
        CancellationTokenSource token = new CancellationTokenSource();
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY,token.getToken())
                .addOnSuccessListener(loc -> {
                    if(loc != null){
                        lastLocation = loc;
                        Log.i(TAG,
                                "Fresh location: LAT="
                                        + loc.getLatitude()
                                        + " LNG="
                                        + loc.getLongitude());
                    }
                    callback.onLocation(loc);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed current location", e);
                    callback.onLocation(null);
                });
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

}
