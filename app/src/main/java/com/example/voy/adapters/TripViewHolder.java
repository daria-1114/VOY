package com.example.voy.adapters;

import android.view.View;
import android.widget.ImageButton;
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
    public TripViewHolder(@NonNull View itemView) {
        super(itemView);
        titleText = itemView.findViewById(R.id.tripTitle);
        dateText = itemView.findViewById(R.id.tripDate);
        deleteBtn = itemView.findViewById(R.id.btn_delete);
    }
        void bind(TripEntity trip, TripAdapter.OnTripActionListener listener) {
            titleText.setText(
                    trip.title != null && !trip.title.isEmpty()
                            ? trip.title
                            : "Untitled Trip"
            );

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            dateText.setText(sdf.format(new Date(trip.startTime)));

            deleteBtn = itemView.findViewById(R.id.btn_delete);
            deleteBtn.setOnClickListener(v -> listener.onDeleteTrip(trip));
        }
    }


