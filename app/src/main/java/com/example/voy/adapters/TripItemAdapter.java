package com.example.voy.adapters;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.voy.R;
import com.example.voy.data.entities.TripItemEntity;
import com.example.voy.viewHolders.AudioViewHolder;
import com.example.voy.viewHolders.LocationViewHolder;
import com.example.voy.viewHolders.PhotoViewHolder;
import com.example.voy.viewHolders.VideoViewHolder;

import java.util.ArrayList;
import java.util.List;

public class TripItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<TripItemEntity> items = new ArrayList<>();

    private static final int TYPE_PHOTO = 1;
    private static final int TYPE_VIDEO = 2;
    private static final int TYPE_AUDIO = 3;
    private static final int TYPE_LOCATION = 4;

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
            case LOCATION:
                return TYPE_LOCATION;
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
        } else if (viewType == TYPE_AUDIO) {
            View v = inflater.inflate(R.layout.trip_item_audio, parent, false);
            return new AudioViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.trip_item_location, parent, false);
            return new LocationViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TripItemEntity item = items.get(position);

        if (holder instanceof PhotoViewHolder) {
            PhotoViewHolder h = (PhotoViewHolder) holder;
            bindImage(h.imageView, item.localUri);
        }
        else if (holder instanceof VideoViewHolder) {
            VideoViewHolder h = (VideoViewHolder) holder;
            bindImage(h.thumbView, item.localUri);

            // your XML already has playOverlay; keep it visible
            if (h.playOverlay != null) h.playOverlay.setVisibility(View.VISIBLE);
        }
        else if (holder instanceof AudioViewHolder) {
            AudioViewHolder h = (AudioViewHolder) holder;

            // audio has no title by design
            // If you want: handle play click here later
            // h.playButton.setOnClickListener(v -> listener.onAudioPlayClick(item));
        }
        else if (holder instanceof LocationViewHolder) {
            LocationViewHolder h = (LocationViewHolder) holder;

            // location currently has only a container (locMark)
            // If you later add a textview inside locMark, bind it then.
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

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(List<TripItemEntity> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    public TripItemEntity getItem(int position) {
        return items.get(position);
    }
}