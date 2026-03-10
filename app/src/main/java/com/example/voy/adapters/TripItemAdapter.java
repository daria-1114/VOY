package com.example.voy.adapters;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.voy.BuildConfig;
import com.example.voy.R;
import com.example.voy.data.entities.TripItemEntity;
import com.example.voy.viewHolders.AudioViewHolder;
import com.example.voy.viewHolders.PhotoViewHolder;
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
    private final Map<String, String> placeCache = new HashMap<>();

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
        } else {
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
            bindPlaceChip(h.placeChip, item.lat, item.lng);
        }
        else if (holder instanceof VideoViewHolder) {
            VideoViewHolder h = (VideoViewHolder) holder;
            bindImage(h.thumbView, item.localUri);
            if (h.playOverlay != null) h.playOverlay.setVisibility(View.VISIBLE);
            bindMapPreview(h.mapPreview, h.mapCard, item.lat, item.lng);
            attachOpenMapsClick(h.mapCard, item.lat, item.lng);
            bindPlaceChip(h.placeChip, item.lat, item.lng);
        }
        else if (holder instanceof AudioViewHolder) {
            AudioViewHolder h = (AudioViewHolder) holder;
            bindMapPreview(h.mapPreview, h.mapCard, item.lat, item.lng);
            attachOpenMapsClick(h.mapCard, item.lat, item.lng);
            bindPlaceChip(h.placeChip, item.lat, item.lng);
            // audio has no title by design
            // If you want: handle play click here later
            // h.playButton.setOnClickListener(v -> listener.onAudioPlayClick(item));
        }
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
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
    private void bindPlaceChip(TextView chip, Double lat, Double lng) {
        if (chip == null) return;

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
            Uri gmmIntentUri = Uri.parse("geo:" + lat + "," + lng + "?q=" + lat + "," + lng);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            // Prefer Google Maps if installed, but not required
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(v.getContext().getPackageManager()) == null) {
                mapIntent.setPackage(null);
            }
            v.getContext().startActivity(mapIntent);
        });
    }
    public TripItemEntity getItem(int position) {
        return items.get(position);
    }
}