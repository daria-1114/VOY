package com.example.voy.viewHolders;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.voy.R;
import com.google.android.material.card.MaterialCardView;

public class PhotoViewHolder extends RecyclerView.ViewHolder {
    public final ImageView imageView;
    public ImageView mapPreview;
    public MaterialCardView mapCard;
    public TextView placeChip;
    public View cardNotes;
    public TextView txtNotesPreview;
    public PhotoViewHolder(View itemView) {
        super(itemView);
        imageView = itemView.findViewById(R.id.imgMedia);
        mapPreview = itemView.findViewById(R.id.imgMapPreview);
        mapCard = itemView.findViewById(R.id.cardMap);
        placeChip = itemView.findViewById(R.id.txtPlaceChip);
        cardNotes = itemView.findViewById(R.id.cardNotes);
        txtNotesPreview = itemView.findViewById(R.id.txtNotesPreview);
    }
}