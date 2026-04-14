package com.example.voy.viewHolders;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voy.R;

public class DayViewHolder extends RecyclerView.ViewHolder {
    public final TextView dayHeader;
    public DayViewHolder(@NonNull View itemView) {
        super(itemView);
        dayHeader = itemView.findViewById(R.id.txtDayHeader);
    }
}
