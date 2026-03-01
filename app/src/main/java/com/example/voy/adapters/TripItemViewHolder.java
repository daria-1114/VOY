package com.example.voy.adapters;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voy.R;

public class TripItemViewHolder extends RecyclerView.ViewHolder {
    public ImageView imageView;
    public TextView textView;

    public TripItemViewHolder(@NonNull View itemView) {
        super(itemView);
        imageView = itemView.findViewById(R.id.imgMedia);
        textView = itemView.findViewById(R.id.txtUserNote);
    }
}
