package com.example.voy.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voy.R;
import com.example.voy.adapters.AttachmentAdapter;
import com.example.voy.adapters.LandmarkAdapter;
import com.example.voy.adapters.TripItemAdapter;
import com.example.voy.background.MediaCloner;
import com.example.voy.network.GeminiApi;
import com.example.voy.network.OverpassApi;
import com.example.voy.data.entities.LandmarkEntity;
import com.example.voy.data.entities.TripEntity;
import com.example.voy.data.entities.TripItemEntity;
import com.example.voy.viewmodels.TravelViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TravelActivity extends AppCompatActivity {

        private TravelViewModel travelViewModel;
        private String tripId;
        private String userId;

        // Trip items
        private RecyclerView recyclerView;
        private TripItemAdapter adapter;

        // Attachments
        private RecyclerView recyclerAttachments;
        private AttachmentAdapter attachmentAdapter;
        private LandmarkAdapter landmarkAdapter;

        // Title views
        private TextView tvTripTitle;
        private TextInputLayout tilTripTitle;
        private TextInputEditText etTripTitle;

        // Notes views
        private TextView tvNotes;
        private TextInputLayout tilNotes;
        private TextInputEditText etNotes;

        // Buttons
        private MaterialButton btnEditTrip;
        private MaterialButton btnSaveTripTitle;
        private MaterialButton btnAddAttachment;
        private MaterialButton btnToDoList;

        private boolean isEditMode = false;
        private TripEntity currentTrip;
        private LinearLayout chapterContainer;
        private static final int REQ_PICK_ATTACHMENT = 601;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_travel);

                // Toolbar
                MaterialToolbar toolbar = findViewById(R.id.headerToolbarTravel);
                toolbar.setNavigationOnClickListener(v -> finish());
                toolbar.setOnMenuItemClickListener(item -> {
                        if (item.getItemId() == R.id.accountIcon) {
                                showPopupMenu(toolbar);
                                return true;
                        }
                        return false;
                });

                // Title views
                tvTripTitle    = findViewById(R.id.tvTripTitle);
                tilTripTitle   = findViewById(R.id.tilTripTitle);
                etTripTitle    = findViewById(R.id.etTripTitle);
                //chapter card
                chapterContainer = findViewById(R.id.chapterContainer);
                // Notes views
                tvNotes        = findViewById(R.id.tvNotes);
                tilNotes       = findViewById(R.id.tilNotes);
                etNotes        = findViewById(R.id.etNotes);

                // Buttons
                btnEditTrip    = findViewById(R.id.btnEditTrip);
                btnSaveTripTitle = findViewById(R.id.btnSaveTripTitle);
                btnAddAttachment = findViewById(R.id.btnAddAttachment);
                btnToDoList = findViewById(R.id.btnToDoList);

                btnEditTrip.setOnClickListener(v -> enterEditMode());
                btnSaveTripTitle.setOnClickListener(v -> saveAndExitEditMode());
                btnToDoList.setOnClickListener(v -> showToDoSheet());
                // Attachments
                recyclerAttachments = findViewById(R.id.recyclerAttachments);
                recyclerAttachments.setLayoutManager(
                        new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
                attachmentAdapter = new AttachmentAdapter(new ArrayList<>(), isEditMode,
                        uri -> confirmRemoveAttachment(uri));
                recyclerAttachments.setAdapter(attachmentAdapter);

                btnAddAttachment.setOnClickListener(v -> pickAttachment());

                // Auth
                userId = FirebaseAuth.getInstance().getCurrentUser() != null
                        ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                        : null;
                tripId = getIntent().getStringExtra("tripId");

                if (userId == null || tripId == null) {
                        finish();
                        return;
                }

                travelViewModel = new ViewModelProvider(this).get(TravelViewModel.class);

                // Observe trip entity for title and notes
                travelViewModel.observeTrip(userId, tripId).observe(this, trip -> {
                        if (trip == null) return;
                        currentTrip = trip;
                        if (!isEditMode) {
                                tvTripTitle.setText(trip.title != null ? trip.title : "");
                                tvNotes.setText(trip.notes != null ? trip.notes : "");
                                tvNotes.setVisibility(
                                        trip.notes != null && !trip.notes.isEmpty()
                                                ? View.VISIBLE : View.GONE);
                                attachmentAdapter.setAttachments(
                                        trip.attachments != null ? trip.attachments : new ArrayList<>());
                        }
                });

                // Trip items RecyclerView
                recyclerView = findViewById(R.id.recyclerViewTripItems);
                LinearLayoutManager llm = new LinearLayoutManager(this);
                llm.setInitialPrefetchItemCount(4);
                recyclerView.setLayoutManager(llm);
                recyclerView.setHasFixedSize(false);
                recyclerView.setItemViewCacheSize(0);
                adapter = new TripItemAdapter(item -> showDeleteDialog(item),
                        (item, notes) -> travelViewModel.updateItemNotes(item.id, notes));
                recyclerView.setAdapter(adapter);

                travelViewModel.observeAllItemsForTrip(userId, tripId)
                        .observe(this, items -> {
                                adapter.setItems(items);
                                buildChapterStrips(items);
                        });
        }

        private void showToDoSheet() {
                BottomSheetDialog sheet = new BottomSheetDialog(this);
                View sheetView = getLayoutInflater()
                        .inflate(R.layout.todo_list, null);
                sheet.setContentView(sheetView);
                RecyclerView       recycler  = sheetView.findViewById(R.id.recyclerLandmarks);
                TextView           tvEmpty   = sheetView.findViewById(R.id.tvEmptyLandmarks);
                TextInputLayout    tilInput  = sheetView.findViewById(R.id.tilLandmarkInput);
                TextInputEditText  etInput   = sheetView.findViewById(R.id.etLandmarkInput);
                MaterialButton     btnAdd    = sheetView.findViewById(R.id.btnAddLandmark);
                MaterialButton btnAiSuggest = sheetView.findViewById(R.id.btnAiSuggest);
                landmarkAdapter = new LandmarkAdapter(landmark ->
                        new AlertDialog.Builder(this)
                                .setTitle("Remove landmark?")
                                .setPositiveButton("Remove", (d, w) ->
                                        travelViewModel.deleteLandmark(landmark.id))
                                .setNegativeButton("Cancel", null)
                                .show()
                );
                recycler.setLayoutManager(new LinearLayoutManager(this));
                recycler.setAdapter(landmarkAdapter);
                travelViewModel.observeLandmarks(tripId).observe(this, landmarks -> {
                        landmarkAdapter.setItems(landmarks);
                        tvEmpty.setVisibility(
                                landmarks == null || landmarks.isEmpty()
                                        ? View.VISIBLE : View.GONE);
                });
                btnAdd.setOnClickListener(v ->
                        tilInput.setVisibility(
                                tilInput.getVisibility() == View.GONE
                                        ? View.VISIBLE : View.GONE)
                );
                if (currentTrip == null || currentTrip.endTime == null || currentTrip.endTime <= 0) {
                        btnAiSuggest.setVisibility(View.GONE);
                } else {
                        btnAiSuggest.setVisibility(View.VISIBLE);
                }
                btnAiSuggest.setOnClickListener(v ->{
                        if(currentTrip == null) return;
                        String city = currentTrip.title != null && !currentTrip.title.isEmpty() ? currentTrip.title : "this city";
                        long durationMs = currentTrip.endTime - currentTrip.startTime;
                        int totalDays = (int) Math.ceil((double) durationMs / TimeUnit.HOURS.toMillis(24));
                        if (totalDays <= 0) totalDays = 1;
                        btnAiSuggest.setEnabled(false);
                        btnAiSuggest.setText("Thinking...");
                        GeminiApi.generateItinerary(city, totalDays, new GeminiApi.OnResult() {
                                @Override
                                public void onSuccess(JSONArray landmarks) {
                                        btnAiSuggest.setEnabled(true);
                                        btnAiSuggest.setText("Auto-Fill with AI");
                                        showItineraryApprovalDialog(city, landmarks);
                                }

                                @Override
                                public void onError(String error) {
                                        btnAiSuggest.setEnabled(true);
                                        btnAiSuggest.setText("Auto-Fill with AI");
                                        Toast.makeText(TravelActivity.this, "AI could not generate itinerary: " + error, Toast.LENGTH_SHORT).show();
                                }
                        });
                });
                etInput.setOnEditorActionListener((v, actionId, event) -> {
                        String name = etInput.getText() != null
                                ? etInput.getText().toString().trim() : "";
                        if (name.isEmpty()) return true;

                        etInput.setEnabled(false);
                        tilInput.setHelperText("Searching…");

                        OverpassApi.fetchCoordinates(name, new OverpassApi.OnResult() {
                                @Override
                                public void onFound(double lat, double lng) {
                                        runOnUiThread(() -> {
                                                LandmarkEntity landmark = new LandmarkEntity(
                                                        UUID.randomUUID().toString(),
                                                        tripId, name,
                                                        lat, lng,
                                                        0,
                                                        false,
                                                        System.currentTimeMillis()
                                                );
                                                travelViewModel.insertLandmark(landmark);
                                                etInput.setText("");
                                                etInput.setEnabled(true);
                                                tilInput.setHelperText(null);
                                                tilInput.setVisibility(View.GONE);
                                        });
                                }

                                @Override
                                public void onNotFound() {
                                        runOnUiThread(() -> {
                                                tilInput.setHelperText(
                                                        "Landmark not found. Try a different name.");
                                                etInput.setEnabled(true);
                                        });
                                }
                        });
                        return true;
                });

                sheet.show();
        }

        private void showItineraryApprovalDialog(String city, JSONArray landmarks) {
                StringBuilder displayList = new StringBuilder();
                try {
                        for (int i = 0; i < landmarks.length(); i++) {
                                org.json.JSONObject obj = landmarks.getJSONObject(i);
                                displayList.append("Day ").append(obj.getInt("dayNumber"))
                                        .append(": ").append(obj.getString("name")).append("\n");
                        }
                } catch (Exception e) {
                        Toast.makeText(this, "Error reading AI data", Toast.LENGTH_SHORT).show();
                        return;
                }

                new AlertDialog.Builder(this)
                        .setTitle("AI Recommendations for " + city)
                        .setMessage("Here are some great spots to track:\n\n" + displayList.toString() + "\nAdd these to your To-Do list?")
                        .setPositiveButton("Accept", (dialog, which) -> saveAndResolveAiLandmarks(landmarks))
                        .setNegativeButton("No Thanks", null)
                        .show();
        }
        private void saveAndResolveAiLandmarks(JSONArray aiLandmarks) {
                new Thread(() -> {
                        try {
                                for (int i = 0; i < aiLandmarks.length(); i++) {
                                        JSONObject obj = aiLandmarks.getJSONObject(i);
                                        String name = obj.getString("name");
                                        int dayNum = obj.getInt("dayNumber");

                                        // 1. Create the landmark with NULL coordinates
                                        LandmarkEntity landmark = new LandmarkEntity(
                                                UUID.randomUUID().toString(),
                                                tripId,
                                                name,
                                                null, null, // lat/lng are null until Overpass finds them
                                                dayNum,     // AI assigned day
                                                false,
                                                System.currentTimeMillis()
                                        );

                                        // 2. Insert it immediately so it shows up in the UI
                                        travelViewModel.insertLandmark(landmark);

                                        // 3. Ask Overpass for the exact GPS map coordinates
                                        OverpassApi.fetchCoordinates(name, new OverpassApi.OnResult() {
                                                @Override
                                                public void onFound(double lat, double lng) {
                                                        travelViewModel.updateLandmarkCoordinates(landmark.id, lat, lng);
                                                }
                                                @Override
                                                public void onNotFound() {
                                                        Log.w("TravelActivity", "Overpass could not find coordinates for: " + name);
                                                }
                                        });
                                }

                                // Show success message on main thread
                                runOnUiThread(() -> Toast.makeText(TravelActivity.this, "Landmarks added!", Toast.LENGTH_SHORT).show());

                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                }).start();
        }
        private void buildChapterStrips(List<TripItemEntity> items) {
                chapterContainer.removeAllViews();
                if (items == null) {
                        Log.d("TravelActivity", "buildChapterStrip: items is null");
                        return;
                }

                Log.d("TravelActivity", "buildChapterStrip: total items = " + items.size());


                for (int i = 0; i < items.size(); i++) {
                        TripItemEntity item = items.get(i);
                        Log.d("TravelActivity", "item " + i + " type=" + item.type + " title=" + item.title);
                        if (item.type != com.example.voy.enums.TripItemType.DAY) continue;

                        final int position = i;
                        View chip = LayoutInflater.from(this)
                                .inflate(R.layout.item_chapter_card, chapterContainer, false);
                        TextView label = chip.findViewById(R.id.txtChapterLabel);
                        label.setText(item.title != null ? item.title : "");

                        chip.setOnClickListener(v -> {
                                recyclerView.post(() -> {
                                        recyclerView.smoothScrollToPosition(position);
                                });
                        });
                        chapterContainer.addView(chip);
                }
        }

        private void enterEditMode() {
                isEditMode = true;

                // Show edit fields, hide view fields
                tvTripTitle.setVisibility(View.GONE);
                tilTripTitle.setVisibility(View.VISIBLE);
                etTripTitle.setText(currentTrip != null ? currentTrip.title : "");

                tvNotes.setVisibility(View.GONE);
                tilNotes.setVisibility(View.VISIBLE);
                etNotes.setText(currentTrip != null ? currentTrip.notes : "");

                btnEditTrip.setVisibility(View.GONE);
                btnSaveTripTitle.setVisibility(View.VISIBLE);
                btnAddAttachment.setVisibility(View.VISIBLE);

                attachmentAdapter.setEditMode(true);
                etTripTitle.requestFocus();
        }

        private void saveAndExitEditMode() {
                isEditMode = false;

                String newTitle = etTripTitle.getText() != null
                        ? etTripTitle.getText().toString().trim() : "";
                String newNotes = etNotes.getText() != null
                        ? etNotes.getText().toString().trim() : "";

                travelViewModel.updateTripTitle(userId, tripId, newTitle);
                travelViewModel.updateTripNotes(userId, tripId, newNotes);

                // Update UI
                tvTripTitle.setText(newTitle);
                tvNotes.setText(newNotes);
                tvNotes.setVisibility(!newNotes.isEmpty() ? View.VISIBLE : View.GONE);

                tilTripTitle.setVisibility(View.GONE);
                tvTripTitle.setVisibility(View.VISIBLE);
                tilNotes.setVisibility(View.GONE);

                btnSaveTripTitle.setVisibility(View.GONE);
                btnEditTrip.setVisibility(View.VISIBLE);
                btnAddAttachment.setVisibility(View.GONE);

                attachmentAdapter.setEditMode(false);
        }

        private void pickAttachment() {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES,
                        new String[]{"image/*", "application/pdf"});
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, REQ_PICK_ATTACHMENT);
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
                super.onActivityResult(requestCode, resultCode, data);
                if (requestCode == REQ_PICK_ATTACHMENT
                        && resultCode == RESULT_OK
                        && data != null
                        && data.getData() != null) {

                        Uri uri = data.getData();
                        // Persist permission so we can read it later
                        String internalUri = MediaCloner.cloneToInternal(this, uri, ".pdf");

                        travelViewModel.addAttachment(userId, tripId,internalUri);
                }
        }

        private void confirmRemoveAttachment(String uri) {
                new AlertDialog.Builder(this)
                        .setTitle("Remove attachment?")
                        .setPositiveButton("Remove", (d, w) ->
                                travelViewModel.removeAttachment(userId, tripId, uri))
                        .setNegativeButton("Cancel", null)
                        .show();
        }

        private void showPopupMenu(MaterialToolbar toolbar) {
                PopupMenu popupMenu = new PopupMenu(this, toolbar);
                popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(menuItem -> {
                        if (menuItem.getItemId() == R.id.LogOut_btn) {
                                LogOut();
                                return true;
                        }
                        if (menuItem.getItemId() == R.id.Info_btn) {
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
        public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
                return super.onOptionsItemSelected(menuItem);
        }

        private void showDeleteDialog(TripItemEntity item) {
                new AlertDialog.Builder(this)
                        .setTitle("Remove from journal?")
                        .setMessage("This will only remove it from your trip journal. It will stay in your phone gallery.")
                        .setPositiveButton("Remove", (d, which) -> {
                                travelViewModel.deleteTripItem(item.userId, item.tripId, item.id);
                                Toast.makeText(this, "Removed from journal.", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
        }

        @Override
        protected void onDestroy() {
                super.onDestroy();
                if (adapter != null) adapter.releaseMediaPlayer();
        }
}