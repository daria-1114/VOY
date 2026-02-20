package com.example.voy.adapters;

import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voy.R;


public class EntryViewHolder extends RecyclerView.ViewHolder {
    protected TextView title;
    protected TextView date;


    public EntryViewHolder(@NonNull View itemView){
        super(itemView);
        this.title = itemView.findViewById(R.id.tripTitle);
        this.date = itemView.findViewById(R.id.tripDate);

    }
}
