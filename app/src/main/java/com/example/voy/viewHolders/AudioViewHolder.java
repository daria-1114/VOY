package com.example.voy.viewHolders;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.voy.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class AudioViewHolder extends RecyclerView.ViewHolder {
    public final MaterialButton playButton;
    public ImageView mapPreview;
    public MaterialCardView mapCard;
    public TextView placeChip;
    public AudioViewHolder(View itemView) {
        super(itemView);
        playButton = itemView.findViewById(R.id.btnPlayAudio);
        mapPreview = itemView.findViewById(R.id.imgMapPreview);
        mapCard = itemView.findViewById(R.id.cardMap);
        placeChip = itemView.findViewById(R.id.txtPlaceChip);

    }
}