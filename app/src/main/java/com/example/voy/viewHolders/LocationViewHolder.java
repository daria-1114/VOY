package com.example.voy.viewHolders;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.voy.R;

public class LocationViewHolder extends RecyclerView.ViewHolder {
    LinearLayout loc;
    public LocationViewHolder(View itemView) {
        super(itemView);
        this.loc = itemView.findViewById(R.id.locMark);
    }
}