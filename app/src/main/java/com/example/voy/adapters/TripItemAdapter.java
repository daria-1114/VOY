package com.example.voy.adapters;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voy.R;
import com.example.voy.data.entities.TripEntity;
import com.example.voy.data.entities.TripItemEntity;

import java.util.ArrayList;
import java.util.List;

public class TripItemAdapter extends RecyclerView.Adapter<TripItemViewHolder> {
    private List<TripItemEntity> items = new ArrayList<>();

    public TripItemAdapter(OnPhotoClickedListener listener) {
        this.listener = listener;
    }

    public interface OnPhotoClickedListener{
        void onPhotoClick(TripItemEntity item);
    }
    private final OnPhotoClickedListener listener;
    @NonNull
    @Override
    public TripItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.trip_item_travel, parent, false);
        return new TripItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripItemViewHolder holder, int position) {
        TripItemEntity item = items.get(position);
        if (item.localUri != null) {
            try {
                holder.imageView.setImageURI(Uri.parse(item.localUri));
            } catch (Exception e) {
                holder.imageView.setImageDrawable(null);
            }
        } else {
            holder.imageView.setImageDrawable(null);
        }
        if (item.title != null && !item.title.trim().isEmpty()) {
            holder.textView.setVisibility(View.VISIBLE);
            holder.textView.setText(item.title);
        } else {
            holder.textView.setVisibility(View.GONE);
        }
        holder.imageView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPhotoClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }
    public void setItems(List<TripItemEntity> newItems) {
        this.items = (newItems != null) ? newItems : new ArrayList<>();
        notifyDataSetChanged(); // simple but works
    }
}
