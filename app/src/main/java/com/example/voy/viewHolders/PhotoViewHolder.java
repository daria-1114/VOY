package com.example.voy.viewHolders;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.voy.R;

public class PhotoViewHolder extends RecyclerView.ViewHolder {
    public final ImageView imageView;
    public PhotoViewHolder(View itemView) {
        super(itemView);
        imageView = itemView.findViewById(R.id.imgMedia);
    }
}