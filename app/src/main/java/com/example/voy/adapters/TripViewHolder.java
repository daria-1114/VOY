package com.example.voy.adapters;

import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voy.R;
import com.example.voy.data.entities.TripEntity;
import com.example.voy.data.dao.TripDao;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TripViewHolder extends RecyclerView.ViewHolder{
    private final TextView titleText;
    private final TextView dateText;
    private ImageButton deleteBtn;
    private LinearLayout tripItemMain;

    public TripViewHolder(@NonNull View itemView) {
        super(itemView);
        titleText = itemView.findViewById(R.id.tripTitle);
        dateText = itemView.findViewById(R.id.tripDateRange);
        deleteBtn = itemView.findViewById(R.id.btn_delete);
        tripItemMain = itemView.findViewById(R.id.tripItem);

    }
        void bind(TripEntity trip, TripAdapter.OnTripActionListener listener) {
            titleText.setText(
                    trip.title != null && !trip.title.isEmpty()
                            ? trip.title
                            : "Untitled Trip"
            );

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            String startStr = sdf.format(new Date(trip.startTime));
            Long endTimestamp = trip.endTime;

            String endStr;
            if (endTimestamp != null && endTimestamp > 0) {
                endStr = sdf.format(new Date(endTimestamp));
            } else {
                endStr = "Present";
            }

            dateText.setText(startStr + " — " + endStr);
            deleteBtn = itemView.findViewById(R.id.btn_delete);
            deleteBtn.setOnClickListener(v -> listener.onDeleteTrip(trip));
            tripItemMain.setOnClickListener(v-> listener.onTripClicked(trip));
        }
    }


