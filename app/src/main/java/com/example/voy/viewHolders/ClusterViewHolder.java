package com.example.voy.viewHolders;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.voy.R;
import com.google.android.material.card.MaterialCardView;

public class ClusterViewHolder extends RecyclerView.ViewHolder {
    public final ImageView primaryImage;
    public final TextView extraBadge;
    public final TextView placeChip;
    public final LinearLayout expandedContainer;
    public final View collapsedContainer;
    public final MaterialCardView mapCard;
    public final ImageView mapPreview;

    public ClusterViewHolder(View itemView) {
        super(itemView);
        primaryImage      = itemView.findViewById(R.id.imgClusterPrimary);
        extraBadge        = itemView.findViewById(R.id.txtClusterBadge);
        placeChip         = itemView.findViewById(R.id.txtPlaceChip);
        expandedContainer = itemView.findViewById(R.id.clusterExpandedContainer);
        collapsedContainer = itemView.findViewById(R.id.collapsedContainer);
        mapCard           = itemView.findViewById(R.id.cardMap);
        mapPreview        = itemView.findViewById(R.id.imgMapPreview);
    }
}