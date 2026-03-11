package com.example.voy.adapters;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.voy.BuildConfig;
import com.example.voy.R;
import com.example.voy.data.entities.TripItemEntity;
import com.example.voy.viewHolders.AudioViewHolder;
import com.example.voy.viewHolders.PhotoViewHolder;
import com.example.voy.viewHolders.StepsViewHolder;
import com.example.voy.viewHolders.VideoViewHolder;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TripItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<TripItemEntity> items = new ArrayList<>();

    private static final int TYPE_PHOTO = 1;
    private static final int TYPE_VIDEO = 2;
    private static final int TYPE_AUDIO = 3;
    private static final int TYPE_STEPS = 4;
    private final Map<String, String> placeCache = new HashMap<>();
    private MediaPlayer mediaPlayer;
    private AudioViewHolder currentAudioHolder;
    public interface OnItemClickedListener {
        void onItemClick(TripItemEntity item);
        // Optional: if you want separate click events later
        // void onAudioPlayClick(TripItemEntity item);
    }

    private final OnItemClickedListener listener;

    public TripItemAdapter(OnItemClickedListener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        TripItemEntity item = items.get(position);
        switch (item.type) {
            case VIDEO:
                return TYPE_VIDEO;
            case AUDIO:
                return TYPE_AUDIO;
            case STEPS:
                return TYPE_STEPS;
            case PHOTO:
            default:
                return TYPE_PHOTO;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_PHOTO) {
            View v = inflater.inflate(R.layout.trip_item_image, parent, false);
            return new PhotoViewHolder(v);
        } else if (viewType == TYPE_VIDEO) {
            View v = inflater.inflate(R.layout.trip_item_video, parent, false);
            return new VideoViewHolder(v);
        }else if(viewType == TYPE_STEPS){
            View v = inflater.inflate(R.layout.trip_item_steps, parent, false);
            return new StepsViewHolder(v);
        }else {
            View v = inflater.inflate(R.layout.trip_item_audio, parent, false);
            return new AudioViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TripItemEntity item = items.get(position);

        if (holder instanceof PhotoViewHolder) {
            PhotoViewHolder h = (PhotoViewHolder) holder;
            bindImage(h.imageView, item.localUri);
            bindMapPreview(h.mapPreview, h.mapCard, item.lat, item.lng);
            attachOpenMapsClick(h.mapCard, item.lat, item.lng);
            bindPlaceChip(h.placeChip, item.lat, item.lng, item.title);
        }
        else if (holder instanceof VideoViewHolder) {
            VideoViewHolder h = (VideoViewHolder) holder;

            // Stop and release any previous playback on this holder
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
                                mp.setOnPreparedListener(prepared -> {
                                    // Seek to first frame as preview, don't autoplay
                                    prepared.seekTo(500);
                                });
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
        }
        else if (holder instanceof AudioViewHolder) {
            AudioViewHolder h = (AudioViewHolder) holder;
            bindMapPreview(h.mapPreview, h.mapCard, item.lat, item.lng);
            attachOpenMapsClick(h.mapCard, item.lat, item.lng);
            bindPlaceChip(h.placeChip, item.lat, item.lng, item.title);

            // Reset waveform to static state when holder is reused
            animateWaveform(h, false);

            h.playButton.setOnClickListener(v -> {
                Uri audioUri = resolveUri(v, item.localUri);
                if (audioUri == null) return;

                // If this holder is already playing, pause it
                if (mediaPlayer != null && mediaPlayer.isPlaying()
                        && currentAudioHolder == h) {
                    mediaPlayer.pause();
                    h.playButton.setIconResource(android.R.drawable.ic_media_play);
                    animateWaveform(h, false);
                    return;
                }

                // If paused on this same holder, resume
                if (mediaPlayer != null && !mediaPlayer.isPlaying()
                        && currentAudioHolder == h) {
                    mediaPlayer.start();
                    h.playButton.setIconResource(android.R.drawable.ic_media_pause);
                    animateWaveform(h, true);
                    return;
                }

                // Stop any previously playing audio on a different holder
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

                // Start new audio
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
        } else if( holder instanceof StepsViewHolder){
            StepsViewHolder h = (StepsViewHolder) holder;
            try{
                org.json.JSONObject meta = new org.json.JSONObject(
                        item.metadataJson != null ? item.metadataJson : "{}");
                int steps = meta.optInt("steps", 0);
                String dayLabel = meta.optString("dayLabel", "");
                h.dayLabel.setText(dayLabel);
                h.stepCount.setText(steps + "steps");
            } catch (Exception e) {
                h.dayLabel.setText(item.title != null ? item.title : "");
                h.stepCount.setText("");
            }
        }
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    public void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
            currentAudioHolder = null;
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
        Uri parsed = Uri.parse(localUri);
        // content:// URIs from real trips work directly
        if ("content".equals(parsed.getScheme())) return parsed;
        // file:// URIs from mock trips need FileProvider
        if ("file".equals(parsed.getScheme())) {
            try {
                return androidx.core.content.FileProvider.getUriForFile(
                        v.getContext(),
                        "com.example.voy.fileprovider",
                        new java.io.File(parsed.getPath())
                );
            } catch (Exception e) {
                return null;
            }
        }
        return parsed;
    }

    private void bindImage(android.widget.ImageView iv, String localUri) {
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

    private void bindMapPreview(android.widget.ImageView iv, com.google.android.material.card.MaterialCardView card, Double lat, Double lng){
        if(iv == null || card == null) return;
        if(lat == null || lng == null){
            card.setVisibility(View.GONE);
            iv.setImageDrawable(null);
            return;
        }
        card.setVisibility(View.VISIBLE);
        String url =
                "https://maps.googleapis.com/maps/api/staticmap"
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

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(List<TripItemEntity> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }
    private void bindPlaceChip(TextView chip, Double lat, Double lng, String landmark) {
        if (chip == null) return;
        if(landmark != null && !landmark.trim().isEmpty()){
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

        // Not cached yet: hide chip until we fetch a label
        chip.setVisibility(View.GONE);

        new Thread(() -> {
            String label = null;
            try {
                Geocoder geocoder = new Geocoder(chip.getContext(), Locale.getDefault());
                List<Address> results = geocoder.getFromLocation(lat, lng, 1);

                if (results != null && !results.isEmpty()) {
                    Address a = results.get(0);

                    String sub = a.getSubLocality();
                    String city = a.getLocality();
                    String admin = a.getAdminArea();
                    String country = a.getCountryName();

                    if (sub != null && !sub.isEmpty()) label = sub;
                    else if (city != null && !city.isEmpty()) label = city;
                    else if (admin != null && !admin.isEmpty()) label = admin;
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
    public TripItemEntity getItem(int position) {
        return items.get(position);
    }
}