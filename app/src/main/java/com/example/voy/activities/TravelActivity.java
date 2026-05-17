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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voy.R;
import com.example.voy.adapters.AttachmentAdapter;
import com.example.voy.adapters.LandmarkAdapter;
import com.example.voy.adapters.TripItemAdapter;
import com.example.voy.background.MediaCloner;
import com.example.voy.background.OverpassApi;
import com.example.voy.data.entities.LandmarkEntity;
import com.example.voy.data.entities.TripEntity;
import com.example.voy.data.entities.TripItemEntity;
import com.example.voy.data.repository.TripRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TravelActivity extends AppCompatActivity {

        private TripRepository tripRepository;
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

                tripRepository = new TripRepository(getApplicationContext());

                // Observe trip entity for title and notes
                tripRepository.observeTrip(userId, tripId).observe(this, trip -> {
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
                        (item, notes) -> tripRepository.updateItemNotes(item.id, notes));
                recyclerView.setAdapter(adapter);

                tripRepository.observeAllItemsForTrip(userId, tripId)
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
                landmarkAdapter = new LandmarkAdapter(landmark ->
                        new AlertDialog.Builder(this)
                                .setTitle("Remove landmark?")
                                .setPositiveButton("Remove", (d, w) ->
                                        tripRepository.deleteLandmark(landmark.id))
                                .setNegativeButton("Cancel", null)
                                .show()
                );
                recycler.setLayoutManager(new LinearLayoutManager(this));
                recycler.setAdapter(landmarkAdapter);
                tripRepository.observeLandmarks(tripId).observe(this, landmarks -> {
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
                                                        false,
                                                        System.currentTimeMillis()
                                                );
                                                tripRepository.insertLandmark(landmark);
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

                tripRepository.updateTripTitle(userId, tripId, newTitle);
                tripRepository.updateTripNotes(userId, tripId, newNotes);

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

                        tripRepository.addAttachment(userId, tripId,internalUri);
                }
        }

        private void confirmRemoveAttachment(String uri) {
                new AlertDialog.Builder(this)
                        .setTitle("Remove attachment?")
                        .setPositiveButton("Remove", (d, w) ->
                                tripRepository.removeAttachment(userId, tripId, uri))
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
                                tripRepository.deleteTripItem(item.userId, item.tripId, item.id);
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