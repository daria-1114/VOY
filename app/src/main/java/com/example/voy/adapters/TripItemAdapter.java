package com.example.voy.adapters;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.location.Address;
import android.location.Geocoder;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.voy.BuildConfig;
import com.example.voy.R;
import com.example.voy.data.entities.TripItemEntity;
import com.example.voy.data.repository.TripRepository;
import com.example.voy.enums.TripItemType;
import com.example.voy.viewHolders.AudioViewHolder;
import com.example.voy.viewHolders.PhotoViewHolder;
import com.example.voy.viewHolders.StepsViewHolder;
import com.example.voy.viewHolders.DayViewHolder;
import com.example.voy.viewHolders.VideoViewHolder;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TripItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // -------------------------------------------------------------------------
    // View type constants
    // -------------------------------------------------------------------------

    private static final int TYPE_PHOTO = 1;
    private static final int TYPE_VIDEO = 2;
    private static final int TYPE_AUDIO = 3;
    private static final int TYPE_STEPS = 4;
    private static final int TYPE_DAY = 5;

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final List<TripItemEntity> displayItems = new ArrayList<>();
    private final Map<String, String> placeCache = new HashMap<>();
    private MediaPlayer mediaPlayer;
    private AudioViewHolder currentAudioHolder;
    private TripRepository repository;
    private GenerativeModelFutures model;
    public interface OnItemClickedListener {
        void onItemClick(TripItemEntity item);
    }
    public interface OnNoteSavedListener {
        void onNoteSaved(TripItemEntity item, String newNotes);
    }
    private final OnItemClickedListener listener;
    private final OnNoteSavedListener savedListener;
    public TripItemAdapter(OnItemClickedListener listener, OnNoteSavedListener savedListener) {
        this.listener = listener;
        this.savedListener = savedListener;

        GenerativeModel generativeModel = new GenerativeModel(
                "gemini-flash-latest",
                BuildConfig.GEMINI_API_KEY
        );
        this.model = GenerativeModelFutures.from(generativeModel);
    }
    // -------------------------------------------------------------------------
    // RecyclerView overrides
    // -------------------------------------------------------------------------

    @Override
    public int getItemViewType(int position) {
        switch (displayItems.get(position).type) {
            case VIDEO: return TYPE_VIDEO;
            case AUDIO: return TYPE_AUDIO;
            case STEPS: return TYPE_STEPS;
            case DAY: return TYPE_DAY;
            case PHOTO: default: return TYPE_PHOTO;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_PHOTO) {
            return new PhotoViewHolder(
                    inflater.inflate(R.layout.trip_item_image, parent, false));
        } else if (viewType == TYPE_VIDEO) {
            return new VideoViewHolder(
                    inflater.inflate(R.layout.trip_item_video, parent, false));
        } else if (viewType == TYPE_STEPS) {
            return new StepsViewHolder(
                    inflater.inflate(R.layout.trip_item_steps, parent, false));
        } else if (viewType == TYPE_AUDIO) {
            return new AudioViewHolder(
                    inflater.inflate(R.layout.trip_item_audio, parent, false));
        } else{
            return new DayViewHolder(
                    inflater.inflate(R.layout.trip_item_day, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TripItemEntity item = displayItems.get(position);

        if (holder instanceof PhotoViewHolder) {
            PhotoViewHolder h = (PhotoViewHolder) holder;
            bindImage(h.imageView, item.localUri);
            bindMapPreview(h.mapPreview, h.mapCard, item.lat, item.lng);
            attachOpenMapsClick(h.mapCard, item.lat, item.lng);
            h.placeChip.setVisibility(View.GONE);
            bindNotesCard(h.cardNotes, h.txtNotesPreview, item, repository);

        } else if (holder instanceof VideoViewHolder) {
            VideoViewHolder h = (VideoViewHolder) holder;

            if (h.mediaPlayer != null) {
                h.mediaPlayer.release();
                h.mediaPlayer = null;
            }
            if (h.playOverlay != null) h.playOverlay.setVisibility(View.VISIBLE);

            Uri videoUri = resolveUri(h.itemView, item.localUri);
            if (videoUri == null) return;

            h.videoView.setSurfaceTextureListener(
                    new android.view.TextureView.SurfaceTextureListener() {
                        @Override
                        public void onSurfaceTextureAvailable(
                                android.graphics.SurfaceTexture surface, int w, int h2) {
                            try {
                                android.media.MediaPlayer mp = new android.media.MediaPlayer();
                                h.mediaPlayer = mp;
                                mp.setSurface(new android.view.Surface(surface));
                                mp.setDataSource(h.itemView.getContext(), videoUri);
                                mp.prepareAsync();
                                mp.setOnPreparedListener(prepared -> prepared.seekTo(500));
                                mp.setOnCompletionListener(completed -> {
                                    if (h.playOverlay != null)
                                        h.playOverlay.setVisibility(View.VISIBLE);
                                });
                            } catch (Exception e) {
                                android.util.Log.e("TripItemAdapter", "Video error", e);
                            }
                        }

                        @Override
                        public void onSurfaceTextureSizeChanged(
                                android.graphics.SurfaceTexture s, int w, int h2) {}

                        @Override
                        public boolean onSurfaceTextureDestroyed(
                                android.graphics.SurfaceTexture s) {
                            if (h.mediaPlayer != null) {
                                h.mediaPlayer.release();
                                h.mediaPlayer = null;
                            }
                            return true;
                        }

                        @Override
                        public void onSurfaceTextureUpdated(
                                android.graphics.SurfaceTexture s) {}
                    });

            if (h.playOverlay != null) {
                h.playOverlay.setOnClickListener(v -> {
                    if (h.mediaPlayer == null) return;
                    if (h.mediaPlayer.isPlaying()) {
                        h.mediaPlayer.pause();
                        h.playOverlay.setVisibility(View.VISIBLE);
                    } else {
                        h.mediaPlayer.start();
                        h.playOverlay.setVisibility(View.GONE);
                    }
                });
            }

            bindMapPreview(h.mapPreview, h.mapCard, item.lat, item.lng);
            attachOpenMapsClick(h.mapCard, item.lat, item.lng);
            bindPlaceChip(h.placeChip, item.lat, item.lng, item.title);
            bindNotesCard(h.cardNotes, h.txtNotesPreview, item, repository);
        } else if (holder instanceof AudioViewHolder) {
            AudioViewHolder h = (AudioViewHolder) holder;
            bindMapPreview(h.mapPreview, h.mapCard, item.lat, item.lng);
            attachOpenMapsClick(h.mapCard, item.lat, item.lng);
            bindPlaceChip(h.placeChip, item.lat, item.lng, item.title);
            bindNotesCard(h.cardNotes, h.txtNotesPreview, item, repository);
            animateWaveform(h, false);

            h.playButton.setOnClickListener(v -> {
                Uri audioUri = resolveUri(v, item.localUri);
                if (audioUri == null) return;

                if (mediaPlayer != null && mediaPlayer.isPlaying() && currentAudioHolder == h) {
                    mediaPlayer.pause();
                    h.playButton.setIconResource(android.R.drawable.ic_media_play);
                    animateWaveform(h, false);
                    return;
                }

                if (mediaPlayer != null && !mediaPlayer.isPlaying() && currentAudioHolder == h) {
                    mediaPlayer.start();
                    h.playButton.setIconResource(android.R.drawable.ic_media_pause);
                    animateWaveform(h, true);
                    return;
                }

                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                    if (currentAudioHolder != null) {
                        currentAudioHolder.playButton.setIconResource(
                                android.R.drawable.ic_media_play);
                        animateWaveform(currentAudioHolder, false);
                    }
                }

                try {
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setDataSource(v.getContext(), audioUri);
                    mediaPlayer.prepareAsync();
                    currentAudioHolder = h;
                    h.playButton.setIconResource(android.R.drawable.ic_media_pause);

                    mediaPlayer.setOnPreparedListener(mp -> {
                        mp.start();
                        animateWaveform(h, true);
                    });
                    mediaPlayer.setOnCompletionListener(mp -> {
                        h.playButton.setIconResource(android.R.drawable.ic_media_play);
                        animateWaveform(h, false);
                        mediaPlayer.release();
                        mediaPlayer = null;
                        currentAudioHolder = null;
                    });
                    mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                        h.playButton.setIconResource(android.R.drawable.ic_media_play);
                        animateWaveform(h, false);
                        mediaPlayer.release();
                        mediaPlayer = null;
                        currentAudioHolder = null;
                        return true;
                    });
                } catch (Exception e) {
                    android.util.Log.e("TripItemAdapter", "Audio playback error", e);
                }
            });

        } else if (holder instanceof StepsViewHolder) {
            StepsViewHolder h = (StepsViewHolder) holder;
            try {
                org.json.JSONObject meta = new org.json.JSONObject(
                        item.metadataJson != null ? item.metadataJson : "{}");
                int steps = meta.optInt("steps", 0);
                String dayLabel = meta.optString("dayLabel", "");
                h.dayLabel.setText(dayLabel);
                h.stepCount.setText(steps + " steps");
            } catch (Exception e) {
                h.dayLabel.setText(item.title != null ? item.title : "");
                h.stepCount.setText("");
            }
        } else if (holder instanceof DayViewHolder) {
        DayViewHolder h = (DayViewHolder) holder;
        h.dayHeader.setText(item.title != null ? item.title : "");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder,
                                 int position, @NonNull List<Object> payloads) {
        onBindViewHolder(holder, position);
    }

    @Override
    public int getItemCount() {
        return displayItems.size();
    }
    public List<TripItemEntity> getItems() {
        return displayItems;
    }
    // -------------------------------------------------------------------------
    // Data
    // -------------------------------------------------------------------------

    public void setItems(List<TripItemEntity> newItems) {
        displayItems.clear();
        if (newItems != null) displayItems.addAll(newItems);
        notifyDataSetChanged();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    public void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (currentAudioHolder != null) {
            animateWaveform(currentAudioHolder, false);
            currentAudioHolder = null;
        }
    }

    private void animateWaveform(AudioViewHolder h, boolean playing) {
        android.view.ViewGroup wave = h.itemView.findViewById(R.id.waveContainer);
        if (wave == null) return;

        for (int i = 0; i < wave.getChildCount(); i++) {
            android.view.View bar = wave.getChildAt(i);
            if (playing) {
                android.animation.ObjectAnimator anim = android.animation.ObjectAnimator
                        .ofFloat(bar, "scaleY", 1f, 0.3f, 1f);
                anim.setDuration(600 + (i * 80L));
                anim.setRepeatCount(android.animation.ObjectAnimator.INFINITE);
                anim.setRepeatMode(android.animation.ObjectAnimator.REVERSE);
                anim.setInterpolator(
                        new android.view.animation.AccelerateDecelerateInterpolator());
                bar.setTag(anim);
                anim.start();
            } else {
                Object tag = bar.getTag();
                if (tag instanceof android.animation.ObjectAnimator) {
                    ((android.animation.ObjectAnimator) tag).cancel();
                }
                bar.setScaleY(1f);
            }
        }
    }

    private Uri resolveUri(View v, String localUri) {
        if (localUri == null) return null;
        Uri parsed = Uri.parse(localUri);
        if ("content".equals(parsed.getScheme())) return parsed;
        if ("file".equals(parsed.getScheme())) {
            try {
                return androidx.core.content.FileProvider.getUriForFile(
                        v.getContext(),
                        "com.example.voy.fileprovider",
                        new java.io.File(parsed.getPath()));
            } catch (Exception e) {
                return null;
            }
        }
        return parsed;
    }

    private void bindImage(ImageView iv, String localUri) {
        if (iv == null) return;
        if (localUri == null || localUri.trim().isEmpty()) {
            iv.setImageDrawable(null);
            return;
        }
        Glide.with(iv.getContext())
                .load(Uri.parse(localUri))
                .centerCrop()
                .into(iv);
    }

    private void bindMapPreview(ImageView iv,
                                com.google.android.material.card.MaterialCardView card,
                                Double lat, Double lng) {
        if (card == null) return;
        if (lat == null || lng == null) {
            card.setVisibility(View.GONE);
            return;
        }
        card.setVisibility(View.VISIBLE);
        if (iv == null) return;

        String url = "https://maps.googleapis.com/maps/api/staticmap"
                + "?center=" + lat + "," + lng
                + "&zoom=15"
                + "&size=300x440"
                + "&scale=2"
                + "&maptype=roadmap"
                + "&markers=color:red%7C" + lat + "," + lng
                + "&key=" + BuildConfig.MAPS_API_KEY;

        Glide.with(iv.getContext())
                .load(url)
                .centerCrop()
                .into(iv);
    }

    private void bindPlaceChip(TextView chip, Double lat, Double lng, String landmark) {
        if (chip == null) return;
        if (landmark != null && !landmark.trim().isEmpty()) {
            chip.setText(landmark);
            chip.setVisibility(View.VISIBLE);
            return;
        }
        if (lat == null || lng == null) {
            chip.setVisibility(View.GONE);
            return;
        }

        String key = String.format(Locale.US, "%.3f,%.3f", lat, lng);
        String cached = placeCache.get(key);
        if (cached != null) {
            chip.setText(cached);
            chip.setVisibility(View.VISIBLE);
            return;
        }

        chip.setVisibility(View.GONE);

        new Thread(() -> {
            String label = null;
            try {
                Geocoder geocoder = new Geocoder(chip.getContext(), Locale.getDefault());
                List<Address> results = geocoder.getFromLocation(lat, lng, 1);
                if (results != null && !results.isEmpty()) {
                    Address a = results.get(0);
                    String sub     = a.getSubLocality();
                    String city    = a.getLocality();
                    String admin   = a.getAdminArea();
                    String country = a.getCountryName();
                    if (sub != null && !sub.isEmpty())              label = sub;
                    else if (city != null && !city.isEmpty())       label = city;
                    else if (admin != null && !admin.isEmpty())     label = admin;
                    else if (country != null && !country.isEmpty()) label = country;
                }
            } catch (Exception ignored) {}

            if (label != null) {
                placeCache.put(key, label);
                String finalLabel = label;
                chip.post(() -> {
                    chip.setText(finalLabel);
                    chip.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }
    private void bindNotesCard(View cardNotes, TextView txtNotesPreview,
                               TripItemEntity item, TripRepository repository) {
        String existingNotes = item.notes != null ? item.notes : "";

        txtNotesPreview.setText(existingNotes.isEmpty() ? null : existingNotes);

        String finalExistingNotes = existingNotes;
        cardNotes.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(v.getContext())
                    .inflate(R.layout.dialog_notes, null);

            TextInputEditText etNote = dialogView.findViewById(R.id.etNoteInput);
            MaterialButton btnAi = dialogView.findViewById(R.id.btnAiGenerate);
            MaterialButton btnMic = dialogView.findViewById(R.id.btnSpeechToText);
            btnAi.setVisibility(item.type == TripItemType.AUDIO ?
                    View.GONE : View.VISIBLE);
            btnMic.setVisibility(item.type == com.example.voy.enums.TripItemType.AUDIO
                    ? View.VISIBLE : View.GONE);
            etNote.setText(existingNotes);

            btnAi.setOnClickListener(v1 -> {
                Log.d("VOYai", "ai button clicked");
                btnAi.setEnabled(false);
                etNote.setHint("Thinking...");

                try{
                    Bitmap bitmap;
                    Uri uri = Uri.parse(item.localUri);
                    try (InputStream inputStream = v1.getContext().getContentResolver().openInputStream(uri)) {
                        bitmap = BitmapFactory.decodeStream(inputStream);
                    }
                    if (bitmap == null) {
                        Log.e("VOYai", "Bitmap is NULL after loading!");
                        etNote.setHint("Error: Could not read image file.");
                        btnAi.setEnabled(true);
                        return;
                    }
                    Log.d("VOYai", "Sending request to Gemini...");
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 1024, 1024, true);
                    Content content = new Content.Builder()
                            .addImage(bitmap)
                            .addText("You are a poetic travel assistant. Write a short, nostalgic " +
                                    "one-sentence diary entry in first person based on what you see in this " + item.type + ".")
                            .build();
                    ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

                    response.addListener(() ->{
                        try{
                            GenerateContentResponse result = response.get();
                            String aiText = result.getText();
                            Log.d("VOYai", "SUCCESS! Text: " + aiText);
                            etNote.post(() -> etNote.setText(aiText));
                        }catch (java.util.concurrent.ExecutionException e) {
                            // THIS is where the 404 or 403 error is actually hiding!
                            Throwable cause = e.getCause();
                            String errorMsg = (cause != null) ? cause.getMessage() : e.getMessage();
                            Log.e("VOYai", "SERVER ERROR: " + errorMsg);
                            etNote.post(() -> etNote.setHint("Server Error: " + errorMsg));
                        } catch (Exception e) {
                            Log.e("VOYai", "General Error: " + e.getMessage());
                        }
                        btnAi.post(() -> btnAi.setEnabled(true));
                        etNote.setHint("Write a note..");
                    },ContextCompat.getMainExecutor(v1.getContext()));
                } catch (SecurityException e) {
                    Log.e("VOYai", "Bitmap load failed: ", e);
                    etNote.setHint("Error loading image");
                    btnAi.setEnabled(true);
                }catch(Exception e){
                    Log.e("VoYai","General load error", e);
                    btnAi.setEnabled(true);
                }
            });
            btnMic.setOnClickListener(v1 -> {
                btnMic.setEnabled(false);
                etNote.setHint("Listening to the recording...");

                try {
                    Uri audioUri = Uri.parse(item.localUri);
                    InputStream inputStream = v1.getContext().getContentResolver().openInputStream(audioUri);

                    java.io.ByteArrayOutputStream byteBuffer = new java.io.ByteArrayOutputStream();
                    int bufferSize = 1024;
                    byte[] buffer = new byte[bufferSize];

                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        byteBuffer.write(buffer, 0, len);
                    }
                    byte[] audioBytes = byteBuffer.toByteArray();
                    inputStream.close();

                    Content content = new Content.Builder()
                            .addBlob("audio/mp3", audioBytes)
                            .addText("Listen to this recording. " +
                                    "If you hear speech, include the essence of the conversation in the entry. " +
                                    "Otherwise, describe the ambient atmosphere of this crowded place. Write only one nostalgic, first-person diary sentence.")
                            .build();


                    ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

                    response.addListener(() -> {
                        try {
                            String aiText = response.get().getText();
                            etNote.setText(aiText);
                        } catch (Exception e) {
                            etNote.setHint("AI couldn't process this audio file.");
                        }
                        btnMic.setEnabled(true);
                        etNote.setHint("Write a note...");
                    }, ContextCompat.getMainExecutor(v1.getContext()));

                } catch (Exception e) {
                    etNote.setHint("Error reading audio file.");
                    btnMic.setEnabled(true);
                }
            });            new androidx.appcompat.app.AlertDialog.Builder(v.getContext())
                    .setView(dialogView)
                    .setPositiveButton("Save", (dialog, which) -> {
                        String newNotes = etNote.getText() != null
                                ? etNote.getText().toString().trim() : "";
                        txtNotesPreview.setText(newNotes.isEmpty() ? null : newNotes);
                        if (savedListener != null) savedListener.onNoteSaved(item, newNotes);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }
    private void attachOpenMapsClick(View mapCardView, Double lat, Double lng) {
        if (mapCardView == null) return;
        if (lat == null || lng == null) {
            mapCardView.setOnClickListener(null);
            mapCardView.setClickable(false);
            return;
        }
        mapCardView.setClickable(true);
        mapCardView.setOnClickListener(v -> {
            String url = "https://www.google.com/maps?q=" + lat + "," + lng;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            v.getContext().startActivity(intent);
        });
    }
}