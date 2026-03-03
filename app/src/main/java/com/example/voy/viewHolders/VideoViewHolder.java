package com.example.voy.viewHolders;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.voy.R;

public class VideoViewHolder extends RecyclerView.ViewHolder {
    public final ImageView thumbView;
    public final ImageView playOverlay;

    public VideoViewHolder(View itemView) {
        super(itemView);


        thumbView = itemView.findViewById(R.id.mediaThumb);

        playOverlay = itemView.findViewById(R.id.playOverlay);

    }
}