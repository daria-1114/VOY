package com.example.voy.activities;


import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voy.R;
import com.example.voy.adapters.TripItemAdapter;
import com.example.voy.data.entities.TripItemEntity;
import com.example.voy.data.repository.TripRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Collections;

public class TravelActivity extends AppCompatActivity {
        private TripRepository tripRepository;
        private String tripId;
        private String userId;
        private RecyclerView recyclerView;
        private TripItemAdapter adapter;
        private static final int REQ_DELETE_MEDIA = 501;
        private TripItemEntity pendingDeleteItem;
        @Override
        protected void onCreate (Bundle savedInstanceState){
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_travel);
                MaterialToolbar toolbar = findViewById(R.id.headerToolbarTravel);
                toolbar.setNavigationOnClickListener(v -> finish());
                toolbar.setOnMenuItemClickListener(item -> {
                        if(item.getItemId() == R.id.accountIcon){
                                showPopupMenu(toolbar);
                                return true;
                        }
                        return false;
                });
                userId = FirebaseAuth.getInstance().getCurrentUser() != null
                        ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                        : null;

                tripId = getIntent().getStringExtra("tripId");

                if (userId == null || tripId == null) {
                        finish();
                        return;
                }
                tripRepository = new TripRepository(getApplicationContext());
                recyclerView = findViewById(R.id.recyclerViewTripItems);
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                adapter = new TripItemAdapter(item -> showDeleteDialog(item));
                recyclerView.setAdapter(adapter);

                tripRepository.observeAllItemsForTrip(userId, tripId)
                        .observe(this, items -> adapter.setItems(items));

        }


        private void showPopupMenu(MaterialToolbar toolbar) {
                PopupMenu popupMenu = new PopupMenu(this, toolbar);
                popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(menuItem ->{
                        if (menuItem.getItemId() == R.id.LogOut_btn) {
                                LogOut();
                                return true;
                        }
                        if(menuItem.getItemId() == R.id.Info_btn){
                                Intent intent = new Intent(this, InfoActivity.class);
                                startActivity(intent);
                        }
                        return false;
                });
                popupMenu.show();
        }

        private void LogOut() {
                FirebaseAuth.getInstance().signOut();

                Intent intent = new Intent(TravelActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
        }

        @Override
        public boolean onOptionsItemSelected(@NonNull MenuItem menuItem){
                return super.onOptionsItemSelected(menuItem);
        }
        private void showDeleteDialog(TripItemEntity item) {
                new AlertDialog.Builder(this)
                        .setTitle("Remove from journal?")
                        .setMessage("This will only remove it from your trip journal. It will stay in your phone gallery.")
                        .setPositiveButton("Remove", (d, which) -> {
                                tripRepository.deleteTripItem(item.userId, item.tripId, item.id);
                                Toast.makeText(this, "Removed from journal.", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
        }



}
