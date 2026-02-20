package com.example.voy.activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.Application;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;

import android.view.MenuItem;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voy.R;
import com.example.voy.adapters.TripAdapter;
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
    private TripRepository tripRepository;
    private TripEntity activeTrip;
    private String userId;
    private RecyclerView recyclerView;
    private TripAdapter tripAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = findViewById(R.id.recyclerView);

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
                // Later: open Trip details/journal screen
                // Intent intent = new Intent(MainActivity.this, TripDetailsActivity.class);
                // intent.putExtra("tripId", trip.getId());
                // startActivity(intent);
            }
        });
        recyclerView.setAdapter(tripAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(tripAdapter);

        tripRepository = new TripRepository(getApplicationContext());
        MaterialToolbar toolbar = findViewById(R.id.headerToolbar);
        fabServiceToggle = findViewById(R.id.fabServiceToggle);
        emptyMessage = findViewById(R.id.emptyMessage);

        toolbar.setNavigationOnClickListener(v->{
            Intent intent = new Intent(this, MapsActivity.class);
            startActivity(intent);
        });

        toolbar.setOnMenuItemClickListener(item ->{
            if(item.getItemId() == R.id.accountIcon){
                showPopupMenu(toolbar);
                return true;
            }
            return false;
        });

        userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ?FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        tripRepository.observeAllTrips(userId).observe(this, trips -> {
            tripAdapter.setTrips(trips);

            if (trips == null || trips.isEmpty()) {
                emptyMessage.setVisibility(VISIBLE);
            } else {
                emptyMessage.setVisibility(GONE);
            }
        });
        if(userId != null){
            tripRepository.observeActiveTrip(userId).observe(this, trip ->{
                activeTrip = trip;
                if(trip == null){
                    fabServiceToggle.setText("Start Trip");
                    fabServiceToggle.setIconResource(R.drawable.baseline_not_started_24);
                    fabServiceToggle.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4B6405")));
                }
                else{
                    fabServiceToggle.setText("Stop Trip");
                    fabServiceToggle.setIconResource(R.drawable.baseline_stop_circle_24);
                    fabServiceToggle.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
                }
            });
        }

        fabServiceToggle.setOnClickListener(v -> {
            if (userId == null) return;
            Intent serviceIntent = new Intent(this, TripForegroundService.class);
            if(activeTrip == null){
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
                serviceIntent.putExtra("userId", userId);
                serviceIntent.putExtra("tripId", tripId);
                ContextCompat.startForegroundService(this, serviceIntent);
            }
            else{
                long end = System.currentTimeMillis();
                tripRepository.finishTrip(userId, activeTrip.getId(), end);
                stopService(serviceIntent);
            }
        });

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
    }

    private void logOut() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);

    }
}