package com.example.voy.activities;


import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.voy.R;
import com.example.voy.dao.TravelEntryDao;
import com.example.voy.database.AppDatabase;
import com.example.voy.model.TravelEntry;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap gMap;
    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";

    private TravelEntryDao travelEntryDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        MaterialToolbar toolbar = findViewById(R.id.mapToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        AppDatabase db = AppDatabase.getInstance(this);
        travelEntryDao = db.travelEntryDao();

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;

        gMap.getUiSettings().setZoomControlsEnabled(true);

        List<TravelEntry> entries = travelEntryDao.getAllEntries(); //will make async later
        if (entries.isEmpty()) {
            Toast.makeText(this, "No travel entries to display", Toast.LENGTH_SHORT).show();
            return;
        }

        for (TravelEntry entry : entries) {
            LatLng location = new LatLng(entry.latitude(), entry.longitude());
            gMap.addMarker(new MarkerOptions()
                    .position(location).title(entry.title()));
        }

        TravelEntry firstEntry = entries.get(0);
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(firstEntry.latitude(), firstEntry.longitude()), 10f));

        gMap.setOnMarkerClickListener(marker -> {
            Toast.makeText(MapActivity.this, marker.getTitle(), Toast.LENGTH_SHORT).show();
            return false;
        });
    }

    @Override
    protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override
    protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override
    protected void onStop() { mapView.onStop(); super.onStop(); }
    @Override
    protected void onPause() { mapView.onPause(); super.onPause(); }
    @Override
    protected void onDestroy() { mapView.onDestroy(); super.onDestroy(); }
    @Override
    public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle);
        }
        mapView.onSaveInstanceState(mapViewBundle);
    }
}
