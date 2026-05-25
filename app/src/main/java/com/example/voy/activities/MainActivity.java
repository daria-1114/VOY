package com.example.voy.activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import android.content.pm.PackageManager;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voy.R;
import com.example.voy.adapters.TripAdapter;
import com.example.voy.background.TripCaptureStateStore;
import com.example.voy.background.TripForegroundService;
import com.example.voy.background.TripScheduler;
import com.example.voy.data.entities.TripEntity;
import com.example.voy.data.repository.TripRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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
    private long plannedStartMs = -1;
    private long plannedEndMs = -1;

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

                        notifOk = true;
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

//        toolbar.setNavigationOnClickListener(v->{
//            Intent intent = new Intent(this, MapsActivity.class);
//            startActivity(intent);
//        });

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
                showTripConfigurationSheet();
            } else if("PLANNED".equals(activeTrip.status)) {
                long end = System.currentTimeMillis();
                tripRepository.finishTrip(userId, activeTrip.getId(), end);
                TripScheduler.cancelTripActivation(MainActivity.this, activeTrip.getId());
                Toast.makeText(this, "Trip timer canceled! Record saved to history.", Toast.LENGTH_SHORT).show();
            }else{
                long end = System.currentTimeMillis();
                tripRepository.finishTrip(userId, activeTrip.getId(), end);
                stopTripService();
                Toast.makeText(this, "Trip stopped and saved!", Toast.LENGTH_SHORT).show();
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

    private void showTripConfigurationSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.trip_options_dialog_main, null);
        bottomSheetDialog.setContentView(sheetView);
        MaterialButtonToggleGroup toggleGroup = sheetView.findViewById(R.id.toggleGroupMode);
        LinearLayout plannedFieldsContainer = sheetView.findViewById(R.id.plannedFieldsContainer);
        TextInputEditText etCity = sheetView.findViewById(R.id.etDialogCity);
        Button btnSelectDates = sheetView.findViewById(R.id.btnDialogSelectDates);
        TextView tvDatesDisplay = sheetView.findViewById(R.id.tvDialogDatesDisplay);
        Button btnConfirm = sheetView.findViewById(R.id.btnDialogConfirm);

        plannedStartMs = -1;
        plannedEndMs = -1;
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) ->{
            if (isChecked) {
                if (checkedId == R.id.btnModePlanned) {
                    plannedFieldsContainer.setVisibility(android.view.View.VISIBLE);
                } else {
                    plannedFieldsContainer.setVisibility(android.view.View.GONE);
                }
            }
        });
        btnSelectDates.setOnClickListener(view ->{
            MaterialDatePicker<Pair<Long,Long>> picker = MaterialDatePicker.Builder.dateRangePicker()
                    .setTitleText("Define trip dates")
                    .build();
            picker.addOnPositiveButtonClickListener(selection ->{
                plannedStartMs = selection.first;
                plannedEndMs = selection.second;
                SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                String range = format.format(new Date(plannedStartMs)) + "-" + format.format(new Date(plannedEndMs));
                tvDatesDisplay.setText(range);
            });
            picker.show(getSupportFragmentManager(), "PLAN_TRIP_CALENDAR");
        });

        btnConfirm.setOnClickListener(view ->{
            boolean isPlannedMode = (toggleGroup.getCheckedButtonId() == R.id.btnModePlanned);
            final String tripId = UUID.randomUUID().toString();
            final long now = System.currentTimeMillis();

            tripRepository.checkSystemLockAsync(userId, isSystemLocked ->{
                if(isSystemLocked){
                    Toast.makeText(MainActivity.this,"Your already have an active or scheduled trip!", Toast.LENGTH_LONG).show();
                    return;
                }
                if(isPlannedMode){
                    String cityInput = etCity.getText().toString().trim();
                    if(cityInput.isEmpty()){
                        etCity.setError("Destination required");
                        return;
                    }
                    if (plannedStartMs <= 0 || plannedEndMs <= 0) {
                        Toast.makeText(this, "Please select trip dates", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    long calculatedTime = plannedStartMs;
                    if (calculatedTime <= System.currentTimeMillis()) {
                        calculatedTime = System.currentTimeMillis() + 5000; // 5 seconds from now for testing
                    }
                    final long finalActivationTime = calculatedTime;
                    bottomSheetDialog.dismiss();
                    requestTripPermissionsThen(() -> {

                        TripEntity plannedTrip = new TripEntity(plannedStartMs, tripId, plannedEndMs, "PLANNED", cityInput, userId);
                        tripRepository.insertTrip(plannedTrip);

                        TripScheduler.scheduleTripActivation(MainActivity.this, tripId, userId, finalActivationTime);
                        Toast.makeText(MainActivity.this, "Trip to " + cityInput + " scheduled!", Toast.LENGTH_LONG).show();
                    });
                }else{
                    bottomSheetDialog.dismiss();
                    requestTripPermissionsThen(() -> {
                        TripEntity liveTrip = new TripEntity(now, tripId, null, "ACTIVE", "", userId);
                        tripRepository.insertTrip(liveTrip);

                        Intent serviceIntent = new Intent(MainActivity.this, TripForegroundService.class);
                        serviceIntent.setAction(TripForegroundService.ACTION_START);
                        serviceIntent.putExtra(TripForegroundService.EXTRA_USER_ID, userId);
                        serviceIntent.putExtra(TripForegroundService.EXTRA_TRIP_ID, tripId);
                        serviceIntent.putExtra(TripForegroundService.EXTRA_TRIP_START_TIME, now);
                        serviceIntent.putExtra(TripForegroundService.EXTRA_IS_PREDEFINED, false);

                        ContextCompat.startForegroundService(MainActivity.this, serviceIntent);
                        serviceStartedByUi = true;
                });
                }
            });
        });
        bottomSheetDialog.show();
    }

    private void stopTripService() {
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
            fabServiceToggle.setEnabled(true);
        } else if ("PLANNED".equals(trip.status)){
            fabServiceToggle.setText("Cancel Scheduled Trip");
            fabServiceToggle.setIconResource(R.drawable.baseline_not_started_24);
            fabServiceToggle.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E65100"))); // Gray out to lock it down
            fabServiceToggle.setEnabled(true);
        } else {
            fabServiceToggle.setText("Stop Trip");
            fabServiceToggle.setIconResource(R.drawable.baseline_stop_circle_24);
            fabServiceToggle.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
            fabServiceToggle.setEnabled(true);
        }
    }
    private void showPopupMenu(MaterialToolbar toolbar) {
        PopupMenu popupMenu = new PopupMenu(this, toolbar);
        popupMenu.setGravity(Gravity.END);
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