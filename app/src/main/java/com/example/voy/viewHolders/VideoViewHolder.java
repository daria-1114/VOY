package com.example.voy.viewHolders;

import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.voy.R;
import com.google.android.material.card.MaterialCardView;

public class VideoViewHolder extends RecyclerView.ViewHolder {
    public final TextureView videoView;
    public final ImageView playOverlay;
    public ImageView mapPreview;
    public MaterialCardView mapCard;
    public TextView placeChip;
    public android.media.MediaPlayer mediaPlayer;
    public VideoViewHolder(View itemView) {
        super(itemView);
        videoView   = itemView.findViewById(R.id.mediaThumb);
        playOverlay = itemView.findViewById(R.id.playOverlay);
        mapPreview  = itemView.findViewById(R.id.imgMapPreview);
        mapCard     = itemView.findViewById(R.id.cardMap);
        placeChip   = itemView.findViewById(R.id.txtPlaceChip);

    }
}