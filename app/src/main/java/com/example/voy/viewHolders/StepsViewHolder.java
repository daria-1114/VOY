package com.example.voy.viewHolders;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.voy.R;

public class StepsViewHolder extends RecyclerView.ViewHolder {
    public final TextView dayLabel;
    public final TextView stepCount;

    public StepsViewHolder(View itemView) {
        super(itemView);
        dayLabel  = itemView.findViewById(R.id.txtDayLabel);
        stepCount = itemView.findViewById(R.id.txtStepCount);
    }
}