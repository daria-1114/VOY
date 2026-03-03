package com.example.voy.viewHolders;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.voy.R;
import com.google.android.material.button.MaterialButton;

public class AudioViewHolder extends RecyclerView.ViewHolder {
    public final MaterialButton playButton;
    public AudioViewHolder(View itemView) {
        super(itemView);
        playButton = itemView.findViewById(R.id.btnPlayAudio);


    }
}