package com.example.voy.activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.app.Application;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import android.view.MenuItem;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.activity.result.ActivityResultLauncher;
import android.content.pm.PackageManager;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voy.R;
import com.example.voy.adapters.TripAdapter;
import com.example.voy.background.TripCaptureStateStore;
import com.example.voy.background.TripForegroundService;
import com.example.voy.data.entities.TripEntity;
import com.example.voy.data.repository.TripRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import androidx.lifecycle.Observer;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private TextView emptyMessage;
    private ExtendedFloatingActionButton fabServiceToggle;
    private ExtendedFloatingActionButton fabMockTrip;
    private TripRepository tripRepository;
    private TripEntity activeTrip;
    private String userId;
    private RecyclerView recyclerView;
    private TripAdapter tripAdapter;
    private ActivityResultLauncher<String[]> permLauncher;
    private Runnable pendingStartTrip;
    private boolean serviceStartedByUi = false;
    private boolean mockTripStartedByUi = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        permLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean mediaOk;
                    boolean notifOk;
                    boolean locOk;
                    Boolean loc = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                    locOk = (loc != null && loc);
                    if (Build.VERSION.SDK_INT >= 33) {
                        Boolean img = result.get(Manifest.permission.READ_MEDIA_IMAGES);
                        Boolean vid = result.get(Manifest.permission.READ_MEDIA_VIDEO);
                        Boolean aud = result.get(Manifest.permission.READ_MEDIA_AUDIO);
                        Boolean post = result.get(Manifest.permission.POST_NOTIFICATIONS);

                        mediaOk = (img != null && img) || (vid != null && vid) || (aud != null && aud);
                        notifOk = (post != null && post);

                    } else {
                        Boolean ext = result.get(Manifest.permission.READ_EXTERNAL_STORAGE);

                        mediaOk = (ext != null && ext);

                        notifOk = true; // no notif runtime perm pre-33
                    }
                    if(mediaOk && notifOk && locOk && pendingStartTrip!= null){
                        pendingStartTrip.run();
                        pendingStartTrip = null;
                    }else{
                        if (mediaOk && notifOk && pendingStartTrip != null) {
                            pendingStartTrip.run();
                            pendingStartTrip = null;
                            Toast.makeText(this,"Location access not granted. Trip items will not have map locations.",Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this,
                                    "Please allow media + notifications so the trip can capture in the background.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );
        recyclerView = findViewById(R.id.recyclerView);

        tripRepository = new TripRepository(getApplicationContext());
        MaterialToolbar toolbar = findViewById(R.id.headerToolbar);
        fabServiceToggle = findViewById(R.id.fabServiceToggle);
        fabMockTrip = findViewById(R.id.fabMockTrip);
        emptyMessage = findViewById(R.id.emptyMessage);

        tripAdapter = new TripAdapter(new TripAdapter.OnTripActionListener() {
            @Override
            public void onDeleteTrip(TripEntity trip) {
                if (userId == null) return;
                if ("ACTIVE".equals(trip.status)) {
                    Toast.makeText(MainActivity.this, "Stop the trip before deleting.", Toast.LENGTH_SHORT).show();
                    return;
                }
                tripRepository.deleteTrip(userId, trip.getId());
            }

            @Override
            public void onTripClicked(TripEntity trip) {
                 Intent intent = new Intent(MainActivity.this, TravelActivity.class);
                 intent.putExtra("tripId", trip.getId());
                 startActivity(intent);
            }
        });
        recyclerView.setAdapter(tripAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        toolbar.setNavigationOnClickListener(v->{
            Intent intent = new Intent(this, MapsActivity.class);
            startActivity(intent);
        });

        toolbar.setOnMenuItemClickListener(item ->{
            if(item.getItemId() == R.id.actionAccount){
                showPopupMenu(toolbar);
                return true;
            }
            return false;
        });

        userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ?FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        if (userId == null) {
            emptyMessage.setVisibility(VISIBLE);
            fabServiceToggle.setEnabled(false);
            fabServiceToggle.setText("Login required");
            return;
        }
        tripRepository.observeAllTrips(userId).observe(this, trips -> {
            tripAdapter.setTrips(trips);

            if (trips == null || trips.isEmpty()) {
                emptyMessage.setVisibility(VISIBLE);
            } else {
                emptyMessage.setVisibility(GONE);
            }
        });
        if(userId != null){
            tripRepository.observeActiveTrip(userId).observe(this, trip -> {
                activeTrip = trip;
                updateFabUi(trip);
                if (trip == null) {
                    stopTripService();
                    return;
                }
                // Resume capture only when the system/boot told us to resume
                boolean needsResume = TripCaptureStateStore.consumeNeedsResume(this);
                if (needsResume) {
                    requestTripPermissionsThen(() ->
                            startTripService(trip.id, userId, trip.startTime)
                    );
                }
            });
        }

        fabServiceToggle.setOnClickListener(v -> {
            if (userId == null) return;
            if(activeTrip == null){
                requestTripPermissionsThen(() -> {
                    String tripId = UUID.randomUUID().toString();
                    long now = System.currentTimeMillis();

                    TripEntity trip = new TripEntity(
                            now,
                            tripId,
                            null,
                            "ACTIVE",
                            "",
                            userId
                    );
                    tripRepository.insertTrip(trip);
                    startTripService(tripId, userId, now);
                });
            } else {
                long end = System.currentTimeMillis();
                tripRepository.finishTrip(userId, activeTrip.getId(), end);
            }
        });
        fabMockTrip.setOnClickListener(v->{
            if(userId == null) return;
            if(activeTrip !=null){
                Toast.makeText(this, "Stop current trip before starting mock trip.",Toast.LENGTH_LONG).show();
                return;
            }
            requestTripPermissionsThen(()->{
                String newTripId = UUID.randomUUID().toString();
                long now = System.currentTimeMillis();
                TripEntity trip = new TripEntity(
                        now,
                        newTripId,
                        null,
                        "ACTIVE",
                        "Rome Mock Trip",
                        userId
                );
                tripRepository.insertTrip(trip);
                new android.os.Handler(getMainLooper()).postDelayed(() -> {
                    startMockTripService(newTripId, userId, now);
                }, 300);
            });
        });

    }

    private void stopTripService() {
        if(!serviceStartedByUi) return;
        Intent serviceIntent = new Intent(this, TripForegroundService.class);
        serviceIntent.setAction(TripForegroundService.ACTION_STOP);
        startService(serviceIntent);
        serviceStartedByUi = false;

    }

    private void startTripService(@NonNull String tripId,@NonNull String userId, long startTime) {
        if(serviceStartedByUi) return;
        Intent serviceIntent = new Intent(this, TripForegroundService.class);
        serviceIntent.setAction(TripForegroundService.ACTION_START);
        serviceIntent.putExtra(TripForegroundService.EXTRA_USER_ID, userId);
        serviceIntent.putExtra(TripForegroundService.EXTRA_TRIP_ID, tripId);
        serviceIntent.putExtra(TripForegroundService.EXTRA_TRIP_START_TIME, startTime);
        ContextCompat.startForegroundService(this, serviceIntent);
        serviceStartedByUi = true;
    }
    private void startMockTripService(@NonNull String tripId, @NonNull String userId, long startTime){
        if(mockTripStartedByUi) return;
        Intent serviceIntent = new Intent(this, TripForegroundService.class);
        serviceIntent.setAction(TripForegroundService.ACTION_START_MOCK);
        serviceIntent.putExtra(TripForegroundService.EXTRA_TRIP_ID, tripId);
        serviceIntent.putExtra(TripForegroundService.EXTRA_USER_ID, userId);
        serviceIntent.putExtra(TripForegroundService.EXTRA_TRIP_START_TIME, startTime);
        ContextCompat.startForegroundService(this, serviceIntent);
        mockTripStartedByUi = true;
    }
    private void updateFabUi(TripEntity trip) {

        if (trip == null) {
            fabServiceToggle.setText("Start Trip");
            fabServiceToggle.setIconResource(R.drawable.baseline_not_started_24);
            fabServiceToggle.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4B6405")));
        } else {
            fabServiceToggle.setText("Stop Trip");
            fabServiceToggle.setIconResource(R.drawable.baseline_stop_circle_24);
            fabServiceToggle.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
        }
    }
    private void showPopupMenu(MaterialToolbar toolbar) {
        PopupMenu popupMenu = new PopupMenu(this, toolbar);
        popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            if(item.getItemId() == R.id.LogOut_btn){
                logOut();
                return true;
            }
            if(item.getItemId() == R.id.Info_btn){
                Intent intent = new Intent(this, InfoActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void logOut() {
        stopTripService();
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
    private boolean hasAllTripPermissions() {
        boolean loc = hasFineLocation();
        if (Build.VERSION.SDK_INT >= 33) {
            boolean img = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
            boolean vid = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                    == PackageManager.PERMISSION_GRANTED;
            boolean post = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
            boolean aud = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                    == PackageManager.PERMISSION_GRANTED;
            boolean mediaOk = (img || vid || aud);
            return mediaOk && loc && post;
        } else {
            boolean ext = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
            return ext && loc;
        }
    }

    private void requestTripPermissionsThen(Runnable onGranted) {
        if (hasAllTripPermissions()) {
            onGranted.run();
            return;
        }
        pendingStartTrip = onGranted;

        if (Build.VERSION.SDK_INT >= 33) {
            permLauncher.launch(new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.ACCESS_FINE_LOCATION
            });
        } else {
            permLauncher.launch(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION
            });
        }
    }
    private boolean hasFineLocation(){
        return ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);

    }

}